package com.wzy020.forcestop

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

class AppManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps = _apps.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _autoRefresh = MutableStateFlow(false)
    val autoRefresh = _autoRefresh.asStateFlow()

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages = _selectedPackages.asStateFlow()

    init {
        loadAutoRefresh()
        // 如果没有开启自动刷新，则在启动时主动加载一次，避免与 onResume 的自动刷新重复
        if (!_autoRefresh.value) {
            loadRunningApps()
        }
    }

    fun onResume() {
    // 仅在开启自动刷新时重新加载运行中的应用
        if (_autoRefresh.value) {
            loadRunningApps()
        }
    }

    private fun loadAutoRefresh() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("app_selection", android.content.Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("auto_refresh", false)
        _autoRefresh.value = enabled
    }

    fun setAutoRefresh(enabled: Boolean) {
        _autoRefresh.value = enabled
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("app_selection", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_refresh", enabled).apply()
    }

    // 获取选中的包名列表
    private fun getSelectedPackages(): Set<String> {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("app_selection", android.content.Context.MODE_PRIVATE)
        return prefs.getStringSet("selected_packages", emptySet()) ?: emptySet()
    }

    // 加载正在运行的应用
    fun loadRunningApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            try {
                val context = getApplication<Application>()
                val pm = context.packageManager
                val selectedPackages = getSelectedPackages()
                
                // 如果没有选中任何应用，返回空列表
                if (selectedPackages.isEmpty()) {
                    _apps.value = emptyList()
                    return@launch
                }
                
                val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

                val runningApps = installedApps.asSequence()
                    // 只查询选中的包名
                    .filter { selectedPackages.contains(it.packageName) }
                    // 不是已停止的应用 (FLAG_STOPPED 表示已停止)
                    .filter { (it.flags and ApplicationInfo.FLAG_STOPPED) == 0 }
                    .map { appInfo ->
                        AppInfo(
                            name = pm.getApplicationLabel(appInfo).toString(),
                            packageName = appInfo.packageName,
                            icon = pm.getApplicationIcon(appInfo)
                        )
                    }
                    .sortedBy { it.name.lowercase() }
                    .toList()

                _apps.value = runningApps
            } finally {
                _loading.value = false
            }
        }
    }

    // 跳转到应用详情页
    fun openAppSettings(packageName: String) {
        this.jumpToAppSettings(packageName)
    }

    // 切换某个应用在选中集合中的状态
    fun toggleSelected(packageName: String) {
        val set = _selectedPackages.value.toMutableSet()
        if (set.contains(packageName)) set.remove(packageName) else set.add(packageName)
        _selectedPackages.value = set
    }

    // 清空选中集合
    fun clearSelected() {
        _selectedPackages.value = emptySet()
    }

    // 批量 force-stop 选中的应用，完成后清空选中并刷新列表（单次 su 调用串联命令）
    fun onPullDown() {
        val tobeKilledPackages = _selectedPackages.value.toList()
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            try {
                if (tobeKilledPackages.isNotEmpty()) {
                    val command = tobeKilledPackages.joinToString("; ") { "am force-stop $it" }
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                    process.waitFor()
                    process.destroy()
                }
                withContext(Dispatchers.Main) { clearSelected() }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // 无论是否选中，都重新加载运行中的应用列表
                loadRunningApps()
            }
        }
    }

    // 点击标题：通过 root 的 am start 直达“运行的服务”页（SubSettings 未导出，需 root 绕过）
    // ColorOS 的运行服务 fragment 为 OplusRunningServices；
    // AOSP 原生系统对应为 com.android.settings.applications.ProcessStatsSummary
    fun openRunningServicesPage() {
        val context = getApplication<Application>()
        val command = "am start -n com.android.settings/.SubSettings " +
                "--es ':settings:show_fragment' " +
                "'com.oplus.settings.feature.othersettings.development.OplusRunningServices'"
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor()
            process.destroy()
        } catch (e: Exception) {
            // root 方式失败，回退到公开的开发者选项总页
            try {
                val devIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(devIntent)
            } catch (e2: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    // 跳转到设置页面
    fun openSettings() {
        val context = getApplication<Application>()
        val intent = Intent(context, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

}
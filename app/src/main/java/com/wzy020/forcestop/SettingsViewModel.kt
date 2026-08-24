package com.wzy020.forcestop

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


data class AppInfoWithTime(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable,
    val lastUpdateTime: Long
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _allApps = MutableStateFlow<List<AppInfoWithTime>>(emptyList())
    val allApps = _allApps.asStateFlow()

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages = _selectedPackages.asStateFlow()

    init {
        loadAllApps()
        loadSelectedPackages()
    }

    private fun loadAllApps() {
        val context = getApplication<Application>()
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val appsWithTime = installedApps.map { appInfo ->
            AppInfoWithTime(
                name = pm.getApplicationLabel(appInfo).toString(),
                packageName = appInfo.packageName,
                icon = pm.getApplicationIcon(appInfo),
                lastUpdateTime = pm.getPackageInfo(appInfo.packageName, 0).lastUpdateTime
            )
        }.sortedByDescending { it.lastUpdateTime }

        _allApps.value = appsWithTime
    }

    private fun loadSelectedPackages() {
        // 从SharedPreferences加载已选中的包名
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("app_selection", android.content.Context.MODE_PRIVATE)
        val selected = prefs.getStringSet("selected_packages", emptySet()) ?: emptySet()
        _selectedPackages.value = selected
    }

    fun togglePackageSelection(packageName: String) {
        val newSelection = if (_selectedPackages.value.contains(packageName)) {
            _selectedPackages.value - packageName
        } else {
            _selectedPackages.value + packageName
        }
        _selectedPackages.value = newSelection

        // 保存到SharedPreferences
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("app_selection", android.content.Context.MODE_PRIVATE)
        prefs.edit().putStringSet("selected_packages", newSelection).apply()
    }

    fun openAppSettings(packageName: String) {
        this.jumpToAppSettings(packageName)
    }

    // 清理已卸载应用的本地记录，仅进入设置页时调用
    fun cleanupUninstalledPackages() {
        val context = getApplication<Application>()
        val pm = context.packageManager
        val remaining = _selectedPackages.value.filter { pkg ->
            try {
                pm.getApplicationInfo(pkg, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }.toSet()

        if (remaining != _selectedPackages.value) {
            _selectedPackages.value = remaining
            val prefs = context.getSharedPreferences("app_selection", android.content.Context.MODE_PRIVATE)
            prefs.edit().putStringSet("selected_packages", remaining).apply()
        }
    }

}
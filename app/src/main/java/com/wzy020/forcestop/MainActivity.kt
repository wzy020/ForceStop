package com.wzy020.forcestop

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.graphics.createBitmap

class MainActivity : ComponentActivity() {

    private val viewModel: AppManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppManagerScreen(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerScreen(viewModel: AppManagerViewModel) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val isLoading by viewModel.loading.collectAsStateWithLifecycle()
    val autoRefresh by viewModel.autoRefresh.collectAsStateWithLifecycle()
    val selected by viewModel.selectedPackages.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ForceStop",
                        modifier = Modifier.clickable { viewModel.openRunningServicesPage() },
                        color = Color.White
                    )
                },
                actions = {
                    Checkbox(
                        checked = autoRefresh,
                        onCheckedChange = { checked ->
                            viewModel.setAutoRefresh(checked)
                            if (checked) {
                                viewModel.loadRunningApps()
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.White,
                            uncheckedColor = Color.White,
                            checkmarkColor = Color.Black
                        )
                    )
                    IconButton(onClick = { viewModel.openSettings() }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.onPullDown() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Empty",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB0B0B0)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        Divider()
                        AppItem(
                            appInfo = app,
                            onClick = {
                                viewModel.openAppSettings(app.packageName)
                            },
                            selected = selected.contains(app.packageName),
                            onToggleSelect = {
                                viewModel.toggleSelected(app.packageName)
                            }
                        )
                        Divider()
                    }
                }
            }

        }
    }
}

@Composable
fun AppItem(
    appInfo: AppInfo,
    onClick: () -> Unit,
    selected: Boolean,
    onToggleSelect: () -> Unit
) {
    val itemHeight = 72.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .clickable(onClick = onClick)
            .background(Color(0xFF1E1E1E)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberDrawablePainter(appInfo.icon),
            contentDescription = "${appInfo.name} icon",
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = appInfo.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            Text(
                text = appInfo.packageName,
                fontSize = 12.sp,
                color = Color(0xFFB0B0B0)
            )
        }
        Box(
            modifier = Modifier
                .width(itemHeight)
                .fillMaxHeight()
                .background(Color(0xFF5C1A1A))
                .clickable(onClick = onToggleSelect),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

// 将 Android Drawable（如应用图标）转为 Compose Painter
@Composable
fun rememberDrawablePainter(drawable: Drawable): Painter {
    return remember(drawable) {
        val bitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            val w = drawable.intrinsicWidth.coerceAtLeast(1)
            val h = drawable.intrinsicHeight.coerceAtLeast(1)
            val bmp = createBitmap(w, h)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            bmp
        }
        BitmapPainter(bitmap.asImageBitmap())
    }
}
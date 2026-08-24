package com.wzy020.forcestop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color

class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 进入设置页时清理已卸载应用的本地记录
        viewModel.cleanupUninstalledPackages()

        setContent {
            SettingsScreen(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val allApps by viewModel.allApps.collectAsState(initial = emptyList())
    val selectedPackages by viewModel.selectedPackages.collectAsState(initial = emptySet())
    val context = LocalContext.current
    
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (context is android.app.Activity) {
                            context.finish()
                        }
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(
                items = allApps,
                key = { it.packageName }
            ) { app ->
                AppSelectionItem(
                    appInfo = app,
                    isSelected = selectedPackages.contains(app.packageName),
                    onSelectionChange = { viewModel.togglePackageSelection(app.packageName) },
                    onIconClick = { viewModel.openAppSettings(app.packageName) }
                )
                Divider()
            }

        }
    }
}

@Composable
fun AppSelectionItem(
    appInfo: AppInfoWithTime,
    isSelected: Boolean,
    onSelectionChange: () -> Unit,
    onIconClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(Color(0xFF1E1E1E))
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onSelectionChange() }
        )
        Spacer(modifier = Modifier.width(16.dp))
        AsyncImage(
            model = appInfo.icon,
            contentDescription = "${appInfo.name} icon",
            modifier = Modifier
                .size(48.dp)
                .clickable { onIconClick() }
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
    }
}

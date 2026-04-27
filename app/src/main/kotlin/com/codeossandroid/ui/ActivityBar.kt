package com.codeossandroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeossandroid.viewmodel.TerminalViewModel

@Composable
fun ActivityBar(viewModel: TerminalViewModel) {
    var showSettings by remember { mutableStateOf(false) }
    val fontSize by viewModel.fontSize.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    val isPanelVisible by viewModel.isPanelVisible.collectAsState()
    val sidebarOpen by viewModel.sidebarOpen.collectAsState()
    val sidebarMode by viewModel.sidebarMode.collectAsState()
    val barWidth = (48 * uiScale).dp
    val iconSize = (24 * uiScale).dp

    Column(modifier = Modifier.fillMaxHeight().width(barWidth).background(Color(0xFF161B22)), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(12.dp))
        
        // Projects Button
        IconButton(onClick = { viewModel.setSidebarMode(TerminalViewModel.SidebarMode.PROJECTS) }, modifier = Modifier.size(barWidth)) {
            Icon(Icons.Default.Menu, "Projects", tint = if (sidebarOpen && sidebarMode == TerminalViewModel.SidebarMode.PROJECTS) Color.White else Color.Gray, modifier = Modifier.size(iconSize))
        }
        
        // Explorer Button (Files)
        IconButton(onClick = { viewModel.setSidebarMode(TerminalViewModel.SidebarMode.EXPLORER) }, modifier = Modifier.size(barWidth)) {
            Icon(Icons.Default.Folder, "Explorer", tint = if (sidebarOpen && sidebarMode == TerminalViewModel.SidebarMode.EXPLORER) Color.White else Color.Gray, modifier = Modifier.size(iconSize))
        }

        // Git Button
        IconButton(onClick = { viewModel.setSidebarMode(TerminalViewModel.SidebarMode.GIT) }, modifier = Modifier.size(barWidth)) {
            Icon(Icons.Default.AccountTree, "Git", tint = if (sidebarOpen && sidebarMode == TerminalViewModel.SidebarMode.GIT) Color.White else Color.Gray, modifier = Modifier.size(iconSize))
        }

        // Search Button
        IconButton(onClick = { viewModel.setSidebarMode(TerminalViewModel.SidebarMode.SEARCH) }, modifier = Modifier.size(barWidth)) {
            Icon(Icons.Default.Search, "Search", tint = if (sidebarOpen && sidebarMode == TerminalViewModel.SidebarMode.SEARCH) Color.White else Color.Gray, modifier = Modifier.size(iconSize))
        }

        // Extensions Button
        IconButton(onClick = { viewModel.setSidebarMode(TerminalViewModel.SidebarMode.EXTENSIONS) }, modifier = Modifier.size(barWidth)) {
            Icon(Icons.Default.Extension, "Extensions", tint = if (sidebarOpen && sidebarMode == TerminalViewModel.SidebarMode.EXTENSIONS) Color.White else Color.Gray, modifier = Modifier.size(iconSize))
        }

        // Marketplace Button
        IconButton(onClick = { viewModel.setSidebarMode(TerminalViewModel.SidebarMode.MARKETPLACE) }, modifier = Modifier.size(barWidth)) {
            Icon(Icons.Default.Storefront, "Marketplace", tint = if (sidebarOpen && sidebarMode == TerminalViewModel.SidebarMode.MARKETPLACE) Color.White else Color.Gray, modifier = Modifier.size(iconSize))
        }

        // Terminal Panel Button
        IconButton(onClick = { viewModel.togglePanel(!isPanelVisible) }, modifier = Modifier.size(barWidth)) {
            Icon(Icons.Default.Terminal, "Terminal", tint = if (isPanelVisible) Color.White else Color.Gray, modifier = Modifier.size(iconSize))
        }

        Spacer(Modifier.weight(1f))
        
        Box {
            IconButton(onClick = { showSettings = true }, modifier = Modifier.size(barWidth)) { 
                Icon(Icons.Default.Settings, null, tint = Color.Gray, modifier = Modifier.size(iconSize)) 
            }
            DropdownMenu(expanded = showSettings, onDismissRequest = { showSettings = false }, modifier = Modifier.background(Color(0xFF161B22)).width(220.dp)) {
                Text("Settings", modifier = Modifier.padding(12.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (14 * uiScale).sp)
                HorizontalDivider(color = Color.Gray)
                SettingRow("Font Size", fontSize.toString(), uiScale, onMinus = { viewModel.updateFontSize(-1) }, onPlus = { viewModel.updateFontSize(1) })
                val scalePercent = (uiScale * 100).toInt()
                SettingRow("UI Size", "$scalePercent%", uiScale, onMinus = { viewModel.updateUIScale(-0.05f) }, onPlus = { viewModel.updateUIScale(0.05f) })
            }
        }
    }
}

@Composable
fun SettingRow(label: String, value: String, uiScale: Float, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$label: $value", color = Color.White, modifier = Modifier.weight(1f), fontSize = (12 * uiScale).sp)
        IconButton(onClick = onMinus, modifier = Modifier.size((32 * uiScale).dp)) { Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size((16 * uiScale).dp)) }
        IconButton(onClick = onPlus, modifier = Modifier.size((32 * uiScale).dp)) { Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size((16 * uiScale).dp)) }
    }
}

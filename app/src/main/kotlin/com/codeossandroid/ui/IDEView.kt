package com.codeossandroid.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeossandroid.viewmodel.TerminalViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun IDEView(viewModel: TerminalViewModel) {
    val isPanelVisible by viewModel.isPanelVisible.collectAsState()
    val panelHeight by viewModel.panelHeight.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    val sidebarOpen by viewModel.sidebarOpen.collectAsState()
    val sidebarMode by viewModel.sidebarMode.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            ActivityBar(viewModel)
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val maxHeight = maxHeight
                val constrainedHeight = panelHeight.coerceAtMost(maxHeight * 0.8f)
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height((32 * uiScale).dp)
                            .background(Color(0xFF161B22)).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeProject?.let { "📁 $it" } ?: "CodeOSS Android",
                            color = Color(0xFF8B949E), fontSize = (11 * uiScale).sp,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                        )
                    }
                    val activeExtensionDetail by viewModel.activeExtensionDetail.collectAsState()
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0D1117))) {
                        if (activeExtensionDetail != null) {
                            ExtensionDetailView(activeExtensionDetail!!, viewModel)
                        } else if (sidebarMode == TerminalViewModel.SidebarMode.BROWSER) {
                            BrowserView(viewModel)
                        } else {
                            val activeTabIndices by viewModel.activeTabIndices.collectAsState()
                            Row(modifier = Modifier.fillMaxSize()) {
                                activeTabIndices.keys.sorted().forEach { viewportId ->
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, Color(0xFF30363D))) {
                                        CodeEditor(viewModel, viewportId)
                                    }
                                }
                            }
                        }
                    }
                    if (isPanelVisible) {
                        val density = LocalDensity.current
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp) // Larger hit target
                            .background(Color.Transparent)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    val deltaDp = with(density) { delta.toDp() }
                                    viewModel.updatePanelHeight(-deltaDp)
                                }
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF30363D)))
                        }
                        BottomPanel(viewModel, constrainedHeight)
                    }
                }
            }
        }
        if (sidebarOpen) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0x88000000)).clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null
            ) { viewModel.toggleSidebar() })
        }
        AnimatedVisibility(
            visible = sidebarOpen,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it }
        ) {
            ProjectSidebar(viewModel)
        }

        val previewFile by viewModel.previewFile.collectAsState()
        if (previewFile != null) {
            FilePreview(previewFile!!, onDismiss = { viewModel.closePreview() })
        }

        val availableUpdate by viewModel.availableUpdate.collectAsState()
        if (availableUpdate != null) {
            UpdateDialog(availableUpdate!!, onDismiss = { viewModel.dismissUpdate() })
        }
    }
}

@Composable
fun UpdateDialog(update: TerminalViewModel.UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Available! 🚀", color = Color.White) },
        text = {
            Column {
                Text("Version ${update.version} is now available on GitHub.", color = Color.Gray, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text("Release Notes:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                    Text(update.releaseNotes, color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(update.downloadUrl))
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636))
            ) {
                Text("Update Now", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF161B22),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    )
}

@Composable
fun ProjectSidebar(viewModel: TerminalViewModel) {
    val sidebarMode by viewModel.sidebarMode.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width((280 * uiScale).dp)
            .background(Color(0xFF161B22))
    ) {
        when (sidebarMode) {
            TerminalViewModel.SidebarMode.PROJECTS -> ProjectListContent(viewModel)
            TerminalViewModel.SidebarMode.EXPLORER -> FileExplorerContent(viewModel)
            TerminalViewModel.SidebarMode.GIT -> SourceControlContent(viewModel)
            TerminalViewModel.SidebarMode.SEARCH -> SearchContent(viewModel)
            TerminalViewModel.SidebarMode.EXTENSIONS -> ExtensionsContent(viewModel)
            TerminalViewModel.SidebarMode.MARKETPLACE -> Text("Marketplace Coming Soon", color = Color.Gray, modifier = Modifier.padding(16.dp))
            TerminalViewModel.SidebarMode.DEBUG -> DebugContent(viewModel)
            TerminalViewModel.SidebarMode.BROWSER -> Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Web Preview Tools\n\nDebugger Bridge Active", color = Color.Gray, fontSize = (11 * uiScale).sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun DebugContent(viewModel: TerminalViewModel) {
    val uiScale by viewModel.uiScale.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("RUN AND DEBUG", color = Color.White.copy(alpha = 0.7f), fontSize = (12 * uiScale).sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF238636), modifier = Modifier.size((18 * uiScale).dp))
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            DebugSection("VARIABLES", uiScale) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("No variables available", color = Color.Gray, fontSize = (11 * uiScale).sp)
                }
            }
            DebugSection("WATCH", uiScale) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("No expressions watched", color = Color.Gray, fontSize = (11 * uiScale).sp)
                }
            }
            DebugSection("CALL STACK", uiScale) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Not paused on debugger", color = Color.Gray, fontSize = (11 * uiScale).sp)
                }
            }
            DebugSection("BREAKPOINTS", uiScale) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("No breakpoints set", color = Color.Gray, fontSize = (11 * uiScale).sp)
                }
            }
        }
    }
}

@Composable
fun DebugSection(title: String, uiScale: Float, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.background(Color(0xFF21262D).copy(alpha = 0.5f)).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight, null, tint = Color.Gray, modifier = Modifier.size((16 * uiScale).dp))
            Spacer(Modifier.width(4.dp))
            Text(title, color = Color.White, fontSize = (11 * uiScale).sp, fontWeight = FontWeight.Bold)
        }
        if (expanded) content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsContent(viewModel: TerminalViewModel) {
    val uiScale by viewModel.uiScale.collectAsState()
    val marketplaceExtensions by viewModel.marketplaceExtensions.collectAsState()
    val isSearching by viewModel.isSearchingExtensions.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            viewModel.searchExtensions(searchQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Premium Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "EXTENSIONS",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = (12 * uiScale).sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Default.MoreVert,
                null,
                tint = Color.Gray,
                modifier = Modifier.size((18 * uiScale).dp)
            )
        }

        // Rebuilt Search Bar for Perfect Text Rendering
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { 
                Text(
                    "Search Extensions in Marketplace", 
                    fontSize = (13 * uiScale).sp, 
                    color = Color.Gray
                ) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(min = (52 * uiScale).dp), // Use heightIn to prevent clipping
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White, 
                fontSize = (15 * uiScale).sp, // Slightly larger for better readability
                fontFamily = FontFamily.SansSerif
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF58A6FF),
                unfocusedBorderColor = Color(0xFF30363D),
                focusedContainerColor = Color(0xFF161B22),
                unfocusedContainerColor = Color(0xFF161B22),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF58A6FF)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size((20 * uiScale).dp)) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (searchQuery.isBlank()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Type to search for extensions...",
                                color = Color.Gray,
                                fontSize = (13 * uiScale).sp
                            )
                        }
                    }
                } else {
                    if (marketplaceExtensions.isEmpty() && !isSearching) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "No extensions found for \"$searchQuery\"",
                                    color = Color.Gray,
                                    fontSize = (13 * uiScale).sp
                                )
                            }
                        }
                    } else {
                        item {
                            ExtensionSection("MARKETPLACE", uiScale) { /* Header only */ }
                        }
                        items(marketplaceExtensions) { ext ->
                            ExtensionItem(
                                name = ext.displayName,
                                author = ext.publisher,
                                desc = ext.description,
                                version = ext.version,
                                iconUrl = ext.iconUrl,
                                downloads = ext.downloadCount,
                                uiScale = uiScale,
                                onDetailClick = { viewModel.setActiveExtensionDetail(ext) }
                            )
                        }
                    }
                }
            }

            // Search Progress Indicator
            if (isSearching) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = Color(0xFF58A6FF),
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@Composable
fun ExtensionSection(title: String, uiScale: Float, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    
    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .background(Color(0xFF21262D).copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                null,
                tint = Color.Gray,
                modifier = Modifier.size((16 * uiScale).dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                title, 
                color = Color.White, 
                fontSize = (11 * uiScale).sp, 
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        if (expanded) {
            content()
        }
    }
}

@Composable
fun ExtensionItem(
    name: String, 
    author: String, 
    desc: String, 
    version: String, 
    iconUrl: String?, 
    downloads: Int,
    uiScale: Float,
    onDetailClick: () -> Unit = {}
) {
    var isHovered by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon Container with Shadow/Border
        Box(
            modifier = Modifier
                .size((48 * uiScale).dp)
                .background(Color(0xFF161B22), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF30363D), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (iconUrl != null) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Default.Extension, null, tint = Color(0xFF58A6FF), modifier = Modifier.size((28 * uiScale).dp))
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        name, 
                        color = Color(0xFF58A6FF), 
                        fontSize = (14 * uiScale).sp, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(author, color = Color.Gray, fontSize = (11 * uiScale).sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.CloudDownload, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size((12 * uiScale).dp))
                        Spacer(Modifier.width(2.dp))
                        Text(formatDownloads(downloads), color = Color.Gray.copy(alpha = 0.5f), fontSize = (10 * uiScale).sp)
                    }
                }
                
                // Premium Install Button
                Button(
                    onClick = { /* Install */ },
                    modifier = Modifier.height((28 * uiScale).dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF238636),
                        contentColor = Color.White
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                ) {
                    Text("Install", fontSize = (11 * uiScale).sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(4.dp))
            
            Text(
                desc, 
                color = Color.Gray, 
                fontSize = (12 * uiScale).sp, 
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                lineHeight = (16 * uiScale).sp
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFF30363D).copy(alpha = 0.5f))
}

@Composable
fun ExtensionDetailView(extension: TerminalViewModel.MarketplaceExtension, viewModel: TerminalViewModel) {
    val uiScale by viewModel.uiScale.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117)).verticalScroll(rememberScrollState())) {
        // Header with Close Button
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF161B22)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Extension Details", color = Color.Gray, fontSize = (12 * uiScale).sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.setActiveExtensionDetail(null) }, modifier = Modifier.size((24 * uiScale).dp)) {
                Icon(Icons.Default.Close, null, tint = Color.Gray)
            }
        }
        
        Row(modifier = Modifier.padding(24.dp)) {
            // Icon
            Box(
                modifier = Modifier
                    .size((128 * uiScale).dp)
                    .background(Color(0xFF161B22), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF30363D), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (extension.iconUrl != null) {
                    AsyncImage(model = extension.iconUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Extension, null, tint = Color(0xFF58A6FF), modifier = Modifier.size((64 * uiScale).dp))
                }
            }
            
            Spacer(Modifier.width(24.dp))
            
            Column {
                Text(extension.displayName, color = Color.White, fontSize = (28 * uiScale).sp, fontWeight = FontWeight.Bold)
                Text(extension.publisher, color = Color(0xFF58A6FF), fontSize = (16 * uiScale).sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { /* Install */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    ) {
                        Text("Install", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(extension.version, color = Color.Gray, fontSize = (14 * uiScale).sp)
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = Color(0xFF30363D))
        
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Description", color = Color.White, fontSize = (18 * uiScale).sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                extension.description,
                color = Color.Gray,
                fontSize = (14 * uiScale).sp,
                lineHeight = (20 * uiScale).sp
            )
            
            Spacer(Modifier.height(32.dp))
            Text("Information", color = Color.White, fontSize = (18 * uiScale).sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            InfoRow("Publisher", extension.publisher, uiScale)
            InfoRow("Downloads", formatDownloads(extension.downloadCount), uiScale)
            InfoRow("Identifier", "${extension.namespace}.${extension.name}", uiScale)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, uiScale: Float) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = Color.Gray, modifier = Modifier.width(100.dp), fontSize = (13 * uiScale).sp)
        Text(value, color = Color.White, fontSize = (13 * uiScale).sp)
    }
}

fun formatDownloads(count: Int): String {
    return when {
        count >= 1_000_000 -> "${String.format("%.1f", count / 1_000_000f)}M"
        count >= 1_000 -> "${count / 1_000}K"
        else -> count.toString()
    }
}

@Composable
fun ProjectListContent(viewModel: TerminalViewModel) {
    val projects by viewModel.projects.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<String?>(null) }
    var pendingExportProject by remember { mutableStateOf<String?>(null) }

    val importAction = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        uri?.let { viewModel.importProjectZip(it) }
    }
    val exportAction = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: android.net.Uri? ->
        uri?.let { pendingExportProject?.let { name -> viewModel.exportProjectZip(name, it) } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height((44 * uiScale).dp)
                .background(Color(0xFF0D1117)).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PROJECTS", color = Color(0xFF58A6FF), fontSize = (11 * uiScale).sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f))
            IconButton(onClick = { showNewProjectDialog = true }, modifier = Modifier.size((32 * uiScale).dp)) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF58A6FF), modifier = Modifier.size((18 * uiScale).dp))
            }
            var showCloneDialog by remember { mutableStateOf(false) }
            IconButton(onClick = { showCloneDialog = true }, modifier = Modifier.size((32 * uiScale).dp)) {
                Icon(Icons.Default.CloudDownload, "Clone Git", tint = Color(0xFF58A6FF), modifier = Modifier.size((18 * uiScale).dp))
            }
            if (showCloneDialog) {
                CloneProjectDialog(
                    onConfirm = { url, name, user, token -> 
                        viewModel.cloneProject(url, name, user, token)
                        showCloneDialog = false 
                    },
                    onDismiss = { showCloneDialog = false }
                )
            }
            IconButton(onClick = { importAction.launch("application/zip") }, modifier = Modifier.size((32 * uiScale).dp)) {
                Icon(Icons.Default.FileUpload, "Import ZIP", tint = Color(0xFF58A6FF), modifier = Modifier.size((18 * uiScale).dp))
            }
            IconButton(onClick = { viewModel.toggleSidebar() }, modifier = Modifier.size((32 * uiScale).dp)) {
                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size((16 * uiScale).dp))
            }
        }
        HorizontalDivider(color = Color(0xFF30363D))

        if (projects.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("No projects yet.\nTap ＋ to create one.", color = Color.Gray,
                    fontSize = (11 * uiScale).sp, fontFamily = FontFamily.Monospace)
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            projects.forEach { name ->
                val isActive = name == activeProject
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (isActive) Color(0xFF21262D) else Color.Transparent)
                        .clickable { viewModel.openProject(name) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isActive) Box(Modifier.width(3.dp).height((16 * uiScale).dp).background(Color(0xFF58A6FF)))
                    Spacer(Modifier.width(if (isActive) 6.dp else 9.dp))
                    Text("📁 $name", color = if (isActive) Color.White else Color(0xFF8B949E),
                        fontSize = (12 * uiScale).sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { 
                        pendingExportProject = name
                        exportAction.launch("$name.zip") 
                    }, modifier = Modifier.size((24 * uiScale).dp)) {
                        Icon(Icons.Default.FileDownload, "Export ZIP", tint = Color(0xFF444C56), modifier = Modifier.size((14 * uiScale).dp))
                    }
                    IconButton(onClick = { projectToDelete = name }, modifier = Modifier.size((24 * uiScale).dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFF444C56), modifier = Modifier.size((14 * uiScale).dp))
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF30363D))
        Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.refreshProjects() }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Refresh, null, tint = Color.Gray, modifier = Modifier.size((14 * uiScale).dp))
            Spacer(Modifier.width(6.dp))
            Text("Refresh List", color = Color.Gray, fontSize = (11 * uiScale).sp)
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(onConfirm = { name -> viewModel.createProject(name); showNewProjectDialog = false },
            onDismiss = { showNewProjectDialog = false })
    }

    projectToDelete?.let { name ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project", color = Color.White) },
            text = { Text("Delete \"$name\" and all its files? This cannot be undone.", color = Color.Gray) },
            confirmButton = { TextButton(onClick = { viewModel.deleteProject(name); projectToDelete = null }) { Text("Delete", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { projectToDelete = null }) { Text("Cancel", color = Color(0xFF58A6FF)) } },
            containerColor = Color(0xFF161B22)
        )
    }
}

@Composable
fun NewProjectDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161B22),
        title = { Text("New Project", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter a project name:", color = Color.Gray, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = name, onValueChange = { name = it },
                    textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF0D1117)).padding(10.dp),
                    decorationBox = { inner ->
                        if (name.isEmpty()) Text("my-project", color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                        inner()
                    }
                )
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("Create", color = Color(0xFF58A6FF)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }
    )
}

@Composable
fun BottomPanel(viewModel: TerminalViewModel, height: Dp) {
    var selectedTab by remember { mutableStateOf("TERMINAL") }
    val tabs = listOf("PROBLEMS", "OUTPUT", "DEBUG CONSOLE", "TERMINAL", "PORTS")
    val uiScale by viewModel.uiScale.collectAsState()

    Surface(modifier = Modifier.fillMaxWidth().height(height), color = Color.Black) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().height((35 * uiScale).dp).background(Color(0xFF161B22)), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(start = 8.dp)) {
                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(modifier = Modifier.width((85 * uiScale).dp).fillMaxHeight().clickable { selectedTab = tab }, contentAlignment = Alignment.Center) {
                            Text(text = tab, color = if (isSelected) Color.White else Color.Gray, fontSize = (10 * uiScale).sp, fontWeight = FontWeight.Normal)
                            if (isSelected) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(Color(0xFF58A6FF)))
                        }
                    }
                }
                IconButton(onClick = { viewModel.togglePanel(false) }, modifier = Modifier.size((35 * uiScale).dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size((16 * uiScale).dp))
                }
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black)) {
                when (selectedTab) {
                    "TERMINAL" -> TerminalTabContent(viewModel)
                    "PROBLEMS" -> ProblemsView(viewModel)
                    "OUTPUT" -> OutputView(viewModel)
                    "DEBUG CONSOLE" -> DebugConsoleView(viewModel)
                    "PORTS" -> PortsTabContent(viewModel)
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(selectedTab, color = Color.DarkGray) }
                }
            }
        }
    }
}

@Composable
fun DebugConsoleView(viewModel: TerminalViewModel) {
    val logcatLines by viewModel.logcatOutput.collectAsState()
    val filter by viewModel.logcatFilter.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    val scrollState = rememberScrollState()
    
    val filteredLines = remember(logcatLines, filter) {
        if (filter.isBlank()) logcatLines
        else logcatLines.filter { it.contains(filter, ignoreCase = true) }
    }

    // Auto-scroll to bottom
    LaunchedEffect(filteredLines.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117))) {
        // Toolbar with Filter and Stop
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = filter,
                onValueChange = { viewModel.updateLogcatFilter(it) },
                modifier = Modifier.weight(1f).background(Color(0xFF161B22), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF30363D), androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(8.dp),
                textStyle = TextStyle(color = Color.White, fontSize = (11 * uiScale).sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(Color(0xFF58A6FF)),
                decorationBox = { innerTextField ->
                    if (filter.isEmpty()) Text("Filter (e.g. text, !exclude)", color = Color.Gray, fontSize = (11 * uiScale).sp)
                    innerTextField()
                }
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.stopDebugProcess() },
                modifier = Modifier.size((32 * uiScale).dp).background(Color(0xFFF85149).copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(Icons.Default.Stop, "Stop Process", tint = Color(0xFFF85149), modifier = Modifier.size((18 * uiScale).dp))
            }
        }
        HorizontalDivider(color = Color(0xFF30363D))

        Column(modifier = Modifier.weight(1f).padding(8.dp).verticalScroll(scrollState)) {
            filteredLines.forEach { line ->
                val color = when {
                    line.contains(" E ") || line.contains("Error") -> Color(0xFFF85149)
                    line.contains(" W ") || line.contains("Warn") -> Color(0xFFD29922)
                    line.contains(" I ") -> Color(0xFF58A6FF)
                    else -> Color.Gray
                }
                Text(
                    text = line,
                    color = color,
                    fontSize = (10 * uiScale).sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun SplashScreen(viewModel: TerminalViewModel) {
    val status by viewModel.setupStatus.collectAsState()
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CodeOSS Android", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(color = Color(0xFF58A6FF))
            Spacer(Modifier.height(16.dp))
            Text(status, color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun FilePreview(file: java.io.File, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        confirmButton = {},
        containerColor = Color(0xFF0D1117),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(file.name, color = Color.White, modifier = Modifier.weight(1f), fontSize = 16.sp)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { context ->
                        android.webkit.WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                            }
                            webViewClient = android.webkit.WebViewClient()
                            loadUrl("file://${file.absolutePath}")
                        }
                    },
                    modifier = Modifier.fillMaxSize().background(Color.White)
                )
            }
        }
    )
}

@Composable
fun PortsTabContent(viewModel: TerminalViewModel) {
    val activeTunnels by viewModel.activeTunnels.collectAsState()
    var portInput by remember { mutableStateOf("") }
    val uiScale by viewModel.uiScale.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = portInput,
                onValueChange = { portInput = it.filter { char -> char.isDigit() } },
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = (13 * uiScale).sp),
                modifier = Modifier.weight(1f).background(Color(0xFF161B22)).padding(10.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (portInput.isEmpty()) Text("Enter port (e.g. 3000)", color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                        inner()
                    }
                }
            )
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = { 
                    portInput.toIntOrNull()?.let { viewModel.startTunnel(it) }
                    portInput = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58A6FF)),
                enabled = portInput.isNotBlank()
            ) {
                Text("Forward Port", color = Color.White, fontSize = (12 * uiScale).sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFF30363D))
        Spacer(Modifier.height(8.dp))

        if (activeTunnels.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No active port forwards", color = Color.Gray, fontSize = (12 * uiScale).sp)
            }
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                activeTunnels.forEach { tunnel ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(Color(0xFF161B22)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Port ${tunnel.port}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (14 * uiScale).sp)
                            Spacer(Modifier.height(4.dp))
                            Text(tunnel.url, color = Color(0xFF58A6FF), fontFamily = FontFamily.Monospace, fontSize = (12 * uiScale).sp,
                                modifier = Modifier.clickable {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("URL", tunnel.url)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "URL Copied", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        IconButton(onClick = { viewModel.stopTunnel(tunnel.port) }) {
                            Icon(Icons.Default.StopCircle, "Stop Tunnel", tint = Color(0xFFF85149))
                        }
                    }
                }
            }
        }
    }
}

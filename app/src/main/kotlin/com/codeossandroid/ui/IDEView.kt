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
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.codeossandroid.viewmodel.TerminalViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun IDEView(viewModel: TerminalViewModel) {
    val isPanelVisible by viewModel.isPanelVisible.collectAsState()
    val panelHeight by viewModel.panelHeight.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    val sidebarOpen by viewModel.sidebarOpen.collectAsState()
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
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0D1117))) {
                        CodeEditor(viewModel)
                    }
                    if (isPanelVisible) {
                        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF30363D)).draggable(
                            orientation = Orientation.Vertical, state = rememberDraggableState { delta -> viewModel.updatePanelHeight((-delta).dp) }
                        ))
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
    }
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
        }
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
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(selectedTab, color = Color.DarkGray) }
                }
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

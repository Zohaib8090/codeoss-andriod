package com.codeossandroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeossandroid.viewmodel.TerminalViewModel
import com.codeossandroid.viewmodel.GitChange
import com.codeossandroid.viewmodel.GitCommit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceControlContent(viewModel: TerminalViewModel) {
    val gitBranch by viewModel.gitBranch.collectAsState()
    val gitChanges by viewModel.gitChanges.collectAsState()
    val gitLog by viewModel.gitLog.collectAsState()
    val gitBranches by viewModel.gitBranches.collectAsState()
    val githubUser by viewModel.githubUser.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()
    
    var commitMessage by remember { mutableStateOf("") }
    var showBranchDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    
    var stagedExpanded by remember { mutableStateOf(true) }
    var changesExpanded by remember { mutableStateOf(true) }
    var timelineExpanded by remember { mutableStateOf(true) }

    val staged = gitChanges.filter { it.isStaged }
    val unstaged = gitChanges.filter { !it.isStaged }

    if (showBranchDialog) {
        AlertDialog(
            onDismissRequest = { showBranchDialog = false },
            title = { Text("Switch Branch", color = Color.White) },
            text = {
                LazyColumn {
                    items(gitBranches) { branch ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    viewModel.gitCheckout(branch)
                                    showBranchDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccountTree, null, 
                                tint = if (branch == gitBranch) Color(0xFF58A6FF) else Color.Gray,
                                modifier = Modifier.size((16 * uiScale).dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(branch, color = if (branch == gitBranch) Color.White else Color.Gray, fontSize = (14 * uiScale).sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showBranchDialog = false }) { Text("Cancel") } },
            containerColor = Color(0xFF161B22)
        )
    }

    if (showLoginDialog) {
        var user by remember { mutableStateOf("") }
        var token by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = { Text("Sign in to GitHub", color = Color.White) },
            text = {
                Column {
                    Text("Enter your GitHub credentials for automatic authentication (Push/Pull).", color = Color.Gray, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = user, onValueChange = { user = it },
                        label = { Text("GitHub Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF58A6FF),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF58A6FF)
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = token, onValueChange = { token = it },
                        label = { Text("Personal Access Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF58A6FF),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF58A6FF)
                        )
                    )
                    TextButton(onClick = { /* Open GitHub Token URL */ }) {
                        Text("Generate Token on GitHub", color = Color(0xFF58A6FF), fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { 
                    if (user.isNotEmpty() && token.isNotEmpty()) {
                        viewModel.saveGithubAuth(user, token)
                        showLoginDialog = false
                    }
                }) { Text("Sign In") }
            },
            dismissButton = { TextButton(onClick = { showLoginDialog = false }) { Text("Cancel") } },
            containerColor = Color(0xFF161B22)
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF161B22))) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SOURCE CONTROL", color = Color(0xFF8B949E), fontSize = (11 * uiScale).sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            // This is the 3-dot menu the user asked about. 
            // It's a standard "More Actions" menu in IDEs for things like Stash, Clean, etc.
            Icon(Icons.Default.MoreHoriz, null, tint = Color.Gray, modifier = Modifier.size((16 * uiScale).dp))
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            // GitHub Account
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF0D1117)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccountCircle, null, tint = Color.Gray, modifier = Modifier.size((20 * uiScale).dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (githubUser != null) "Logged in as $githubUser" else "GitHub Account",
                            color = Color.White, fontSize = (12 * uiScale).sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (githubUser != null) "Automatic Auth Active" else "Not signed in",
                            color = Color.Gray, fontSize = (10 * uiScale).sp
                        )
                    }
                    Button(
                        onClick = { if (githubUser != null) viewModel.logoutGithub() else showLoginDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (githubUser != null) Color(0xFFF85149) else Color(0xFF238636)),
                        shape = MaterialTheme.shapes.extraSmall,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text(if (githubUser != null) "Logout" else "Sign In", color = Color.White, fontSize = (10 * uiScale).sp)
                    }
                }
                HorizontalDivider(color = Color(0xFF30363D))
            }

            // Repositories stub
            item {
                SourceControlSectionHeader("REPOSITORIES", uiScale, true) {}
                RepositoryItem(activeProject ?: "No Project", gitBranch, uiScale, viewModel) {
                    showBranchDialog = true
                }
            }
// ... rest of the content remains same

            // Commit Input
            item {
                Box(modifier = Modifier.padding(12.dp)) {
                    Column {
                        OutlinedTextField(
                            value = commitMessage,
                            onValueChange = { commitMessage = it },
                            placeholder = { Text("Message (Ctrl+Enter to commit)", fontSize = (12 * uiScale).sp, color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = (12 * uiScale).sp, fontFamily = FontFamily.Monospace, color = Color.White),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF30363D),
                                unfocusedBorderColor = Color(0xFF30363D),
                                containerColor = Color(0xFF0D1117)
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { 
                                if (commitMessage.isNotEmpty()) {
                                    viewModel.gitCommit(commitMessage)
                                    commitMessage = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                            shape = MaterialTheme.shapes.extraSmall,
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("Commit", color = Color.White, fontSize = (12 * uiScale).sp)
                        }
                    }
                }
            }

            // Staged Changes
            if (staged.isNotEmpty()) {
                item {
                    SourceControlSectionHeader("STAGED CHANGES", uiScale, stagedExpanded, staged.size) { stagedExpanded = !stagedExpanded }
                }
                if (stagedExpanded) {
                    items(staged) { change ->
                        GitFileItem(change, uiScale, onAction = { viewModel.gitUnstage(change.path) }, actionIcon = Icons.Default.Remove)
                    }
                }
            }

            // Changes
            item {
                SourceControlSectionHeader("CHANGES", uiScale, changesExpanded, unstaged.size) { changesExpanded = !changesExpanded }
            }
            if (changesExpanded) {
                items(unstaged) { change ->
                    GitFileItem(change, uiScale, onAction = { viewModel.gitStage(change.path) }, actionIcon = Icons.Default.Add)
                }
            }

            // Timeline
            item {
                SourceControlSectionHeader("TIMELINE", uiScale, timelineExpanded) { timelineExpanded = !timelineExpanded }
            }
            if (timelineExpanded) {
                items(gitLog) { commit ->
                    CommitTimelineItem(commit, uiScale)
                }
            }
        }
    }
}

@Composable
fun SourceControlSectionHeader(title: String, uiScale: Float, expanded: Boolean, badgeCount: Int = 0, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
            null, tint = Color.Gray, modifier = Modifier.size((16 * uiScale).dp)
        )
        Text(title, color = Color.White, fontSize = (11 * uiScale).sp, fontWeight = FontWeight.Bold)
        if (badgeCount > 0) {
            Spacer(Modifier.width(8.dp))
            Surface(color = Color(0xFF30363D), shape = CircleShape) {
                Text(
                    badgeCount.toString(), color = Color.White, fontSize = (9 * uiScale).sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun RepositoryItem(name: String, branch: String, uiScale: Float, viewModel: TerminalViewModel, onBranchClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBranchClick() }
            .background(Color(0xFF090C10))
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = Color(0xFF58A6FF), fontSize = (12 * uiScale).sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(branch, color = Color.Gray, fontSize = (11 * uiScale).sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.width(8.dp))
        
        IconButton(onClick = { viewModel.gitFetch() }, modifier = Modifier.size((24 * uiScale).dp)) {
            Icon(Icons.Default.Refresh, "Fetch", tint = Color.Gray, modifier = Modifier.size((14 * uiScale).dp))
        }
        IconButton(onClick = { viewModel.gitPull() }, modifier = Modifier.size((24 * uiScale).dp)) {
            Icon(Icons.Default.Download, "Pull", tint = Color.Gray, modifier = Modifier.size((14 * uiScale).dp))
        }
        IconButton(onClick = { viewModel.gitPush() }, modifier = Modifier.size((24 * uiScale).dp)) {
            Icon(Icons.Default.Upload, "Push", tint = Color.Gray, modifier = Modifier.size((14 * uiScale).dp))
        }
    }
}

@Composable
fun GitFileItem(change: GitChange, uiScale: Float, onAction: () -> Unit, actionIcon: androidx.compose.ui.graphics.vector.ImageVector) {
    val statusColor = when(change.status) {
        "M" -> Color(0xFFE3B341) // Yellow
        "A" -> Color(0xFF3FB950) // Green
        "D" -> Color(0xFFF85149) // Red
        "?" -> Color(0xFF79C0FF) // Blue
        else -> Color.Gray
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.InsertDriveFile, null, tint = Color.Gray, modifier = Modifier.size((14 * uiScale).dp))
        Spacer(Modifier.width(8.dp))
        Text(change.path, color = Color(0xFFCDD9E5), fontSize = (12 * uiScale).sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
        Text(change.status, color = statusColor, fontSize = (10 * uiScale).sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onAction, modifier = Modifier.size((20 * uiScale).dp)) {
            Icon(actionIcon, null, tint = Color.Gray, modifier = Modifier.size((12 * uiScale).dp))
        }
    }
}

@Composable
fun CommitTimelineItem(commit: GitCommit, uiScale: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(20.dp)) {
            Box(Modifier.size(8.dp).background(Color(0xFF58A6FF), CircleShape))
            Box(Modifier.width(1.dp).height(32.dp).background(Color(0xFF30363D)))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(commit.message, color = Color.White, fontSize = (12 * uiScale).sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(commit.author, color = Color.Gray, fontSize = (10 * uiScale).sp)
                Spacer(Modifier.width(8.dp))
                Text(commit.date, color = Color.DarkGray, fontSize = (10 * uiScale).sp)
            }
        }
    }
}

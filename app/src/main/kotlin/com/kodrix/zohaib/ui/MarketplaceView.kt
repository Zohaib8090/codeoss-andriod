package com.kodrix.zohaib.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kodrix.zohaib.viewmodel.TerminalViewModel
import com.kodrix.zohaib.bridge.Extension

@Composable
fun MarketplaceView(viewModel: TerminalViewModel) {
    val uiScale by viewModel.uiScale.collectAsState()
    val extensions by viewModel.availableExtensions.collectAsState()
    val isScanning by viewModel.isScanningMarketplace.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // --- Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF161B22), Color(0xFF0D1117))
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(end = 80.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Storefront,
                    contentDescription = null,
                    tint = Color(0xFF58A6FF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "MARKETPLACE",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "• One-click installation",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterEnd).size(16.dp),
                    color = Color(0xFF58A6FF),
                    strokeWidth = 2.dp
                )
            } else {
                Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri ->
                        uri?.let { viewModel.installLocalExtension(it) }
                    }

                    IconButton(
                        onClick = { launcher.launch("application/zip") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, null, tint = Color(0xFF58A6FF), modifier = Modifier.size(18.dp))
                    }
                    
                    IconButton(
                        onClick = { viewModel.scanMarketplace() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // --- Grid ---
        if (extensions.isEmpty() && !isScanning) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No plugins found in the repository", color = Color.Gray)
            }
        } else {
            var versionDialogExtension by remember { mutableStateOf<Extension?>(null) }
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(extensions) { ext ->
                    MarketplaceCard(ext, viewModel, uiScale, onInstallClick = {
                        if (ext.versions.size > 1) {
                            versionDialogExtension = ext
                        } else {
                            viewModel.installGithubExtension(ext)
                        }
                    })
                }
            }
            
            if (versionDialogExtension != null) {
                VersionPickerDialog(
                    extension = versionDialogExtension!!,
                    onDismiss = { versionDialogExtension = null },
                    onVersionSelected = { version ->
                        viewModel.installGithubExtension(versionDialogExtension!!, version)
                        versionDialogExtension = null
                    }
                )
            }
        }
    }
}

@Composable
fun VersionPickerDialog(
    extension: Extension,
    onDismiss: () -> Unit,
    onVersionSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Version", color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                extension.versions.forEach { version ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVersionSelected(version) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(version, color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        },
        containerColor = Color(0xFF161B22),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun MarketplaceCard(extension: Extension, viewModel: TerminalViewModel, uiScale: Float, onInstallClick: () -> Unit) {
    val installingIds by viewModel.installingIds.collectAsState()
    val installingProgress by viewModel.installingProgress.collectAsState()
    
    val isInstalling = installingIds.contains(extension.id)
    val progress = installingProgress[extension.id] ?: 0f
    val isInstalled = extension.isInstalled
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable(enabled = !isInstalling) { viewModel.selectGithubExtension(extension) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(if (isInstalling) 0xFF58A6FF else 0xFF30363D))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF21262D)),
                contentAlignment = Alignment.Center
            ) {
                if (extension.iconUrl != null) {
                    AsyncImage(
                        model = extension.iconUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Extension,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
                if (isInstalling) {
                    Box(Modifier.fillMaxSize().background(Color(0xAA000000)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF58A6FF),
                                trackColor = Color(0xFF30363D)
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Title
            Text(
                text = extension.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Author
            Text(
                text = extension.author,
                color = Color.Gray,
                fontSize = 10.sp,
                maxLines = 1
            )
            
            Spacer(Modifier.weight(1f))
            
            // Install Button
            Button(
                onClick = { if (!isInstalling) onInstallClick() },
                enabled = !isInstalling,
                modifier = Modifier.fillMaxWidth().height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isInstalling -> Color(0xFF161B22)
                        isInstalled -> Color(0xFF21262D)
                        else -> Color(0xFF238636)
                    },
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                if (isInstalling) {
                    Text("Installing ${(progress * 100).toInt()}%", fontSize = 10.sp)
                } else if (isInstalled) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Installed", fontSize = 11.sp)
                } else {
                    Text("Install", fontSize = 11.sp)
                }
            }
        }
    }
}

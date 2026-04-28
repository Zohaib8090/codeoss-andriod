package com.codeossandroid.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeossandroid.viewmodel.TerminalViewModel

@Composable
fun CodeEditor(viewModel: TerminalViewModel, viewportId: Int = 0) {
    val openTabs by viewModel.openTabs.collectAsState()
    val activeTabIndices by viewModel.activeTabIndices.collectAsState()
    val activeTabIndex = activeTabIndices[viewportId] ?: -1
    val fontSize by viewModel.fontSize.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    val focusedViewportId by viewModel.focusedViewportId.collectAsState()
    val isFocused = focusedViewportId == viewportId

    if (activeTabIndex == -1 || openTabs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Code, null, tint = Color.DarkGray, modifier = Modifier.size((48 * uiScale).dp))
                Spacer(Modifier.height(16.dp))
                Text("Select a file to edit", color = Color.Gray, fontSize = (14 * uiScale).sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.setSidebarMode(TerminalViewModel.SidebarMode.EXPLORER) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D))
                ) {
                    Text("Open Explorer", color = Color.White)
                }
            }
        }
        return
    }

    val activeTab = openTabs.getOrNull(activeTabIndex) ?: return

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117))
        .clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null
        ) { 
            viewModel.updateFocusedViewport(viewportId) 
        }
        .border(1.dp, if (isFocused) Color(0xFF58A6FF) else Color.Transparent)
    ) {
        // Tab Bar
        Row(
            modifier = Modifier.fillMaxWidth().height((35 * uiScale).dp).background(Color(0xFF161B22)).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom
        ) {
            openTabs.forEachIndexed { index, tab ->
                val isActive = index == activeTabIndex
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = (100 * uiScale).dp, max = (200 * uiScale).dp)
                        .background(if (isActive) Color(0xFF0D1117) else Color(0xFF161B22))
                        .clickable { viewModel.switchTab(viewportId, index) }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tab.file.name,
                            color = if (isActive) Color.White else Color.Gray,
                            fontSize = (11 * uiScale).sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        if (tab.isModified) {
                            Box(Modifier.size(8.dp).background(Color(0xFF58A6FF), shape = androidx.compose.foundation.shape.CircleShape))
                        }
                        IconButton(onClick = { viewModel.closeTab(index) }, modifier = Modifier.size((16 * uiScale).dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size((10 * uiScale).dp))
                        }
                    }
                    if (isActive) {
                        Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFF78166)).align(Alignment.TopCenter))
                    }
                }
                Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Color(0xFF30363D))
            }
        }

        // Toolbar for Split Actions
        Row(
            modifier = Modifier.fillMaxWidth().height((28 * uiScale).dp).background(Color(0xFF0D1117)).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(activeTab.file.path.substringAfter("projects/"), color = Color(0xFF8B949E), fontSize = (9 * uiScale).sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.saveCurrentFile(viewportId) }, modifier = Modifier.size((24 * uiScale).dp)) {
                Icon(Icons.Default.Save, null, tint = Color(0xFF58A6FF), modifier = Modifier.size((14 * uiScale).dp))
            }
            IconButton(onClick = { viewModel.splitViewport(viewportId) }, modifier = Modifier.size((24 * uiScale).dp)) {
                Icon(Icons.Default.VerticalSplit, null, tint = Color.Gray, modifier = Modifier.size((14 * uiScale).dp))
            }
            if (activeTabIndices.size > 1) {
                IconButton(onClick = { viewModel.removeViewport(viewportId) }, modifier = Modifier.size((24 * uiScale).dp)) {
                    Icon(Icons.Default.Close, null, tint = Color(0xFFF85149), modifier = Modifier.size((14 * uiScale).dp))
                }
            }
        }

        Divider(color = Color(0xFF30363D))

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val scrollState = rememberScrollState()
            val lineCount = activeTab.text.text.count { it == '\n' } + 1
            
            Row(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                Column(
                    modifier = Modifier.width((40 * uiScale).dp).background(Color(0xFF0D1117)).padding(top = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            text = i.toString(),
                            color = Color(0xFF484F58),
                            fontSize = fontSize.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(end = 8.dp),
                            lineHeight = (fontSize * 1.5).sp
                        )
                    }
                }

                val extension = activeTab.file.extension
                BasicTextField(
                    value = activeTab.text,
                    onValueChange = { viewModel.updateEditorText(viewportId, it) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 8.dp),
                    textStyle = TextStyle(
                        color = Color(0xFFCDD9E5),
                        fontSize = fontSize.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = (fontSize * 1.5).sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF58A6FF)),
                    visualTransformation = remember(extension) { SyntaxVisualTransformation(extension) },
                    keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.None)
                )
            }
        }
    }
}

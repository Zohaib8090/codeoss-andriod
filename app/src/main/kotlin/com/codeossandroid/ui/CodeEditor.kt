package com.codeossandroid.ui

import androidx.compose.foundation.background
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
fun CodeEditor(viewModel: TerminalViewModel) {
    val editorText by viewModel.editorText.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    val currentFile by viewModel.currentFile.collectAsState()

    if (currentFile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Code, null, tint = Color.DarkGray, modifier = Modifier.size((48 * uiScale).dp))
                Spacer(Modifier.height(16.dp))
                Text("Select a file to edit", color = Color.Gray, fontSize = (14 * uiScale).sp, fontFamily = FontFamily.Monospace)
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117))) {
        Row(
            modifier = Modifier.fillMaxWidth().height((28 * uiScale).dp).background(Color(0xFF161B22)).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(currentFile?.path?.substringAfter("projects/") ?: "", color = Color(0xFF8B949E), fontSize = (10 * uiScale).sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.saveCurrentFile() }, modifier = Modifier.size((24 * uiScale).dp)) {
                Icon(Icons.Default.Save, null, tint = Color(0xFF58A6FF), modifier = Modifier.size((14 * uiScale).dp))
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { viewModel.closeFile() }, modifier = Modifier.size((24 * uiScale).dp)) {
                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size((14 * uiScale).dp))
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val scrollState = rememberScrollState()
            val lineCount = editorText.text.count { it == '\n' } + 1
            
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

                val extension = currentFile?.extension ?: ""
                BasicTextField(
                    value = editorText,
                    onValueChange = { viewModel.updateEditorText(it) },
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

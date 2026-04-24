package com.zohaib.codeossandriod

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CodeOSSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D1117)
                ) {
                    val viewModel: TerminalViewModel = viewModel()
                    IDEView(viewModel)
                }
            }
        }
    }
}

class TerminalViewModel : ViewModel() {
    private val pty = PtyBridge()
    private val _output = MutableStateFlow<List<String>>(listOf("Initializing Terminal..."))
    val output = _output.asStateFlow()

    init {
        // Start shell in background
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            pty.startShell()
            val buffer = ByteArray(4096)
            while (true) {
                val read = pty.read(buffer)
                if (read > 0) {
                    val text = String(buffer, 0, read)
                    _output.value = _output.value + text.split("\n")
                } else if (read == -1) {
                    break
                }
            }
        }
    }

    fun sendCommand(cmd: String) {
        pty.write(cmd + "\n")
    }
}

@Composable
fun IDEView(viewModel: TerminalViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Mock Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF161B22)),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "  CodeOSS Android",
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Mock Editor Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "// Editor Engine (Phase 3) will be here",
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
        }

        // Terminal Area (Phase 1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
                .background(Color.Black)
        ) {
            TerminalComponent(viewModel)
        }
    }
}

@Composable
fun TerminalComponent(viewModel: TerminalViewModel) {
    val output by viewModel.output.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        items(output) { line ->
            Text(
                text = line,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun CodeOSSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF58A6FF),
            background = Color(0xFF0D1117),
            surface = Color(0xFF161B22)
        ),
        content = content
    )
}

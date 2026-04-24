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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CodeOSSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D1117) // Dark GitHub-like background
                ) {
                    IDEView()
                }
            }
        }
    }
}

@Composable
fun IDEView() {
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
            TerminalComponent()
        }
    }
}

@Composable
fun TerminalComponent() {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(
            text = "zohaib@android:~$ ls",
            color = Color.Green,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
        Text(
            text = "app  build  gradle  settings.gradle.kts",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
        Text(
            text = "zohaib@android:~$ _",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
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

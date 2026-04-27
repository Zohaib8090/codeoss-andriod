package com.codeossandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeossandroid.ui.IDEView
import com.codeossandroid.ui.SplashScreen
import com.codeossandroid.viewmodel.TerminalViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: TerminalViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CodeOSSTheme {
                viewModel = viewModel()
                val isReady by viewModel.isReady.collectAsState()
                
                if (isReady) {
                    IDEView(viewModel)
                } else {
                    SplashScreen(viewModel)
                }
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "codeoss" && uri.host == "github-auth") {
                val code = uri.getQueryParameter("code")
                if (code != null && ::viewModel.isInitialized) {
                    viewModel.handleGithubCallback(code)
                }
            }
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

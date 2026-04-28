package com.codeossandroid.ui

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeossandroid.model.TerminalInstance
import com.codeossandroid.viewmodel.TerminalViewModel
import kotlinx.coroutines.*

@Composable
fun TerminalTabContent(viewModel: TerminalViewModel) {
    val instances by viewModel.instances.collectAsState()
    val activeIdx by viewModel.activeInstanceIndex.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().height((28 * uiScale).dp).background(Color(0xFF0D1117)).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("SESSIONS:", color = Color.DarkGray, fontSize = (9 * uiScale).sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                instances.forEachIndexed { index, instance ->
                    val isActive = index == activeIdx
                    Box(modifier = Modifier.padding(horizontal = 2.dp).clickable { viewModel.switchTerminal(index) }.background(if (isActive) Color(0xFF30363D) else Color.Transparent).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("${instance.id}: bash", color = if (isActive) Color.White else Color.Gray, fontSize = (9 * uiScale).sp)
                    }
                }
            }
            IconButton(onClick = { viewModel.addNewTerminal() }, modifier = Modifier.size((20 * uiScale).dp)) { Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size((14 * uiScale).dp)) }
            IconButton(onClick = { viewModel.copyTerminalLogs(activeIdx) }, modifier = Modifier.size((20 * uiScale).dp)) { Icon(Icons.Default.ContentCopy, "Copy Logs", tint = Color.Gray, modifier = Modifier.size((12 * uiScale).dp)) }
            if (instances.size > 1) {
                IconButton(onClick = { viewModel.removeTerminal(activeIdx) }, modifier = Modifier.size((20 * uiScale).dp)) { Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size((14 * uiScale).dp)) }
            }
        }
        HorizontalDivider(color = Color(0xFF30363D))
        TerminalControlBar(viewModel)
        TerminalComponent(viewModel, instances.getOrNull(activeIdx))
    }
}

@Composable
fun TerminalControlBar(viewModel: TerminalViewModel) {
    val uiScale by viewModel.uiScale.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height((36 * uiScale).dp)
            .background(Color(0xFF161B22))
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val btnModifier = Modifier.weight(1f).fillMaxHeight()
        ControlButton("ESC", btnModifier, uiScale) { viewModel.sendInput("\u001b") }
        ControlButton("TAB", btnModifier, uiScale) { viewModel.sendInput("\t") }
        ControlButton("←", btnModifier, uiScale) { viewModel.sendInput("\u001b[D") }
        ControlButton("↓", btnModifier, uiScale) { viewModel.sendInput("\u001b[B") }
        ControlButton("↑", btnModifier, uiScale) { viewModel.sendInput("\u001b[A") }
        ControlButton("→", btnModifier, uiScale) { viewModel.sendInput("\u001b[C") }
        
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val currentOnClick by rememberUpdatedState { viewModel.sendInput("\u007f") }
        LaunchedEffect(isPressed) {
            if (isPressed) {
                delay(400)
                while (isPressed) {
                    currentOnClick()
                    delay(80)
                }
            }
        }
        Box(
            modifier = btnModifier.pointerInput(Unit) {
                detectTapGestures(onPress = { offset ->
                    val press = PressInteraction.Press(offset)
                    interactionSource.emit(press)
                    currentOnClick()
                    try { awaitRelease(); interactionSource.emit(PressInteraction.Release(press)) }
                    catch (c: CancellationException) { interactionSource.emit(PressInteraction.Cancel(press)) }
                })
            }.indication(interactionSource, rememberRipple(bounded = true)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Backspace, null, tint = Color.White, modifier = Modifier.size((16 * uiScale).dp))
        }
        
        val isCtrlActive by viewModel.isCtrlActive.collectAsState()
        ControlButton("CTRL", btnModifier.background(if (isCtrlActive) Color(0xFF58A6FF).copy(alpha = 0.3f) else Color.Transparent), uiScale) { 
            viewModel.toggleCtrl() 
        }
    }
}

@Composable
fun ControlButton(text: String, modifier: Modifier, uiScale: Float, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shouldRepeat = text == "←" || text == "→" || text == "↑" || text == "↓"
    val currentOnClick by rememberUpdatedState(onClick)

    LaunchedEffect(isPressed) {
        if (isPressed && shouldRepeat) {
            delay(400)
            while (isPressed) {
                currentOnClick()
                delay(80)
            }
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        currentOnClick()
                        try {
                            awaitRelease()
                            interactionSource.emit(PressInteraction.Release(press))
                        } catch (c: CancellationException) {
                            interactionSource.emit(PressInteraction.Cancel(press))
                        }
                    }
                )
            }
            .indication(interactionSource, rememberRipple(bounded = true)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = (10 * uiScale).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TerminalComponent(viewModel: TerminalViewModel, instance: TerminalInstance?) {
    if (instance == null) return
    val fontSize by viewModel.fontSize.collectAsState()

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context ->
            com.termux.view.TerminalView(context, null).apply {
                // TerminalViewClient must be set BEFORE setTextSize/attachSession
                // otherwise updateSize() throws NPE on onEmulatorSet()
                setTerminalViewClient(object : com.termux.view.TerminalViewClient {
                    override fun onScale(scale: Float): Float = scale
                    override fun onSingleTapUp(e: android.view.MotionEvent) {
                        requestFocus()
                        val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                        imm.showSoftInput(this@apply, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                    }
                    override fun shouldBackButtonBeMappedToEscape(): Boolean = false
                    override fun shouldEnforceCharBasedInput(): Boolean = false
                    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
                    override fun isTerminalViewSelected(): Boolean = true
                    override fun copyModeChanged(copyMode: Boolean) {}
                    override fun onKeyDown(keyCode: Int, e: android.view.KeyEvent, session: com.termux.terminal.TerminalSession): Boolean = false
                    override fun onKeyUp(keyCode: Int, e: android.view.KeyEvent): Boolean = false
                    override fun onLongPress(event: android.view.MotionEvent): Boolean = false
                    override fun readControlKey(): Boolean = viewModel.isCtrlActive.value
                    override fun readAltKey(): Boolean = false
                    override fun readShiftKey(): Boolean = false
                    override fun readFnKey(): Boolean = false
                    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: com.termux.terminal.TerminalSession): Boolean = false
                    override fun onEmulatorSet() {}
                    override fun logError(tag: String, message: String) {}
                    override fun logWarn(tag: String, message: String) {}
                    override fun logInfo(tag: String, message: String) {}
                    override fun logDebug(tag: String, message: String) {}
                    override fun logVerbose(tag: String, message: String) {}
                    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
                    override fun logStackTrace(tag: String, e: Exception) {}
                })
                
                isFocusable = true
                isFocusableInTouchMode = true
                
                // setTextSize creates the TerminalRenderer — must come before attachSession
                setTextSize(fontSize)
                attachSession(instance.session)
                
                // Crucial: The TerminalView needs to be notified when the session text changes
                // to trigger a redraw. We set a client that calls postInvalidate().
                instance.session.updateTerminalSessionClient(object : com.termux.terminal.TerminalSessionClient {
                    override fun onTextChanged(changedSession: com.termux.terminal.TerminalSession) {
                        postInvalidate()
                    }
                    override fun onTitleChanged(changedSession: com.termux.terminal.TerminalSession) {}
                    override fun onSessionFinished(finishedSession: com.termux.terminal.TerminalSession) {}
                    override fun onCopyTextToClipboard(session: com.termux.terminal.TerminalSession, text: String) {}
                    override fun onPasteTextFromClipboard(session: com.termux.terminal.TerminalSession) {}
                    override fun onBell(session: com.termux.terminal.TerminalSession) {}
                    override fun onColorsChanged(session: com.termux.terminal.TerminalSession) {}
                    override fun onTerminalCursorStateChange(state: Boolean) {}
                    override fun getTerminalCursorStyle(): Int = com.termux.terminal.TerminalEmulator.TERMINAL_CURSOR_STYLE_BLOCK
                    override fun logError(tag: String, message: String) {}
                    override fun logWarn(tag: String, message: String) {}
                    override fun logInfo(tag: String, message: String) {}
                    override fun logDebug(tag: String, message: String) {}
                    override fun logVerbose(tag: String, message: String) {}
                    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
                    override fun logStackTrace(tag: String, e: Exception) {}
                })
            }
        },
        update = { view ->
            view.setTextSize(fontSize)
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ProblemsView(viewModel: TerminalViewModel) {
    val problems by viewModel.problems.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()

    if (problems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No problems have been detected in the workspace.", color = Color.Gray, fontSize = (12 * uiScale).sp)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        items(problems.size) { index ->
            val prob = problems[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openFile(prob.file) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (prob.severity == "ERROR") Icons.Default.Error else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (prob.severity == "ERROR") Color(0xFFF85149) else Color(0xFFE3B341),
                    modifier = Modifier.size((14 * uiScale).dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = prob.message,
                        color = Color(0xFFCDD9E5),
                        fontSize = (12 * uiScale).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${prob.file.name} [${prob.line}, ${prob.column}]",
                        color = Color.Gray,
                        fontSize = (10 * uiScale).sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            HorizontalDivider(color = Color(0xFF21262D))
        }
    }
}

@Composable
fun OutputView(viewModel: TerminalViewModel) {
    val logs by viewModel.outputLogs.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    
    val scrollState = rememberScrollState()
    LaunchedEffect(logs) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF0D1117)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("OUTPUT", color = Color.Gray, fontSize = (10 * uiScale).sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.clearOutput() }, modifier = Modifier.size((24 * uiScale).dp)) {
                Icon(Icons.Default.Block, null, tint = Color.Gray, modifier = Modifier.size((14 * uiScale).dp))
            }
        }
        HorizontalDivider(color = Color(0xFF30363D))
        Box(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(8.dp)) {
            Text(
                text = logs,
                color = Color(0xFFCCCCCC),
                fontFamily = FontFamily.Monospace,
                fontSize = (fontSize - 1).sp,
                lineHeight = (fontSize + 2).sp
            )
        }
    }
}

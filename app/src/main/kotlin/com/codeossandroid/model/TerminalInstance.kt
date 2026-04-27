package com.codeossandroid.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.codeossandroid.bridge.PtyBridge

class TerminalInstance(val id: Int, val pty: PtyBridge) {
    val output = mutableStateListOf<AnnotatedString>(AnnotatedString(""))
    private var escapeBuffer = ""
    private var isInEscapeSequence = false
    private val maxLines = 1000
    private var currentStyle = SpanStyle()

    fun processOutput(text: String) {
        var lastLineBuilder = AnnotatedString.Builder()
        if (output.isNotEmpty()) {
            lastLineBuilder.append(output.removeAt(output.size - 1))
        }
        var i = 0
        while (i < text.length) {
            val char = text[i]
            if (isInEscapeSequence) {
                escapeBuffer += char
                if (char.isLetter() || char == 'H' || char == 'J') {
                    if (escapeBuffer.contains("[2J") || escapeBuffer.contains("[H")) {
                        output.clear()
                        lastLineBuilder = AnnotatedString.Builder()
                    } else if (char == 'm' && escapeBuffer.startsWith("[")) {
                        val codesStr = escapeBuffer.substring(1, escapeBuffer.length - 1)
                        val codes = if (codesStr.isEmpty()) listOf(0) else codesStr.split(";").mapNotNull { it.toIntOrNull() }
                        for (code in codes) {
                            when (code) {
                                0 -> currentStyle = SpanStyle()
                                1 -> currentStyle = currentStyle.copy(fontWeight = FontWeight.Bold)
                                30 -> currentStyle = currentStyle.copy(color = Color.Black)
                                31 -> currentStyle = currentStyle.copy(color = Color.Red)
                                32 -> currentStyle = currentStyle.copy(color = Color.Green)
                                33 -> currentStyle = currentStyle.copy(color = Color(0xFFDAAA00))
                                34 -> currentStyle = currentStyle.copy(color = Color.Blue)
                                35 -> currentStyle = currentStyle.copy(color = Color.Magenta)
                                36 -> currentStyle = currentStyle.copy(color = Color.Cyan)
                                37 -> currentStyle = currentStyle.copy(color = Color.White)
                                90 -> currentStyle = currentStyle.copy(color = Color.DarkGray)
                                91 -> currentStyle = currentStyle.copy(color = Color(0xFFFF5555))
                                92 -> currentStyle = currentStyle.copy(color = Color(0xFF55FF55))
                                93 -> currentStyle = currentStyle.copy(color = Color(0xFFFFFF55))
                                94 -> currentStyle = currentStyle.copy(color = Color(0xFF5555FF))
                                95 -> currentStyle = currentStyle.copy(color = Color(0xFFFF55FF))
                                96 -> currentStyle = currentStyle.copy(color = Color(0xFF55FFFF))
                                97 -> currentStyle = currentStyle.copy(color = Color.White)
                            }
                        }
                    }
                    isInEscapeSequence = false
                    escapeBuffer = ""
                }
            } else {
                when (char) {
                    '\u001b' -> { isInEscapeSequence = true; escapeBuffer = "" }
                    '\n', '\r' -> {
                        output.add(lastLineBuilder.toAnnotatedString())
                        lastLineBuilder = AnnotatedString.Builder()
                        if (output.size > maxLines) output.removeAt(0)
                    }
                    '\b' -> {
                        val currentText = lastLineBuilder.toAnnotatedString()
                        if (currentText.length > 0) {
                            lastLineBuilder = AnnotatedString.Builder().apply {
                                append(currentText.subSequence(0, currentText.length - 1))
                            }
                        }
                    }
                    else -> {
                        lastLineBuilder.pushStyle(currentStyle)
                        lastLineBuilder.append(char.toString())
                        lastLineBuilder.pop()
                    }
                }
            }
            i++
        }
        output.add(lastLineBuilder.toAnnotatedString())
    }

    fun getAllOutput(): String {
        return output.joinToString("\n") { it.text }
    }
}

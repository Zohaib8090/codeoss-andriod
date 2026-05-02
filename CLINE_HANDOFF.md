# CodeOSS Android - Project Handoff

Hello Cline! You are taking over development for **CodeOSS Android**, a native Android code editor that embeds a full Node.js environment and supports VS Code Language Server Protocol (LSP) extensions natively on the phone.

## Architecture Overview
- **App Stack:** Native Android (Kotlin, Jetpack Compose).
- **Embedded Node.js:** The app bundles a Termux-based Node.js runtime. 
- **LSP Bridge:** We run Language Servers (like `html-languageserver`) in the background. The app communicates with them over standard I/O (stdin/stdout) using JSON-RPC.

## Current State (Phase 4a - Complete)
The previous AI (Antigravity) successfully set up the Language Server background process and JSON-RPC bridge.
- The `TerminalViewModel.kt` successfully launches the language server via `/system/bin/sh` to bypass Android 14 linker restrictions.
- The `LspClient.kt` successfully parses JSON-RPC messages and triggers diagnostic events (red error badges in the Problems panel).

## Your Next Task (Phase 4b - Autocomplete)
The next goal is to implement **Autocomplete Injection**.
Currently, the IDE can detect errors, but we need it to suggest code.

**What you need to build:**
1. **Send Autocomplete Requests:** Modify the editor's text change listener to send `textDocument/completion` JSON-RPC requests to the active `LspClient` when the user types (especially after typing `<` or `.`).
2. **Handle Responses:** Parse the completion items returned by the language server.
3. **UI Integration:** Display these completion items in a floating dropdown box (or a bottom sheet) in the Compose UI.
4. **Injection:** When the user taps a suggestion, inject that text directly into the `CodeEditor`'s `TextFieldValue` at the cursor position.

## Key Files to Review
- `app/src/main/kotlin/com/codeossandroid/viewmodel/TerminalViewModel.kt` (LSP Lifecycle and state)
- `app/src/main/kotlin/com/codeossandroid/lsp/LspClient.kt` (JSON-RPC communication bridge)

You are authorized to execute terminal commands to build (`.\gradlew.bat assembleDebug`) and push the APK to the attached ADB device to test your changes. 

Good luck!

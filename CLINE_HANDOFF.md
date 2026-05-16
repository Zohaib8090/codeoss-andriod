# CodeOSS Android - Project Handoff

Hello Cline! You are taking over development for **CodeOSS Android**, a native Android code editor that embeds a full Node.js environment and supports VS Code Language Server Protocol (LSP) extensions natively on the phone.

## Architecture Overview
- **App Stack:** Native Android (Kotlin, Jetpack Compose).
- **Embedded Node.js:** The app bundles a Termux-based Node.js runtime. 
- **LSP Bridge:** We run Language Servers (like `html-languageserver`) in the background. The app communicates with them over standard I/O (stdin/stdout) using JSON-RPC.
- **Native SWC:** The app now bundles native SWC binaries for Next.js and high-performance React compilation.

## Current State (Phase 4a - In Progress)
The core infrastructure for the IDE is now fully operational:
- **Native SWC Integration (Complete)**: Next.js builds are now supported via bundled `aarch64-linux-android` binaries and `$ORIGIN` patching.
- **LSP Bridge (Complete)**: The `LspClient.kt` streaming engine and JSON-RPC bridge are functional.
- **Editor UI (In Progress)**: Autocomplete dropdowns and real-time Diagnostics are partially implemented.
- **APK Verified**: The latest build has been successfully installed and verified on a mobile device.

## Your Next Task (Phase 4b - Autocomplete)
The next goal is to implement **Autocomplete Injection**.
Currently, the IDE can detect errors, but we need it to suggest code.

**What you need to build:**
1. **Send Autocomplete Requests:** Modify the editor's text change listener to send `textDocument/completion` JSON-RPC requests to the active `LspClient` when the user types (especially after typing `<` or `.`).
2. **Handle Responses:** Parse the completion items returned by the language server.
3. **UI Integration:** Display these completion items in a floating dropdown box (or a bottom sheet) in the Compose UI.
4. **Injection:** When the user taps a suggestion, inject that text directly into the `CodeEditor`'s `TextFieldValue` at the cursor position.

## Key Files to Review
- `app/src/main/kotlin/com.kodrix.zohaib/viewmodel/TerminalViewModel.kt` (LSP Lifecycle and state)
- `app/src/main/kotlin/com.kodrix.zohaib/lsp/LspClient.kt` (JSON-RPC communication bridge)

You are authorized to execute terminal commands to build (`.\gradlew.bat assembleDebug`) and push the APK to the attached ADB device to test your changes. 

Good luck!

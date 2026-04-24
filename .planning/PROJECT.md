# CodeOSS Android

A high-performance, purely native IDE for Android that provides a "VS Code-like" experience using Jetpack Compose and Skia.

## Context
Traditional "VS Code on Android" solutions rely on WebViews and local web servers (code-server). This project aims to break away from the browser, offering a 100% local, native Android application with a high-performance editor, integrated shell, and full extension support via a bundled Node.js runtime.

## Core Value
* **Performance**: Sub-16ms frame times even with large files.
* **Native Experience**: Deep integration with Android's system, file picker, and multitasking.
* **Extensibility**: Support for Language Server Protocol (LSP) and existing VS Code extensions.
* **Complete Dev Environment**: Bundled Node.js, Git, and a full terminal shell.

## Architecture
* **Frontend**: Kotlin + Jetpack Compose (using Canvas for Editor rendering).
* **Editor**: High-performance custom text engine (inspired by Sora Editor).
* **Backend Runtime**: Bundled Node.js (ARM64) for Extension Host and LSPs.
* **Version Control**: Bundled Native Git binary.
* **Communication**: JSON-RPC over stdin/stdout for LSP.

## Requirements

### Validated
- [x] Create a basic Terminal Emulator component with PTY support. (Validated in Phase 01: The Shell Foundation)

### Active
- [ ] Implement a Skia-based text rendering engine for the editor.
- [ ] Bundle and execute Node.js binary within the Android app context.
- [ ] Implement a Kotlin-based LSP client.
- [ ] Integrate Git binary for version control operations.

### Out of Scope
- [ ] Web-based UI / WebView support (this is a pure native project).
- [ ] Cloud sync (V1 focuses on 100% local operation).

## Key Decisions
| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Jetpack Compose | Modern Android standard with built-in Skia support. | Validated |
| Bundled Node.js | Necessary for running VS Code extension host and LSPs. | — Pending |
| PTY Bridge | Required for a "real" shell experience in the terminal. | Validated |

## Evolution
This document evolves at phase transitions and milestone boundaries.

---
*Last updated: 2026-04-25 after Phase 01*

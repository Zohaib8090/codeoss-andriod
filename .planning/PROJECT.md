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

### Phase 02: Binary Integration (Node.js & Git) [COMPLETED]
- [x] Bundle Node.js and Git binaries via jniLibs.
- [x] Update Native PTY Bridge to inject PATH.
- [x] Implement blocking splash screen for environment verification.

### Phase 03: Project Management & File Operations [COMPLETED]
- [x] Implement slide-in sidebar UI for Project Switching and File Tree.
- [x] Automated per-project directory isolation.
- [x] Integrated ZIP Import/Export for entire projects via Storage Access Framework.
- [x] Added single file upload/download functionality.

### Phase 03.5: Terminal Hardening & DNS [COMPLETED]
- [x] **Shell Function Strategy**: Bypassed Android 10+ `W^X` security policy by replacing wrapper scripts with in-memory shell functions.
- [x] **DNS Monkey-Patch**: Implemented a universal DNS redirection for Node.js using `NODE_OPTIONS` to ensure connectivity in restricted environments.
- [x] Fixed `Permission denied` errors for `git` and `node`.

### Phase 04: Native Editor Polish [IN PROGRESS]
- [ ] Implement robust syntax highlighting engine.
- [ ] Add support for multiple tabs.
- [ ] Integrate LSP (Language Server Protocol) via Node.js.

## Out of Scope
- [ ] Web-based UI / WebView support (this is a pure native project).
- [ ] Cloud sync (V1 focuses on 100% local operation).

## Key Decisions
| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Jetpack Compose | Modern Android standard with built-in Skia support. | Validated |
| Bundled Node.js | Necessary for running VS Code extension host and LSPs. | Validated |
| PTY Bridge | Required for a "real" shell experience in the terminal. | Validated |
| **Shell Functions** | **Crucial bypass for Android 10+ security (prevents Permission Denied on internal binaries).** | **Validated** |
| DNS Redirect | Overrides broken system DNS lookups for stable npm/git networking. | Validated |

## Evolution
This document evolves at phase transitions and milestone boundaries.

---
*Last updated: 2026-04-25 after Phase 03.5 Terminal Hardening*

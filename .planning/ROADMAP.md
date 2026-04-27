# Roadmap — CodeOSS Android

## Milestone 1: Core Native Environment
Focus on setting up the native Android foundation, terminal, and bundled binaries.

### Phase 1: The Shell Foundation
- [ ] Initialize Android Project (Kotlin/Jetpack Compose).
- [ ] Implement PTY bridge for native shell access.
- [ ] Create basic Terminal UI component.
- [ ] Verify local shell execution (`ls`, `cd`, `mkdir`).

### Phase 2: Binary Integration (Node.js & Git)
- [x] Bundle Node.js (ARM64) and Git binaries in assets.
- [x] Implement binary extraction and execution logic.
- [x] Add `node` and `git` to the terminal environment path.
- [x] Verify `node -v` and `git --version` in the integrated terminal.

### Phase 3: IDE Core & Project Management [COMPLETED]
- [x] Create a slide-in sidebar UI for project switching.
- [x] Implement file tree navigation with directory expansion.
- [x] Refine project isolation (Set HOME/PWD in native shell).
- [x] Implement ZIP Import/Export for entire projects.
- [x] Implement local file loading/saving in the editor.

### Phase 3.5: Terminal Hardening & DNS [COMPLETED]
- [x] Implement Shell Function strategy for Android 10+ binary execution.
- [x] Implement DNS monkey-patch for Node.js internet access.
- [x] Fix `git clone` permission and connectivity issues.

### Phase 4: Language Server Protocol (LSP)
- [ ] Implement Kotlin JSON-RPC client.
- [ ] Set up communication between Editor and Node.js-based LSPs.
- [ ] Add basic syntax highlighting (TextMate grammar or treesitter).
- [ ] Implement Autocomplete and Go-to-Definition.

### Phase 5: Extension Host & UX Polish
- [ ] Implement VS Code Extension Host bridge.
- [ ] Support VS Code themes and keybindings.
- [ ] Final performance optimizations and testing.

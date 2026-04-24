# Roadmap — CodeOSS Android

## Milestone 1: Core Native Environment
Focus on setting up the native Android foundation, terminal, and bundled binaries.

### Phase 1: The Shell Foundation
- [ ] Initialize Android Project (Kotlin/Jetpack Compose).
- [ ] Implement PTY bridge for native shell access.
- [ ] Create basic Terminal UI component.
- [ ] Verify local shell execution (`ls`, `cd`, `mkdir`).

### Phase 2: Binary Integration (Node.js & Git)
- [ ] Bundle Node.js (ARM64) and Git binaries in assets.
- [ ] Implement binary extraction and execution logic.
- [ ] Add `node` and `git` to the terminal environment path.
- [ ] Verify `node -v` and `git --version` in the integrated terminal.

### Phase 3: The Native Editor Engine
- [ ] Create a Canvas-based text renderer in Compose.
- [ ] Implement line-based virtualization for large file performance.
- [ ] Add basic editing capabilities (cursor, selection, typing).
- [ ] Implement local file loading/saving.

### Phase 4: Language Server Protocol (LSP)
- [ ] Implement Kotlin JSON-RPC client.
- [ ] Set up communication between Editor and Node.js-based LSPs.
- [ ] Add basic syntax highlighting (TextMate grammar or treesitter).
- [ ] Implement Autocomplete and Go-to-Definition.

### Phase 5: Extension Host & UX Polish
- [ ] Implement VS Code Extension Host bridge.
- [ ] Add sidebar for file explorer and search.
- [ ] Support VS Code themes and keybindings.
- [ ] Final performance optimizations and testing.

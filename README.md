<div align="center">

<img src="https://img.shields.io/badge/Platform-Android%2010%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
<img src="https://img.shields.io/badge/Architecture-arm64--v8a-FF6B35?style=for-the-badge"/>
<img src="https://img.shields.io/badge/License-Source--Available-red?style=for-the-badge"/>
<img src="https://img.shields.io/badge/Status-Beta-yellow?style=for-the-badge"/>
<img src="https://img.shields.io/github/stars/Zohaib8090/kodrixide?style=for-the-badge"/>

# KodrixIDE

### A fully standalone, native IDE for Android. No PC. No Termux. No compromises.

*Built by [@Zohaib8090](https://github.com/Zohaib8090)*

</div>

---

## What is KodrixIDE?

KodrixIDE is a **full development environment** that runs entirely on your Android phone. Unlike other mobile code editors, it doesn't require Termux, a remote server, or a PC. Everything runs natively on-device.

Clone a repo, install npm packages, and run a dev server — all from your phone.

---

## Installation

1. Download the latest APK from [Releases](https://github.com/Zohaib8090/kodrixide/releases)
2. On your Android phone, go to **Settings → Security → Install unknown apps** and allow your browser or file manager
3. Open the downloaded APK and tap **Install**
4. Open KodrixIDE and start coding

> **Minimum requirements:** Android 10+, arm64-v8a device (most modern Android phones), ~500MB free storage

---

## Features

### Editor
- **Multi-file Tabs** — Open multiple files simultaneously with unsaved change indicators
- **Split Screen** — View and edit files side by side horizontally or vertically
- **Syntax Highlighting** — Color highlighting for Kotlin, JavaScript, TypeScript, HTML, CSS, Markdown and more
- **Smart Keyboard** — Extra keys row (ESC, TAB, arrows, CTRL) that sits above the soft keyboard

### Terminal
- **Real Terminal** — Full PTY-based terminal powered by the termux-terminal-emulator library
- **Multiple Sessions** — Run multiple terminal sessions simultaneously with tab switching
- **ANSI Support** — Full color and cursor control support
- **TUI Support** — Interactive CLI tools render correctly

### Git & Source Control
- **Git over HTTPS** — Clone, commit, push, pull via libgit2 JNI bridge (no binary execution)
- **GitHub OAuth** — Sign in with GitHub — no manual config needed
- **Source Control UI** — Visual git panel with commit, push, pull, branch switching, changes list and timeline
- **One-click Clone** — Browse your GitHub repos and clone with a single tap

### Runtime
- **Node.js Runtime** — Full Node.js running on-device
- **npm Support** — Install packages and run scripts natively
- **Auto Binary Updates** — Node.js and Git binaries update automatically via GitHub releases

### Browser & DevTools
- **Built-in Browser** — Open your dev server directly inside the IDE
- **Auto Detection** — Automatically detects running dev servers and offers to open them
- **DevTools Console** — Browser console showing logs from your web app
- **Desktop Mode** — Switch between mobile and desktop user agent
- **Camera/Mic/File** — Full permission support for testing web apps
- **Zoom Support** — Pinch to zoom and text size controls

### Port Forwarding
- **One-tap Tunnels** — Expose localhost to the internet instantly via bore.pub
- **Auto Detection** — Automatically detects active dev server ports
- **Open Anywhere** — Open tunneled URLs in internal or external browser

### Debugging
- **Debug Console** — Variables, Watch, Call Stack, Breakpoints panel
- **Live Logcat** — Real-time system log viewer with filtering and color coding
- **Problems Panel** — Automatically parses build errors with file and line info
- **Output Panel** — Dedicated output view for running tasks

### Marketplace & Extensions
- **Extension Marketplace** — Browse and search VS Code extensions via Open VSX
- **Extension Details** — Full description, publisher info and version for each extension

### Project Management
- **File Manager** — Full file explorer with create, rename, delete, copy, paste, cut
- **Project Import/Export** — Import and export projects as ZIP files
- **File Upload/Download** — Upload files from device storage into projects

### Updates & Settings
- **Auto Update Notifications** — Get notified when a new app version is available
- **Configurable Font Size** — Adjust editor and terminal font size
- **Configurable UI Scale** — Adjust icon and UI element sizes
- **Zero Termux Dependency** — Completely standalone APK
- **Android 15 Compatible** — Works with Android's W^X security policy

---

## Supported Frameworks

| Framework | Status | Notes |
|-----------|--------|-------|
| React + Vite | ✅ Working | Use `@vitejs/plugin-react` (not swc) |
| Express / Node.js | ✅ Working | Full support |
| Vue + Vite | ✅ Working | Pure JS build |
| Svelte + Vite | ✅ Working | Pure JS build |
| Next.js | ✅ Working | Full support via native babel |

---

## Architecture

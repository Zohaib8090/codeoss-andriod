<div align="center">

<img src="https://img.shields.io/badge/Platform-Android%2010%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
<img src="https://img.shields.io/badge/Architecture-arm64--v8a-FF6B35?style=for-the-badge"/>
<img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge"/>
<img src="https://img.shields.io/badge/Status-Beta-yellow?style=for-the-badge"/>
<img src="https://img.shields.io/github/stars/Zohaib8090/codeoss-android?style=for-the-badge"/>

# CodeOSS Android

### A fully standalone, native IDE for Android. No PC. No Termux. No compromises.

*Built by [@Zohaib8090](https://github.com/Zohaib8090)*

</div>

---

## What is CodeOSS Android?

CodeOSS Android is a **full development environment** that runs entirely on your Android phone. Unlike other mobile code editors, it doesn't require Termux, a remote server, or a PC. Everything runs natively on-device.

Clone a repo, install npm packages, and run a dev server — all from your phone.

---

## Installation

1. Download the latest APK from [Releases](https://github.com/Zohaib8090/codeoss-android/releases)
2. On your Android phone, go to **Settings → Security → Install unknown apps** and allow your browser or file manager
3. Open the downloaded APK and tap **Install**
4. Open CodeOSS Android and start coding

> **Minimum requirements:** Android 10+, arm64-v8a device (most modern Android phones), ~200MB free storage

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
| Next.js | ⏳ Pending | Blocked by missing SWC binary for Android arm64. [Track progress](https://github.com/vercel/next.js/discussions/93267) |

---

## Architecture

```
CodeOSS Android
├── Kotlin / Jetpack Compose (UI)
├── JNI Bridge (native-lib.cpp)
│   ├── PTY Bridge (forkpty → real terminal)
│   ├── Git Bridge (libgit2 → HTTPS clone/push/pull)
│   └── DNS Override (Google DNS for Node.js)
├── Terminal Engine (termux-terminal-emulator)
│   └── Full VT100/xterm emulation
├── Bundled Binaries (jniLibs/arm64-v8a)
│   ├── libnode.so (Node.js runtime)
│   ├── libgit2.so (Git operations)
│   ├── libcurl.so (HTTP/HTTPS)
│   ├── libssl.so / libcrypto.so (OpenSSL 3.x)
│   └── libicui18n/uc/data.so (Unicode support)
└── Zero-Termux Policy (no external dependencies)
```

---

## Known Limitations

| Limitation | Reason | Status |
|------------|--------|--------|
| Next.js not supported | No SWC binary for Android arm64 | [Reported to Next.js](https://github.com/vercel/next.js/discussions/93267) |
| Use `@vitejs/plugin-react` not swc | Same SWC issue | Workaround available |
| No iOS support | Platform limitation | Not planned |
| x86 devices not supported | All binaries are arm64-v8a | Not planned |

---

## Roadmap

- [x] Terminal with PTY
- [x] Git clone over HTTPS
- [x] GitHub OAuth login
- [x] npm install
- [x] Vite + React support
- [x] Built-in browser with DevTools
- [x] Port forwarding (bore.pub)
- [x] Multi-file tabs + split screen
- [x] Source control UI
- [x] Debug console + Logcat
- [x] Extension marketplace UI
- [x] Auto update notifications
- [x] Node/Git binary update system
- [x] Termux terminal emulator integration
- [x] Syntax highlighting
- [x] Smart keyboard row
- [ ] LSP autocomplete (downloadable language packs)
- [ ] AI assistant (bring your own API key)
- [ ] Android app development support
- [ ] Next.js support (pending SWC Android arm64)

---

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a PR.

**Areas where help is most needed:**
- Testing on different Android devices and reporting bugs
- Rust experience for SWC Android arm64 compilation
- UI improvements in Jetpack Compose
- Documentation

---

## For Developers — Building from Source

> This section is only for developers who want to modify or contribute to CodeOSS Android. Regular users just need the APK above.

**Prerequisites:**
- Android Studio Hedgehog or newer
- Android NDK r25+
- CMake 3.22+
- JDK 17+

**Steps:**
```bash
git clone https://github.com/Zohaib8090/codeoss-android.git
cd codeoss-android
./gradlew assembleDebug
```

APK will be at `app/build/outputs/apk/debug/app-debug.apk`

---

## License

MIT License — see [LICENSE](LICENSE) for details.

---

## Acknowledgements

- [libgit2](https://libgit2.org/) — Git operations
- [Termux](https://termux.dev/) — Binary sources and terminal emulator library
- [Node.js](https://nodejs.org/) — JavaScript runtime
- [bore](https://github.com/ekzhang/bore) — Port forwarding
- [Open VSX](https://open-vsx.org/) — Extension marketplace

---

<div align="center">

Made with ❤️ by a 17-year-old developer from Karachi, Pakistan

*If CodeOSS Android helps you, consider [sponsoring](https://github.com/sponsors/Zohaib8090)*

</div>

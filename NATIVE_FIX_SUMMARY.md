# Android Native Linker & Git Fix Summary

This document summarizes the technical fixes applied to resolve the "library not found" errors for bundled native binaries (`git`, `node`) on Android.

## 1. The Root Cause: Android Linker Namespaces
Since Android 7.0+, the OS uses **Linker Namespaces** for security. This means:
*   **Environment Isolation**: `LD_LIBRARY_PATH` is stripped or ignored for processes launched from the system shell (`/system/bin/sh`).
*   **Hardcoded Paths**: Our binaries were compiled for Termux, with a hardcoded `RUNPATH` pointing to `/data/data/com.termux/files/usr/lib`. If Termux isn't installed or is missing specific libs (like `libpcre2-8.so`), the app crashes.

## 2. The Solution: Binary RUNPATH Patching
Instead of relying on environment variables (which Android blocks), we modified the native binaries (`.so` files) directly to tell the system where to find their dependencies.

### The $ORIGIN Trick
We used a special linker keyword called `$ORIGIN`, which tells the system to look for libraries in the **same directory as the binary itself**.

### Precision Hex-Patching
To avoid corrupting the ELF file structure or breaking embedded JSON/JavaScript strings (which caused `SyntaxError` crashes in Node.js), we applied a surgical 35-character patch:
*   **Original String**: `/data/data/com.termux/files/usr/lib` (35 chars)
*   **Patched String**: `$ORIGIN/./././././././././././././.` (35 chars)

Using the `/./` (current directory) padding allowed us to match the exact length of the original string without using null bytes, keeping the binaries compatible with both the Android Linker and Node.js bootstrap scripts.

## 3. Applied Changes

### Native Binaries Patched
The following files in `app/src/main/jniLibs/arm64-v8a/` were hex-patched:
*   `libgit.so`
*   `libnode.so`
*   `libgit_remote_http.so`

### Code Cleanup
*   **`PtyBridge.kt`**: Removed `LD_PRELOAD` hacks and restored standard wrapper logic.
*   **`TerminalViewModel.kt`**: Removed `LD_PRELOAD` from `runGit` and `cloneRepository` methods.
*   **`BrowserView.kt`**: Implemented `DisposableEffect` to explicitly `.destroy()` WebView instances and nullify references when the browser is toggled off, preventing memory leaks and background audio persistence.

## 4. Verification
*   **Binary Headers**: Verified using `readelf -d` on the device.
*   **Runtime**: Confirmed that `npm install` and `git --version` execute successfully without linker errors or syntax crashes.

---
**Note**: If you replace these binaries in the future with newer versions from Termux or another source, you must re-apply the 35-character `$ORIGIN` patch for them to work standalone in the app.

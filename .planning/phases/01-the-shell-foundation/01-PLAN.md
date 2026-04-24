# Phase 01 Plan: The Shell Foundation

## Objective
Establish the native PTY bridge and integrate a functional Terminal UI component into the Jetpack Compose app.

## Requirements Addressed
- [ ] Native Editor UI (Foundation)
- [ ] Integrated Terminal
- [ ] PTY Support

## Waves

### Wave 1: The Native Bridge (NDK/JNI)
This wave sets up the low-level communication between Android and the shell process.

#### 1-01-01: Set up NDK Build [W1]
- **Objective**: Configure CMake and NDK for the project.
- **Action**: 
    - Create `app/src/main/cpp/CMakeLists.txt`.
    - Create `app/src/main/cpp/native-lib.cpp` with JNI stubs.
    - Update `app/build.gradle.kts` to include `externalNativeBuild`.
- **Read First**: `app/build.gradle.kts`
- **Acceptance Criteria**: 
    - `app/src/main/cpp/CMakeLists.txt` exists.
    - `./gradlew assembleDebug` completes without NDK errors.

#### 1-01-02: Implement forkpty JNI wrapper [W1]
- **Objective**: Implement the native logic to spawn a shell process.
- **Action**:
    - Add `openpty` and `fork` logic to `native-lib.cpp`.
    - Expose `createPty` JNI function.
- **Read First**: `app/src/main/cpp/native-lib.cpp`
- **Acceptance Criteria**: 
    - `native-lib.cpp` contains `JNIEXPORT jint JNICALL Java_com_zohaib_codeossandriod_PtyBridge_createPty`.

### Wave 2: The Terminal UI
This wave connects the native bridge to the Compose UI.

#### 1-02-01: Implement PtyBridge Kotlin class [W2]
- **Objective**: Create the Kotlin wrapper for the JNI functions.
- **Action**:
    - Create `app/src/main/kotlin/com/zohaib/codeossandriod/PtyBridge.kt`.
    - Handle IO streams from the PTY master FD.
- **Read First**: `app/src/main/kotlin/com/zohaib/codeossandriod/MainActivity.kt`
- **Acceptance Criteria**: 
    - `PtyBridge.kt` exists and loads `native-lib`.

#### 1-02-02: Integrate Terminal UI with PTY [W2]
- **Objective**: Update the Compose UI to show real PTY output.
- **Action**:
    - Update `MainActivity.kt` to use `PtyBridge`.
    - Implement a basic buffer to store terminal output.
- **Read First**: `app/src/main/kotlin/com/zohaib/codeossandriod/MainActivity.kt`
- **Acceptance Criteria**: 
    - App starts and the terminal shows output from the system shell.

## Verification
- Run `./gradlew testDebugUnitTest` to verify JNI bridge (if tests added).
- Manual: Launch app and verify shell interactivity.

#include <jni.h>
#include <string>
#include <unistd.h>
#include <pty.h>
#include <sys/wait.h>
#include <android/log.h>

#define LOG_TAG "CodeOSS-Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jint JNICALL
Java_com_zohaib_codeossandriod_PtyBridge_createPty(JNIEnv *env, jobject thiz, jstring shell, jint rows, jint cols) {
    int master_fd;
    pid_t pid;

    const char *shell_path = env->GetStringUTFChars(shell, nullptr);

    pid = forkpty(&master_fd, nullptr, nullptr, nullptr);

    if (pid < 0) {
        LOGE("Failed to forkpty");
        env->ReleaseStringUTFChars(shell, shell_path);
        return -1;
    }

    if (pid == 0) {
        // Child process
        // Set environment variables if needed
        setenv("TERM", "xterm-256color", 1);
        setenv("HOME", "/data/data/com.zohaib.codeossandriod/files", 1);
        
        execl(shell_path, shell_path, nullptr);
        
        // If execl returns, it failed
        LOGE("Failed to exec shell: %s", shell_path);
        exit(1);
    }

    // Parent process
    LOGI("Spawned shell PID: %d, Master FD: %d", pid, master_fd);
    
    env->ReleaseStringUTFChars(shell, shell_path);
    return master_fd;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zohaib_codeossandriod_PtyBridge_setWindowSize(JNIEnv *env, jobject thiz, jint fd, jint rows, jint cols) {
    struct winsize ws;
    ws.ws_row = rows;
    ws.ws_col = cols;
    ws.ws_xpixel = 0;
    ws.ws_ypixel = 0;
    ioctl(fd, TIOCSWINSZ, &ws);
}

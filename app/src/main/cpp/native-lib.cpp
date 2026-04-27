#include <jni.h>
#include <string>
#include <unistd.h>
#include <pty.h>
#include <sys/wait.h>
#include <android/log.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <string.h>
#include <git2.h>
#include <vector>

#define TAG "GitBridge"

static int cred_acquire_cb(git_credential **out,
                            const char *url,
                            const char *username,
                            unsigned int allowed,
                            void *payload) {
    const char **creds = (const char **)payload;
    // creds[0] is username, creds[1] is token/password
    return git_credential_userpass_plaintext_new(out, creds[0], creds[1]);
}

static int cert_check_cb(git_cert *cert, int valid, const char *host, void *payload) {
    // 0 means unconditionally allow the connection, bypassing SSL validation errors.
    return 0;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_codeossandroid_bridge_GitBridge_cloneRepo(
        JNIEnv *env,
        jobject,
        jstring url,
        jstring localPath,
        jstring username,
        jstring token) {

    git_libgit2_init();

    const char *c_url = env->GetStringUTFChars(url, nullptr);
    const char *c_path = env->GetStringUTFChars(localPath, nullptr);
    const char *c_user = env->GetStringUTFChars(username, nullptr);
    const char *c_token = env->GetStringUTFChars(token, nullptr);

    const char *creds[2] = {c_user, c_token};

    git_clone_options opts = GIT_CLONE_OPTIONS_INIT;
    opts.fetch_opts.callbacks.credentials = cred_acquire_cb;
    opts.fetch_opts.callbacks.certificate_check = cert_check_cb;
    opts.fetch_opts.callbacks.payload = creds;

    git_repository *repo = nullptr;
    int error = git_clone(&repo, c_url, c_path, &opts);

    std::string result;
    if (error < 0) {
        const git_error *e = git_error_last();
        result = std::string("ERROR: ") + (e ? e->message : "unknown");
        __android_log_print(ANDROID_LOG_ERROR, TAG, "libgit2 clone failed: %s", result.c_str());
    } else {
        result = "SUCCESS";
        git_repository_free(repo);
    }

    env->ReleaseStringUTFChars(url, c_url);
    env->ReleaseStringUTFChars(localPath, c_path);
    env->ReleaseStringUTFChars(username, c_user);
    env->ReleaseStringUTFChars(token, c_token);

    git_libgit2_shutdown();
    return env->NewStringUTF(result.c_str());
}

#define LOG_TAG "CodeOSS-Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Hooking to bypass Android 10+ W^X for Git helpers
// These will be used when LD_PRELOADed into git or node
extern "C" {

typedef int (*execve_t)(const char *, char *const [], char *const []);
typedef int (*access_t)(const char *, int);

static execve_t real_execve = nullptr;
static access_t real_access = nullptr;

__attribute__((constructor))
static void init() {
    LOGI("Native hooks library loaded!");
}

static const char* resolve_helper(const char* path) {
    if (strstr(path, "git-remote-https")) return "libgit-remote-https.so";
    if (strstr(path, "git-remote-http")) return "libgit-remote-http.so";
    if (strstr(path, "git-remote-ftp")) return "libgit-remote-ftp.so";
    if (strstr(path, "git-remote-ftps")) return "libgit-remote-ftps.so";
    
    // Also handle node and git binaries for subshells
    const char* last_slash = strrchr(path, '/');
    const char* basename = last_slash ? last_slash + 1 : path;
    
    if (strcmp(basename, "node") == 0) return "libnode.so";
    if (strcmp(basename, "git") == 0) return "libgit.so";
    
    return nullptr;
}

int access(const char *path, int mode) {
    if (!real_access) real_access = (access_t)dlsym(RTLD_NEXT, "access");
    if (resolve_helper(path)) {
        return 0; // Always pretend Git helpers exist and are accessible
    }
    return real_access(path, mode);
}

int execve(const char *filename, char *const argv[], char *const envp[]) {
    if (!real_execve) real_execve = (execve_t)dlsym(RTLD_NEXT, "execve");
    
    const char* helper = resolve_helper(filename);
    if (helper) {
        const char* lib_dir = getenv("APP_LIB_DIR");
        if (lib_dir) {
            static char actual_path[1024];
            snprintf(actual_path, sizeof(actual_path), "%s/%s", lib_dir, helper);
            LOGI("Redirecting execve: %s -> %s", filename, actual_path);
            return real_execve(actual_path, argv, envp);
        }
    }
    
    return real_execve(filename, argv, envp);
}

// Global DNS Interceptor
typedef int (*getaddrinfo_t)(const char *node, const char *service, const struct addrinfo *hints, struct addrinfo **res);
static getaddrinfo_t real_getaddrinfo = nullptr;

int getaddrinfo(const char *node, const char *service, const struct addrinfo *hints, struct addrinfo **res) {
    if (!real_getaddrinfo) real_getaddrinfo = (getaddrinfo_t)dlsym(RTLD_NEXT, "getaddrinfo");
    
    if (node && (strcmp(node, "registry.npmjs.org") == 0 || strcmp(node, "github.com") == 0)) {
        LOGI("Intercepting DNS for %s", node);
    }
    
    return real_getaddrinfo(node, service, hints, res);
}

// File Open Interceptor for OpenSSL Shadowing
typedef int (*open_t)(const char *pathname, int flags, ...);
static open_t real_open = nullptr;

int open(const char *pathname, int flags, ...) {
    if (!real_open) real_open = (open_t)dlsym(RTLD_NEXT, "open");
    
    const char* final_path = pathname;
    
    // Redirect OpenSSL requests to our isolated versions to avoid system conflicts
    if (pathname && (strstr(pathname, "libcrypto.so") || strstr(pathname, "libssl.so"))) {
        const char* lib_dir = getenv("LD_LIBRARY_PATH"); // We store our libs here
        if (lib_dir) {
            static char redirect_path[1024];
            const char* target = strstr(pathname, "libcrypto.so") ? "libc3.so" : "libs3.so";
            // Extract the first directory from LD_LIBRARY_PATH (which should be our files/lib)
            char first_dir[512];
            sscanf(lib_dir, "%511[^:]", first_dir);
            snprintf(redirect_path, sizeof(redirect_path), "%s/%s", first_dir, target);
            LOGI("Redirecting open: %s -> %s", pathname, redirect_path);
            final_path = redirect_path;
        }
    }

    va_list args;
    va_start(args, flags);
    mode_t mode = va_arg(args, mode_t);
    va_end(args);
    return real_open(final_path, flags, mode);
}

}

extern "C"
JNIEXPORT jint JNICALL
Java_com_codeossandroid_bridge_PtyBridge_createPty(JNIEnv *env, jobject thiz, jstring shell, jstring bin_path, jstring lib_path, jstring home_path, jint rows, jint cols) {
    int master_fd;
    pid_t pid;

    const char *shell_path_raw = env->GetStringUTFChars(shell, nullptr);
    const char *bin_dir_raw = env->GetStringUTFChars(bin_path, nullptr);
    const char *lib_dir_raw = env->GetStringUTFChars(lib_path, nullptr);
    const char *home_dir_raw = env->GetStringUTFChars(home_path, nullptr);

    std::string s_shell(shell_path_raw);
    std::string s_bin(bin_dir_raw);
    std::string s_lib(lib_dir_raw);
    std::string s_home(home_dir_raw);

    env->ReleaseStringUTFChars(shell, shell_path_raw);
    env->ReleaseStringUTFChars(bin_path, bin_dir_raw);
    env->ReleaseStringUTFChars(lib_path, lib_dir_raw);
    env->ReleaseStringUTFChars(home_path, home_dir_raw);

    pid = forkpty(&master_fd, nullptr, nullptr, nullptr);

    if (pid < 0) {
        LOGE("Failed to forkpty");
        return -1;
    }

    if (pid == 0) {
        // Child process
        chdir(s_home.c_str());
        
        // Set basic environment.
        std::string bin_subdir = s_bin + "/bin";
        std::string git_exec_dir = s_bin + "/usr/git-exec";
        std::string new_path = bin_subdir + ":" + git_exec_dir + ":/system/bin:/system/xbin:/vendor/bin";
        setenv("PATH", new_path.c_str(), 1);
        
        setenv("TERM", "xterm-256color", 1);
        setenv("HOME", s_home.c_str(), 1);
        setenv("APP_LIB_DIR", s_lib.c_str(), 1);
        setenv("GIT_EXEC_PATH", s_lib.c_str(), 1);
        
        std::string env_path = s_bin + "/init.sh";
        setenv("ENV", env_path.c_str(), 1);
        
        execl(s_shell.c_str(), s_shell.c_str(), "-i", "-l", nullptr);
        
        LOGE("Failed to exec shell: %s", s_shell.c_str());
        _exit(1); // Use _exit in child after fork
    }

    LOGI("Spawned shell PID: %d, Master FD: %d", pid, master_fd);
    return master_fd;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_codeossandroid_bridge_PtyBridge_setWindowSize(JNIEnv *env, jobject thiz, jint fd, jint rows, jint cols) {
    struct winsize ws;
    ws.ws_row = rows;
    ws.ws_col = cols;
    ws.ws_xpixel = 0;
    ws.ws_ypixel = 0;
    ioctl(fd, TIOCSWINSZ, &ws);
}

static const JNINativeMethod pty_methods[] = {
    {"createPty", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)I", (void*)Java_com_codeossandroid_bridge_PtyBridge_createPty},
    {"setWindowSize", "(III)V", (void*)Java_com_codeossandroid_bridge_PtyBridge_setWindowSize}
};

static const JNINativeMethod git_methods[] = {
    {"cloneRepo", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (void*)Java_com_codeossandroid_bridge_GitBridge_cloneRepo}
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass ptyClass = env->FindClass("com/codeossandroid/bridge/PtyBridge");
    if (ptyClass) {
        env->RegisterNatives(ptyClass, pty_methods, sizeof(pty_methods) / sizeof(pty_methods[0]));
        env->DeleteLocalRef(ptyClass);
    }

    jclass gitClass = env->FindClass("com/codeossandroid/bridge/GitBridge");
    if (gitClass) {
        env->RegisterNatives(gitClass, git_methods, sizeof(git_methods) / sizeof(git_methods[0]));
        env->DeleteLocalRef(gitClass);
    }

    return JNI_VERSION_1_6;
}

package com.codeossandroid.bridge

import android.os.Build
import android.system.Os
import android.util.Log
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Field

class PtyBridge {
    private var masterFd: Int = -1
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null
    
    fun getFd(): Int = masterFd

    companion object {
        // NativeLibLoader.init() is called in MainActivity.onCreate
    }

    external fun createPty(shell: String, binPath: String, libPath: String, homePath: String, rows: Int, cols: Int): Int
    external fun setWindowSize(fd: Int, rows: Int, cols: Int)

    fun setupEnvironment(context: android.content.Context) {
        val binFile = java.io.File(context.filesDir, "bin")
        if (binFile.exists()) binFile.deleteRecursively()
        binFile.mkdirs()

        val libPath = context.applicationInfo.nativeLibraryDir
        setupSymlinks(context, binFile.absolutePath, libPath)
    }

    fun startShell(context: android.content.Context, shell: String = "/system/bin/sh", homeDir: String? = null, rows: Int = 24, cols: Int = 80) {
        setupEnvironment(context)
        
        val actualHome = homeDir ?: context.filesDir.absolutePath
        val libPath = context.applicationInfo.nativeLibraryDir
        masterFd = createPty(shell, context.filesDir.absolutePath, libPath, actualHome, rows, cols)
        
        if (masterFd != -1) {
            val fd = FileDescriptor()
            try {
                val field: Field = FileDescriptor::class.java.getDeclaredField("descriptor")
                field.isAccessible = true
                field.setInt(fd, masterFd)
                
                inputStream = FileInputStream(fd)
                outputStream = FileOutputStream(fd)
            } catch (e: Exception) {
                Log.e("PtyBridge", "Failed to create streams", e)
            }
        }
    }

    private fun extractGitTemplatesIfNeeded(context: android.content.Context) {
        val templatesDir = java.io.File(context.filesDir, "git_templates")
        val stampFile = java.io.File(templatesDir, ".extracted_v1")
        if (stampFile.exists()) return
        if (templatesDir.exists()) templatesDir.deleteRecursively()
        templatesDir.mkdirs()
        try {
            java.util.zip.ZipInputStream(context.assets.open("git-templates.zip")).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name.replace('\\', '/')
                    val file = java.io.File(templatesDir, entryName)
                    if (entry.isDirectory || entryName.endsWith("/")) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        java.io.FileOutputStream(file).use { out ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                out.write(buffer, 0, len)
                            }
                        }
                    }
                    entry = zis.nextEntry
                }
            }
            stampFile.createNewFile()
        } catch (e: Exception) {
            Log.e("PtyBridge", "Failed to extract Git templates", e)
        }
    }

    private fun extractNpmIfNeeded(context: android.content.Context) {
        val npmDir = java.io.File(context.filesDir, "npm_pkg")
        val stampFile = java.io.File(npmDir, ".extracted_v3")
        if (stampFile.exists()) return
        if (npmDir.exists()) npmDir.deleteRecursively()
        npmDir.mkdirs()
        try {
            java.util.zip.ZipInputStream(context.assets.open("npm.zip")).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name.replace('\\', '/')
                    val file = java.io.File(npmDir, entryName)
                    if (entry.isDirectory || entryName.endsWith("/")) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        java.io.FileOutputStream(file).use { out ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                out.write(buffer, 0, len)
                            }
                        }
                    }
                    entry = zis.nextEntry
                }
            }
            stampFile.createNewFile()
        } catch (e: Exception) {
            Log.e("PtyBridge", "Failed to extract NPM", e)
        }
    }

    private fun extractTunnelScript(context: android.content.Context) {
        val outFile = java.io.File(context.filesDir, "tunnel.js")
        try {
            context.assets.open("tunnel/tunnel.js").use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            Log.e("PtyBridge", "Failed to extract tunnel.js", e)
        }
    }

    private fun extractNextSwc(context: android.content.Context) {
        val usrDir = java.io.File(context.filesDir, "usr")
        val libDir = java.io.File(usrDir, "lib")
        libDir.mkdirs()
        val outFile = java.io.File(libDir, "next-swc.android-arm64.node")
        if (outFile.exists() && outFile.length() > 0) return
        try {
            java.util.zip.ZipInputStream(context.assets.open("next-swc.zip")).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name.replace('\\', '/')
                    if (entryName == "next-swc.android-arm64.node") {
                        java.io.FileOutputStream(outFile).use { out ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                out.write(buffer, 0, len)
                            }
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e("PtyBridge", "Failed to extract next-swc", e)
        }
    }

    private fun setupSymlinks(context: android.content.Context, binPath: String, libPath: String) {
        extractNpmIfNeeded(context)
        extractGitTemplatesIfNeeded(context)
        extractTunnelScript(context)
        extractNextSwc(context)

        val filesDir   = context.filesDir.absolutePath
        val usrDir     = filesDir + "/usr"
        val usrBinDir  = usrDir + "/bin"
        val usrEtcDir  = usrDir + "/etc"
        val tmpDir     = filesDir + "/tmp"
        val cacheDir   = filesDir + "/cache"
        val libLinksDir = java.io.File(context.filesDir, "lib").absolutePath
        val nativeLibPath = context.applicationInfo.nativeLibraryDir
        
        java.io.File(usrBinDir).mkdirs()
        java.io.File(usrEtcDir).mkdirs()
        java.io.File(usrDir + "/lib").mkdirs()
        java.io.File(tmpDir).mkdirs()
        java.io.File(cacheDir).mkdirs()
        
        // Setup binary symlinks
        try {
            val ln = arrayOf("/system/bin/ln", "-sf")
            Runtime.getRuntime().exec(ln + arrayOf("$nativeLibPath/libgit.so", "$usrBinDir/git")).waitFor()
            Runtime.getRuntime().exec(ln + arrayOf("$nativeLibPath/libgit_remote_http.so", "$usrBinDir/git-remote-http")).waitFor()
            Runtime.getRuntime().exec(ln + arrayOf("$nativeLibPath/libgit_remote_http.so", "$usrBinDir/git-remote-https")).waitFor()
            Runtime.getRuntime().exec(ln + arrayOf("$nativeLibPath/libnode.so", "$usrBinDir/node")).waitFor()
        } catch (e: Exception) {
            Log.e("PtyBridge", "Failed to create symlinks", e)
        }

        // 1. DNS Configs
        try {
            val resolvConf = java.io.File(usrEtcDir, "resolv.conf")
            java.io.FileOutputStream(resolvConf).use { it.write("nameserver 8.8.8.8\nnameserver 8.8.4.4\nnameserver 1.1.1.1\n".toByteArray()) }

            val gitConfig = java.io.File(usrEtcDir, "gitconfig")
            if (!gitConfig.exists()) {
                java.io.FileOutputStream(gitConfig).use {
                    it.write("[core]\n\tfileMode = false\n\tautocrlf = false\n[safe]\n\tdirectory = *\n".toByteArray())
                }
            }
            
            val dnsOverride = java.io.File(usrEtcDir, "dns-override.js")
            val dnsContent = """
                const dns = require('dns');
                const net = require('net');
                const { Resolver } = dns;
                const promises = dns.promises;
                const resolver = new Resolver();
                resolver.setServers(['8.8.8.8', '8.8.4.4', '1.1.1.1']);

                
                const origLookup = dns.lookup;
                dns.lookup = function(hostname, options, callback) {
                  if (typeof options === 'function') { callback = options; options = {}; }
                  if (typeof options === 'number') { options = { family: options }; }
                  const family = (options && options.family) || 0;
                  const all = (options && options.all) || false;
                  
                  if (!hostname || hostname === 'localhost') {
                    const res = all ? [{address: '127.0.0.1', family: 4}] : '127.0.0.1';
                    return process.nextTick(() => callback(null, res, 4));
                  }
                  
                  if (net.isIP(hostname)) {
                    const ipFamily = net.isIPv4(hostname) ? 4 : 6;
                    const res = all ? [{address: hostname, family: ipFamily}] : hostname;
                    return process.nextTick(() => callback(null, res, ipFamily));
                  }
                  
                  const resolveMethod = (family === 6) ? 'resolve6' : 'resolve4';
                  resolver[resolveMethod](hostname, (err, addresses) => {
                    if (err || !addresses || addresses.length === 0) {
                      if (family === 0 && resolveMethod === 'resolve4') {
                        return resolver.resolve6(hostname, (err6, addresses6) => {
                          if (err6) return origLookup(hostname, options, callback);
                          finish(addresses6, 6);
                        });
                      }
                      return origLookup(hostname, options, callback);
                    }
                    finish(addresses, family === 6 ? 6 : 4);
                  });
                  
                  function finish(addresses, detectedFamily) {
                    if (all) {
                      const res = addresses.map(a => ({address: a, family: detectedFamily}));
                      return callback(null, res);
                    }
                    callback(null, addresses[0], detectedFamily);
                  }
                };
                
                if (promises && promises.lookup) {
                  promises.lookup = function(hostname, options) {
                    return new Promise((resolve, reject) => {
                      dns.lookup(hostname, options, (err, address, family) => {
                        if (err) return reject(err);
                        if (options && options.all) resolve(address);
                        else resolve({ address, family });
                      });
                    });
                  };
                }
                
                dns.resolve4 = (hostname, callback) => resolver.resolve4(hostname, callback);
                dns.resolve6 = (hostname, callback) => resolver.resolve6(hostname, callback);
                if (promises) {
                  promises.resolve4 = (hostname) => new Promise((resolve, reject) => resolver.resolve4(hostname, (err, addr) => err ? reject(err) : resolve(addr)));
                  promises.resolve6 = (hostname) => new Promise((resolve, reject) => resolver.resolve6(hostname, (err, addr) => err ? reject(err) : resolve(addr)));
                }
                
                // --- SWC / Next.js Android Native Fixes ---

                // Fix 1: Inject __NEXT_VERSION so SWC Rust layer gets a valid String, not undefined.
                // This is the PRIMARY cause of 'Failed to convert JavaScript value Undefined into rust type String'
                // (see next/dist/build/swc/index.js line: const nextVersion = process.env.__NEXT_VERSION)
                (function injectNextVersion() {
                    if (!process.env.__NEXT_VERSION) {
                        try {
                            // Walk up from cwd to find next/package.json
                            const fs = require('fs');
                            const path = require('path');
                            const cwd = process.cwd();
                            const candidates = [
                                path.join(cwd, 'node_modules/next/package.json'),
                                '/data/user/0/com.codeossandroid/files/projects/Spoton/node_modules/next/package.json'
                            ];
                            let ver = '16.1.6';
                            for (const p of candidates) {
                                try { ver = JSON.parse(fs.readFileSync(p,'utf8')).version || ver; break; } catch(e2) {}
                            }
                            process.env.__NEXT_VERSION = ver;
                        } catch(e) {
                            process.env.__NEXT_VERSION = '16.1.6';
                        }
                    }
                })();

                // Fix 1.5: Auto-patch next-swc binary if running in a Next.js project
                (function autoPatchNextSWC() {
                    try {
                        const fs = require('fs');
                        const path = require('path');
                        const cwd = process.cwd();
                        const dest = path.join(cwd, 'node_modules', '@next', 'swc-android-arm64', 'next-swc.android-arm64.node');
                        if (fs.existsSync(path.dirname(dest))) {
                            const src = '/data/user/0/com.codeossandroid/files/usr/lib/next-swc.android-arm64.node';
                            if (fs.existsSync(src)) {
                                const srcStat = fs.statSync(src);
                                let needsCopy = true;
                                if (fs.existsSync(dest)) {
                                    if (fs.statSync(dest).size === srcStat.size) needsCopy = false;
                                }
                                if (needsCopy) {
                                    fs.copyFileSync(src, dest);
                                    console.log("[CodeOSS] Automatically patched next-swc native binary for Turbopack support");
                                }
                            }
                        }
                    } catch(e) {}
                })();

                // Fix 2: Sanitize SWC options to prevent undefined String fields from crashing Rust (napi-rs)
                // Uses variadic args — scans ALL Buffer arguments regardless of position in the call.
                function sanitizeSwcOpts(obj) {
                    if (obj === null || typeof obj !== 'object') return obj;
                    if (Array.isArray(obj)) return obj.map(sanitizeSwcOpts);
                    const STR_FIELDS = new Set([
                        'cacheRoot','serverReferenceHashSalt','relativeFilePathFromRoot',
                        'filename','importPath','swcCacheDir','hashSalt',
                        'globalInjects','cacheKey','root'
                    ]);
                    const out = {};
                    for (const k of Object.keys(obj)) {
                        const v = obj[k];
                        if (v === undefined || v === null) {
                            out[k] = STR_FIELDS.has(k) ? '' : v;
                        } else {
                            out[k] = sanitizeSwcOpts(v);
                        }
                    }
                    return out;
                }

                function wrapSwcFn(val, target) {
                    return function() {
                        const args = Array.from(arguments);
                        // Sanitize every Buffer argument (opts can be at any position)
                        for (let i = 0; i < args.length; i++) {
                            if (Buffer.isBuffer(args[i])) {
                                try {
                                    const parsed = JSON.parse(args[i].toString());
                                    args[i] = Buffer.from(JSON.stringify(sanitizeSwcOpts(parsed)));
                                } catch(e2) {}
                            }
                        }
                        return val.apply(target, args);
                    };
                }

                // Fix 3: semver.default for Next.js compatibility (Node.js v25 CJS interop issue)
                try {
                    const semver = require('semver');
                    if (semver && typeof semver.satisfies === 'function' && !semver.default) {
                        semver.default = semver;
                    }
                } catch(e) {}

                const Module = require('module');
                const originalLoad = Module._load;
                const proxyCache = new WeakMap();
                const SWC_TRANSFORM_METHODS = new Set(['transform','transformSync','minify','minifySync','parse','parseSync']);
                Module._load = function(request, parent, isMain) {
                    const result = originalLoad.apply(this, arguments);

                    // Intercept .node native binary loads AND the SWC JS index wrapper
                    const isNativeBinary = (typeof request === 'string') && (
                        request.endsWith('.node') ||
                        (request.includes('@next/swc') && !request.includes('loader')) ||
                        (request.includes('next-swc') && request.endsWith('.node')) ||
                        (request.includes('turbopack') && request.endsWith('.node'))
                    );

                    if (isNativeBinary && result && (typeof result === 'object' || typeof result === 'function')) {
                        if (proxyCache.has(result)) return proxyCache.get(result);
                        try {
                            const proxy = new Proxy(result, {
                                get(target, prop) {
                                    if (prop in target) {
                                        const val = target[prop];
                                        if (typeof val === 'function') {
                                            if (SWC_TRANSFORM_METHODS.has(prop)) {
                                                return wrapSwcFn(val, target);
                                            }
                                            return val.bind(target);
                                        }
                                        return val;
                                    }
                                    if (typeof prop === 'string') {
                                        if (prop === 'lockfileTryAcquireSync' || prop === 'lockfileReleaseSync' || prop === 'lockfileUnlockSync') return () => true;
                                        if (prop === 'expandNextJsTemplate') return () => '';
                                        if (prop === 'projectNew' || prop === 'projectUpdate') return () => ({});
                                        if (prop.startsWith('project')) return () => ({});
                                        return (...args) => prop.includes('Sync') ? true : '';
                                    }
                                    return target[prop];
                                },
                                getOwnPropertyDescriptor(target, prop) {
                                    const desc = Reflect.getOwnPropertyDescriptor(target, prop);
                                    if (desc) return desc;
                                    return { configurable: true, enumerable: true, value: () => {}, writable: true };
                                }
                            });
                            proxyCache.set(result, proxy);
                            return proxy;
                        } catch (e) { /* ignore proxy errors */ }
                    }
                    return result;
                };
            """.trimIndent()
            java.io.FileOutputStream(dnsOverride).use { it.write(dnsContent.toByteArray()) }
        } catch (e: Exception) {
            Log.e("PtyBridge", "Failed to create configs", e)
        }

        // 2. Generate init.sh
        val initScript = """
            export HOME="$filesDir"
            export TMPDIR="$tmpDir"
            export PREFIX="$usrDir"
            export PATH="$usrBinDir:/system/bin:/system/xbin"
            export OPENSSL_CONF="/dev/null"
            export RESOLV_CONF="$usrEtcDir/resolv.conf"
            export GIT_SSL_NO_VERIFY=true
            export GIT_TEMPLATE_DIR="$filesDir/git_templates"
            export GIT_CONFIG_NOSYSTEM=1
            export GIT_CONFIG_GLOBAL="$usrEtcDir/gitconfig"
            export GIT_EXEC_PATH="$nativeLibPath"
            export NATIVE_LIB_PATH="$nativeLibPath"
            
            # Force Git to use HTTPS instead of SSH (Fixes Permission Denied errors)
            git config --global url."https://github.com/".insteadOf "git@github.com:"
            git config --global url."https://".insteadOf "ssh://"
            git config --global core.autocrlf false
            
            # Vite & Node Stability + Universal DNS Monkey-Patch
            export CHOKIDAR_USEPOLLING=1
            export WATCHPACK_POLLING=true
            export NODE_OPTIONS="--require $usrEtcDir/dns-override.js --preserve-symlinks --preserve-symlinks-main"
            export NODE_PATH=".:${'$'}PWD/node_modules:$usrDir/lib/node_modules"
            export NEXT_TELEMETRY_DISABLED=1
            
            # Remove existing symlinks if they exist so we don't try to overwrite read-only files
            rm -f "$usrBinDir/node" "$usrBinDir/git" "$usrBinDir/git-remote-http" "$usrBinDir/git-remote-https"
            
            # Use shell wrappers instead of symlinks to force LD_LIBRARY_PATH and bypass linker namespace restrictions
            cat << 'WRAPPER' > "$usrBinDir/node"
            #!/system/bin/sh
            export LD_LIBRARY_PATH="$nativeLibPath:$libLinksDir"
            exec "$nativeLibPath/libnode.so" "${'$'}@"
            WRAPPER
            chmod 755 "$usrBinDir/node"

            cat << 'WRAPPER' > "$usrBinDir/git"
            #!/system/bin/sh
            export LD_LIBRARY_PATH="$nativeLibPath:$libLinksDir"
            exec "$nativeLibPath/libgit.so" "${'$'}@"
            WRAPPER
            chmod 755 "$usrBinDir/git"

            cat << 'WRAPPER' > "$usrBinDir/git-remote-http"
            #!/system/bin/sh
            export LD_LIBRARY_PATH="$nativeLibPath:$libLinksDir"
            exec "$nativeLibPath/libgit_remote_http.so" "${'$'}@"
            WRAPPER
            chmod 755 "$usrBinDir/git-remote-http"

            cat << 'WRAPPER' > "$usrBinDir/git-remote-https"
            #!/system/bin/sh
            export LD_LIBRARY_PATH="$nativeLibPath:$libLinksDir"
            exec "$nativeLibPath/libgit_remote_http.so" "${'$'}@"
            WRAPPER
            chmod 755 "$usrBinDir/git-remote-https"
            
            # THE SMART SHELL FIX: Mock /system/bin/sh behavior
            cat << 'EOF' > "$usrBinDir/sh"
            #!/system/bin/sh
            if [ "$1" = "-c" ]; then
                target_cmd=$(echo "$2" | awk '{print ${'$'}1}')
                full_path=$(PATH="$usrBinDir:${'$'}PATH" command -v "${'$'}target_cmd")
                if [ -n "${'$'}full_path" ] && [ -f "${'$'}full_path" ]; then
                    first_line=$(head -n 1 "${'$'}full_path")
                    if echo "${'$'}first_line" | grep -qE "env node|bin/node"; then
                        shift 2
                        remaining_args=$(echo "$2" | cut -d' ' -f2-)
                        if [ "${'$'}remaining_args" = "${'$'}target_cmd" ]; then remaining_args=""; fi
                        exec "$usrBinDir/node" "${'$'}full_path" ${'$'}remaining_args
                    fi
                fi
            fi
            exec /system/bin/sh "${'$'}@"
            EOF
            chmod 755 "$usrBinDir/sh"

            export PATH="$usrBinDir:${'$'}PATH"
            export LD_LIBRARY_PATH="$nativeLibPath:$libLinksDir"
            export SHELL="$usrBinDir/sh"
            export NPM_CONFIG_SHELL="$usrBinDir/sh"
            
            # NPM Stability tweaks
            export NPM_CONFIG_MAXSOCKETS=2
            export NPM_CONFIG_REGISTRY="https://registry.npmjs.org/"
            
            node() { "$usrBinDir/node" "$@"; }
            git() { "$usrBinDir/git" "$@"; }
            
            # The "W^X" and Shebang fix: 
            _npm_fix() {
                if [ -d "node_modules" ]; then
                    echo "Applying Android Native Fixes..."
                    # Fix EACCES by making binaries read-only
                    find node_modules -type f -name "*" -executable -exec chmod 555 {} + 2>/dev/null || true
                    
                    # Specific fix for esbuild (Vite)
                    if [ -d "node_modules/esbuild" ]; then
                        find node_modules/esbuild -name "esbuild" -exec chmod 555 {} + 2>/dev/null || true
                    fi

                    # Specific fix for SWC (Next.js)
                    # Next.js on Android detects platform as "android/arm64" (not linux/arm64)
                    # so it looks for @next/swc-android-arm64 package first.
                    # Specific fix for SWC (Next.js)
                    if [ -d "node_modules/@next" ]; then
                        # Read the actual Next.js version robustly (disable NODE_OPTIONS so dns patch doesn't spam stdout)
                        NEXT_VER=$( env NODE_OPTIONS="" "$usrBinDir/node" -e "try{process.stdout.write(require('./node_modules/next/package.json').version)}catch(e){try{process.stdout.write(require('next/package.json').version)}catch(ee){process.stdout.write('16.1.6')}}" 2>/dev/null )

                        # Link ALL available SWC bindings
                        SWC_ANDROID_DIR="node_modules/@next/swc-android-arm64"
                        mkdir -p "${'$'}SWC_ANDROID_DIR"
                        
                        echo "Linking Native SWC Binaries for Next.js ${'$'}NEXT_VER..."
                        for lib in "${'$'}NATIVE_LIB_PATH"/libbinding_*.so; do
                            [ -f "${'$'}lib" ] || continue
                            libname=$(basename "${'$'}lib" .so | sed 's/lib//')
                            
                            case "${'$'}libname" in
                                "binding_core_node") target="next-swc.android-arm64.node" ;;
                                "binding_html_node") target="next-swc.android-arm64-html.node" ;;
                                "binding_minifier_node") target="next-swc.android-arm64-minifier.node" ;;
                                "binding_react_compiler_node") target="next-swc.android-arm64-react-compiler.node" ;;
                                *) target="next-swc.android-arm64-${'$'}libname.node" ;;
                            esac
                            
                            ln -sf "${'$'}lib" "${'$'}SWC_ANDROID_DIR/${'$'}target"
                        done
                        
                        printf '{"name":"@next/swc-android-arm64","version":"%s","main":"next-swc.android-arm64.node"}' "${'$'}NEXT_VER" > "${'$'}SWC_ANDROID_DIR/package.json"

                        # Fallback for older Next.js versions that look for linux-arm64-gnu
                        SWC_LINUX_DIR="node_modules/@next/swc-linux-arm64-gnu"
                        mkdir -p "${'$'}SWC_LINUX_DIR"
                        [ -f "${'$'}NATIVE_LIB_PATH/libbinding_core_node.so" ] && ln -sf "${'$'}NATIVE_LIB_PATH/libbinding_core_node.so" "${'$'}SWC_LINUX_DIR/next-swc.linux-arm64-gnu.node"
                    fi

                    # Repair broken Next.js wrappers from previous sessions
                    if [ -d "node_modules/next" ]; then
                        rm -f "node_modules/swc-patch.js"
                        mkdir -p "node_modules/.bin"
                        rm -f "node_modules/.bin/next"
                        cat << 'NEXTWRAPPER' > "node_modules/.bin/next"
            #!/system/bin/sh
            has_dev_or_build=false
            for arg in "${'$'}@"; do
                if [ "${'$'}arg" = "dev" ] || [ "${'$'}arg" = "build" ]; then
                    has_dev_or_build=true
                    break
                fi
            done

            # Android Environment Fixes
            export NEXT_TELEMETRY_DISABLED=1
            export CHOKIDAR_USEPOLLING=true
            export WATCHPACK_POLLING=true
            export WATCHPACK_POLLING_INTERVAL=500
            # Keep SWC enabled (native binary patched via dns-override.js proxy)
            # __NEXT_VERSION is injected by dns-override.js before Next.js loads

            if [ "${'$'}has_dev_or_build" = "true" ]; then
                # Clear corrupted webpack cache before every dev/build start
                rm -rf "${'$'}PWD/.next/cache"
                exec "$usrBinDir/node" "${'$'}PWD/node_modules/next/dist/bin/next" "${'$'}@" --webpack
            else
                exec "$usrBinDir/node" "${'$'}PWD/node_modules/next/dist/bin/next" "${'$'}@"
            fi
            NEXTWRAPPER
                        chmod 755 "node_modules/.bin/next"
                    fi
                fi
            }

            npm() { 
                node "$filesDir/npm_pkg/bin/npm-cli.js" "$@"
                _npm_fix
            }
            npx() { 
                node "$filesDir/npm_pkg/bin/npx-cli.js" "$@"
                _npm_fix
            }
            
            alias clear="printf '\033[2J\033[H'"
            cd() { 
                builtin cd "$@" && export NODE_PATH=".:${'$'}PWD/node_modules:$usrDir/lib/node_modules"
            }
            
            # SWC Native Support (Enabled)
            # export NEXT_SWC_LOAD_WASM=1
            # export NEXT_IGNORE_NATIVE_SWC=1
            
            # Setup DNS
            echo "nameserver 8.8.8.8" > "${'$'}RESOLV_CONF"
            echo "nameserver 1.1.1.1" >> "${'$'}RESOLV_CONF"

            # Auto-apply fixes if node_modules already exists
            [ -d "node_modules" ] && _npm_fix

        """.trimIndent()

        try {
            java.io.File(filesDir, "init.sh").writeText(initScript)
        } catch (e: Exception) {
            Log.e("PtyBridge", "Failed to create init.sh", e)
        }
    }

    fun write(data: String) {
        try {
            outputStream?.write(data.toByteArray())
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e("PtyBridge", "Write failed", e)
        }
    }

    fun read(buffer: ByteArray): Int {
        return try {
            inputStream?.read(buffer) ?: -1
        } catch (e: IOException) {
            Log.e("PtyBridge", "Read failed", e)
            -1
        }
    }

    fun resize(rows: Int, cols: Int) {
        if (masterFd != -1) {
            setWindowSize(masterFd, rows, cols)
        }
    }
}

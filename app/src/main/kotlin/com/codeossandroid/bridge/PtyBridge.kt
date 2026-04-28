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

    private fun setupSymlinks(context: android.content.Context, binPath: String, libPath: String) {
        extractNpmIfNeeded(context)
        extractGitTemplatesIfNeeded(context)
        extractTunnelScript(context)

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
                const resolver = new Resolver();
                resolver.setServers(['8.8.8.8', '8.8.4.4', '1.1.1.1']);
                const origLookup = dns.lookup;
                dns.lookup = function(hostname, options, callback) {
                  if (typeof options === 'function') { callback = options; options = {}; }
                  if (typeof options === 'number') { options = { family: options }; }
                  const family = (options && options.family) || 0;
                  const all = (options && options.all) || false;
                  if (net.isIP(hostname)) {
                    const ipFamily = net.isIPv4(hostname) ? 4 : 6;
                    const res = all ? [{address: hostname, family: ipFamily}] : hostname;
                    return process.nextTick(() => callback(null, res, ipFamily));
                  }
                  const resolve = (family === 6) ? 'resolve6' : 'resolve4';
                  resolver[resolve](hostname, (err, addresses) => {
                    if (err || !addresses || addresses.length === 0) {
                      if (all) return callback(err || new Error('ENOTFOUND'), []);
                      return callback(err || new Error('ENOTFOUND'), null, 0);
                    }
                    if (all) {
                      const res = addresses.map(a => ({address: a, family: (family === 6 ? 6 : 4)}));
                      return callback(null, res);
                    }
                    callback(null, addresses[0], (family === 6 ? 6 : 4));
                  });
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
            # export NODE_OPTIONS="--require $usrEtcDir/dns-override.js"
            # export ARES_SERVERS="8.8.8.8,8.8.4.4,1.1.1.1"
            export NATIVE_LIB_PATH="$nativeLibPath"
            
            # Use symlinks instead of functions where possible for better sub-process support
            ln -sf "$nativeLibPath/libnode.so" "$usrBinDir/node"
            ln -sf "$nativeLibPath/libgit.so" "$usrBinDir/git"
            ln -sf "$nativeLibPath/libgit_remote_http.so" "$usrBinDir/git-remote-http"
            ln -sf "$nativeLibPath/libgit_remote_http.so" "$usrBinDir/git-remote-https"
            ln -sf "$nativeLibPath/libgit_remote_http.so" "$usrBinDir/git-remote-ftp"
            ln -sf "$nativeLibPath/libgit_remote_http.so" "$usrBinDir/git-remote-ftps"
            
            # THE SMART SHELL FIX: Mock /system/bin/sh behavior
            # We create a 'sh' in our PATH that intercepts all execution calls.
            # If a command has a broken node shebang, we fix it on the fly.
            cat << 'EOF' > "$usrBinDir/sh"
            #!/system/bin/sh
            
            if [ "$1" = "-c" ]; then
                # Extract the first word (the actual command/script)
                target_cmd=$(echo "$2" | awk '{print ${'$'}1}')
                
                # Use 'command -v' to find the full path of the command
                full_path=$(PATH="$usrBinDir:${'$'}PATH" command -v "${'$'}target_cmd")
                
                if [ -n "${'$'}full_path" ] && [ -f "${'$'}full_path" ]; then
                    # Peek at the shebang
                    first_line=$(head -n 1 "${'$'}full_path")
                    if echo "${'$'}first_line" | grep -qE "env node|bin/node"; then
                        # It's a Node script! Run it with our node.
                        shift 2
                        # We reconstruct the rest of the arguments if any
                        remaining_args=$(echo "$2" | cut -d' ' -f2-)
                        if [ "${'$'}remaining_args" = "${'$'}target_cmd" ]; then remaining_args=""; fi
                        exec "$usrBinDir/node" "${'$'}full_path" ${'$'}remaining_args
                    fi
                fi
            fi
            # Fallback to the real system shell
            exec /system/bin/sh "${'$'}@"
            EOF
            chmod 755 "$usrBinDir/sh"

            # Create the 'env' mock as well just in case
            cat << 'EOF' > "$usrBinDir/env"
            #!/system/bin/sh
            if [ "$1" = "node" ]; then
                shift
                exec "$usrBinDir/node" "$@"
            else
                exec "$@"
            fi
            EOF
            chmod 755 "$usrBinDir/env"

            export PATH="$usrBinDir:${'$'}PATH"
            export LD_LIBRARY_PATH="$nativeLibPath:$libLinksDir"
            
            # Force npm to use our smart shell
            export SHELL="$usrBinDir/sh"
            export NPM_CONFIG_SHELL="$usrBinDir/sh"
            
            # NPM Stability tweaks
            export NPM_CONFIG_MAXSOCKETS=2
            export NPM_CONFIG_FETCH_RETRIES=5
            export NPM_CONFIG_FETCH_RETRY_MINTIMEOUT=15000
            export NPM_CONFIG_FETCH_RETRY_MAXTIMEOUT=60000
            export NPM_CONFIG_REGISTRY="https://registry.npmjs.org/"
            export NPM_CONFIG_SCRIPTS_PREPEND_NODE=true
            
            # Use symlinks instead of functions where possible for better sub-process support
            node() { "$usrBinDir/node" "$@"; }
            git() { "$usrBinDir/git" "$@"; }
            
            # The "W^X" and Shebang fix: 
            # 1. Makes binaries read-only (satisfies Android 10+ W^X policy)
            # 2. Replaces broken .bin symlinks with shell wrappers (fixes missing /usr/bin/env)
            _npm_fix() {
                if [ -d "node_modules" ]; then
                    echo "Applying Android Native Fixes..."
                    # Fix EACCES by making binaries read-only
                    find node_modules -type f -name "*" -executable -exec chmod 555 {} + 2>/dev/null || true
                    find node_modules -type f -path "*/bin/*" -exec chmod 555 {} + 2>/dev/null || true
                    
                    # Fix Shebangs by converting .bin symlinks to wrappers
                    if [ -d "node_modules/.bin" ]; then
                        for bin in node_modules/.bin/*; do
                            if [ -L "${'$'}bin" ]; then
                                target=${'$'}(readlink -f "${'$'}bin")
                                if head -n 1 "${'$'}target" | grep -q "node"; then
                                    rm "${'$'}bin"
                                    echo "#!/system/bin/sh" > "${'$'}bin"
                                    echo "exec \"$usrBinDir/node\" \"${'$'}target\" \"\$@\"" >> "${'$'}bin"
                                    chmod 755 "${'$'}bin"
                                fi
                            fi
                        done
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
            alias ll='ls -al'
            
            # Force Next.js to use WASM instead of native SWC
            export NEXT_SWC_LOAD_WASM=1
            export NEXT_IGNORE_NATIVE_SWC=1
            
            # Setup DNS (Google and Cloudflare)
            echo "options no-aaaa" > "${'$'}RESOLV_CONF"
            echo "nameserver 8.8.8.8" >> "${'$'}RESOLV_CONF"
            echo "nameserver 8.8.4.4" >> "${'$'}RESOLV_CONF"
            echo "nameserver 1.1.1.1" >> "${'$'}RESOLV_CONF"
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

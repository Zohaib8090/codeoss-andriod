package com.codeossandroid.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.codeossandroid.bridge.GitBridge
import com.codeossandroid.bridge.PtyBridge
import com.codeossandroid.bridge.ZipUtils
import com.codeossandroid.model.TerminalInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONObject
import org.json.JSONArray

data class IDEProblem(
    val file: java.io.File,
    val line: Int,
    val column: Int,
    val message: String,
    val severity: String // ERROR, WARNING
)

data class GitCommit(
    val hash: String,
    val message: String,
    val author: String,
    val date: String
)

data class GitChange(
    val path: String,
    val status: String, // M, A, D, ??
    val isStaged: Boolean
)

data class NpmPackage(
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val date: String
)

data class ForwardedPort(val port: Int, val url: String, val process: Process)

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val _instances = MutableStateFlow<List<TerminalInstance>>(emptyList())
    val instances = _instances.asStateFlow()
    private val _activeInstanceIndex = MutableStateFlow(0)
    val activeInstanceIndex = _activeInstanceIndex.asStateFlow()
    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()
    private val _isCtrlActive = MutableStateFlow(false)
    val isCtrlActive = _isCtrlActive.asStateFlow()

    private val _installingIds = MutableStateFlow<Set<String>>(emptySet())
    val installingIds = _installingIds.asStateFlow()

    private val _installingProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val installingProgress = _installingProgress.asStateFlow()

    fun toggleCtrl() { _isCtrlActive.value = !_isCtrlActive.value }
    fun setCtrl(active: Boolean) { _isCtrlActive.value = active }

    private val _isAltActive = MutableStateFlow(false)
    val isAltActive = _isAltActive.asStateFlow()
    fun toggleAlt() { _isAltActive.value = !_isAltActive.value }
    fun setAlt(active: Boolean) { _isAltActive.value = active }


    private val _setupStatus = MutableStateFlow("Initializing...")
    val setupStatus = _setupStatus.asStateFlow()

    private val prefs = getApplication<Application>().getSharedPreferences("codeoss_settings", android.content.Context.MODE_PRIVATE)

    private val _githubUser = MutableStateFlow(prefs.getString("github_user", null))
    val githubUser = _githubUser.asStateFlow()
    private val _githubToken = MutableStateFlow(prefs.getString("github_token", null))
    val githubToken = _githubToken.asStateFlow()

    fun loginGithub() {
        val clientId = "Ov23liGDwcWLayi70rk2"
        val redirectUri = "codeoss://github-auth"
        val url = "https://github.com/login/oauth/authorize?client_id=$clientId&scope=repo,user&redirect_uri=$redirectUri"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun handleGithubCallback(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _setupStatus.value = "Exchanging GitHub code..."
                val clientId = "Ov23liGDwcWLayi70rk2"
                val clientSecret = "961d371f7bd737f4d3de71f13f6b9dfebfed118c"
                val url = java.net.URL("https://github.com/login/oauth/access_token")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true
                val params = "client_id=$clientId&client_secret=$clientSecret&code=$code"
                conn.outputStream.write(params.toByteArray())
                val response = conn.inputStream.bufferedReader().readText()
                Log.d("CodeOSS", "OAuth Response: $response")
                val json = org.json.JSONObject(response)
                val token = json.optString("access_token")
                if (token.isNotEmpty()) {
                    val userUrl = java.net.URL("https://api.github.com/user")
                    val userConn = userUrl.openConnection() as java.net.HttpURLConnection
                    userConn.setRequestProperty("Authorization", "token $token")
                    userConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    val userResponse = userConn.inputStream.bufferedReader().readText()
                    Log.d("CodeOSS", "User Response: $userResponse")
                    val userJson = org.json.JSONObject(userResponse)
                    val login = userJson.getString("login")
                    withContext(Dispatchers.Main) {
                        saveGithubAuth(login, token)
                        android.widget.Toast.makeText(getApplication(), "Logged in as $login", android.widget.Toast.LENGTH_LONG).show()
                        _setupStatus.value = "Ready"
                    }
                } else {
                    val error = json.optString("error_description", "Unknown error")
                    Log.e("CodeOSS", "OAuth failed: $response")
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(getApplication(), "Login Failed: $error", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "GitHub callback failed", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            } finally {
                _setupStatus.value = "Ready"
            }
        }
    }

    fun logoutGithub() {
        _githubUser.value = null
        _githubToken.value = null
        prefs.edit().remove("github_user").remove("github_token").apply()
    }

    fun saveGithubAuth(user: String, token: String) {
        _githubUser.value = user
        _githubToken.value = token
        prefs.edit().putString("github_user", user).putString("github_token", token).apply()
    }

    private val _fontSize = MutableStateFlow(prefs.getInt("font_size", 12))
    val fontSize = _fontSize.asStateFlow()
    private val _uiScale = MutableStateFlow(prefs.getFloat("ui_scale", 1.0f))
    val uiScale = _uiScale.asStateFlow()
    private val _panelHeight = MutableStateFlow(250.dp)
    val panelHeight = _panelHeight.asStateFlow()

    private val _disabledExtensions = MutableStateFlow(prefs.getStringSet("disabled_extensions", emptySet()) ?: emptySet())
    val disabledExtensions = _disabledExtensions.asStateFlow()

    fun toggleExtensionEnabled(id: String, enabled: Boolean) {
        val current = _disabledExtensions.value.toMutableSet()
        if (enabled) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _disabledExtensions.value = current
        prefs.edit().putStringSet("disabled_extensions", current).apply()
    }

    fun isExtensionEnabled(id: String): Boolean {
        return !_disabledExtensions.value.contains(id)
    }
    private val _isPanelVisible = MutableStateFlow(true)
    val isPanelVisible = _isPanelVisible.asStateFlow()
    private val _autoScroll = MutableStateFlow(true)
    val autoScroll = _autoScroll.asStateFlow()

    // ── Project Management ────────────────────────────────────────────────────
    private val projectsRoot get() = java.io.File(getApplication<Application>().filesDir, "projects")
    private val _projects = MutableStateFlow<List<String>>(emptyList())
    val projects = _projects.asStateFlow()
    private val _activeProject = MutableStateFlow<String?>(prefs.getString("active_project", null))
    val activeProject = _activeProject.asStateFlow()
    private val _sidebarOpen = MutableStateFlow(false)
    val sidebarOpen = _sidebarOpen.asStateFlow()

    private val _fileTree = MutableStateFlow<Map<String, List<java.io.File>>>(emptyMap())
    val fileTree = _fileTree.asStateFlow()
    private val _expandedDirs = MutableStateFlow<Set<String>>(emptySet())
    val expandedDirs = _expandedDirs.asStateFlow()

    enum class SidebarMode { PROJECTS, EXPLORER, SEARCH, GIT, EXTENSIONS, MARKETPLACE, DEBUG, BROWSER, SETTINGS }
    private val _sidebarMode = MutableStateFlow(SidebarMode.EXPLORER)
    val sidebarMode = _sidebarMode.asStateFlow()

    enum class MarketMode { EXTENSIONS, PACKAGES }
    private val _marketMode = MutableStateFlow(MarketMode.EXTENSIONS)
    val marketMode = _marketMode.asStateFlow()

    fun setMarketMode(mode: MarketMode) { _marketMode.value = mode }

    private val _npmResults = MutableStateFlow<List<NpmPackage>>(emptyList())
    val npmResults = _npmResults.asStateFlow()

    private val _isNpmSearching = MutableStateFlow(false)
    val isNpmSearching = _isNpmSearching.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun openExternalBrowser(url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            Log.e("CodeOSS", "Failed to open external browser", e)
        }
    }

    private val _logcatOutput = MutableStateFlow<List<String>>(listOf("Initializing Debug Console..."))
    val logcatOutput = _logcatOutput.asStateFlow()

    private val _logcatFilter = MutableStateFlow("")
    val logcatFilter = _logcatFilter.asStateFlow()

    data class UpdateInfo(val version: String, val downloadUrl: String, val releaseNotes: String)
    private val _availableUpdate = MutableStateFlow<UpdateInfo?>(null)
    val availableUpdate = _availableUpdate.asStateFlow()

    fun updateLogcatFilter(query: String) {
        _logcatFilter.value = query
    }

    init {
        refreshProjects()
        startLogcatStream()
        checkUpdate()
    }

    private fun checkUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            var connection: java.net.HttpURLConnection? = null
            try {
                val context = getApplication<Application>()
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersion = packageInfo.versionName ?: "1.0"
                
                val url = java.net.URL("https://api.github.com/repos/Zohaib8090/codeoss-andriod/releases/latest")
                connection = url.openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.setRequestProperty("User-Agent", "CodeOSS-Android-App")
                
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val latestTag = json.getString("tag_name").replace("v", "")
                if (latestTag != currentVersion) {
                    val assets = json.getJSONArray("assets")
                    var downloadUrl = json.getString("html_url")
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.getString("name").endsWith(".apk")) {
                            downloadUrl = asset.getString("browser_download_url")
                            break
                        }
                    }
                    _availableUpdate.value = UpdateInfo(
                        version = latestTag,
                        downloadUrl = downloadUrl,
                        releaseNotes = json.optString("body", "No release notes provided.")
                    )
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Update check failed", e)
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun dismissUpdate() { _availableUpdate.value = null }

    fun appendLogcat(line: String) {
        val currentList = _logcatOutput.value.toMutableList()
        currentList.add(line)
        if (currentList.size > 500) currentList.removeAt(0)
        _logcatOutput.value = currentList
    }

    private var logcatProcess: Process? = null

    private fun startLogcatStream() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Runtime.getRuntime().exec("logcat -c").waitFor()
                val process = Runtime.getRuntime().exec("logcat -v time")
                logcatProcess = process
                val reader = process.inputStream.bufferedReader()
                var line: String?
                while (isActive) {
                    line = reader.readLine()
                    if (line != null) {
                        val currentList = _logcatOutput.value.toMutableList()
                        currentList.add(line)
                        if (currentList.size > 500) currentList.removeAt(0)
                        _logcatOutput.value = currentList
                    } else {
                        delay(500)
                    }
                }
            } catch (e: Exception) {
                _logcatOutput.value = _logcatOutput.value + "Logcat stream error: ${e.message}"
            } finally {
                logcatProcess?.destroy()
                logcatProcess = null
            }
        }
    }

    data class MarketplaceExtension(
        val name: String,
        val displayName: String,
        val publisher: String,
        val description: String,
        val version: String,
        val iconUrl: String?,
        val downloadCount: Int,
        val namespace: String,
        val versions: List<String> = emptyList()
    )

    private val _marketplaceExtensions = MutableStateFlow<List<MarketplaceExtension>>(emptyList())
    val marketplaceExtensions = _marketplaceExtensions.asStateFlow()
    private val _isSearchingExtensions = MutableStateFlow(false)
    val isSearchingExtensions = _isSearchingExtensions.asStateFlow()

    private val _activeExtensionDetail = MutableStateFlow<MarketplaceExtension?>(null)
    val activeExtensionDetail = _activeExtensionDetail.asStateFlow()

    fun setActiveExtensionDetail(extension: MarketplaceExtension?) {
        if (extension == null) {
            _activeExtensionDetail.value = null
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            var connection: java.net.HttpURLConnection? = null
            try {
                val url = java.net.URL("https://open-vsx.org/api/${extension.namespace}/${extension.name}")
                connection = url.openConnection() as java.net.HttpURLConnection
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(text)
                val versionsObj = json.optJSONObject("allVersions")
                val versionList = mutableListOf<String>()
                if (versionsObj != null) {
                    val keys = versionsObj.keys()
                    while (keys.hasNext()) {
                        versionList.add(keys.next())
                    }
                }
                
                withContext(Dispatchers.Main) {
                    _activeExtensionDetail.value = extension.copy(versions = versionList.sortedDescending())
                    _sidebarOpen.value = false
                    _activeGithubExtensionDetail.value = null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _activeExtensionDetail.value = extension
                    _sidebarOpen.value = false
                    _activeGithubExtensionDetail.value = null
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun searchExtensions(query: String) {
        if (query.isBlank()) {
            _marketplaceExtensions.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isSearchingExtensions.value = true
            var connection: java.net.HttpURLConnection? = null
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val url = java.net.URL("https://open-vsx.org/api/-/search?text=$encodedQuery&size=50")
                connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(text)
                val results = json.getJSONArray("extensions")
                val extensions = mutableListOf<MarketplaceExtension>()
                val lowerQuery = query.lowercase()
                for (i in 0 until results.length()) {
                    val ext = results.getJSONObject(i)
                    val dName = ext.optString("displayName", "").lowercase()
                    val mName = ext.optString("name", "").lowercase()
                    val desc = ext.optString("description", "").lowercase()
                    val pub = ext.optString("namespace", "").lowercase()
                    if (dName.contains(lowerQuery) || mName.contains(lowerQuery) ||
                        desc.contains(lowerQuery) || pub.contains(lowerQuery)) {
                        val files = ext.optJSONObject("files")
                        val iconUrl = files?.optString("icon", null)
                        extensions.add(MarketplaceExtension(
                            name = ext.optString("name", "unknown"),
                            displayName = ext.optString("displayName", ext.optString("name", "Unknown")),
                            publisher = ext.optString("namespace", "unknown"),
                            description = ext.optString("description", "No description"),
                            version = ext.optString("version", "0.0.1"),
                            iconUrl = iconUrl,
                            downloadCount = ext.optInt("downloadCount", 0),
                            namespace = ext.optString("namespace", "unknown")
                        ))
                    }
                }
                if (lowerQuery.contains("gemini")) {
                    extensions.sortByDescending { it.publisher.lowercase() == "google" }
                }
                _marketplaceExtensions.value = extensions
            } catch (e: Exception) {
                Log.e("CodeOSS", "Marketplace search failed", e)
            } finally {
                _isSearchingExtensions.value = false
                connection?.disconnect()
            }
        }
    }

    fun setSidebarMode(mode: SidebarMode) {
        _sidebarMode.value = mode
        if (mode == SidebarMode.MARKETPLACE) {
            scanMarketplace()
        }
        if (!_sidebarOpen.value) _sidebarOpen.value = true
        if (mode == SidebarMode.PROJECTS) refreshProjects()
        if (mode == SidebarMode.EXPLORER) _activeProject.value?.let { refreshFileTree(java.io.File(projectsRoot, it)) }
        if (mode == SidebarMode.GIT) refreshGitStatus()
    }

    private val _gitBranch = MutableStateFlow("main")
    val gitBranch = _gitBranch.asStateFlow()
    private val _gitChanges = MutableStateFlow<List<GitChange>>(emptyList())
    val gitChanges = _gitChanges.asStateFlow()
    private val _gitLog = MutableStateFlow<List<GitCommit>>(emptyList())
    val gitLog = _gitLog.asStateFlow()
    private val _gitBranches = MutableStateFlow<List<String>>(emptyList())
    val gitBranches = _gitBranches.asStateFlow()
    private val _gitRemoteUrl = MutableStateFlow<String?>(null)
    val gitRemoteUrl = _gitRemoteUrl.asStateFlow()
    // Marketplace Extensions
    private val _availableExtensions = MutableStateFlow<List<com.codeossandroid.bridge.Extension>>(emptyList())
    val availableExtensions = _availableExtensions.asStateFlow()
    private val _isScanningMarketplace = MutableStateFlow(false)
    val isScanningMarketplace = _isScanningMarketplace.asStateFlow()
    private val _activeGithubExtensionDetail = MutableStateFlow<com.codeossandroid.bridge.Extension?>(null)
    val activeGithubExtensionDetail = _activeGithubExtensionDetail.asStateFlow()

    fun scanMarketplace() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanningMarketplace.value = true
            val list = com.codeossandroid.bridge.ExtensionManager.scanMarketplace(getApplication(), _githubToken.value, _activeProject.value)
            _availableExtensions.value = list
            _isScanningMarketplace.value = false
        }
    }

    fun selectGithubExtension(extension: com.codeossandroid.bridge.Extension?) {
        _activeGithubExtensionDetail.value = extension
        if (extension != null) {
            _sidebarOpen.value = false
            _activeExtensionDetail.value = null // Clear OpenVSX detail if any
        }
    }

    private fun runNpm(vararg args: String): Process? {
        val proj = _activeProject.value ?: return null
        val projDir = java.io.File(projectsRoot, proj)
        val filesDir = getApplication<Application>().filesDir.absolutePath
        val nativeLibPath = getApplication<Application>().applicationInfo.nativeLibraryDir
        val nodeBin = "$nativeLibPath/libnode.so"
        val npmCli = "$filesDir/npm_pkg/bin/npm-cli.js"
        val libLinksDir = java.io.File(filesDir, "lib").absolutePath
        
        val env = arrayOf(
            "HOME=$filesDir",
            "LD_LIBRARY_PATH=$nativeLibPath:$libLinksDir",
            "NODE_PATH=.:$filesDir/npm_pkg/node_modules",
            "PATH=$filesDir/bin:/system/bin:/system/xbin"
        )
        
        return try {
            Runtime.getRuntime().exec(arrayOf(nodeBin, npmCli) + args, env, projDir)
        } catch (e: Exception) {
            Log.e("CodeOSS", "NPM run failed: ${args.joinToString(" ")}", e)
            null
        }
    }

    fun installGithubExtension(extension: com.codeossandroid.bridge.Extension, version: String? = null) {
        val proj = _activeProject.value
        if (proj == null) {
            Log.w("CodeOSS", "Install aborted: No active project")
            viewModelScope.launch(Dispatchers.Main) {
                android.widget.Toast.makeText(getApplication(), "Please open a project first!", android.widget.Toast.LENGTH_LONG).show()
            }
            return
        }

        // Update state on Main thread for instant UI feedback
        _installingIds.value = _installingIds.value + extension.id
        _installingProgress.value = _installingProgress.value + (extension.id to 0f)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (extension.type == "npm") {
                    val pkgName = extension.packageName ?: extension.id
                    val targetPkg = if (version != null && version != "Latest") "$pkgName@$version" else pkgName
                    _binaryUpdateStatus.value = "Installing $targetPkg..."
                    _isUpdatingBinaries.value = true
                    
                    try {
                        val proc = runNpm("install", targetPkg)
                        proc?.let { p ->
                            val reader = p.inputStream.bufferedReader()
                            var line: String?
                            var simulatedProgress = 0.1f
                            while (reader.readLine().also { line = it } != null) {
                                Log.d("CodeOSS", "NPM Out: $line")
                                val status = line?.take(40) ?: ""
                                _binaryUpdateStatus.value = "NPM: $status"
                                // Simulating progress for NPM since it doesn't give clean percentages
                                simulatedProgress = (simulatedProgress + 0.05f).coerceAtMost(0.95f)
                                _installingProgress.value = _installingProgress.value + (extension.id to simulatedProgress)
                            }
                            p.waitFor()
                        }
                    } finally {
                        _isUpdatingBinaries.value = false
                        _binaryUpdateStatus.value = "Ready"
                    }
                } else {
                    com.codeossandroid.bridge.ExtensionManager.installExtension(getApplication(), extension, version) { progress ->
                        _installingProgress.value = _installingProgress.value + (extension.id to progress)
                    }
                }
                _installingProgress.value = _installingProgress.value + (extension.id to 1f)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "${extension.name} installed successfully!", android.widget.Toast.LENGTH_SHORT).show()
                }
                scanMarketplace() // Refresh list to show installed state
            } finally {
                _installingIds.value = _installingIds.value - extension.id
                // Delay clearing progress so user sees 100% for a moment
                delay(1000)
                _installingProgress.value = _installingProgress.value - extension.id
            }
        }
    }

    fun installLocalExtension(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            try {
                _binaryUpdateStatus.value = "Installing local extension..."
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val tempZip = java.io.File(context.cacheDir, "manual_ext.zip")
                tempZip.outputStream().use { inputStream.copyTo(it) }
                
                // We'll extract the ID from the folder name inside the zip or ask for it?
                // For now, let's just unzip it and refresh. 
                // We assume the ZIP contains a folder with the extension ID.
                val targetDir = java.io.File(context.filesDir, "extensions")
                targetDir.mkdirs()
                
                java.util.zip.ZipInputStream(java.io.FileInputStream(tempZip)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val newFile = java.io.File(targetDir, entry.name)
                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            newFile.outputStream().use { zis.copyTo(it) }
                        }
                        entry = zis.nextEntry
                    }
                }
                
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Local extension installed!", android.widget.Toast.LENGTH_SHORT).show()
                    scanMarketplace()
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Local install failed", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Install failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            } finally {
                _binaryUpdateStatus.value = "Ready"
            }
        }
    }
    private val _activeTunnels = MutableStateFlow<List<ForwardedPort>>(emptyList())
    val activeTunnels = _activeTunnels.asStateFlow()

    // Binary Update System
    private val _binaryUpdateStatus = MutableStateFlow("Up to date")
    val binaryUpdateStatus = _binaryUpdateStatus.asStateFlow()
    private val _binaryUpdateProgress = MutableStateFlow(0f)
    val binaryUpdateProgress = _binaryUpdateProgress.asStateFlow()
    private val _binaryUpdateProgressInfo = MutableStateFlow("")
    val binaryUpdateProgressInfo = _binaryUpdateProgressInfo.asStateFlow()
    private val _isUpdatingBinaries = MutableStateFlow(false)
    val isUpdatingBinaries = _isUpdatingBinaries.asStateFlow()
    private val _nodeVersion = MutableStateFlow(prefs.getString("node_version", "18.x (bundled)") ?: "18.x (bundled)")
    val nodeVersion = _nodeVersion.asStateFlow()
    private val _gitVersion = MutableStateFlow(prefs.getString("git_version", "2.x (bundled)") ?: "2.x (bundled)")
    val gitVersion = _gitVersion.asStateFlow()

    fun checkBinaryUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            _binaryUpdateStatus.value = "Checking..."
            val registryUrl = "https://raw.githubusercontent.com/Zohaib8090/codeoss-andriod/main/binaries.json"
            try {
                val update = com.codeossandroid.bridge.BinaryUpdater.checkUpdates(registryUrl)
                if (update != null) {
                    if (update.nodeVersion != _nodeVersion.value || update.gitVersion != _gitVersion.value) {
                        _binaryUpdateStatus.value = "Update Available: Node ${update.nodeVersion}"
                    } else {
                        _binaryUpdateStatus.value = "Binaries are up to date"
                    }
                } else {
                    _binaryUpdateStatus.value = "Check failed (Registry missing/404)"
                }
            } catch (e: Exception) {
                _binaryUpdateStatus.value = "Network error: ${e.message}"
            }
        }
    }

    fun applyBinaryUpdate() {
        if (_isUpdatingBinaries.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isUpdatingBinaries.value = true
            try {
                _binaryUpdateStatus.value = "Downloading Node.js..."
                val registryUrl = "https://raw.githubusercontent.com/Zohaib8090/codeoss-andriod/main/binaries.json"
                val update = com.codeossandroid.bridge.BinaryUpdater.checkUpdates(registryUrl) ?: return@launch
                
                val successNode = com.codeossandroid.bridge.BinaryUpdater.downloadAndInstall(getApplication(), update.nodeUrl, "usr") { p, d, t ->
                    _binaryUpdateProgress.value = p * 0.5f
                    _binaryUpdateProgressInfo.value = "Node.js: ${formatBytes(d)} / ${formatBytes(t)}"
                }
                
                if (successNode) {
                    _binaryUpdateStatus.value = "Downloading Git..."
                    val successGit = com.codeossandroid.bridge.BinaryUpdater.downloadAndInstall(getApplication(), update.gitUrl, "usr") { p, d, t ->
                        _binaryUpdateProgress.value = 0.5f + (p * 0.5f)
                        _binaryUpdateProgressInfo.value = "Git: ${formatBytes(d)} / ${formatBytes(t)}"
                    }
                    
                    if (successGit) {
                        _nodeVersion.value = update.nodeVersion
                        _gitVersion.value = update.gitVersion
                        prefs.edit().putString("node_version", update.nodeVersion).putString("git_version", update.gitVersion).apply()
                        _binaryUpdateStatus.value = "Binaries updated! Please restart the app."
                        _binaryUpdateProgressInfo.value = "Installation Complete"
                    } else {
                        _binaryUpdateStatus.value = "Git update failed"
                    }
                } else {
                    _binaryUpdateStatus.value = "Node update failed"
                }
            } catch (e: Exception) {
                _binaryUpdateStatus.value = "Error: ${e.message}"
            } finally {
                _isUpdatingBinaries.value = false
                _binaryUpdateProgress.value = 0f
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val i = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
        return String.format("%.2f %s", bytes / Math.pow(1024.0, i.toDouble()), units[i])
    }

    fun startTunnel(port: Int) {
        if (_activeTunnels.value.any { it.port == port }) return
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val nativeLibPath = context.applicationInfo.nativeLibraryDir
            val filesDir = context.filesDir.absolutePath
            val boreBin = "$nativeLibPath/libbore.so"
            val logFile = java.io.File(filesDir, "tunnel.log")
            val env = arrayOf(
                "HOME=$filesDir",
                "LD_LIBRARY_PATH=$nativeLibPath"
            )
            try {
                logFile.writeText("--- Bore Tunnel Starting ---\n")
                val process = Runtime.getRuntime().exec(
                    arrayOf(boreBin, "local", port.toString(), "--to", "161.35.110.36"),
                    env
                )
                val newTunnel = ForwardedPort(port, "Starting...", process)
                _activeTunnels.value = _activeTunnels.value + newTunnel
                viewModelScope.launch(Dispatchers.IO) {
                    val reader = process.inputStream.bufferedReader()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line ?: continue
                        Log.d("CodeOSS-Bore", l)
                        logFile.appendText("OUT: $l\n")
                        if (l.contains("listening at 161.35.110.36:")) {
                            val remotePort = l.substringAfter("listening at 161.35.110.36:").trim()
                            val url = "http://bore.pub:$remotePort"
                            _activeTunnels.value = _activeTunnels.value.map {
                                if (it.port == port) it.copy(url = url) else it
                            }
                        }
                    }
                }
                viewModelScope.launch(Dispatchers.IO) {
                    val errReader = process.errorStream.bufferedReader()
                    var errLine: String?
                    while (errReader.readLine().also { errLine = it } != null) {
                        val l = errLine ?: continue
                        Log.e("CodeOSS-Bore", "ERR: $l")
                        logFile.appendText("ERR: $l\n")
                        if (l.contains("error") || l.contains("failed")) {
                            _activeTunnels.value = _activeTunnels.value.map {
                                if (it.port == port && it.url == "Starting...") it.copy(url = "Error: ${l.take(50)}") else it
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Bore failed for port $port", e)
                _activeTunnels.value = _activeTunnels.value.map {
                    if (it.port == port) it.copy(url = "Error: ${e.message}") else it
                }
            }
        }
    }

    fun stopTunnel(port: Int) {
        val tunnel = _activeTunnels.value.find { it.port == port } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tunnel.process.destroy()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    tunnel.process.destroyForcibly()
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Failed to destroy tunnel process", e)
            }
        }
        _activeTunnels.value = _activeTunnels.value.filter { it.port != port }
    }

    private fun runGit(vararg args: String): Process? {
        val proj = _activeProject.value ?: return null
        val projDir = java.io.File(projectsRoot, proj)
        val nativeLibPath = getApplication<Application>().applicationInfo.nativeLibraryDir
        val gitBin = "$nativeLibPath/libgit.so"
        val libLinksDir = java.io.File(getApplication<Application>().filesDir, "lib").absolutePath
        val env = arrayOf(
            "GIT_EXEC_PATH=$nativeLibPath",
            "GIT_TEMPLATE_DIR=${getApplication<Application>().filesDir}/git_templates",
            "GIT_CONFIG_NOSYSTEM=1",
            "HOME=${getApplication<Application>().filesDir}",
            "LD_LIBRARY_PATH=$nativeLibPath:$libLinksDir"
        )
        return try {
            Runtime.getRuntime().exec(arrayOf(gitBin) + args, env, projDir)
        } catch (e: Exception) {
            Log.e("CodeOSS", "Git run failed: ${args.joinToString(" ")}", e)
            null
        }
    }

    fun refreshGitStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val proj = _activeProject.value ?: return@launch
            val projDir = java.io.File(projectsRoot, proj)
            if (!java.io.File(projDir, ".git").exists()) {
                _gitChanges.value = emptyList()
                _gitLog.value = emptyList()
                _gitBranch.value = "Not a Git repo"
                return@launch
            }
            try {
                val branchProc = runGit("rev-parse", "--abbrev-ref", "HEAD")
                val branch = branchProc?.let { proc ->
                    try { proc.inputStream.bufferedReader().readText().trim() }
                    finally { proc.inputStream.close(); proc.errorStream.close(); proc.destroy() }
                } ?: ""
                _gitBranch.value = if (branch.isEmpty()) "Detached" else branch

                val statusProc = runGit("status", "--porcelain")
                val statusLines = statusProc?.let { proc ->
                    try { proc.inputStream.bufferedReader().readLines() }
                    finally { proc.inputStream.close(); proc.errorStream.close(); proc.destroy() }
                } ?: emptyList()
                _gitChanges.value = statusLines.map { line ->
                    val stagedStatus = line.take(1)
                    val unstagedStatus = line.substring(1, 2)
                    val path = line.substring(3).trim()
                    val isStaged = stagedStatus != " " && stagedStatus != "?"
                    val status = if (isStaged) stagedStatus.trim() else unstagedStatus.trim()
                    GitChange(path, if (status.isEmpty()) stagedStatus.trim() else status, isStaged)
                }

                val branchesProc = runGit("branch", "-a")
                val branches = branchesProc?.let { proc ->
                    try { proc.inputStream.bufferedReader().readLines() }
                    finally { proc.inputStream.close(); proc.errorStream.close(); proc.destroy() }
                } ?: emptyList()
                _gitBranches.value = branches.mapNotNull { line ->
                    val clean = line.replace("*", "").trim()
                    if (clean.contains("HEAD ->")) null else clean
                }.filter { it.isNotEmpty() }.distinct()

                val logProc = runGit("log", "--pretty=format:%h|%s|%an|%ar", "--max-count=30")
                val logLines = logProc?.let { proc ->
                    try { proc.inputStream.bufferedReader().readLines() }
                    finally { proc.inputStream.close(); proc.errorStream.close(); proc.destroy() }
                } ?: emptyList()
                _gitLog.value = logLines.mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size >= 4) GitCommit(parts[0], parts[1], parts[2], parts[3]) else null
                }

                val remoteProc = runGit("remote", "get-url", "origin")
                val remoteUrl = remoteProc?.let { proc ->
                    try { proc.inputStream.bufferedReader().readText().trim() }
                    finally { proc.inputStream.close(); proc.errorStream.close(); proc.destroy() }
                } ?: ""
                _gitRemoteUrl.value = if (remoteUrl.isEmpty()) "No Remote" else remoteUrl
            } catch (e: Exception) {
                Log.e("CodeOSS", "Git refresh failed", e)
            }

        }
    }

    fun gitStage(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runGit("add", path)?.waitFor()
            refreshGitStatus()
        }
    }

    fun gitUnstage(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runGit("reset", "HEAD", "--", path)?.waitFor()
            refreshGitStatus()
        }
    }

    fun gitDiscard(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runGit("checkout", "--", path)?.waitFor()
            refreshGitStatus()
        }
    }

    fun gitCommit(message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _setupStatus.value = "Committing..."
            runGit("commit", "-m", message)?.waitFor()
            refreshGitStatus()
            _setupStatus.value = "Ready"
        }
    }

    fun gitPush() {
        viewModelScope.launch(Dispatchers.IO) {
            _setupStatus.value = "Pushing to origin..."
            runGit("push")?.waitFor()
            refreshGitStatus()
            _setupStatus.value = "Ready"
        }
    }

    fun gitFetch() {
        viewModelScope.launch(Dispatchers.IO) {
            _setupStatus.value = "Fetching from remote..."
            runGit("fetch")?.waitFor()
            refreshGitStatus()
            _setupStatus.value = "Ready"
        }
    }

    fun gitPull() {
        viewModelScope.launch(Dispatchers.IO) {
            _setupStatus.value = "Pulling from origin..."
            runGit("pull")?.waitFor()
            refreshGitStatus()
            _activeProject.value?.let { refreshFileTree(java.io.File(projectsRoot, it)) }
            _setupStatus.value = "Ready"
        }
    }

    fun gitCheckout(branch: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _setupStatus.value = "Switching to $branch..."
            runGit("stash")?.waitFor()
            val proc = runGit("checkout", branch)
            val exitCode = proc?.waitFor() ?: -1
            if (exitCode == 0) {
                runGit("stash", "pop")?.waitFor()
                refreshGitStatus()
                _activeProject.value?.let { refreshFileTree(java.io.File(projectsRoot, it)) }
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "Switched to $branch", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                val error = proc?.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                runGit("stash", "pop")?.waitFor()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "Checkout failed: $error", android.widget.Toast.LENGTH_LONG).show()
                }
            }
            _setupStatus.value = "Ready"
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _clipboardFile = MutableStateFlow<java.io.File?>(null)
    val clipboardFile = _clipboardFile.asStateFlow()
    private val _isCutMode = MutableStateFlow(false)
    val isCutMode = _isCutMode.asStateFlow()

    data class EditorTab(
        val file: java.io.File,
        var text: TextFieldValue,
        var isModified: Boolean = false
    )

    private val _openTabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val openTabs = _openTabs.asStateFlow()
    private val _activeTabIndices = MutableStateFlow<Map<Int, Int>>(mapOf(0 to -1))
    val activeTabIndices = _activeTabIndices.asStateFlow()
    private val _focusedViewportId = MutableStateFlow(0)
    val focusedViewportId = _focusedViewportId.asStateFlow()
    private val _browserUrl = MutableStateFlow("https://google.com")
    val browserUrl = _browserUrl.asStateFlow()

    fun openInBrowser(url: String) {
        _browserUrl.value = url
        _sidebarMode.value = SidebarMode.BROWSER
    }

    fun stopDebugProcess() {
        val index = activeInstanceIndex.value
        instances.value.getOrNull(index)?.let { instance ->
            instance.sendInput("\u0003")
            appendLogcat("SYSTEM: Stop command sent (Ctrl+C) to session ${instance.id}")
        }
    }

    fun closeBrowser() {
        _sidebarMode.value = SidebarMode.EXPLORER
    }

    fun updateFocusedViewport(id: Int) {
        _focusedViewportId.value = id
    }

    private val _previewFile = MutableStateFlow<java.io.File?>(null)
    val previewFile = _previewFile.asStateFlow()

    fun previewFile(file: java.io.File) {
        _previewFile.value = file
    }

    fun closePreview() {
        _previewFile.value = null
    }

    private val _problems = MutableStateFlow<List<IDEProblem>>(emptyList())
    val problems = _problems.asStateFlow()
    private val _outputLogs = MutableStateFlow("")
    val outputLogs = _outputLogs.asStateFlow()

    fun clearProblems() { _problems.value = emptyList() }
    fun clearOutput() { _outputLogs.value = "" }

    fun runTask(name: String, command: String) {
        val proj = _activeProject.value ?: return
        val projDir = java.io.File(projectsRoot, proj)
        val maxLogSize = 50000
        _outputLogs.value = (_outputLogs.value + "\n>>> Running task: $name [$command]\n").takeLast(maxLogSize)
        togglePanel(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", command), null, projDir)
                proc.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        _outputLogs.value = (_outputLogs.value + line + "\n").takeLast(maxLogSize)
                        parseProblem(line, projDir)
                    }
                }
                proc.waitFor()
                _outputLogs.value += "<<< Task completed.\n"
            } catch (e: Exception) {
                _outputLogs.value += "ERROR: ${e.message}\n"
            }
        }
    }

    private fun parseProblem(line: String, root: java.io.File) {
        val regex = Regex("(.*?):(\\d+):(\\d+):\\s*(error|warning):\\s*(.*)", RegexOption.IGNORE_CASE)
        regex.find(line)?.let { match ->
            val path = match.groupValues[1]
            val lineNum = match.groupValues[2].toIntOrNull() ?: 0
            val colNum = match.groupValues[3].toIntOrNull() ?: 0
            val severity = match.groupValues[4].uppercase()
            val msg = match.groupValues[5]
            val file = if (path.startsWith("/")) java.io.File(path) else java.io.File(root, path)
            if (file.exists()) {
                val problem = IDEProblem(file, lineNum, colNum, msg, severity)
                _problems.value = _problems.value + problem
            }
        }
    }

    fun refreshProjects() {
        projectsRoot.mkdirs()
        _projects.value = (projectsRoot.listFiles()?.filter { it.isDirectory }?.map { it.name }?.sorted() ?: emptyList())
        val current = _activeProject.value
        if (current != null && !java.io.File(projectsRoot, current).exists()) {
            _activeProject.value = null
            prefs.edit().remove("active_project").apply()
        }
    }

    fun toggleSidebar() {
        _sidebarOpen.value = !_sidebarOpen.value
        if (_sidebarOpen.value) {
            refreshProjects()
            _activeProject.value?.let { refreshFileTree(java.io.File(projectsRoot, it)) }
        }
    }

    fun openProject(name: String) {
        _activeProject.value = name
        prefs.edit().putString("active_project", name).apply()
        val projDir = java.io.File(projectsRoot, name)
        refreshFileTree(projDir)
        viewModelScope.launch(Dispatchers.IO) {
            _instances.value.forEach { it.sendInput("cd \"${projDir.absolutePath}\"\n") }
        }
        refreshGitStatus()
    }

    fun openFile(file: java.io.File, targetViewportId: Int? = null) {
        if (file.isDirectory) return
        val vid = targetViewportId ?: _focusedViewportId.value
        val existingIndex = _openTabs.value.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existingIndex != -1) {
            switchTab(vid, existingIndex)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = file.readText()
                withContext(Dispatchers.Main) {
                    val newTab = EditorTab(file, TextFieldValue(content, TextRange(0)))
                    _openTabs.value = _openTabs.value + newTab
                    switchTab(vid, _openTabs.value.size - 1)
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Failed to read file", e)
            }
        }
    }

    fun switchTab(viewportId: Int, tabIndex: Int) {
        val current = _activeTabIndices.value.toMutableMap()
        current[viewportId] = tabIndex
        _activeTabIndices.value = current
    }

    fun closeTab(index: Int) {
        val currentTabs = _openTabs.value.toMutableList()
        if (index in currentTabs.indices) {
            currentTabs.removeAt(index)
            _openTabs.value = currentTabs
            val currentIndices = _activeTabIndices.value.toMutableMap()
            currentIndices.forEach { (vid, tid) ->
                when {
                    tid == index -> {
                        currentIndices[vid] = if (currentTabs.isEmpty()) -1 else (tid - 1).coerceAtLeast(0)
                    }
                    tid > index -> {
                        currentIndices[vid] = tid - 1
                    }
                }
            }
            _activeTabIndices.value = currentIndices
        }
    }

    fun splitViewport(sourceId: Int, horizontal: Boolean = true) {
        val newId = (_activeTabIndices.value.keys.maxOrNull() ?: 0) + 1
        val sourceTab = _activeTabIndices.value[sourceId] ?: -1
        val current = _activeTabIndices.value.toMutableMap()
        current[newId] = sourceTab
        _activeTabIndices.value = current
    }

    fun removeViewport(id: Int) {
        if (_activeTabIndices.value.size > 1) {
            val current = _activeTabIndices.value.toMutableMap()
            current.remove(id)
            _activeTabIndices.value = current
        }
    }

    fun updateEditorText(viewportId: Int, value: TextFieldValue) {
        val tabIndex = _activeTabIndices.value[viewportId] ?: return
        val currentTabs = _openTabs.value.toMutableList()
        if (tabIndex in currentTabs.indices) {
            currentTabs[tabIndex] = currentTabs[tabIndex].copy(text = value, isModified = true)
            _openTabs.value = currentTabs
        }
    }

    fun saveCurrentFile(viewportId: Int) {
        val tabIndex = _activeTabIndices.value[viewportId] ?: return
        val tab = _openTabs.value.getOrNull(tabIndex) ?: return
        val file = tab.file
        viewModelScope.launch(Dispatchers.IO) {
            try {
                file.writeText(tab.text.text)
                withContext(Dispatchers.Main) {
                    val updatedTabs = _openTabs.value.toMutableList()
                    updatedTabs[tabIndex] = updatedTabs[tabIndex].copy(isModified = false)
                    _openTabs.value = updatedTabs
                    android.widget.Toast.makeText(getApplication(), "Saved: ${file.name}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Failed to save file", e)
            }
        }
    }

    fun closeFile() {
        // Legacy compat
    }

    fun createProject(name: String) {
        val trimmed = name.trim().replace(" ", "-")
        if (trimmed.isEmpty()) return
        java.io.File(projectsRoot, trimmed).mkdirs()
        refreshProjects()
        openProject(trimmed)
        refreshGitStatus()
    }

    fun deleteProject(name: String) {
        java.io.File(projectsRoot, name).deleteRecursively()
        if (_activeProject.value == name) _activeProject.value = null
        refreshProjects()
    }

    fun cloneProject(url: String, name: String, user: String, token: String) {
        val trimmedName = name.trim().replace(" ", "-")
        if (trimmedName.isEmpty() || url.isEmpty()) return
        val projDir = java.io.File(projectsRoot, trimmedName)
        if (projDir.exists()) {
            android.widget.Toast.makeText(getApplication(), "Project already exists", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        projDir.mkdirs()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _setupStatus.value = "Preparing clone..."
                val context = getApplication<android.app.Application>()
                com.codeossandroid.bridge.PtyBridge().setupEnvironment(context)
                val filesDir = context.filesDir.absolutePath
                val logFile = java.io.File(filesDir, "git_clone.log")
                logFile.writeText("Clone started at ${java.util.Date()}\n")
                val usrDir = "$filesDir/usr"
                val usrBinDir = "$usrDir/bin"
                val usrEtcDir = "$usrDir/etc"
                val nativeLibPath = context.applicationInfo.nativeLibraryDir
                val gitBinary = "$usrBinDir/git"
                logFile.appendText("Binary: $gitBinary (exists: ${java.io.File(gitBinary).exists()})\n")
                val authUrl = if (user.isNotEmpty() && token.isNotEmpty()) {
                    val encodedToken = java.net.URLEncoder.encode(token, "UTF-8")
                    url.replace("https://", "https://$user:$encodedToken@")
                } else url
                logFile.appendText("URL: $url\n")
                val pb = ProcessBuilder(
                    if (java.io.File(gitBinary).exists()) gitBinary else "$nativeLibPath/libgit.so",
                    "clone", "--progress", authUrl, projDir.absolutePath
                )
                val env = pb.environment()
                val libLinksDir = java.io.File(context.filesDir, "lib").absolutePath
                env["PATH"] = "$usrBinDir:/system/bin:/system/xbin"
                env["LD_LIBRARY_PATH"] = "$nativeLibPath:$libLinksDir"
                env["HOME"] = filesDir
                env["GIT_TEMPLATE_DIR"] = "$filesDir/git_templates"
                env["GIT_CONFIG_NOSYSTEM"] = "1"
                env["GIT_CONFIG_GLOBAL"] = "$usrEtcDir/gitconfig"
                env["GIT_EXEC_PATH"] = usrBinDir
                env["GIT_SSL_NO_VERIFY"] = "true"
                pb.redirectErrorStream(true)
                val process = pb.start()
                val reader = process.inputStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: ""
                    logFile.appendText("> $currentLine\n")
                    val match = Regex("(\\d+)%").find(currentLine)
                    if (match != null) {
                        val percent = match.groupValues[1]
                        val phase = if (currentLine.contains("Receiving")) "Receiving" else if (currentLine.contains("Resolving")) "Resolving" else "Cloning"
                        _setupStatus.value = "$phase: $percent%"
                    } else {
                        _setupStatus.value = "Cloning: $currentLine"
                    }
                }
                val exitCode = process.waitFor()
                withContext(Dispatchers.Main) {
                    if (exitCode == 0) {
                        android.widget.Toast.makeText(getApplication(), "Clone Successful!", android.widget.Toast.LENGTH_LONG).show()
                        refreshProjects()
                        openProject(trimmedName)
                    } else {
                        projDir.deleteRecursively()
                        android.widget.Toast.makeText(getApplication(), "Clone Failed (Exit $exitCode)", android.widget.Toast.LENGTH_LONG).show()
                    }
                    _setupStatus.value = "Ready"
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Clone failed", e)
                withContext(Dispatchers.Main) {
                    projDir.deleteRecursively()
                    android.widget.Toast.makeText(getApplication(), "Clone Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    _setupStatus.value = "Ready"
                }
            }
        }
    }

    fun refreshFileTree(root: java.io.File) {
        val tree = mutableMapOf<String, List<java.io.File>>()
        fun scan(dir: java.io.File) {
            val children = dir.listFiles()
                ?.filter { !it.name.startsWith(".") && it.name != "node_modules" }
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                ?: emptyList()
            tree[dir.absolutePath] = children
            children.filter { it.isDirectory }.forEach { scan(it) }
        }
        scan(root)
        _fileTree.value = tree
    }

    fun toggleDir(path: String) {
        _expandedDirs.value = if (_expandedDirs.value.contains(path))
            _expandedDirs.value - path else _expandedDirs.value + path
    }

    fun createFile(parent: java.io.File, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                java.io.File(parent, name).createNewFile()
                _activeProject.value?.let { refreshFileTree(java.io.File(projectsRoot, it)) }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Failed to create file", e)
            }
        }
    }

    fun createFolder(parent: java.io.File, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                java.io.File(parent, name).mkdirs()
                _activeProject.value?.let { refreshFileTree(java.io.File(projectsRoot, it)) }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Failed to create folder", e)
            }
        }
    }

    fun renameFile(file: java.io.File, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newFile = java.io.File(file.parentFile, newName)
                file.renameTo(newFile)
                _activeProject.value?.let { refreshFileTree(java.io.File(projectsRoot, it)) }
                val updatedTabs = _openTabs.value.map {
                    if (it.file == file) it.copy(file = newFile) else it
                }
                _openTabs.value = updatedTabs
            } catch (e: Exception) {
                Log.e("CodeOSS", "Failed to rename file", e)
            }
        }
    }

    fun deleteFile(file: java.io.File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                file.deleteRecursively()
                _activeProject.value?.let { refreshFileTree(java.io.File(projectsRoot, it)) }
                val index = _openTabs.value.indexOfFirst { it.file == file }
                if (index != -1) {
                    withContext(Dispatchers.Main) {
                        closeTab(index)
                    }
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Failed to delete file", e)
            }
        }
    }

    fun setClipboard(file: java.io.File, cut: Boolean) {
        _clipboardFile.value = file
        _isCutMode.value = cut
    }

    fun pasteFile(targetDir: java.io.File) {
        val source = _clipboardFile.value ?: return
        val dest = java.io.File(targetDir, source.name)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (_isCutMode.value) {
                    source.renameTo(dest)
                    _clipboardFile.value = null
                } else {
                    source.copyRecursively(dest, overwrite = true)
                }
                _activeProject.value?.let { refreshFileTree(java.io.File(projectsRoot, it)) }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Failed to paste file", e)
            }
        }
    }

    fun openInTerminal(file: java.io.File) {
        val dir = if (file.isDirectory) file else file.parentFile ?: return
        togglePanel(true)
        viewModelScope.launch(Dispatchers.IO) {
            _instances.value.getOrNull(_activeInstanceIndex.value)?.sendInput("cd \"${dir.absolutePath}\"\n")
        }
    }

    init {
        refreshProjects()
        val current = _activeProject.value
        if (current != null) {
            refreshFileTree(java.io.File(projectsRoot, current))
            refreshGitStatus()
        }
        addNewTerminal()
        _isReady.value = true
    }

    // ── FIXED: Multi-terminal fix using instanceHolder pattern ────────────────
    fun addNewTerminal() {
        val nextId = (_instances.value.maxByOrNull { it.id }?.id ?: 0) + 1
        val projDir = _activeProject.value?.let { java.io.File(projectsRoot, it) }
        val cwd = projDir?.absolutePath ?: "/"

        // Use holder so the client callback can reference the instance
        var instanceHolder: TerminalInstance? = null

        val client = object : com.termux.terminal.TerminalSessionClient {
            override fun onTextChanged(changedSession: com.termux.terminal.TerminalSession) {
                // This now invalidates only the correct bound view
                instanceHolder?.boundView?.postInvalidate()
            }
            override fun onTitleChanged(changedSession: com.termux.terminal.TerminalSession) {}
            override fun onSessionFinished(finishedSession: com.termux.terminal.TerminalSession) {}
            override fun onCopyTextToClipboard(session: com.termux.terminal.TerminalSession, text: String) {}
            override fun onPasteTextFromClipboard(session: com.termux.terminal.TerminalSession) {}
            override fun onBell(session: com.termux.terminal.TerminalSession) {}
            override fun onColorsChanged(session: com.termux.terminal.TerminalSession) {}
            override fun onTerminalCursorStateChange(state: Boolean) {}
            override fun getTerminalCursorStyle(): Int { return com.termux.terminal.TerminalEmulator.TERMINAL_CURSOR_STYLE_BLOCK }
            override fun logError(tag: String, message: String) {}
            override fun logWarn(tag: String, message: String) {}
            override fun logInfo(tag: String, message: String) {}
            override fun logDebug(tag: String, message: String) {}
            override fun logVerbose(tag: String, message: String) {}
            override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
            override fun logStackTrace(tag: String, e: Exception) {}
        }

        val context = getApplication<android.app.Application>().applicationContext
        com.codeossandroid.bridge.PtyBridge().setupEnvironment(context)

        val binDir = context.filesDir.absolutePath
        val libDir = context.applicationInfo.nativeLibraryDir
        val newPath = "$binDir/bin:$binDir/usr/git-exec:/system/bin:/system/xbin:/vendor/bin"
        val envPath = "$binDir/init.sh"

        val env = arrayOf(
            "PATH=$newPath",
            "TERM=xterm-256color",
            "HOME=$cwd",
            "APP_LIB_DIR=$libDir",
            "GIT_EXEC_PATH=$libDir",
            "ENV=$envPath"
        )

        val instance = TerminalInstance(nextId, "sh", cwd, arrayOf("-i", "-l"), env, client)
        instanceHolder = instance // Link holder to instance

        _instances.value = _instances.value + instance
        _activeInstanceIndex.value = _instances.value.size - 1
        _setupStatus.value = "Starting Shell $nextId..."
        _isReady.value = true
    }

    fun switchTerminal(index: Int) { _activeInstanceIndex.value = index }

    fun removeTerminal(index: Int) {
        if (_instances.value.size > 1) {
            val newList = _instances.value.toMutableList()
            val removed = newList.removeAt(index)
            try { removed.session.finishIfRunning() } catch (_: Exception) {}
            removed.boundView = null
            _instances.value = newList
            _activeInstanceIndex.value = (_activeInstanceIndex.value).coerceAtMost(newList.size - 1)
        }
    }

    fun sendInput(data: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _instances.value.getOrNull(_activeInstanceIndex.value)?.sendInput(data)
        }
    }

    fun sendSpecialKey(key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeInstance = _instances.value.getOrNull(_activeInstanceIndex.value)
            activeInstance?.let { instance ->
                val code = when (key) {
                    "ESC" -> "\u001b"
                    "UP" -> "\u001b[A"
                    "DOWN" -> "\u001b[B"
                    "RIGHT" -> "\u001b[C"
                    "LEFT" -> "\u001b[D"
                    "HOME" -> "\u001b[1~"
                    "END" -> "\u001b[4~"
                    "PGUP" -> "\u001b[5~"
                    "PGDN" -> "\u001b[6~"
                    "TAB" -> "\t"
                    else -> key
                }
                instance.sendInput(code)
            }
        }
    }


    fun updateFontSize(delta: Int) {
        _fontSize.value = (_fontSize.value + delta).coerceIn(8, 30)
        prefs.edit().putInt("font_size", _fontSize.value).apply()
    }

    fun updateUIScale(delta: Float) {
        _uiScale.value = (_uiScale.value + delta).coerceIn(0.7f, 1.5f)
        prefs.edit().putFloat("ui_scale", _uiScale.value).apply()
    }

    fun updatePanelHeight(delta: Dp) { _panelHeight.value = (_panelHeight.value + delta).coerceIn(100.dp, 500.dp) }
    fun togglePanel(visible: Boolean) { _isPanelVisible.value = visible }
    fun toggleSidebar(visible: Boolean) { _sidebarOpen.value = visible }

    fun toggleAutoScroll() { _autoScroll.value = !_autoScroll.value }

    fun importProjectZip(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val tempZip = java.io.File(context.cacheDir, "import.zip")
                    tempZip.outputStream().use { output -> input.copyTo(output) }
                    ZipUtils.unzip(tempZip, projectsRoot)
                    tempZip.delete()
                    withContext(Dispatchers.Main) {
                        refreshProjects()
                        android.widget.Toast.makeText(context, "Project Imported", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Import failed", e)
            }
        }
    }

    fun exportProjectZip(projectName: String, uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val projDir = java.io.File(projectsRoot, projectName)
                val tempZip = java.io.File(context.cacheDir, "export.zip")
                ZipUtils.zipDirectory(projDir, tempZip)
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    tempZip.inputStream().use { input -> input.copyTo(output) }
                }
                tempZip.delete()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Project Exported", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Export failed", e)
            }
        }
    }

    fun importFile(targetDir: java.io.File, uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    cursor.getString(nameIndex)
                } ?: "uploaded_file"
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.File(targetDir, fileName).outputStream().use { output -> input.copyTo(output) }
                }
                _activeProject.value?.let { refreshFileTree(java.io.File(projectsRoot, it)) }
            } catch (e: Exception) {
                Log.e("CodeOSS", "File import failed", e)
            }
        }
    }

    fun exportFile(file: java.io.File, uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    file.inputStream().use { input -> input.copyTo(output) }
                }
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "File Exported", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "File export failed", e)
            }
        }
    }

    fun copyTerminalLogs(index: Int) {
        val instance = _instances.value.getOrNull(index) ?: return
        val text = instance.getAllOutput()
        val context = getApplication<Application>()
        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("Terminal Logs", text))
        android.widget.Toast.makeText(context, "Logs copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun searchNpm(query: String) {
        if (query.isEmpty()) {
            _npmResults.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isNpmSearching.value = true
            var connection: java.net.HttpURLConnection? = null
            try {
                val url = java.net.URL("https://registry.npmjs.org/-/v1/search?text=${java.net.URLEncoder.encode(query, "UTF-8")}&size=20")
                connection = url.openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("User-Agent", "CodeOSS-Android-App")
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val objects = json.getJSONArray("objects")
                    val results = mutableListOf<NpmPackage>()
                    for (i in 0 until objects.length()) {
                        val pkg = objects.getJSONObject(i).getJSONObject("package")
                        results.add(NpmPackage(
                            name = pkg.getString("name"),
                            version = pkg.getString("version"),
                            description = pkg.optString("description", "No description"),
                            author = pkg.optJSONObject("author")?.optString("name") ?: pkg.optString("publisher", "Unknown"),
                            date = pkg.optString("date", "")
                        ))
                    }
                    _npmResults.value = results
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "NPM Search failed", e)
            } finally {
                connection?.disconnect()
                _isNpmSearching.value = false
            }
        }
    }

    fun installNpmPackage(packageName: String) {
        runTask("NPM Install", "npm install $packageName")
        _sidebarMode.value = SidebarMode.EXPLORER
        _isPanelVisible.value = true
    }

    override fun onCleared() {
        super.onCleared()
        // Destroy logcat process
        logcatProcess?.destroy()
        logcatProcess = null
        // Destroy all terminal sessions
        _instances.value.forEach { instance ->
            try { instance.session.finishIfRunning() } catch (_: Exception) {}
        }
        // Destroy all tunnel processes
        _activeTunnels.value.forEach { tunnel ->
            try { tunnel.process.destroy() } catch (_: Exception) {}
        }
        _activeTunnels.value = emptyList()
    }
}
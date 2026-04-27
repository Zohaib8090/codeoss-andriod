package com.codeossandroid.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeossandroid.bridge.GitBridge
import com.codeossandroid.bridge.PtyBridge
import com.codeossandroid.bridge.ZipUtils
import com.codeossandroid.model.TerminalInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateListOf

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

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val _instances = MutableStateFlow<List<TerminalInstance>>(emptyList())
    val instances = _instances.asStateFlow()
    private val _activeInstanceIndex = MutableStateFlow(0)
    val activeInstanceIndex = _activeInstanceIndex.asStateFlow()
    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()
    private val _isCtrlActive = MutableStateFlow(false)
    val isCtrlActive = _isCtrlActive.asStateFlow()
    
    fun toggleCtrl() { _isCtrlActive.value = !_isCtrlActive.value }
    fun setCtrl(active: Boolean) { _isCtrlActive.value = active }
    
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
                    // Get User Info
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
    
    enum class SidebarMode { PROJECTS, EXPLORER, SEARCH, GIT }
    private val _sidebarMode = MutableStateFlow(SidebarMode.EXPLORER)
    val sidebarMode = _sidebarMode.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    fun setSidebarMode(mode: SidebarMode) {
        _sidebarMode.value = mode
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

    private fun runGit(vararg args: String): Process? {
        val proj = _activeProject.value ?: return null
        val projDir = java.io.File(projectsRoot, proj)
        val nativeLibPath = getApplication<Application>().applicationInfo.nativeLibraryDir
        val gitBin = "$nativeLibPath/libgit.so"
        val env = arrayOf(
            "GIT_EXEC_PATH=$nativeLibPath",
            "GIT_TEMPLATE_DIR=${getApplication<Application>().filesDir}/git_templates",
            "GIT_CONFIG_NOSYSTEM=1",
            "HOME=${getApplication<Application>().filesDir}",
            "LD_LIBRARY_PATH=$nativeLibPath"
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
                // Get branch
                val branchProc = runGit("rev-parse", "--abbrev-ref", "HEAD")
                val branch = branchProc?.inputStream?.bufferedReader()?.readText()?.trim() ?: ""
                _gitBranch.value = if (branch.isEmpty()) "Detached" else branch

                // Get status
                val statusProc = runGit("status", "--porcelain")
                val statusLines = statusProc?.inputStream?.bufferedReader()?.readLines() ?: emptyList()
                _gitChanges.value = statusLines.map { line ->
                    val stagedStatus = line.take(1)
                    val unstagedStatus = line.substring(1, 2)
                    val path = line.substring(3).trim()
                    val isStaged = stagedStatus != " " && stagedStatus != "?"
                    val status = if (isStaged) stagedStatus.trim() else unstagedStatus.trim()
                    GitChange(path, if (status.isEmpty()) stagedStatus.trim() else status, isStaged)
                }

                // Get branches
                val branchesProc = runGit("branch", "-a")
                val branches = branchesProc?.inputStream?.bufferedReader()?.readLines() ?: emptyList()
                _gitBranches.value = branches.mapNotNull { line ->
                    val clean = line.replace("*", "").trim()
                    if (clean.contains("HEAD ->")) null else clean
                }.filter { it.isNotEmpty() }.distinct()

                // Get log
                val logProc = runGit("log", "--pretty=format:%h|%s|%an|%ar", "--max-count=30")
                val logLines = logProc?.inputStream?.bufferedReader()?.readLines() ?: emptyList()
                _gitLog.value = logLines.mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size >= 4) GitCommit(parts[0], parts[1], parts[2], parts[3]) else null
                }

                // Get Remote URL
                val remoteProc = runGit("remote", "get-url", "origin")
                val remoteUrl = remoteProc?.inputStream?.bufferedReader()?.readText()?.trim() ?: ""
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
            
            // 1. Attempt Auto-Stash (Save current work)
            runGit("stash")?.waitFor()
            
            // 2. Perform Checkout
            val proc = runGit("checkout", branch)
            val exitCode = proc?.waitFor() ?: -1
            
            if (exitCode == 0) {
                // 3. Re-apply work if switch was successful
                runGit("stash", "pop")?.waitFor()
                refreshGitStatus()
                _activeProject.value?.let { refreshFileTree(java.io.File(projectsRoot, it)) }
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "Switched to $branch", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                val error = proc?.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                // Try to pop stash back if checkout failed to restore state
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
    
    private val _editorText = MutableStateFlow(TextFieldValue(""))
    val editorText = _editorText.asStateFlow()
    private val _currentFile = MutableStateFlow<java.io.File?>(null)
    val currentFile = _currentFile.asStateFlow()
    
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
        _outputLogs.value += "\n>>> Running task: $name [$command]\n"
        togglePanel(true)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", command), null, projDir)
                proc.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        _outputLogs.value += line + "\n"
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
            _instances.value.forEach { it.pty.write("cd \"${projDir.absolutePath}\"\n") }
        }
        refreshGitStatus()
    }

    fun openFile(file: java.io.File) {
        if (file.isDirectory) return
        _currentFile.value = file
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = file.readText()
                withContext(Dispatchers.Main) {
                    _editorText.value = TextFieldValue(content, TextRange(0))
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Failed to read file", e)
            }
        }
    }
    
    fun updateEditorText(value: TextFieldValue) {
        _editorText.value = value
    }

    fun saveCurrentFile() {
        val file = _currentFile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                file.writeText(_editorText.value.text)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "Saved: ${file.name}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Failed to save file", e)
            }
        }
    }

    fun closeFile() {
        _currentFile.value = null
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
            _setupStatus.value = "Cloning $url..."
            val result = GitBridge.cloneRepo(url, projDir.absolutePath, user, token)
            
            withContext(Dispatchers.Main) {
                if (result == "SUCCESS") {
                    android.widget.Toast.makeText(getApplication(), "Clone Successful!", android.widget.Toast.LENGTH_LONG).show()
                    refreshProjects()
                    openProject(trimmedName)
                } else {
                    projDir.deleteRecursively()
                    android.widget.Toast.makeText(getApplication(), "Clone Failed: $result", android.widget.Toast.LENGTH_LONG).show()
                }
                _setupStatus.value = "Ready"
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
                if (_currentFile.value == file) _currentFile.value = newFile
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
                if (_currentFile.value == file) _currentFile.value = null
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
            _instances.value.getOrNull(_activeInstanceIndex.value)?.pty?.write("cd \"${dir.absolutePath}\"\n")
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

    fun addNewTerminal() {
        val nextId = (_instances.value.maxByOrNull { it.id }?.id ?: 0) + 1
        val pty = PtyBridge()
        val instance = TerminalInstance(nextId, pty)
        _instances.value = _instances.value + instance
        _activeInstanceIndex.value = _instances.value.size - 1
        viewModelScope.launch(Dispatchers.IO) {
            _setupStatus.value = "Starting Shell $nextId..."
            val projDir = _activeProject.value?.let { java.io.File(projectsRoot, it) }
            pty.startShell(getApplication<Application>().applicationContext, homeDir = projDir?.absolutePath)
            
            withContext(Dispatchers.Main) {
                if (pty.getFd() != -1) {
                    android.widget.Toast.makeText(getApplication(), "Shell $nextId Started", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(getApplication(), "Failed to start Shell $nextId", android.widget.Toast.LENGTH_LONG).show()
                }
            }
            val buffer = ByteArray(8192)
            try {
                while (true) {
                    val read = pty.read(buffer)
                    if (read > 0) {
                        val text = String(buffer, 0, read)
                        withContext(Dispatchers.Main) { 
                            instance.processOutput(text) 
                        }
                    } else if (read <= 0) {
                        if (read == -1) {
                            withContext(Dispatchers.Main) {
                                instance.processOutput("\r\n[Process Exited]\r\n")
                            }
                        }
                        break 
                    }
                }
            } catch (e: Exception) {
                Log.e("CodeOSS", "Terminal read error", e)
                withContext(Dispatchers.Main) {
                    instance.processOutput("\r\n[Terminal Error: ${e.message}]\r\n")
                }
            }
        }
    }

    fun switchTerminal(index: Int) { _activeInstanceIndex.value = index }
    fun removeTerminal(index: Int) {
        if (_instances.value.size > 1) {
            val newList = _instances.value.toMutableList(); newList.removeAt(index)
            _instances.value = newList; _activeInstanceIndex.value = (_activeInstanceIndex.value).coerceAtMost(newList.size - 1)
        }
    }

    fun sendInput(data: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _instances.value.getOrNull(_activeInstanceIndex.value)?.pty?.write(data)
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
}

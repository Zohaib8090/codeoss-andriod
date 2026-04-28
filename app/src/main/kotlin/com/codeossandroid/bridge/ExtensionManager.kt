package com.codeossandroid.bridge

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class Extension(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val iconUrl: String?,
    val downloadUrl: String,
    val isInstalled: Boolean = false
)

object ExtensionManager {
    private const val TAG = "ExtensionManager"
    private const val GITHUB_REPO = "Zohaib8090/codeoss-android"
    private const val MARKETPLACE_PATH = "marketplace"

    suspend fun scanMarketplace(context: Context): List<Extension> = withContext(Dispatchers.IO) {
        val extensions = mutableListOf<Extension>()
        try {
            val url = URL("https://api.github.com/repos/$GITHUB_REPO/contents/$MARKETPLACE_PATH")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "CodeOSS-Android-App")
            if (connection.responseCode != 200) return@withContext emptyList()
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val items = JSONArray(response)
            
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                if (item.getString("type") == "dir") {
                    val name = item.getString("name")
                    val metadata = fetchMetadata(name)
                    if (metadata != null) {
                        val id = metadata.getString("id")
                        val installedDir = File(context.filesDir, "extensions/$id")
                        extensions.add(Extension(
                            id = id,
                            name = metadata.getString("name"),
                            description = metadata.optString("description", "No description"),
                            version = metadata.optString("version", "1.0.0"),
                            author = metadata.optString("author", "Unknown"),
                            iconUrl = metadata.optString("icon", null),
                            downloadUrl = "https://github.com/$GITHUB_REPO/archive/refs/heads/main.zip", // Temporary: Should point to subfolder ZIP or specific release
                            isInstalled = installedDir.exists()
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Marketplace scan failed", e)
        }
        extensions
    }

    private suspend fun fetchMetadata(dirName: String): JSONObject? {
        return try {
            val url = URL("https://raw.githubusercontent.com/$GITHUB_REPO/main/$MARKETPLACE_PATH/$dirName/manifest.json")
            val connection = url.openConnection() as HttpURLConnection
            if (connection.responseCode != 200) return null
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(response)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun installExtension(context: Context, extension: Extension, onProgress: (Float) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // In a real scenario, we'd use a specific ZIP for the extension folder.
                // For now, we'll simulate the download/extract into the extensions folder.
                val targetDir = File(context.filesDir, "extensions/${extension.id}")
                targetDir.mkdirs()
                
                // Placeholder download logic - in production this would be the actual extension ZIP
                // For this demo, we'll just create the directory to show it's "installed"
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}

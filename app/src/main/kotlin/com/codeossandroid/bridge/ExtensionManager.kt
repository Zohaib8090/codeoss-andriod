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
    val isInstalled: Boolean = false,
    val versions: List<String> = emptyList(),
    val screenshots: List<String> = emptyList()
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
                        val versionsList = fetchVersions(name)
                        val screenshotsArray = metadata.optJSONArray("screenshots")
                        val screenshots = mutableListOf<String>()
                        if (screenshotsArray != null) {
                            for (i in 0 until screenshotsArray.length()) {
                                screenshots.add(screenshotsArray.getString(i))
                            }
                        }

                        extensions.add(Extension(
                            id = id,
                            name = metadata.getString("name"),
                            description = metadata.optString("description", "No description"),
                            version = metadata.optString("version", "1.0.0"),
                            author = metadata.optString("author", "Unknown"),
                            iconUrl = metadata.optString("icon", null),
                            downloadUrl = "https://github.com/$GITHUB_REPO/archive/refs/heads/main.zip",
                            isInstalled = installedDir.exists(),
                            versions = versionsList,
                            screenshots = screenshots
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

    suspend fun installExtension(context: Context, extension: Extension, version: String? = null, onProgress: (Float) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // If version is provided and different from "main", we might want to download a specific tag
                val downloadUrl = if (version != null && version != "Latest") {
                    "https://github.com/$GITHUB_REPO/archive/refs/tags/$version.zip"
                } else {
                    extension.downloadUrl
                }

                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "CodeOSS-Android-App")
                connection.connect()
                
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext false
                
                val fileLength = connection.contentLength.toLong()
                val tempZip = File(context.cacheDir, "ext_${extension.id}.zip")
                val input = java.io.BufferedInputStream(connection.inputStream)
                val output = java.io.FileOutputStream(tempZip)
                
                val data = ByteArray(8192)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) onProgress(total.toFloat() / fileLength)
                    output.write(data, 0, count)
                }
                output.close()
                input.close()
                
                val targetDir = File(context.filesDir, "extensions/${extension.id}")
                targetDir.mkdirs()
                
                unzipExtension(tempZip, targetDir, "marketplace/${extension.id}")
                
                tempZip.delete()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Installation failed", e)
                false
            }
        }
    }

    private suspend fun fetchVersions(dirName: String): List<String> {
        return try {
            val url = URL("https://raw.githubusercontent.com/$GITHUB_REPO/main/$MARKETPLACE_PATH/$dirName/versions.json")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "CodeOSS-Android-App")
            if (connection.responseCode != 200) return listOf("Latest")
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val array = json.getJSONArray("versions")
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (e: Exception) {
            listOf("Latest")
        }
    }

    private fun unzipExtension(zipFile: File, targetDir: File, internalPath: String) {
        java.util.zip.ZipInputStream(java.io.FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                // GitHub ZIPs usually have a root folder like 'repo-main/'
                val name = entry.name
                val pathAfterRoot = name.substringAfter('/')
                
                if (pathAfterRoot.startsWith(internalPath)) {
                    val relativeName = pathAfterRoot.removePrefix(internalPath).removePrefix("/")
                    if (relativeName.isNotEmpty()) {
                        val newFile = File(targetDir, relativeName)
                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            newFile.outputStream().use { zis.copyTo(it) }
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }
}

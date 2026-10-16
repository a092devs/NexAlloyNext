package io.github.nexalloy.updates

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : UpdateDownloadState
    data class ReadyToInstall(val apkFile: File) : UpdateDownloadState
    data class Error(val message: String) : UpdateDownloadState
}

object AppInstaller {

    private const val BUFFER_SIZE = 8192

    /**
     * Downloads an APK from [apkUrl] to the app's cache directory without relying on an external browser.
     * Reports progress via [onProgress].
     */
    suspend fun downloadApk(
        context: Context,
        tag: String,
        apkUrl: String,
        onProgress: (UpdateDownloadState) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val cleanTag = tag.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val targetFile = File(updatesDir, "NexAlloyNext-$cleanTag.apk")

            onProgress(UpdateDownloadState.Downloading(0f, 0L, -1L))

            var connection: HttpURLConnection? = null
            try {
                var currentUrl = apkUrl
                var redirects = 0
                while (redirects < 5) {
                    val url = URL(currentUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 15000
                    conn.readTimeout = 30000
                    conn.setRequestProperty("User-Agent", "NexAlloyNext-Updater")

                    val code = conn.responseCode
                    if (code in 300..399) {
                        val location = conn.getHeaderField("Location") ?: break
                        conn.disconnect()
                        currentUrl = location
                        redirects++
                        continue
                    }
                    connection = conn
                    break
                }

                val conn = connection ?: error("Failed to establish download connection")
                if (conn.responseCode !in 200..299) {
                    error("Server responded with HTTP ${conn.responseCode}")
                }

                val totalLength = conn.contentLengthLong
                val tempFile = File.createTempFile("update_", ".tmp", updatesDir)

                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        var downloaded = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            val progress = if (totalLength > 0) {
                                (downloaded.toFloat() / totalLength.toFloat()).coerceIn(0f, 1f)
                            } else {
                                -1f
                            }
                            onProgress(UpdateDownloadState.Downloading(progress, downloaded, totalLength))
                        }
                    }
                }

                if (targetFile.exists()) targetFile.delete()
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                onProgress(UpdateDownloadState.ReadyToInstall(targetFile))
                targetFile
            } catch (e: Exception) {
                onProgress(UpdateDownloadState.Error(e.message ?: "Download failed"))
                throw e
            } finally {
                connection?.disconnect()
            }
        }
    }

    /**
     * Launches Android's native package installer via FileProvider to install the downloaded APK.
     */
    fun installApk(context: Context, apkFile: File): Result<Unit> = runCatching {
        require(apkFile.exists() && apkFile.length() > 0) { "Target APK file does not exist or is empty" }

        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(installIntent)
    }
}


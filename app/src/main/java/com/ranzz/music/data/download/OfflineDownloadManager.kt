package com.ranzz.music.data.download

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.ranzz.music.data.lyrics.LyricsRepository
import com.ranzz.music.data.model.Track
import com.ranzz.music.data.remote.HttpClientProvider
import com.ranzz.music.data.remote.MusicRepository
import com.ranzz.music.data.settings.SettingsManager
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

enum class DownloadStatus {
    NONE,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

data class OfflineTrackEntry(
    val track: Track,
    val localAudioPath: String,
    val localArtworkPath: String?,
    val mimeType: String,
    val fileSizeBytes: Long,
    val downloadedAt: Long
)

class OfflineDownloadManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val audioDir = File(appContext.filesDir, "offline_tracks").apply { mkdirs() }
    private val artworkDir = File(appContext.filesDir, "offline_artwork").apply { mkdirs() }

    private val downloadClient by lazy {
        HttpClientProvider.client.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val entriesMap = ConcurrentHashMap<String, OfflineTrackEntry>()
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val _downloadedTracks = MutableStateFlow<List<Track>>(emptyList())
    val downloadedTracks: StateFlow<List<Track>> = _downloadedTracks.asStateFlow()

    private val _downloadStatuses = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStatuses: StateFlow<Map<String, DownloadStatus>> = _downloadStatuses.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    private val _totalStorageBytes = MutableStateFlow(0L)
    val totalStorageBytes: StateFlow<Long> = _totalStorageBytes.asStateFlow()

    init {
        loadSavedDownloads()
    }

    private fun showToast(message: String) {
        mainHandler.post {
            try {
                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }

    private fun sanitizeFileName(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(64).ifBlank { "track" }
    }

    private fun loadSavedDownloads() {
        val raw = prefs.getString(KEY_DOWNLOAD_LIST, null) ?: return
        try {
            val arr = JSONArray(raw)
            val statusMap = mutableMapOf<String, DownloadStatus>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id")
                val audioPath = obj.optString("localAudioPath")
                if (id.isBlank() || audioPath.isBlank()) continue
                val audioFile = File(audioPath)
                if (!audioFile.exists() || audioFile.length() <= 0L) continue

                val artPath = obj.optString("localArtworkPath").takeIf { it.isNotBlank() && File(it).exists() }
                val mimeType = obj.optString("mimeType", "audio/mp4")
                val localAudioUri = Uri.fromFile(audioFile).toString()
                val rawThumb = obj.optString("thumbnailUrl")
                val effectiveThumb = if (artPath != null) Uri.fromFile(File(artPath)).toString() else rawThumb

                val track = Track(
                    id = id,
                    url = obj.optString("url", "https://www.youtube.com/watch?v=$id"),
                    title = obj.optString("title", "Unknown Title"),
                    artist = obj.optString("artist", "Unknown Artist"),
                    thumbnailUrl = effectiveThumb,
                    durationSeconds = obj.optLong("durationSeconds", 0L),
                    streamUrl = localAudioUri,
                    mimeType = mimeType,
                    isFavorite = obj.optBoolean("isFavorite", false)
                )

                val entry = OfflineTrackEntry(
                    track = track,
                    localAudioPath = audioFile.absolutePath,
                    localArtworkPath = artPath,
                    mimeType = mimeType,
                    fileSizeBytes = audioFile.length(),
                    downloadedAt = obj.optLong("downloadedAt", audioFile.lastModified())
                )
                entriesMap[id] = entry
                statusMap[id] = DownloadStatus.DOWNLOADED
            }
            refreshStateFlows(statusMap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load offline downloads: ${e.message}", e)
        }
    }

    private fun persistDownloads() {
        try {
            val sorted = entriesMap.values.sortedByDescending { it.downloadedAt }
            val arr = JSONArray()
            for (entry in sorted) {
                val obj = JSONObject().apply {
                    put("id", entry.track.id)
                    put("url", entry.track.url)
                    put("title", entry.track.title)
                    put("artist", entry.track.artist)
                    put("thumbnailUrl", entry.track.thumbnailUrl)
                    put("durationSeconds", entry.track.durationSeconds)
                    put("isFavorite", entry.track.isFavorite)
                    put("localAudioPath", entry.localAudioPath)
                    put("localArtworkPath", entry.localArtworkPath ?: "")
                    put("mimeType", entry.mimeType)
                    put("fileSizeBytes", entry.fileSizeBytes)
                    put("downloadedAt", entry.downloadedAt)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_DOWNLOAD_LIST, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist offline downloads: ${e.message}", e)
        }
    }

    private fun refreshStateFlows(overrideStatuses: Map<String, DownloadStatus>? = null) {
        val sortedEntries = entriesMap.values.sortedByDescending { it.downloadedAt }
        _downloadedTracks.value = sortedEntries.map { it.track }
        _totalStorageBytes.value = sortedEntries.sumOf { it.fileSizeBytes }
        if (overrideStatuses != null) {
            _downloadStatuses.value = overrideStatuses
        } else {
            val current = _downloadStatuses.value.toMutableMap()
            entriesMap.keys.forEach { id -> current[id] = DownloadStatus.DOWNLOADED }
            _downloadStatuses.value = current
        }
    }

    fun isDownloaded(trackId: String): Boolean {
        val entry = entriesMap[trackId] ?: return false
        val file = File(entry.localAudioPath)
        return file.exists() && file.length() > 0L
    }

    fun getStatus(trackId: String): DownloadStatus {
        return _downloadStatuses.value[trackId]
            ?: if (isDownloaded(trackId)) DownloadStatus.DOWNLOADED else DownloadStatus.NONE
    }

    fun getOfflineEntry(trackId: String): OfflineTrackEntry? {
        val entry = entriesMap[trackId] ?: return null
        val file = File(entry.localAudioPath)
        return if (file.exists() && file.length() > 0L) entry else null
    }

    fun toggleDownload(track: Track) {
        val currentStatus = getStatus(track.id)
        when (currentStatus) {
            DownloadStatus.DOWNLOADED -> removeDownload(track.id)
            DownloadStatus.DOWNLOADING -> cancelDownload(track.id)
            DownloadStatus.NONE, DownloadStatus.FAILED -> downloadTrack(track)
        }
    }

    fun cancelDownload(trackId: String) {
        activeJobs.remove(trackId)?.cancel()
        val updatedStatus = _downloadStatuses.value.toMutableMap()
        updatedStatus.remove(trackId)
        _downloadStatuses.value = updatedStatus

        val updatedProgress = _downloadProgress.value.toMutableMap()
        updatedProgress.remove(trackId)
        _downloadProgress.value = updatedProgress

        val safeId = sanitizeFileName(trackId)
        File(audioDir, "$safeId.part").delete()
    }

    fun removeDownload(trackId: String) {
        activeJobs.remove(trackId)?.cancel()
        val removed = entriesMap.remove(trackId)
        if (removed != null) {
            try {
                File(removed.localAudioPath).delete()
                removed.localArtworkPath?.let { File(it).delete() }
            } catch (_: Exception) {}
        } else {
            val safeId = sanitizeFileName(trackId)
            audioDir.listFiles()?.filter { it.name.startsWith(safeId) }?.forEach { it.delete() }
            artworkDir.listFiles()?.filter { it.name.startsWith(safeId) }?.forEach { it.delete() }
        }
        val updatedStatus = _downloadStatuses.value.toMutableMap()
        updatedStatus.remove(trackId)
        _downloadStatuses.value = updatedStatus

        val updatedProgress = _downloadProgress.value.toMutableMap()
        updatedProgress.remove(trackId)
        _downloadProgress.value = updatedProgress

        persistDownloads()
        refreshStateFlows()
        showToast("Unduhan lagu dihapus")
    }

    fun clearAllDownloads() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        entriesMap.clear()
        try {
            audioDir.listFiles()?.forEach { it.delete() }
            artworkDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
        _downloadStatuses.value = emptyMap()
        _downloadProgress.value = emptyMap()
        persistDownloads()
        refreshStateFlows(emptyMap())
        showToast("Semua unduhan offline dihapus")
    }

    fun downloadTrack(track: Track) {
        if (track.id.isBlank()) return
        if (getStatus(track.id) == DownloadStatus.DOWNLOADING) return
        if (isDownloaded(track.id)) {
            showToast("${track.title} sudah tersimpan offline")
            return
        }

        val statusMap = _downloadStatuses.value.toMutableMap()
        statusMap[track.id] = DownloadStatus.DOWNLOADING
        _downloadStatuses.value = statusMap

        val progressMap = _downloadProgress.value.toMutableMap()
        progressMap[track.id] = 0
        _downloadProgress.value = progressMap

        showToast("Mengunduh ${track.title}...")

        val job = scope.launch {
            val safeId = sanitizeFileName(track.id)
            val tempFile = File(audioDir, "$safeId.part")
            try {
                val repo = MusicRepository()
                val quality = SettingsManager.getInstance().audioQuality.value
                val resolved = repo.getAudioStream(track.url, quality)
                    ?: repo.getFallbackStreamUrl(track.url)?.let { com.ranzz.music.data.remote.ResolvedStream(it, "audio/webm") }

                if (resolved == null || resolved.url.isBlank()) {
                    markDownloadFailed(track)
                    return@launch
                }

                var downloadSuccess = downloadStreamToFile(track.id, resolved.url, tempFile)
                var finalMime = resolved.mimeType ?: "audio/mp4"

                if (!downloadSuccess) {
                    repo.evictStreamUrl(track.url)
                    val fallbackUrl = repo.getFallbackStreamUrl(track.url)
                    if (!fallbackUrl.isNullOrBlank()) {
                        finalMime = "audio/webm"
                        downloadSuccess = downloadStreamToFile(track.id, fallbackUrl, tempFile)
                    }
                }

                if (!downloadSuccess || !tempFile.exists() || tempFile.length() <= 0L) {
                    tempFile.delete()
                    markDownloadFailed(track)
                    return@launch
                }

                val ext = if (finalMime.contains("webm") || finalMime.contains("opus")) "webm" else "m4a"
                val finalAudioFile = File(audioDir, "$safeId.$ext")
                if (finalAudioFile.exists()) finalAudioFile.delete()
                if (!tempFile.renameTo(finalAudioFile)) {
                    tempFile.copyTo(finalAudioFile, overwrite = true)
                    tempFile.delete()
                }

                val artworkFile = File(artworkDir, "$safeId.jpg")
                val savedArtworkPath = downloadArtwork(track, repo, artworkFile)

                scope.launch {
                    try {
                        LyricsRepository(appContext).getLyrics(
                            trackId = track.id,
                            title = track.title,
                            artist = track.artist,
                            durationSeconds = track.durationSeconds,
                            autoTranslate = SettingsManager.getInstance().autoTranslate.value
                        )
                    } catch (_: Exception) {}
                }

                val localAudioUri = Uri.fromFile(finalAudioFile).toString()
                val effectiveThumb = if (savedArtworkPath != null) {
                    Uri.fromFile(File(savedArtworkPath)).toString()
                } else {
                    track.highResThumbnailUrl
                }

                val offlineTrack = track.copy(
                    thumbnailUrl = effectiveThumb,
                    streamUrl = localAudioUri,
                    mimeType = finalMime
                )

                val entry = OfflineTrackEntry(
                    track = offlineTrack,
                    localAudioPath = finalAudioFile.absolutePath,
                    localArtworkPath = savedArtworkPath,
                    mimeType = finalMime,
                    fileSizeBytes = finalAudioFile.length(),
                    downloadedAt = System.currentTimeMillis()
                )

                entriesMap[track.id] = entry

                val doneProgress = _downloadProgress.value.toMutableMap()
                doneProgress.remove(track.id)
                _downloadProgress.value = doneProgress

                val doneStatus = _downloadStatuses.value.toMutableMap()
                doneStatus[track.id] = DownloadStatus.DOWNLOADED
                _downloadStatuses.value = doneStatus

                persistDownloads()
                refreshStateFlows()
                showToast("Berhasil diunduh: ${track.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Download error for ${track.title}: ${e.message}", e)
                tempFile.delete()
                markDownloadFailed(track)
            } finally {
                activeJobs.remove(track.id)
            }
        }
        activeJobs[track.id] = job
    }

    private fun downloadStreamToFile(trackId: String, streamUrl: String, targetFile: File): Boolean {
        return try {
            if (targetFile.exists()) targetFile.delete()
            val req = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            downloadClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val body = resp.body ?: return false
                val totalBytes = body.contentLength()
                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var downloaded = 0L
                        var lastReportedProgress = -1
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (totalBytes > 0L) {
                                val pct = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                                if (pct != lastReportedProgress && (pct - lastReportedProgress >= 3 || pct == 100)) {
                                    lastReportedProgress = pct
                                    val pMap = _downloadProgress.value.toMutableMap()
                                    pMap[trackId] = pct
                                    _downloadProgress.value = pMap
                                }
                            }
                        }
                        output.flush()
                    }
                }
            }
            targetFile.exists() && targetFile.length() > 1024L
        } catch (e: Exception) {
            Log.e(TAG, "Stream download failed: ${e.message}")
            false
        }
    }

    private suspend fun downloadArtwork(track: Track, repo: MusicRepository, targetFile: File): String? {
        return try {
            val bestUrl = runCatching { repo.resolveBestThumbnail(track) }.getOrDefault(track.highResThumbnailUrl)
            if (bestUrl.isBlank() || bestUrl.startsWith("file://")) return null
            val req = Request.Builder()
                .url(bestUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            downloadClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body ?: return null
                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            if (targetFile.exists() && targetFile.length() > 0L) targetFile.absolutePath else null
        } catch (_: Exception) {
            null
        }
    }

    private fun markDownloadFailed(track: Track) {
        val pMap = _downloadProgress.value.toMutableMap()
        pMap.remove(track.id)
        _downloadProgress.value = pMap

        val sMap = _downloadStatuses.value.toMutableMap()
        sMap[track.id] = DownloadStatus.FAILED
        _downloadStatuses.value = sMap

        showToast("Gagal mengunduh ${track.title}")
    }

    companion object {
        private const val TAG = "OfflineDownloadManager"
        private const val PREFS_NAME = "ranzz_offline_downloads"
        private const val KEY_DOWNLOAD_LIST = "offline_tracks_json"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

        @Volatile
        private var instance: OfflineDownloadManager? = null

        fun getInstance(context: Context): OfflineDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: OfflineDownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

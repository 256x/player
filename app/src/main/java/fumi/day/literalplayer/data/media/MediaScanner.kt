package fumi.day.literalplayer.data.media

import android.media.MediaMetadataRetriever
import fumi.day.literalplayer.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val SUPPORTED_EXTENSIONS = setOf("mp3", "m4a", "aac", "opus", "flac", "ogg", "wav")

@Singleton
class MediaScanner @Inject constructor() {

    suspend fun scan(rootFolders: Set<String>): List<Track> =
        scan(rootFolders, emptyMap())

    suspend fun scan(rootFolders: Set<String>, existing: Map<String, Track>): List<Track> =
        withContext(Dispatchers.IO) {
            rootFolders.flatMap { path ->
                File(path).walkTopDown()
                    .filter { it.isFile && it.extension.lowercase() in SUPPORTED_EXTENSIONS }
                    .mapNotNull { file ->
                        val cached = existing[file.absolutePath]
                        if (cached != null &&
                            cached.lastModified == file.lastModified() &&
                            cached.fileSizeBytes == file.length()
                        ) {
                            cached
                        } else {
                            readTrack(file)
                        }
                    }
                    .toList()
            }
        }

    private fun readTrack(file: File): Track? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            Track(
                id = file.absolutePath,
                path = file.absolutePath,
                fileName = file.name,
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "",
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "",
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "",
                year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR) ?: "",
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L,
                fileSizeBytes = file.length(),
                mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "",
                lastModified = file.lastModified(),
                trackNumber = parseTrackNumber(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ) ?: parseLeadingNumber(file.name) ?: 0,
            )
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    /** ID3 TRCK は "3" or "3/12" 形式 */
    private fun parseTrackNumber(raw: String?): Int? =
        raw?.substringBefore('/')?.trim()?.toIntOrNull()?.takeIf { it > 0 }

    /** タグが無い場合のフォールバック: ファイル名先頭の "01 " 等を拾う */
    private fun parseLeadingNumber(fileName: String): Int? =
        Regex("""^(\d{1,3})[\s._-]""").find(fileName)?.groupValues?.get(1)?.toIntOrNull()
            ?.takeIf { it > 0 }
}

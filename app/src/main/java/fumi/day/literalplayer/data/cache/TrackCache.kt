package fumi.day.literalplayer.data.cache

import android.content.Context
import fumi.day.literalplayer.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object TrackCache {
    private const val FILE = "track_cache.json"

    // trackNumber 追加時に上げる。ロード時に不一致なら強制フルスキャン
    private const val CACHE_VERSION = 2

    suspend fun save(context: Context, folders: Set<String>, tracks: List<Track>) =
        withContext(Dispatchers.IO) {
            val root = JSONObject()
            root.put("cacheVersion", CACHE_VERSION)
            val fArr = JSONArray()
            folders.sorted().forEach { fArr.put(it) }
            root.put("folders", fArr)
            val tArr = JSONArray()
            tracks.forEach { t ->
                tArr.put(JSONObject().apply {
                    put("id", t.id)
                    put("path", t.path)
                    put("fileName", t.fileName)
                    put("title", t.title)
                    put("artist", t.artist)
                    put("album", t.album)
                    put("year", t.year)
                    put("durationMs", t.durationMs)
                    put("fileSizeBytes", t.fileSizeBytes)
                    put("mimeType", t.mimeType)
                    put("lastModified", t.lastModified)
                    put("trackNumber", t.trackNumber)
                })
            }
            root.put("tracks", tArr)
            context.filesDir.resolve(FILE).writeText(root.toString())
        }

    suspend fun load(context: Context, folders: Set<String>): List<Track>? =
        withContext(Dispatchers.IO) {
            val file = context.filesDir.resolve(FILE)
            if (!file.exists()) return@withContext null
            runCatching {
                val root = JSONObject(file.readText())
                if (root.optInt("cacheVersion", 0) != CACHE_VERSION) return@withContext null
                val fArr = root.getJSONArray("folders")
                val cached = (0 until fArr.length()).map { fArr.getString(it) }.toSet()
                if (cached != folders) return@withContext null
                val tArr = root.getJSONArray("tracks")
                (0 until tArr.length()).map { i ->
                    tArr.getJSONObject(i).let { o ->
                        Track(
                            id           = o.getString("id"),
                            path         = o.getString("path"),
                            fileName     = o.getString("fileName"),
                            title        = o.getString("title"),
                            artist       = o.getString("artist"),
                            album        = o.getString("album"),
                            year         = o.getString("year"),
                            durationMs   = o.getLong("durationMs"),
                            fileSizeBytes = o.getLong("fileSizeBytes"),
                            mimeType     = o.getString("mimeType"),
                            lastModified = o.getLong("lastModified"),
                            trackNumber  = o.optInt("trackNumber", 0),
                        )
                    }
                }
            }.getOrNull()
        }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        context.filesDir.resolve(FILE).delete()
    }
}

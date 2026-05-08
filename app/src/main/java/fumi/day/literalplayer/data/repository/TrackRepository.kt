package fumi.day.literalplayer.data.repository

import fumi.day.literalplayer.data.db.dao.TrackPositionDao
import fumi.day.literalplayer.data.db.entity.TrackPositionEntity
import fumi.day.literalplayer.data.media.MediaScanner
import fumi.day.literalplayer.domain.model.SortOrder
import fumi.day.literalplayer.domain.model.Track
import fumi.day.literalplayer.domain.model.displayAlbum
import fumi.day.literalplayer.domain.model.displayArtist
import fumi.day.literalplayer.domain.model.displayTitle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepository @Inject constructor(
    private val scanner: MediaScanner,
    private val positionDao: TrackPositionDao,
) {
    private var cachedTracks: List<Track> = emptyList()

    suspend fun loadTracks(rootFolders: Set<String>): List<Track> {
        cachedTracks = scanner.scan(rootFolders)
        return cachedTracks
    }

    fun getCached(): List<Track> = cachedTracks

    fun sorted(tracks: List<Track>, order: SortOrder): List<Track> = when (order) {
        SortOrder.NAME -> tracks.sortedBy { it.displayTitle.lowercase() }
        SortOrder.DATE -> tracks.sortedByDescending { it.lastModified }
        SortOrder.SIZE -> tracks.sortedByDescending { it.fileSizeBytes }
    }

    fun groupedByArtist(tracks: List<Track>): Map<String, List<Track>> =
        tracks.groupBy { it.displayArtist }.toSortedMap()

    fun groupedByAlbum(tracks: List<Track>): Map<String, Pair<String, List<Track>>> =
        tracks.groupBy { it.displayAlbum }
            .mapValues { (_, v) -> Pair(v.first().displayArtist, v) }
            .toSortedMap()

    fun tracksForArtist(tracks: List<Track>, artist: String): Map<String, List<Track>> =
        tracks.filter { it.displayArtist == artist }
            .groupBy { it.displayAlbum }
            .toSortedMap()

    suspend fun savePosition(trackId: String, positionMs: Long) {
        positionDao.save(TrackPositionEntity(trackId, positionMs))
    }

    suspend fun getPosition(trackId: String): Long =
        positionDao.get(trackId)?.positionMs ?: 0L

    suspend fun clearPosition(trackId: String) {
        positionDao.delete(trackId)
    }
}

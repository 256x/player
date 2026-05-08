package fumi.day.literalplayer.data.repository

import fumi.day.literalplayer.data.db.dao.FavoritesDao
import fumi.day.literalplayer.data.db.entity.FavoritesListEntity
import fumi.day.literalplayer.data.db.entity.FavoritesTrackEntity
import fumi.day.literalplayer.domain.model.FavoritesList
import fumi.day.literalplayer.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val dao: FavoritesDao,
    private val trackRepository: TrackRepository,
) {
    val lists: Flow<List<FavoritesList>> = dao.getAllLists().map { entities ->
        entities.map { FavoritesList(id = it.id, name = it.name) }
    }

    suspend fun createList(name: String): Long =
        dao.createList(FavoritesListEntity(name = name))

    suspend fun deleteList(listId: Long) {
        dao.deleteList(listId)
    }

    suspend fun addTrack(listId: Long, track: Track) {
        dao.addTrack(FavoritesTrackEntity(listId = listId, trackId = track.id, trackPath = track.path))
    }

    suspend fun removeTrack(listId: Long, trackId: String) {
        dao.removeTrack(listId, trackId)
    }

    fun tracksForList(listId: Long): Flow<List<Track>> =
        dao.getTracksForList(listId).map { entities ->
            val allTracks = trackRepository.getCached().associateBy { it.id }
            entities.mapNotNull { allTracks[it.trackId] }
        }

    suspend fun getListIdsForTrack(trackId: String): List<Long> =
        dao.getListIdsForTrack(trackId)
}

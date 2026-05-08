package fumi.day.literalplayer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fumi.day.literalplayer.data.db.entity.FavoritesListEntity
import fumi.day.literalplayer.data.db.entity.FavoritesTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites_lists ORDER BY createdAt DESC")
    fun getAllLists(): Flow<List<FavoritesListEntity>>

    @Insert
    suspend fun createList(entity: FavoritesListEntity): Long

    @Query("DELETE FROM favorites_lists WHERE id = :listId")
    suspend fun deleteList(listId: Long)

    @Query("SELECT * FROM favorites_tracks WHERE listId = :listId ORDER BY addedAt ASC")
    fun getTracksForList(listId: Long): Flow<List<FavoritesTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrack(entity: FavoritesTrackEntity)

    @Query("DELETE FROM favorites_tracks WHERE listId = :listId AND trackId = :trackId")
    suspend fun removeTrack(listId: Long, trackId: String)

    @Query("SELECT listId FROM favorites_tracks WHERE trackId = :trackId")
    suspend fun getListIdsForTrack(trackId: String): List<Long>
}

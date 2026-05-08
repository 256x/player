package fumi.day.literalplayer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fumi.day.literalplayer.data.db.entity.TrackPositionEntity

@Dao
interface TrackPositionDao {
    @Query("SELECT * FROM track_positions WHERE trackId = :trackId")
    suspend fun get(trackId: String): TrackPositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: TrackPositionEntity)

    @Query("DELETE FROM track_positions WHERE trackId = :trackId")
    suspend fun delete(trackId: String)
}

package fumi.day.literalplayer.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_positions")
data class TrackPositionEntity(
    @PrimaryKey val trackId: String,
    val positionMs: Long,
    val updatedAt: Long = System.currentTimeMillis(),
)

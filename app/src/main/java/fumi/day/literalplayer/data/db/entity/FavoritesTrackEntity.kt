package fumi.day.literalplayer.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "favorites_tracks",
    primaryKeys = ["listId", "trackId"],
    foreignKeys = [ForeignKey(
        entity = FavoritesListEntity::class,
        parentColumns = ["id"],
        childColumns = ["listId"],
        onDelete = ForeignKey.CASCADE,
    )]
)
data class FavoritesTrackEntity(
    val listId: Long,
    val trackId: String,
    val trackPath: String,
    val addedAt: Long = System.currentTimeMillis(),
)

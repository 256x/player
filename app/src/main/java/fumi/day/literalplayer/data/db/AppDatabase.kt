package fumi.day.literalplayer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import fumi.day.literalplayer.data.db.dao.FavoritesDao
import fumi.day.literalplayer.data.db.dao.TrackPositionDao
import fumi.day.literalplayer.data.db.entity.FavoritesListEntity
import fumi.day.literalplayer.data.db.entity.FavoritesTrackEntity
import fumi.day.literalplayer.data.db.entity.TrackPositionEntity

@Database(
    entities = [
        TrackPositionEntity::class,
        FavoritesListEntity::class,
        FavoritesTrackEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackPositionDao(): TrackPositionDao
    abstract fun favoritesDao(): FavoritesDao
}

package fumi.day.literalplayer.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fumi.day.literalplayer.data.db.AppDatabase
import fumi.day.literalplayer.data.db.dao.FavoritesDao
import fumi.day.literalplayer.data.db.dao.TrackPositionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "literalplayer.db").build()

    @Provides
    fun provideTrackPositionDao(db: AppDatabase): TrackPositionDao = db.trackPositionDao()

    @Provides
    fun provideFavoritesDao(db: AppDatabase): FavoritesDao = db.favoritesDao()
}

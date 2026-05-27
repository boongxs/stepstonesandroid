package com.flutter.stepstonesflt.di

import android.content.Context
import androidx.room.Room
import com.flutter.stepstonesflt.data.local.AppDatabase
import com.flutter.stepstonesflt.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "stepstones.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides fun provideAlbumDao(db: AppDatabase): AlbumDao = db.albumDao()
    @Provides fun provideMediaItemDao(db: AppDatabase): MediaItemDao = db.mediaItemDao()
    @Provides fun provideMediaAlbumDao(db: AppDatabase): MediaAlbumDao = db.mediaAlbumDao()
    @Provides fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()
}

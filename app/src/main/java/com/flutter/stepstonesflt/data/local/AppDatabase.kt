package com.flutter.stepstonesflt.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.flutter.stepstonesflt.data.local.dao.*
import com.flutter.stepstonesflt.data.local.entity.*

@Database(
    entities = [
        Album::class,
        MediaItem::class,
        MediaAlbum::class,
        Tag::class,
        MediaTag::class,
        PendingReview::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun mediaAlbumDao(): MediaAlbumDao
    abstract fun tagDao(): TagDao
    abstract fun pendingReviewDao(): PendingReviewDao
}

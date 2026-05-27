package com.flutter.stepstonesflt.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flutter.stepstonesflt.data.local.dao.*
import com.flutter.stepstonesflt.data.local.entity.*

@Database(
    entities = [
        Album::class,
        MediaItem::class,
        MediaAlbum::class,
        Tag::class,
        MediaTag::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun mediaAlbumDao(): MediaAlbumDao
    abstract fun tagDao(): TagDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE media_items_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fileHash TEXT NOT NULL,
                        filePath TEXT NOT NULL,
                        originalFileName TEXT NOT NULL,
                        fileType TEXT NOT NULL,
                        thumbnailPath TEXT,
                        durationMs INTEGER,
                        width INTEGER,
                        height INTEGER,
                        mediaDate INTEGER,
                        addedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO media_items_new (id, fileHash, filePath, originalFileName, fileType, thumbnailPath, durationMs, width, height, mediaDate, addedAt)
                    SELECT id, fileHash, filePath, originalFileName, fileType, thumbnailPath, durationMs, width, height, mediaDate, addedAt
                    FROM media_items
                """.trimIndent())
                database.execSQL("DROP TABLE media_items")
                database.execSQL("ALTER TABLE media_items_new RENAME TO media_items")
                database.execSQL("CREATE UNIQUE INDEX index_media_items_fileHash ON media_items (fileHash)")
                database.execSQL("DROP TABLE IF EXISTS pending_reviews")
            }
        }
    }
}

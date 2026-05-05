package com.flutter.stepstonesflt.data.local.dao

import androidx.room.*
import com.flutter.stepstonesflt.data.local.entity.MediaAlbum

@Dao
interface MediaAlbumDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(mediaAlbum: MediaAlbum)

    @Delete
    suspend fun delete(mediaAlbum: MediaAlbum)

    @Query("SELECT albumId FROM media_albums WHERE mediaId = :mediaId")
    suspend fun getAlbumIdsForMedia(mediaId: Long): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM media_albums WHERE mediaId = :mediaId AND albumId = :albumId)")
    suspend fun exists(mediaId: Long, albumId: Long): Boolean

    @Query("SELECT COUNT(*) FROM media_albums WHERE mediaId = :mediaId")
    suspend fun albumCountForMedia(mediaId: Long): Int
}

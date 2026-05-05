package com.flutter.stepstonesflt.data.local.dao

import androidx.room.*
import com.flutter.stepstonesflt.data.local.entity.MediaItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Query("""
        SELECT mi.* FROM media_items mi
        INNER JOIN media_albums ma ON mi.id = ma.mediaId
        WHERE ma.albumId = :albumId
        ORDER BY mi.addedAt DESC
    """)
    fun getByAlbum(albumId: Long): Flow<List<MediaItem>>

    @Query("""
        SELECT mi.* FROM media_items mi
        INNER JOIN media_albums ma ON mi.id = ma.mediaId
        INNER JOIN media_tags mt ON mi.id = mt.mediaId
        INNER JOIN tags t ON mt.tagId = t.id
        WHERE ma.albumId = :albumId AND t.name LIKE '%' || :query || '%'
        GROUP BY mi.id
        ORDER BY mi.addedAt DESC
    """)
    fun searchByAlbumAndTag(albumId: Long, query: String): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE fileHash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): MediaItem?

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: Long): MediaItem?

    @Query("SELECT * FROM media_items WHERE perceptualHash IS NULL")
    suspend fun getWithoutPHash(): List<MediaItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: MediaItem): Long

    @Update
    suspend fun update(item: MediaItem)

    @Delete
    suspend fun delete(item: MediaItem)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}

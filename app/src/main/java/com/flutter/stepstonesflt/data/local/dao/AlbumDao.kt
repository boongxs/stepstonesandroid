package com.flutter.stepstonesflt.data.local.dao

import androidx.room.*
import com.flutter.stepstonesflt.data.local.entity.Album
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY isDefault DESC, name ASC")
    fun getAll(): Flow<List<Album>>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getById(id: Long): Album?

    @Query("SELECT * FROM albums WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): Album?

    @Insert
    suspend fun insert(album: Album): Long

    @Update
    suspend fun update(album: Album)

    @Delete
    suspend fun delete(album: Album)
}

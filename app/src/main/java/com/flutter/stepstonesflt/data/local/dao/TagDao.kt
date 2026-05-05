package com.flutter.stepstonesflt.data.local.dao

import androidx.room.*
import com.flutter.stepstonesflt.data.local.entity.MediaTag
import com.flutter.stepstonesflt.data.local.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN media_tags mt ON t.id = mt.tagId
        WHERE mt.mediaId = :mediaId
        ORDER BY t.name ASC
    """)
    fun getTagsForMedia(mediaId: Long): Flow<List<Tag>>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN media_tags mt ON t.id = mt.tagId
        WHERE mt.mediaId = :mediaId
        ORDER BY t.name ASC
    """)
    suspend fun getTagsForMediaOnce(mediaId: Long): List<Tag>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Tag?

    @Query("SELECT * FROM tags WHERE name LIKE :prefix || '%' ORDER BY name ASC LIMIT 10")
    suspend fun getSuggestions(prefix: String): List<Tag>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaTag(mediaTag: MediaTag)

    @Delete
    suspend fun deleteMediaTag(mediaTag: MediaTag)

    @Query("DELETE FROM tags WHERE id NOT IN (SELECT DISTINCT tagId FROM media_tags)")
    suspend fun deleteOrphanTags()
}

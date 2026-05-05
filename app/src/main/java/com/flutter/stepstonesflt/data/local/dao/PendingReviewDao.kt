package com.flutter.stepstonesflt.data.local.dao

import androidx.room.*
import com.flutter.stepstonesflt.data.local.entity.PendingReview
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingReviewDao {
    @Query("SELECT * FROM pending_reviews WHERE albumId = :albumId ORDER BY id ASC LIMIT 1")
    fun getNextForAlbum(albumId: Long): Flow<PendingReview?>

    @Query("SELECT COUNT(*) FROM pending_reviews WHERE albumId = :albumId")
    fun countForAlbum(albumId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(review: PendingReview)

    @Delete
    suspend fun delete(review: PendingReview)

    @Query("DELETE FROM pending_reviews WHERE albumId = :albumId AND (itemAId = :mediaId OR itemBId = :mediaId)")
    suspend fun deleteForMedia(albumId: Long, mediaId: Long)
}

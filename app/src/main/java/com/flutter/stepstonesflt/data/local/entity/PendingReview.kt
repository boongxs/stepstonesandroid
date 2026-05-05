package com.flutter.stepstonesflt.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_reviews",
    foreignKeys = [
        ForeignKey(
            entity = MediaItem::class,
            parentColumns = ["id"],
            childColumns = ["itemAId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MediaItem::class,
            parentColumns = ["id"],
            childColumns = ["itemBId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Album::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("itemAId"), Index("itemBId"), Index("albumId")],
)
data class PendingReview(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemAId: Long,
    val itemBId: Long,
    val albumId: Long,
    val similarityPercent: Float,
)

package com.flutter.stepstonesflt.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    indices = [Index(value = ["fileHash"], unique = true)],
)
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileHash: String,
    val filePath: String,
    val originalFileName: String,
    val fileType: MediaType,
    val thumbnailPath: String? = null,
    val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val mediaDate: Long? = null,
    val addedAt: Long = System.currentTimeMillis(),
)

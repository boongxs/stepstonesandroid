package com.flutter.stepstonesflt.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.flutter.stepstonesflt.data.local.dao.MediaAlbumDao
import com.flutter.stepstonesflt.data.local.dao.MediaItemDao
import com.flutter.stepstonesflt.data.local.entity.MediaAlbum
import com.flutter.stepstonesflt.data.local.entity.MediaItem
import com.flutter.stepstonesflt.data.local.entity.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class IngestResult {
    data class Success(val mediaItemId: Long) : IngestResult()
    object AlreadyInAlbum : IngestResult()
    object UnsupportedType : IngestResult()
    data class Error(val message: String) : IngestResult()
}

@Singleton
class IngestRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaItemDao: MediaItemDao,
    private val mediaAlbumDao: MediaAlbumDao,
) {
    private val mediaDir = File(context.filesDir, "media").also { it.mkdirs() }
    private val thumbnailDir = File(context.filesDir, "thumbnails").also { it.mkdirs() }

    suspend fun ingest(uri: Uri, albumId: Long): IngestResult = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri)
            ?: return@withContext IngestResult.UnsupportedType
        val mediaType = mimeTypeToMediaType(mimeType)
            ?: return@withContext IngestResult.UnsupportedType

        val ext = extensionFromMime(mimeType)
        val originalName = resolveFileName(uri)
        val destFile = File(mediaDir, "${UUID.randomUUID()}.$ext")

        val hash = try {
            copyAndHash(uri, destFile)
        } catch (e: Exception) {
            destFile.delete()
            return@withContext IngestResult.Error(e.message ?: "Failed to read file")
        }

        if (hash == null) {
            destFile.delete()
            return@withContext IngestResult.Error("Could not open file")
        }

        // Already in DB — link to album if not already, discard the copy
        val existing = mediaItemDao.getByHash(hash)
        if (existing != null) {
            destFile.delete()
            if (mediaAlbumDao.exists(existing.id, albumId)) return@withContext IngestResult.AlreadyInAlbum
            mediaAlbumDao.insert(MediaAlbum(existing.id, albumId))
            return@withContext IngestResult.Success(existing.id)
        }

        val meta = try {
            extractMetadata(mediaType, destFile)
        } catch (e: Exception) {
            Metadata(null, 0, 0, null)
        }

        val item = MediaItem(
            fileHash = hash,
            filePath = destFile.absolutePath,
            originalFileName = originalName,
            fileType = mediaType,
            thumbnailPath = meta.thumbnailPath,
            durationMs = meta.durationMs,
            width = meta.width,
            height = meta.height,
            mediaDate = System.currentTimeMillis(),
        )

        val id = mediaItemDao.insert(item)
        mediaAlbumDao.insert(MediaAlbum(id, albumId))
        IngestResult.Success(id)
    }

    private fun mimeTypeToMediaType(mimeType: String): MediaType? = when {
        mimeType == "image/gif" -> MediaType.GIF
        mimeType.startsWith("image/") -> MediaType.IMAGE
        mimeType.startsWith("video/") -> MediaType.VIDEO
        mimeType.startsWith("audio/") -> MediaType.AUDIO
        else -> null
    }

    // Streams the URI into dest while computing SHA-256, avoiding loading large files into memory.
    private fun copyAndHash(uri: Uri, dest: File): String? {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { stream ->
            FileOutputStream(dest).use { out ->
                val buf = ByteArray(8192)
                var read: Int
                while (stream.read(buf).also { read = it } != -1) {
                    digest.update(buf, 0, read)
                    out.write(buf, 0, read)
                }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun resolveFileName(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && col >= 0) return cursor.getString(col)
        }
        return uri.lastPathSegment ?: "unknown"
    }

    private fun extensionFromMime(mimeType: String): String = when (mimeType) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "video/mp4" -> "mp4"
        "video/webm" -> "webm"
        "video/3gpp" -> "3gp"
        "audio/mpeg" -> "mp3"
        "audio/mp4" -> "m4a"
        "audio/ogg" -> "ogg"
        "audio/wav" -> "wav"
        "audio/flac" -> "flac"
        else -> mimeType.substringAfter("/").substringBefore(";").ifBlank { "bin" }
    }

    private data class Metadata(
        val thumbnailPath: String?,
        val width: Int,
        val height: Int,
        val durationMs: Long?,
    )

    private fun extractMetadata(mediaType: MediaType, file: File): Metadata {
        return when (mediaType) {
            MediaType.IMAGE -> {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, opts)
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                val thumb = bitmap?.let { saveThumbnail(it) }
                bitmap?.recycle()
                Metadata(thumb, opts.outWidth.coerceAtLeast(0), opts.outHeight.coerceAtLeast(0), null)
            }
            MediaType.GIF -> {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, opts)
                val w = opts.outWidth.coerceAtLeast(0)
                val h = opts.outHeight.coerceAtLeast(0)
                // BitmapFactory can fail on some animated GIFs; fall back to MediaMetadataRetriever
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: run {
                    val r = MediaMetadataRetriever()
                    try {
                        r.setDataSource(file.absolutePath)
                        r.getFrameAtTime(0)
                    } finally {
                        r.release()
                    }
                }
                val thumb = bitmap?.let { saveThumbnail(it) }
                bitmap?.recycle()
                Metadata(thumb, w, h, null)
            }
            MediaType.VIDEO -> {
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(file.absolutePath)
                    val duration = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                    val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                    val frame = r.getFrameAtTime(0)
                    val thumb = frame?.let { saveThumbnail(it) }
                    frame?.recycle()
                    Metadata(thumb, w, h, duration)
                } finally {
                    r.release()
                }
            }
            MediaType.AUDIO -> {
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(file.absolutePath)
                    val duration = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    Metadata(null, 0, 0, duration)
                } finally {
                    r.release()
                }
            }
        }
    }

    private fun saveThumbnail(bitmap: Bitmap): String? {
        val scaled = scaleBitmap(bitmap, 512)
        val file = File(thumbnailDir, "${UUID.randomUUID()}.jpg")
        return try {
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (scaled !== bitmap) scaled.recycle()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleBitmap(src: Bitmap, maxPx: Int): Bitmap {
        val w = src.width; val h = src.height
        if (w <= maxPx && h <= maxPx) return src
        val scale = maxPx.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(src, (w * scale).toInt(), (h * scale).toInt(), true)
    }
}

package com.flutter.stepstonesflt.data.repository

import android.content.Context
import com.flutter.stepstonesflt.data.local.entity.MediaItem
import com.flutter.stepstonesflt.data.local.entity.MediaType
import com.flutter.stepstonesflt.data.local.entity.Tag
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BundleExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val bundleDir = File(context.cacheDir, "bundles").also { it.mkdirs() }

    suspend fun export(
        items: List<MediaItem>,
        tagMap: Map<Long, List<Tag>>,
    ): File = withContext(Dispatchers.IO) {
        bundleDir.listFiles()?.forEach { it.delete() }
        val outFile = File(bundleDir, "${System.currentTimeMillis()}.stepstone")

        ZipOutputStream(FileOutputStream(outFile).buffered()).use { zip ->
            val usedNames = mutableSetOf<String>()
            val metadata = JSONObject()

            items.forEach { item ->
                val entryName = uniqueName(item.originalFileName, usedNames)
                usedNames += entryName

                zip.putNextEntry(ZipEntry("media/$entryName"))
                File(item.filePath).inputStream().use { it.copyTo(zip) }
                zip.closeEntry()

                item.thumbnailPath?.let { thumbPath ->
                    zip.putNextEntry(ZipEntry("thumbs/$entryName"))
                    File(thumbPath).inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }

                val tagsString = tagMap[item.id]?.joinToString(" ") { it.name } ?: ""
                metadata.put(entryName, JSONObject().apply {
                    put("tags", tagsString)
                    put("thumbnailPath", if (item.thumbnailPath != null) entryName else JSONObject.NULL)
                    put("fileType", item.fileType.toDesktopString())
                    put("width", item.width ?: JSONObject.NULL)
                    put("height", item.height ?: JSONObject.NULL)
                    put("duration", item.durationMs ?: JSONObject.NULL)
                })
            }

            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write(metadata.toString().toByteArray())
            zip.closeEntry()
        }

        outFile
    }

    private fun uniqueName(original: String, used: Set<String>): String {
        if (original !in used) return original
        val dot = original.lastIndexOf('.')
        val base = if (dot >= 0) original.substring(0, dot) else original
        val ext = if (dot >= 0) original.substring(dot) else ""
        var n = 2
        while ("${base}_$n$ext" in used) n++
        return "${base}_$n$ext"
    }

    private fun MediaType.toDesktopString() = when (this) {
        MediaType.IMAGE -> "image"
        MediaType.GIF -> "gif"
        MediaType.VIDEO -> "video"
        MediaType.AUDIO -> "audio"
    }
}

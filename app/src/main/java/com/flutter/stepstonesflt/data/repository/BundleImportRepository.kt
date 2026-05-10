package com.flutter.stepstonesflt.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.flutter.stepstonesflt.data.local.dao.TagDao
import com.flutter.stepstonesflt.data.local.entity.MediaTag
import com.flutter.stepstonesflt.data.local.entity.Tag
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ImportSummary(val added: Int, val skipped: Int, val failed: Int) {
    fun toMessage(): String = buildString {
        if (added > 0) append("Imported $added item${if (added != 1) "s" else ""}")
        if (skipped > 0) { if (isNotEmpty()) append(" · "); append("$skipped already in album") }
        if (failed > 0) { if (isNotEmpty()) append(" · "); append("$failed failed") }
    }.ifEmpty { "Nothing imported" }
}

@Singleton
class BundleImportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ingestRepository: IngestRepository,
    private val tagDao: TagDao,
) {
    private val tmpDir = File(context.cacheDir, "import_tmp").also { it.mkdirs() }

    suspend fun import(uri: Uri, albumId: Long): ImportSummary = withContext(Dispatchers.IO) {
        // Two-pass: buffer all media files + read metadata in one traversal,
        // then ingest after zip is closed (guarantees metadata.json is parsed
        // before tags are applied regardless of zip entry order).
        val bufferedFiles = mutableMapOf<String, File>() // entryName -> tempFile
        val tagsByFile = mutableMapOf<String, List<String>>()

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return@withContext ImportSummary(0, 0, 0)
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == "metadata.json" -> {
                        val metadata = JSONObject(zip.readBytes().decodeToString())
                        metadata.keys().forEach { key ->
                            val obj = metadata.getJSONObject(key)
                            val tagsRaw = obj.optString("tags", "").trim()
                            val tags = if (tagsRaw.isEmpty()) emptyList()
                                       else tagsRaw.split(Regex("\\s+")).filter { it.isNotEmpty() }
                            tagsByFile[key] = tags
                        }
                    }
                    entry.name.startsWith("media/") && !entry.isDirectory -> {
                        val entryName = entry.name.removePrefix("media/")
                        val tmpFile = File(tmpDir, entryName)
                        tmpFile.outputStream().use { zip.copyTo(it) }
                        bufferedFiles[entryName] = tmpFile
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        var added = 0; var skipped = 0; var failed = 0

        bufferedFiles.forEach { (entryName, tmpFile) ->
            try {
                val tmpUri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", tmpFile)
            when (val result = ingestRepository.ingest(tmpUri, albumId)) {
                    is IngestResult.Success -> {
                        added++
                        applyTags(result.mediaItemId, tagsByFile[entryName] ?: emptyList())
                    }
                    is IngestResult.AlreadyInAlbum -> skipped++
                    else -> failed++
                }
            } catch (e: Exception) {
                failed++
            } finally {
                tmpFile.delete()
            }
        }

        tmpDir.listFiles()?.forEach { it.delete() }
        ImportSummary(added, skipped, failed)
    }

    private suspend fun applyTags(itemId: Long, tagNames: List<String>) {
        tagNames.forEach { name ->
            val tag = tagDao.getByName(name) ?: run {
                val id = tagDao.insert(Tag(name = name))
                Tag(id = id, name = name)
            }
            tagDao.insertMediaTag(MediaTag(itemId, tag.id))
        }
    }
}

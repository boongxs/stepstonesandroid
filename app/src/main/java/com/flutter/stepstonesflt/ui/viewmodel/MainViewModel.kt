package com.flutter.stepstonesflt.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flutter.stepstonesflt.data.local.dao.AlbumDao
import com.flutter.stepstonesflt.data.local.dao.MediaAlbumDao
import com.flutter.stepstonesflt.data.local.dao.MediaItemDao
import com.flutter.stepstonesflt.data.local.dao.TagDao
import com.flutter.stepstonesflt.data.local.entity.Album
import com.flutter.stepstonesflt.data.local.entity.MediaAlbum
import com.flutter.stepstonesflt.data.local.entity.MediaItem
import com.flutter.stepstonesflt.data.local.entity.MediaTag
import com.flutter.stepstonesflt.data.local.entity.Tag
import kotlinx.coroutines.flow.Flow
import com.flutter.stepstonesflt.data.repository.IngestRepository
import com.flutter.stepstonesflt.data.repository.IngestResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val albumDao: AlbumDao,
    private val mediaItemDao: MediaItemDao,
    private val mediaAlbumDao: MediaAlbumDao,
    private val tagDao: TagDao,
    private val ingestRepository: IngestRepository,
) : ViewModel() {

    private val _selectedAlbumId = MutableStateFlow(-1L)

    val albums: StateFlow<List<Album>> = albumDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedAlbum: StateFlow<Album?> = combine(_selectedAlbumId, albums) { id, list ->
        list.find { it.id == id } ?: list.firstOrNull { it.isDefault } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val mediaItemCount: StateFlow<Int> = selectedAlbum
        .flatMapLatest { album ->
            if (album != null) mediaItemDao.countByAlbum(album.id) else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeSearchQuery = MutableStateFlow("")
    val activeSearchQuery: StateFlow<String> = _activeSearchQuery.asStateFlow()

    val mediaItems: StateFlow<List<MediaItem>> = combine(selectedAlbum, activeSearchQuery) { album, query ->
        Pair(album, query)
    }.flatMapLatest { (album, query) ->
        when {
            album == null -> flowOf(emptyList())
            query.isBlank() -> mediaItemDao.getByAlbum(album.id)
            else -> mediaItemDao.searchByAlbumAndTag(album.id, query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selection
    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItemIds: StateFlow<Set<Long>> = _selectedItemIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    // Enlarge view
    private val _enlargeItemId = MutableStateFlow<Long?>(null)
    val enlargeItemId: StateFlow<Long?> = _enlargeItemId.asStateFlow()

    fun openEnlargeView(id: Long) { _enlargeItemId.value = id }
    fun closeEnlargeView() { _enlargeItemId.value = null }

    fun deleteSingleItem(id: Long) {
        val albumId = selectedAlbum.value?.id ?: return
        closeEnlargeView()
        viewModelScope.launch {
            mediaAlbumDao.delete(MediaAlbum(id, albumId))
            if (mediaAlbumDao.albumCountForMedia(id) == 0) {
                mediaItemDao.getById(id)?.let { item ->
                    File(item.filePath).delete()
                    item.thumbnailPath?.let { File(it).delete() }
                    mediaItemDao.deleteById(id)
                }
            }
        }
    }

    fun addEnlargeItemToAlbum(id: Long, album: Album) {
        viewModelScope.launch {
            if (!mediaAlbumDao.exists(id, album.id)) {
                mediaAlbumDao.insert(MediaAlbum(id, album.id))
                _snackbarMessages.tryEmit("Added to ${album.name}")
            } else {
                _snackbarMessages.tryEmit("Already in ${album.name}")
            }
        }
    }

    // Ingest
    private val _pendingUris = MutableStateFlow<List<Uri>>(emptyList())
    val pendingCount: StateFlow<Int> = _pendingUris
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _ingestInProgress = MutableStateFlow(false)
    val ingestInProgress: StateFlow<Boolean> = _ingestInProgress.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    init {
        viewModelScope.launch { ensureDefaultAlbum() }
    }

    private suspend fun ensureDefaultAlbum() {
        val existing = albumDao.getDefault()
        val id = existing?.id ?: albumDao.insert(Album(name = "Library", isDefault = true))
        _selectedAlbumId.value = id
    }

    fun selectAlbum(album: Album) {
        _selectedAlbumId.value = album.id
        clearSelection()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun triggerSearch() {
        _activeSearchQuery.value = _searchQuery.value
    }

    fun createAlbum(name: String) {
        viewModelScope.launch {
            val id = albumDao.insert(Album(name = name))
            _selectedAlbumId.value = id
        }
    }

    fun renameAlbum(album: Album, newName: String) {
        viewModelScope.launch {
            albumDao.update(album.copy(name = newName))
        }
    }

    fun deleteAlbum(album: Album) {
        if (album.isDefault) return
        viewModelScope.launch {
            albumDao.delete(album)
            if (_selectedAlbumId.value == album.id) {
                _selectedAlbumId.value = albumDao.getDefault()?.id ?: -1L
            }
        }
    }

    // Selection actions

    fun toggleSelection(id: Long) {
        _isSelectionMode.value = true
        _selectedItemIds.update { ids -> if (id in ids) ids - id else ids + id }
    }

    fun selectAll() {
        _selectedItemIds.value = mediaItems.value.map { it.id }.toSet()
    }

    fun deselectAll() {
        _selectedItemIds.value = emptySet()
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun deleteSelected() {
        val albumId = selectedAlbum.value?.id ?: return
        val toDelete = _selectedItemIds.value.toSet()
        clearSelection()
        viewModelScope.launch {
            toDelete.forEach { id ->
                mediaAlbumDao.delete(MediaAlbum(id, albumId))
                if (mediaAlbumDao.albumCountForMedia(id) == 0) {
                    mediaItemDao.getById(id)?.let { item ->
                        File(item.filePath).delete()
                        item.thumbnailPath?.let { File(it).delete() }
                        mediaItemDao.deleteById(id)
                    }
                }
            }
        }
    }

    fun addSelectedToAlbum(album: Album) {
        val ids = _selectedItemIds.value.toSet()
        clearSelection()
        viewModelScope.launch {
            val newIds = ids.filterNot { mediaAlbumDao.exists(it, album.id) }
            if (newIds.isEmpty()) {
                _snackbarMessages.tryEmit("Already in ${album.name}")
            } else {
                newIds.forEach { id -> mediaAlbumDao.insert(MediaAlbum(id, album.id)) }
                _selectedAlbumId.value = album.id
            }
        }
    }

    fun getSelectedItems(): List<MediaItem> =
        mediaItems.value.filter { it.id in _selectedItemIds.value }

    // Tags and metadata

    fun tagsForItem(itemId: Long): Flow<List<Tag>> =
        tagDao.getTagsForMedia(itemId)

    fun addTagToItem(itemId: Long, tagName: String) {
        viewModelScope.launch {
            val tag = tagDao.getByName(tagName) ?: run {
                val id = tagDao.insert(Tag(name = tagName))
                Tag(id = id, name = tagName)
            }
            tagDao.insertMediaTag(MediaTag(itemId, tag.id))
        }
    }

    fun removeTagFromItem(itemId: Long, tagId: Long) {
        viewModelScope.launch {
            tagDao.deleteMediaTag(MediaTag(itemId, tagId))
            tagDao.deleteOrphanTags()
        }
    }

    fun updateMediaDate(itemId: Long, newDate: Long) {
        viewModelScope.launch {
            mediaItemDao.getById(itemId)?.let { item ->
                mediaItemDao.update(item.copy(mediaDate = newDate))
            }
        }
    }

    // Ingest

    fun handleSharedUris(uris: List<Uri>) {
        _pendingUris.value = uris
    }

    fun dismissIngest() {
        _pendingUris.value = emptyList()
    }

    fun ingestToAlbum(album: Album) {
        val uris = _pendingUris.value
        _pendingUris.value = emptyList()
        _ingestInProgress.value = true
        viewModelScope.launch {
            var added = 0; var skipped = 0; var failed = 0
            uris.forEach { uri ->
                when (ingestRepository.ingest(uri, album.id)) {
                    is IngestResult.Success -> added++
                    is IngestResult.AlreadyInAlbum -> skipped++
                    else -> failed++
                }
            }
            _ingestInProgress.value = false
            val parts = buildList {
                if (added > 0) add("Added $added item${if (added > 1) "s" else ""} to ${album.name}")
                if (skipped > 0) add("$skipped already in album")
                if (failed > 0) add("$failed failed")
            }
            if (parts.isNotEmpty()) _snackbarMessages.tryEmit(parts.joinToString(" · "))
        }
    }
}

package com.flutter.stepstonesflt.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flutter.stepstonesflt.data.local.dao.AlbumDao
import com.flutter.stepstonesflt.data.local.dao.MediaItemDao
import com.flutter.stepstonesflt.data.local.entity.Album
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val albumDao: AlbumDao,
    private val mediaItemDao: MediaItemDao,
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

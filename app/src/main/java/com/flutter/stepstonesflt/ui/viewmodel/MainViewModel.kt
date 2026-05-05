package com.flutter.stepstonesflt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flutter.stepstonesflt.data.local.dao.AlbumDao
import com.flutter.stepstonesflt.data.local.dao.MediaItemDao
import com.flutter.stepstonesflt.data.local.entity.Album
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val albumDao: AlbumDao,
    private val mediaItemDao: MediaItemDao,
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
            albumDao.insert(Album(name = name))
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
}

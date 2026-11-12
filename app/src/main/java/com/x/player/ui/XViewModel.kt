package com.x.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.x.player.data.LibraryRepository
import com.x.player.data.db.StatsRow
import com.x.player.data.db.TopRow
import com.x.player.domain.LibraryIndex
import com.x.player.domain.Playlist
import com.x.player.domain.SearchResults
import com.x.player.domain.SongSort
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class XViewModel(private val repository: LibraryRepository) : ViewModel() {

    val index: StateFlow<LibraryIndex> = repository.tracks
        .map { LibraryIndex(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, LibraryIndex())

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _sort = MutableStateFlow(SongSort.TITLE)
    val sort: StateFlow<SongSort> = _sort.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<SearchResults> = combine(index, _query.debounce(150)) { idx, q -> idx.search(q) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, SearchResults())

    val favoriteIds: StateFlow<List<Long>> = repository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun toggleFavorite(trackId: Long) {
        viewModelScope.launch { repository.toggleFavorite(trackId) }
    }

    fun createPlaylist(name: String, firstTrackId: Long? = null) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { repository.createPlaylist(clean, firstTrackId) }
    }

    fun renamePlaylist(id: Long, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { repository.renamePlaylist(id, clean) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { repository.deletePlaylist(id) }
    }

    fun addToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch { repository.addToPlaylist(playlistId, trackId) }
    }

    fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch { repository.removeFromPlaylist(playlistId, trackId) }
    }

    val topTracks: StateFlow<List<TopRow>> = repository.topTracks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun statsSince(since: Long): Flow<StatsRow> = repository.statsSince(since)

    fun setSort(sort: SongSort) {
        _sort.value = sort
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun rescan() {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            try {
                repository.rescan()
            } catch (e: Exception) {
                // Keep whatever is cached; the next scan will try again.
            } finally {
                _scanning.value = false
            }
        }
    }

    class Factory(private val repository: LibraryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = XViewModel(repository) as T
    }
}

package fumi.day.literalplayer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fumi.day.literalplayer.data.prefs.UserPreferences
import fumi.day.literalplayer.util.deleteAudioFile
import fumi.day.literalplayer.data.repository.FavoritesRepository
import fumi.day.literalplayer.data.repository.TrackRepository
import fumi.day.literalplayer.domain.model.FavoritesList
import fumi.day.literalplayer.domain.model.SortOrder
import fumi.day.literalplayer.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackRepository: TrackRepository,
    private val favoritesRepository: FavoritesRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks = _tracks.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val prefs = userPreferences.prefs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        fumi.day.literalplayer.data.prefs.UserPrefs()
    )

    val favoritesLists = favoritesRepository.lists.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder = _selectedFolder.asStateFlow()

    private val _favoritesSheetTrack = MutableStateFlow<Track?>(null)
    val favoritesSheetTrack = _favoritesSheetTrack.asStateFlow()

    val filteredTracks = combine(_tracks, _searchQuery, _selectedFolder) { tracks, query, folder ->
        val byFolder = if (folder != null) tracks.filter { it.path.startsWith(folder) } else tracks
        val q = query.trim().lowercase()
        if (q.isBlank()) byFolder
        else byFolder.filter { t ->
            t.fileName.lowercase().contains(q) ||
            t.title.lowercase().contains(q) ||
            t.artist.lowercase().contains(q) ||
            t.album.lowercase().contains(q)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            userPreferences.prefs
                .map { it.rootFolders }
                .distinctUntilChanged()
                .collect { folders ->
                    if (folders.isNotEmpty()) loadTracks(folders)
                }
        }
        viewModelScope.launch {
            trackRepository.rescanTrigger.collect {
                val folders = prefs.value.rootFolders
                if (folders.isNotEmpty()) loadTracks(folders)
            }
        }
    }

    private suspend fun loadTracks(folders: Set<String>) {
        _isLoading.value = true
        _tracks.value = trackRepository.loadTracks(folders)
        _isLoading.value = false
    }

    fun refresh() {
        viewModelScope.launch {
            val folders = prefs.value.rootFolders
            if (folders.isNotEmpty()) loadTracks(folders)
        }
    }

    fun setSearch(query: String) { _searchQuery.value = query }
    fun selectFolder(folder: String?) { _selectedFolder.value = folder }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { userPreferences.setSortOrder(order) }
    }

    fun showFavoritesSheet(track: Track) { _favoritesSheetTrack.value = track }
    fun hideFavoritesSheet() { _favoritesSheetTrack.value = null }

    fun addToFavorites(listId: Long, track: Track) {
        viewModelScope.launch { favoritesRepository.addTrack(listId, track) }
    }

    fun createFavoritesListAndAdd(name: String, track: Track) {
        viewModelScope.launch {
            val id = favoritesRepository.createList(name)
            favoritesRepository.addTrack(id, track)
        }
    }

    fun updatePlaylist(tracks: List<Track>) { trackRepository.setPlaylist(tracks) }

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId = _selectedPlaylistId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val playlistTracks: kotlinx.coroutines.flow.StateFlow<List<Track>> = _selectedPlaylistId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else favoritesRepository.tracksForList(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectPlaylist(id: Long?) { _selectedPlaylistId.value = id }

    fun deletePlaylist(id: Long) {
        if (_selectedPlaylistId.value == id) _selectedPlaylistId.value = null
        viewModelScope.launch { favoritesRepository.deleteList(id) }
    }

    private val _playlistActionTrack = MutableStateFlow<Track?>(null)
    val playlistActionTrack = _playlistActionTrack.asStateFlow()

    fun showPlaylistActionSheet(track: Track) { _playlistActionTrack.value = track }
    fun hidePlaylistActionSheet() { _playlistActionTrack.value = null }

    fun removeFromCurrentPlaylist(trackId: String) {
        val listId = _selectedPlaylistId.value ?: return
        viewModelScope.launch { favoritesRepository.removeTrack(listId, trackId) }
    }

    fun deleteFile(track: Track) {
        viewModelScope.launch {
            deleteAudioFile(context, track.path)
            val updated = _tracks.value.filter { it.id != track.id }
            _tracks.value = updated
            trackRepository.updateCached(updated)
            trackRepository.setPlaylist(trackRepository.currentPlaylist.filter { it.id != track.id })
        }
    }
}

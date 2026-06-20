package fumi.day.literalplayer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fumi.day.literalplayer.data.prefs.UserPreferences
import fumi.day.literalplayer.util.TrackDeleteHandler
import fumi.day.literalplayer.data.repository.FavoritesRepository
import fumi.day.literalplayer.data.repository.TrackRepository
import fumi.day.literalplayer.domain.model.SortOrder
import fumi.day.literalplayer.domain.model.Track
import fumi.day.literalplayer.ui.shared.PlaylistActionDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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

    val tracks = trackRepository.cachedTracksFlow

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

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder = _selectedFolder.asStateFlow()

    val filteredTracks = combine(trackRepository.cachedTracksFlow, _searchQuery, _selectedFolder) { tracks, query, folder ->
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

    private val playlistDelegate = PlaylistActionDelegate(favoritesRepository, viewModelScope)
    val trackAction = playlistDelegate.trackAction
    val trackMemberOf = playlistDelegate.trackMemberOf
    val multiPlaylistSheetTracks = playlistDelegate.multiPlaylistSheetTracks

    fun showTrackAction(track: Track, playlistId: Long? = null, playlistName: String? = null) =
        playlistDelegate.showTrackAction(track, playlistId, playlistName)
    fun hideTrackAction() = playlistDelegate.hideTrackAction()
    fun toggleTrackInPlaylist(listId: Long, track: Track) = playlistDelegate.toggleTrackInPlaylist(listId, track)
    fun createPlaylistAndAdd(name: String, track: Track) = playlistDelegate.createPlaylistAndAdd(name, track)
    fun showMultiPlaylistSheet(tracks: List<Track>) = playlistDelegate.showMultiPlaylistSheet(tracks)
    fun hideMultiPlaylistSheet() = playlistDelegate.hideMultiPlaylistSheet()
    fun addAllToPlaylist(listId: Long, tracks: List<Track>) = playlistDelegate.addAllToPlaylist(listId, tracks)
    fun createPlaylistAndAddAll(name: String, tracks: List<Track>) = playlistDelegate.createPlaylistAndAddAll(name, tracks)

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

    fun removeFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch { favoritesRepository.removeTrack(playlistId, trackId) }
    }

    private val deleteHandler = TrackDeleteHandler(context, viewModelScope) { ids ->
        val updated = trackRepository.getCached().filter { it.id !in ids }
        trackRepository.updateCached(updated)
        trackRepository.setPlaylist(trackRepository.currentPlaylist.filter { it.id !in ids })
    }
    val pendingDeleteSender = deleteHandler.pendingDeleteSender
    fun deleteFile(track: Track) = deleteHandler.deleteFile(track)
    fun deleteFiles(tracks: List<Track>) = deleteHandler.deleteFiles(tracks)
    fun onDeleteConfirmed() = deleteHandler.onDeleteConfirmed()
    fun onDeleteDismissed() = deleteHandler.onDeleteDismissed()

    init {
        viewModelScope.launch {
            val initial = userPreferences.prefs.first()
            _selectedTab.value = initial.lastTab
            _selectedFolder.value = initial.lastFolder
        }
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
                if (folders.isNotEmpty()) {
                    _isLoading.value = true
                    trackRepository.rescanTracks(folders)
                    _isLoading.value = false
                }
            }
        }
    }

    private suspend fun loadTracks(folders: Set<String>) {
        _isLoading.value = true
        trackRepository.loadTracks(folders)
        _isLoading.value = false
    }

    fun refresh() {
        viewModelScope.launch {
            val folders = prefs.value.rootFolders
            if (folders.isNotEmpty()) loadTracks(folders)
        }
    }

    fun setSearch(query: String) { _searchQuery.value = query }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
        viewModelScope.launch { userPreferences.setLastTab(tab) }
    }

    fun selectFolder(folder: String?) {
        _selectedFolder.value = folder
        viewModelScope.launch { userPreferences.setLastFolder(folder) }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { userPreferences.setSortOrder(order) }
    }
}

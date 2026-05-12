package fumi.day.literalplayer.ui.artist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fumi.day.literalplayer.data.prefs.UserPreferences
import fumi.day.literalplayer.data.repository.FavoritesRepository
import fumi.day.literalplayer.data.repository.TrackRepository
import fumi.day.literalplayer.domain.model.Track
import fumi.day.literalplayer.ui.list.TrackActionState
import fumi.day.literalplayer.util.TrackDeleteHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackRepository: TrackRepository,
    private val favoritesRepository: FavoritesRepository,
    userPreferences: UserPreferences,
) : ViewModel() {

    val prefs = userPreferences.prefs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        fumi.day.literalplayer.data.prefs.UserPrefs()
    )

    val favoritesLists = favoritesRepository.lists.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _artistName = MutableStateFlow("")
    fun setArtistName(name: String) { _artistName.value = name }

    val albumMap = combine(trackRepository.cachedTracksFlow, _artistName) { tracks, artist ->
        if (artist.isEmpty()) trackRepository.tracksForArtist(trackRepository.getCached(), "")
        else trackRepository.tracksForArtist(tracks, artist)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun updatePlaylist(tracks: List<Track>) { trackRepository.setPlaylist(tracks) }

    // Single-track action sheet
    private val _trackAction = MutableStateFlow<TrackActionState?>(null)
    val trackAction = _trackAction.asStateFlow()

    private val _trackMemberOf = MutableStateFlow<Set<Long>>(emptySet())
    val trackMemberOf = _trackMemberOf.asStateFlow()

    fun showTrackAction(track: Track) {
        _trackAction.value = TrackActionState(track)
        viewModelScope.launch {
            _trackMemberOf.value = favoritesRepository.getListIdsForTrack(track.id).toSet()
        }
    }

    fun hideTrackAction() {
        _trackAction.value = null
        _trackMemberOf.value = emptySet()
    }

    fun toggleTrackInPlaylist(listId: Long, track: Track) {
        viewModelScope.launch {
            if (listId in _trackMemberOf.value) {
                favoritesRepository.removeTrack(listId, track.id)
                _trackMemberOf.value = _trackMemberOf.value - listId
            } else {
                favoritesRepository.addTrack(listId, track)
                _trackMemberOf.value = _trackMemberOf.value + listId
            }
        }
    }

    fun createPlaylistAndAdd(name: String, track: Track) {
        viewModelScope.launch {
            val id = favoritesRepository.createList(name)
            favoritesRepository.addTrack(id, track)
            _trackMemberOf.value = _trackMemberOf.value + id
        }
    }

    // Multi-select playlist sheet
    private val _multiPlaylistSheetTracks = MutableStateFlow<List<Track>>(emptyList())
    val multiPlaylistSheetTracks = _multiPlaylistSheetTracks.asStateFlow()

    fun showMultiPlaylistSheet(tracks: List<Track>) { _multiPlaylistSheetTracks.value = tracks }
    fun hideMultiPlaylistSheet() { _multiPlaylistSheetTracks.value = emptyList() }

    fun addAllToPlaylist(listId: Long, tracks: List<Track>) {
        viewModelScope.launch { tracks.forEach { favoritesRepository.addTrack(listId, it) } }
    }

    fun createPlaylistAndAddAll(name: String, tracks: List<Track>) {
        viewModelScope.launch {
            val id = favoritesRepository.createList(name)
            tracks.forEach { favoritesRepository.addTrack(id, it) }
        }
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
}

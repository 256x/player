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
import android.content.IntentSender
import fumi.day.literalplayer.util.deleteAudioFile
import fumi.day.literalplayer.util.mediaStoreDeleteRequest
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

    private val _pendingDeleteSender = MutableStateFlow<Pair<IntentSender, Set<String>>?>(null)
    val pendingDeleteSender = _pendingDeleteSender.asStateFlow()

    fun deleteFile(track: Track) {
        viewModelScope.launch {
            if (deleteAudioFile(context, track.path)) {
                removeFromCache(setOf(track.id)); return@launch
            }
            val pi = mediaStoreDeleteRequest(context, listOf(track.path))
            if (pi != null) _pendingDeleteSender.value = Pair(pi.intentSender, setOf(track.id))
        }
    }

    fun deleteFiles(tracks: List<Track>) {
        viewModelScope.launch {
            val directDeleted = tracks.filter { deleteAudioFile(context, it.path) }.map { it.id }.toSet()
            if (directDeleted.isNotEmpty()) removeFromCache(directDeleted)
            val remaining = tracks.filter { it.id !in directDeleted }
            if (remaining.isEmpty()) return@launch
            val pi = mediaStoreDeleteRequest(context, remaining.map { it.path })
            if (pi != null) _pendingDeleteSender.value = Pair(pi.intentSender, remaining.map { it.id }.toSet())
        }
    }

    fun onDeleteConfirmed() {
        val ids = _pendingDeleteSender.value?.second ?: return
        _pendingDeleteSender.value = null
        removeFromCache(ids)
    }

    fun onDeleteDismissed() { _pendingDeleteSender.value = null }

    private fun removeFromCache(ids: Set<String>) {
        val updated = trackRepository.getCached().filter { it.id !in ids }
        trackRepository.updateCached(updated)
        trackRepository.setPlaylist(trackRepository.currentPlaylist.filter { it.id !in ids })
    }
}

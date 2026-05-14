package fumi.day.literalplayer.ui.shared

import fumi.day.literalplayer.data.repository.FavoritesRepository
import fumi.day.literalplayer.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrackActionState(
    val track: Track,
    val playlistId: Long? = null,
    val playlistName: String? = null,
)

class PlaylistActionDelegate(
    private val favoritesRepository: FavoritesRepository,
    private val scope: CoroutineScope,
) {
    private val _trackAction = MutableStateFlow<TrackActionState?>(null)
    val trackAction: StateFlow<TrackActionState?> = _trackAction.asStateFlow()

    private val _trackMemberOf = MutableStateFlow<Set<Long>>(emptySet())
    val trackMemberOf: StateFlow<Set<Long>> = _trackMemberOf.asStateFlow()

    private val _multiPlaylistSheetTracks = MutableStateFlow<List<Track>>(emptyList())
    val multiPlaylistSheetTracks: StateFlow<List<Track>> = _multiPlaylistSheetTracks.asStateFlow()

    fun showTrackAction(track: Track, playlistId: Long? = null, playlistName: String? = null) {
        _trackAction.value = TrackActionState(track, playlistId, playlistName)
        scope.launch {
            _trackMemberOf.value = favoritesRepository.getListIdsForTrack(track.id).toSet()
        }
    }

    fun hideTrackAction() {
        _trackAction.value = null
        _trackMemberOf.value = emptySet()
    }

    fun toggleTrackInPlaylist(listId: Long, track: Track) {
        scope.launch {
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
        scope.launch {
            val id = favoritesRepository.createList(name)
            favoritesRepository.addTrack(id, track)
            _trackMemberOf.value = _trackMemberOf.value + id
        }
    }

    fun showMultiPlaylistSheet(tracks: List<Track>) { _multiPlaylistSheetTracks.value = tracks }
    fun hideMultiPlaylistSheet() { _multiPlaylistSheetTracks.value = emptyList() }

    fun addAllToPlaylist(listId: Long, tracks: List<Track>) {
        scope.launch { tracks.forEach { favoritesRepository.addTrack(listId, it) } }
    }

    fun createPlaylistAndAddAll(name: String, tracks: List<Track>) {
        scope.launch {
            val id = favoritesRepository.createList(name)
            tracks.forEach { favoritesRepository.addTrack(id, it) }
        }
    }
}

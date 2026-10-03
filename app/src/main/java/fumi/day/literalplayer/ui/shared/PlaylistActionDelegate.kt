package fumi.day.literalplayer.ui.shared

import fumi.day.literalplayer.data.repository.FavoritesRepository
import fumi.day.literalplayer.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistActionDelegate(
    private val favoritesRepository: FavoritesRepository,
    private val scope: CoroutineScope,
) {
    private val _multiPlaylistSheetTracks = MutableStateFlow<List<Track>>(emptyList())
    val multiPlaylistSheetTracks: StateFlow<List<Track>> = _multiPlaylistSheetTracks.asStateFlow()

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

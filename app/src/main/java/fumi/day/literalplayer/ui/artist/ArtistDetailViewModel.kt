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
import fumi.day.literalplayer.ui.shared.PlaylistActionDelegate
import fumi.day.literalplayer.util.TrackDeleteHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    private val playlistDelegate = PlaylistActionDelegate(favoritesRepository, viewModelScope)
    val trackAction = playlistDelegate.trackAction
    val trackMemberOf = playlistDelegate.trackMemberOf
    val multiPlaylistSheetTracks = playlistDelegate.multiPlaylistSheetTracks

    fun showTrackAction(track: Track) = playlistDelegate.showTrackAction(track)
    fun hideTrackAction() = playlistDelegate.hideTrackAction()
    fun toggleTrackInPlaylist(listId: Long, track: Track) = playlistDelegate.toggleTrackInPlaylist(listId, track)
    fun createPlaylistAndAdd(name: String, track: Track) = playlistDelegate.createPlaylistAndAdd(name, track)
    fun showMultiPlaylistSheet(tracks: List<Track>) = playlistDelegate.showMultiPlaylistSheet(tracks)
    fun hideMultiPlaylistSheet() = playlistDelegate.hideMultiPlaylistSheet()
    fun addAllToPlaylist(listId: Long, tracks: List<Track>) = playlistDelegate.addAllToPlaylist(listId, tracks)
    fun createPlaylistAndAddAll(name: String, tracks: List<Track>) = playlistDelegate.createPlaylistAndAddAll(name, tracks)

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

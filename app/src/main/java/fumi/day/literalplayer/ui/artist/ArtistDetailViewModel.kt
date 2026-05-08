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
import fumi.day.literalplayer.util.deleteAudioFile
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

    private val _favoritesSheetTrack = MutableStateFlow<Track?>(null)
    val favoritesSheetTrack = _favoritesSheetTrack.asStateFlow()

    private val _artistName = MutableStateFlow("")
    fun setArtistName(name: String) { _artistName.value = name }

    val albumMap = combine(trackRepository.cachedTracksFlow, _artistName) { tracks, artist ->
        if (artist.isEmpty()) trackRepository.tracksForArtist(trackRepository.getCached(), "")
        else trackRepository.tracksForArtist(tracks, artist)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun updatePlaylist(tracks: List<Track>) { trackRepository.setPlaylist(tracks) }

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

    fun deleteFile(track: Track) {
        viewModelScope.launch {
            deleteAudioFile(context, track.path)
            val updated = trackRepository.getCached().filter { it.id != track.id }
            trackRepository.updateCached(updated)
            trackRepository.setPlaylist(trackRepository.currentPlaylist.filter { it.id != track.id })
        }
    }
}

package fumi.day.literalplayer.ui.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalplayer.data.prefs.UserPreferences
import fumi.day.literalplayer.data.repository.FavoritesRepository
import fumi.day.literalplayer.data.repository.TrackRepository
import fumi.day.literalplayer.domain.model.FavoritesList
import fumi.day.literalplayer.domain.model.Track
import fumi.day.literalplayer.domain.model.displayArtist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
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

    fun albumsForArtist(artistName: String): Map<String, List<Track>> =
        trackRepository.tracksForArtist(trackRepository.getCached(), artistName)

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
}

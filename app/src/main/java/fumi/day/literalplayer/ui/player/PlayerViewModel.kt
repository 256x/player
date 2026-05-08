package fumi.day.literalplayer.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fumi.day.literalplayer.data.prefs.UserPreferences
import fumi.day.literalplayer.data.repository.FavoritesRepository
import fumi.day.literalplayer.data.repository.TrackRepository
import fumi.day.literalplayer.domain.model.Track
import fumi.day.literalplayer.domain.model.displayAlbum
import fumi.day.literalplayer.domain.model.displayArtist
import fumi.day.literalplayer.domain.model.displayTitle
import fumi.day.literalplayer.service.PlayerService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speedIndex: Int = 1,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
)

val SPEED_PRESETS = listOf(0.8f, 1.0f, 1.2f, 1.4f, 1.6f, 1.8f, 2.0f)
val SPEED_LABELS = listOf("0.8x", "1.0x", "1.2x", "1.4x", "1.6x", "1.8x", "2.0x")

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackRepository: TrackRepository,
    private val userPreferences: UserPreferences,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state = _state.asStateFlow()

    val prefs = userPreferences.prefs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        fumi.day.literalplayer.data.prefs.UserPrefs()
    )

    val favoritesLists = favoritesRepository.lists.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _showFavSheet = MutableStateFlow(false)
    val showFavSheet = _showFavSheet.asStateFlow()

    fun openFavSheet() { _showFavSheet.value = true }
    fun closeFavSheet() { _showFavSheet.value = false }

    fun addTrackToFavorites(listId: Long) {
        val track = _state.value.track ?: return
        viewModelScope.launch { favoritesRepository.addTrack(listId, track) }
    }

    fun createListAndAddTrack(name: String) {
        val track = _state.value.track ?: return
        viewModelScope.launch {
            val id = favoritesRepository.createList(name)
            favoritesRepository.addTrack(id, track)
        }
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    fun init(trackId: String) {
        if (_state.value.track?.id == trackId && controller != null) return
        val cached = trackRepository.getCached()
        val track = cached.find { it.id == trackId } ?: return
        val playlist = trackRepository.currentPlaylist.ifEmpty { listOf(track) }
        val startIndex = playlist.indexOfFirst { it.id == trackId }.let { if (it < 0) 0 else it }
        _state.value = _state.value.copy(track = track, speedIndex = 1)

        val token = SessionToken(context, ComponentName(context, PlayerService::class.java))
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = MediaController.Builder(context, token).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.let { c ->
                val mediaItems = playlist.map { MediaItem.fromUri(it.path) }
                c.setMediaItems(mediaItems, startIndex, 0L)
                c.setPlaybackSpeed(SPEED_PRESETS[1])
                viewModelScope.launch {
                    val savedPos = trackRepository.getPosition(track.id)
                    c.seekTo(startIndex, savedPos)
                    c.prepare()
                    c.play()
                }
                c.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val idx = c.currentMediaItemIndex
                        if (idx >= 0 && idx < playlist.size) {
                            _state.value = _state.value.copy(track = playlist[idx])
                        }
                    }
                })
                startProgressTracking()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun startProgressTracking() {
        viewModelScope.launch {
            while (true) {
                controller?.let { c ->
                    _state.value = _state.value.copy(
                        isPlaying = c.isPlaying,
                        positionMs = c.currentPosition,
                        durationMs = c.duration.coerceAtLeast(0L),
                    )
                    if (!c.isPlaying && c.currentPosition > 0) {
                        trackRepository.savePosition(_state.value.track?.id ?: return@let, c.currentPosition)
                    }
                }
                delay(100)
            }
        }
    }

    fun playPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun skipForward(long: Boolean = false) {
        val sec = if (long) prefs.value.longSkipSec else prefs.value.shortSkipSec
        controller?.seekTo((controller!!.currentPosition + sec * 1000L).coerceAtMost(controller!!.duration))
    }

    fun skipBackward(long: Boolean = false) {
        val sec = if (long) prefs.value.longSkipSec else prefs.value.shortSkipSec
        controller?.seekTo((controller!!.currentPosition - sec * 1000L).coerceAtLeast(0L))
    }

    fun nextTrack() { controller?.seekToNextMediaItem() }
    fun prevTrack() { controller?.seekToPreviousMediaItem() }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    fun setSpeed(index: Int) {
        _state.value = _state.value.copy(speedIndex = index)
        controller?.setPlaybackSpeed(SPEED_PRESETS[index])
    }

    fun toggleShuffle() {
        val next = !_state.value.shuffle
        _state.value = _state.value.copy(shuffle = next)
        controller?.shuffleModeEnabled = next
    }

    fun cycleRepeat() {
        val next = when (_state.value.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _state.value = _state.value.copy(repeatMode = next)
        controller?.repeatMode = next
    }

    override fun onCleared() {
        controller?.let { c ->
            viewModelScope.launch {
                trackRepository.savePosition(_state.value.track?.id ?: return@launch, c.currentPosition)
            }
        }
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}

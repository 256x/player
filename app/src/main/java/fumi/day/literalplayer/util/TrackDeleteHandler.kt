package fumi.day.literalplayer.util

import android.content.Context
import android.content.IntentSender
import fumi.day.literalplayer.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrackDeleteHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onRemove: (Set<String>) -> Unit,
) {
    private val _pendingDeleteSender = MutableStateFlow<Pair<IntentSender, Set<String>>?>(null)
    val pendingDeleteSender: StateFlow<Pair<IntentSender, Set<String>>?> = _pendingDeleteSender.asStateFlow()

    fun deleteFile(track: Track) {
        scope.launch {
            if (deleteAudioFile(context, track.path)) { onRemove(setOf(track.id)); return@launch }
            mediaStoreDeleteRequest(context, listOf(track.path))
                ?.let { _pendingDeleteSender.value = Pair(it.intentSender, setOf(track.id)) }
        }
    }

    fun deleteFiles(tracks: List<Track>) {
        scope.launch {
            val directDeleted = tracks.filter { deleteAudioFile(context, it.path) }.map { it.id }.toSet()
            if (directDeleted.isNotEmpty()) onRemove(directDeleted)
            val remaining = tracks.filter { it.id !in directDeleted }
            if (remaining.isEmpty()) return@launch
            mediaStoreDeleteRequest(context, remaining.map { it.path })
                ?.let { _pendingDeleteSender.value = Pair(it.intentSender, remaining.map { it.id }.toSet()) }
        }
    }

    fun onDeleteConfirmed() {
        _pendingDeleteSender.value?.second?.let { onRemove(it) }
        _pendingDeleteSender.value = null
    }

    fun onDeleteDismissed() { _pendingDeleteSender.value = null }
}

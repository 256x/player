package fumi.day.literalplayer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalplayer.data.prefs.AppFont
import fumi.day.literalplayer.data.prefs.UserPreferences
import fumi.day.literalplayer.data.repository.TrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val trackRepository: TrackRepository,
) : ViewModel() {

    val state = prefs.prefs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        fumi.day.literalplayer.data.prefs.UserPrefs()
    )

    val isScanning = trackRepository.isScanning

    private val _pendingSubfolders = MutableStateFlow<List<String>>(emptyList())
    val pendingSubfolders = _pendingSubfolders.asStateFlow()

    private val _checkedSubfolders = MutableStateFlow<Set<String>>(emptySet())
    val checkedSubfolders = _checkedSubfolders.asStateFlow()

    fun scanAndShowSubfolders(path: String) {
        val subs = java.io.File(path).listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.map { it.absolutePath }
            ?.sorted()
            ?: emptyList()
        if (subs.isEmpty()) {
            viewModelScope.launch { prefs.setRootFolders(state.value.rootFolders + path) }
        } else {
            _pendingSubfolders.value = subs
            _checkedSubfolders.value = subs.toSet()
        }
    }

    fun toggleSubfolder(path: String) {
        _checkedSubfolders.value = if (path in _checkedSubfolders.value)
            _checkedSubfolders.value - path else _checkedSubfolders.value + path
    }

    fun confirmSubfolders() {
        viewModelScope.launch {
            prefs.setRootFolders(state.value.rootFolders + _checkedSubfolders.value)
        }
        _pendingSubfolders.value = emptyList()
        _checkedSubfolders.value = emptySet()
    }

    fun dismissSubfolders() {
        _pendingSubfolders.value = emptyList()
        _checkedSubfolders.value = emptySet()
    }

    fun removeFolder(path: String) {
        viewModelScope.launch { prefs.setRootFolders(state.value.rootFolders - path) }
    }

    fun setShortSkip(sec: Int) { viewModelScope.launch { prefs.setShortSkipSec(sec) } }
    fun setLongSkip(sec: Int) { viewModelScope.launch { prefs.setLongSkipSec(sec) } }
    fun setAccentColor(hex: String) { viewModelScope.launch { prefs.setAccentColor(hex) } }
    fun setTextColor(hex: String) { viewModelScope.launch { prefs.setTextColor(hex) } }
    fun setBackgroundColor(hex: String) { viewModelScope.launch { prefs.setBackgroundColor(hex) } }
    fun setFont(font: AppFont) { viewModelScope.launch { prefs.setFont(font) } }
    fun setFontSize(size: Float) { viewModelScope.launch { prefs.setFontSize(size.coerceIn(12f, 24f)) } }
    fun rescan() { trackRepository.triggerRescan() }
}

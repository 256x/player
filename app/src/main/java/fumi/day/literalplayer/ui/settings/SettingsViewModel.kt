package fumi.day.literalplayer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalplayer.data.prefs.AppFont
import fumi.day.literalplayer.data.prefs.UserPreferences
import fumi.day.literalplayer.data.repository.TrackRepository
import fumi.day.literalplayer.ui.shared.FolderSelectionHelper
import kotlinx.coroutines.flow.SharingStarted
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

    private val folderHelper = FolderSelectionHelper(viewModelScope) { newFolders ->
        prefs.setRootFolders(state.value.rootFolders + newFolders)
    }
    val pendingSubfolders = folderHelper.pendingSubfolders
    val checkedSubfolders = folderHelper.checkedSubfolders

    fun scanAndShowSubfolders(path: String) = folderHelper.scanAndShowSubfolders(path)
    fun toggleSubfolder(path: String) = folderHelper.toggleSubfolder(path)
    fun confirmSubfolders() = folderHelper.confirm()
    fun dismissSubfolders() = folderHelper.dismiss()

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
    fun setNormalize(enabled: Boolean) { viewModelScope.launch { prefs.setNormalize(enabled) } }
    fun rescan() { trackRepository.triggerRescan() }
}

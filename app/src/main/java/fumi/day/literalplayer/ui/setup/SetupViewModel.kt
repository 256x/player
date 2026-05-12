package fumi.day.literalplayer.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalplayer.data.prefs.UserPreferences
import fumi.day.literalplayer.ui.shared.FolderSelectionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val prefs: UserPreferences,
) : ViewModel() {

    private val _folders = MutableStateFlow<Set<String>>(emptySet())
    val folders = _folders.asStateFlow()

    private val folderHelper = FolderSelectionHelper(viewModelScope) { newFolders ->
        _folders.value = _folders.value + newFolders
    }
    val pendingSubfolders = folderHelper.pendingSubfolders
    val checkedSubfolders = folderHelper.checkedSubfolders

    fun scanAndShowSubfolders(path: String) = folderHelper.scanAndShowSubfolders(path)
    fun toggleSubfolder(path: String) = folderHelper.toggleSubfolder(path)
    fun confirmSubfolders() = folderHelper.confirm()
    fun dismissSubfolders() = folderHelper.dismiss()

    fun removeFolder(path: String) { _folders.value = _folders.value - path }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.setRootFolders(_folders.value)
            onDone()
        }
    }
}

package fumi.day.literalplayer.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalplayer.data.prefs.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val prefs: UserPreferences,
) : ViewModel() {

    private val _folders = MutableStateFlow<Set<String>>(emptySet())
    val folders = _folders.asStateFlow()

    private val _pendingSubfolders = MutableStateFlow<List<String>>(emptyList())
    val pendingSubfolders = _pendingSubfolders.asStateFlow()

    private val _checkedSubfolders = MutableStateFlow<Set<String>>(emptySet())
    val checkedSubfolders = _checkedSubfolders.asStateFlow()

    fun scanAndShowSubfolders(path: String) {
        val subs = File(path).listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.map { it.absolutePath }
            ?.sorted()
            ?: emptyList()
        if (subs.isEmpty()) {
            _folders.value = _folders.value + path
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
        _folders.value = _folders.value + _checkedSubfolders.value
        _pendingSubfolders.value = emptyList()
        _checkedSubfolders.value = emptySet()
    }

    fun dismissSubfolders() {
        _pendingSubfolders.value = emptyList()
        _checkedSubfolders.value = emptySet()
    }

    fun removeFolder(path: String) {
        _folders.value = _folders.value - path
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.setRootFolders(_folders.value)
            onDone()
        }
    }
}

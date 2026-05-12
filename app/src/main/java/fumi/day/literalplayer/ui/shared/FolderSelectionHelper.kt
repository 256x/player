package fumi.day.literalplayer.ui.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class FolderSelectionHelper(
    private val scope: CoroutineScope,
    private val onAddFolders: suspend (Set<String>) -> Unit,
) {
    private val _pendingSubfolders = MutableStateFlow<List<String>>(emptyList())
    val pendingSubfolders = _pendingSubfolders.asStateFlow()

    private val _checkedSubfolders = MutableStateFlow<Set<String>>(emptySet())
    val checkedSubfolders = _checkedSubfolders.asStateFlow()

    fun scanAndShowSubfolders(path: String) {
        val subs = File(path).listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.map { it.absolutePath }?.sorted() ?: emptyList()
        if (subs.isEmpty()) {
            scope.launch { onAddFolders(setOf(path)) }
        } else {
            _pendingSubfolders.value = subs
            _checkedSubfolders.value = subs.toSet()
        }
    }

    fun toggleSubfolder(path: String) {
        _checkedSubfolders.value = if (path in _checkedSubfolders.value)
            _checkedSubfolders.value - path else _checkedSubfolders.value + path
    }

    fun confirm() {
        scope.launch { onAddFolders(_checkedSubfolders.value) }
        dismiss()
    }

    fun dismiss() {
        _pendingSubfolders.value = emptyList()
        _checkedSubfolders.value = emptySet()
    }
}

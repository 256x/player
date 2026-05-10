package fumi.day.literalplayer.ui.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fumi.day.literalplayer.domain.model.Track
import fumi.day.literalplayer.domain.model.displayTitle
import fumi.day.literalplayer.domain.model.toDisplayDuration

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    currentTrackId: String? = null,
    onTrackClick: (Track) -> Unit,
    onAlbumPlay: (List<Track>) -> Unit,
    onArtistPlay: (List<Track>) -> Unit,
    onBack: () -> Unit,
    viewModel: ArtistDetailViewModel = hiltViewModel(),
) {
    val favoritesLists by viewModel.favoritesLists.collectAsState()
    val trackAction by viewModel.trackAction.collectAsState()
    val trackMemberOf by viewModel.trackMemberOf.collectAsState()
    val albumMap by viewModel.albumMap.collectAsState()
    val multiPlaylistTracks by viewModel.multiPlaylistSheetTracks.collectAsState()
    var newListName by remember { mutableStateOf("") }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var showMultiNewPlaylistDialog by remember { mutableStateOf(false) }
    var confirmDeleteTrack by remember { mutableStateOf<Track?>(null) }
    var confirmDeleteSelected by remember { mutableStateOf(false) }

    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelecting = selectedIds.isNotEmpty()
    fun toggleSelect(track: Track) {
        selectedIds = if (track.id in selectedIds) selectedIds - track.id else selectedIds + track.id
    }

    LaunchedEffect(artistName) { viewModel.setArtistName(artistName) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = { Text(artistName, style = MaterialTheme.typography.titleMedium) },
                    actions = {
                        val allTracks = albumMap.values.flatten()
                        if (allTracks.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updatePlaylist(allTracks); onArtistPlay(allTracks) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play all")
                            }
                        }
                    },
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        bottomBar = {
            if (isSelecting) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${selectedIds.size} selected",
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = {
                        val tracks = albumMap.values.flatten().filter { it.id in selectedIds }
                        viewModel.showMultiPlaylistSheet(tracks)
                    }) { Text("Playlist") }
                    TextButton(onClick = { confirmDeleteSelected = true }) {
                        Text("Delete", color = Color(0xFFCF6679))
                    }
                    IconButton(onClick = { selectedIds = emptySet() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            albumMap.forEach { (album, tracks) ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = album,
                            modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(onClick = { viewModel.updatePlaylist(tracks); onAlbumPlay(tracks) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play album",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(tracks) { track ->
                    val isSelected = track.id in selectedIds
                    val bg = when {
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        track.id == currentTrackId -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { if (isSelecting) toggleSelect(track) else { viewModel.updatePlaylist(tracks); onTrackClick(track) } },
                                onLongClick = { if (isSelecting) toggleSelect(track) else viewModel.showTrackAction(track) },
                            )
                            .background(bg)
                            .padding(start = if (isSelecting) 16.dp else 32.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isSelecting) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle
                                              else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                        }
                        Text(track.displayTitle, modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge)
                        Text(track.durationMs.toDisplayDuration(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 32.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }

    trackAction?.let { action ->
        ModalBottomSheet(
            onDismissRequest = viewModel::hideTrackAction,
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(action.track.displayTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                TextButton(onClick = { showNewPlaylistDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ New playlist")
                }
                HorizontalDivider()
                favoritesLists.forEach { list ->
                    val isMember = list.id in trackMemberOf
                    TextButton(
                        onClick = { viewModel.toggleTrackInPlaylist(list.id, action.track) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (isMember) "✓ " else "    ",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(list.name)
                        }
                    }
                }
                HorizontalDivider()
                TextButton(
                    onClick = { selectedIds = setOf(action.track.id); viewModel.hideTrackAction() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Select multiple") }
                HorizontalDivider()
                TextButton(
                    onClick = { viewModel.hideTrackAction(); confirmDeleteTrack = action.track },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Delete file", color = Color(0xFFCF6679)) }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showNewPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showNewPlaylistDialog = false },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(value = newListName, onValueChange = { newListName = it },
                    placeholder = { Text("List name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newListName.isNotBlank()) {
                        trackAction?.let { viewModel.createPlaylistAndAdd(newListName.trim(), it.track) }
                        newListName = ""; showNewPlaylistDialog = false; viewModel.hideTrackAction()
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewPlaylistDialog = false }) { Text("Cancel") } }
        )
    }

    confirmDeleteTrack?.let { track ->
        AlertDialog(
            onDismissRequest = { confirmDeleteTrack = null },
            title = { Text("Delete file?") },
            text = { Text(track.displayTitle) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFile(track)
                    confirmDeleteTrack = null
                }) { Text("Delete", color = Color(0xFFCF6679)) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteTrack = null }) { Text("Cancel") } }
        )
    }

    if (confirmDeleteSelected) {
        AlertDialog(
            onDismissRequest = { confirmDeleteSelected = false },
            title = { Text("Delete ${selectedIds.size} files?") },
            confirmButton = {
                TextButton(onClick = {
                    val tracks = albumMap.values.flatten().filter { it.id in selectedIds }
                    viewModel.deleteFiles(tracks)
                    selectedIds = emptySet()
                    confirmDeleteSelected = false
                }) { Text("Delete", color = Color(0xFFCF6679)) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteSelected = false }) { Text("Cancel") } }
        )
    }

    if (multiPlaylistTracks.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = viewModel::hideMultiPlaylistSheet,
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("${multiPlaylistTracks.size} tracks", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                TextButton(onClick = { showMultiNewPlaylistDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ New playlist")
                }
                HorizontalDivider()
                favoritesLists.forEach { list ->
                    TextButton(
                        onClick = {
                            viewModel.addAllToPlaylist(list.id, multiPlaylistTracks)
                            viewModel.hideMultiPlaylistSheet()
                            selectedIds = emptySet()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(list.name) }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showMultiNewPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showMultiNewPlaylistDialog = false },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(
                    value = newListName, onValueChange = { newListName = it },
                    placeholder = { Text("List name") }, singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newListName.isNotBlank()) {
                        viewModel.createPlaylistAndAddAll(newListName.trim(), multiPlaylistTracks)
                        newListName = ""; showMultiNewPlaylistDialog = false
                        viewModel.hideMultiPlaylistSheet(); selectedIds = emptySet()
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showMultiNewPlaylistDialog = false }) { Text("Cancel") } }
        )
    }
}

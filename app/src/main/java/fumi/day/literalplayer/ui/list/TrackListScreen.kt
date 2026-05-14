package fumi.day.literalplayer.ui.list

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import fumi.day.literalplayer.domain.model.FavoritesList
import fumi.day.literalplayer.domain.model.SortOrder
import fumi.day.literalplayer.ui.shared.MultiPlaylistSheet
import fumi.day.literalplayer.ui.shared.TrackActionSheet
import fumi.day.literalplayer.ui.player.PlayerViewModel
import fumi.day.literalplayer.domain.model.Track
import fumi.day.literalplayer.domain.model.displayAlbum
import fumi.day.literalplayer.domain.model.displayArtist
import fumi.day.literalplayer.domain.model.displayTitle
import fumi.day.literalplayer.domain.model.toDisplayDuration

private val TABS = listOf("Artist", "Album", "All", "Playlist")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackListScreen(
    onTrackClick: (Track) -> Unit,
    onArtistClick: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlayer: () -> Unit = {},
    playerViewModel: PlayerViewModel,
    viewModel: TrackListViewModel = hiltViewModel(),
) {
    val prefs by viewModel.prefs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val playerState by playerViewModel.state.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val favoritesLists by viewModel.favoritesLists.collectAsState()
    val trackAction by viewModel.trackAction.collectAsState()
    val trackMemberOf by viewModel.trackMemberOf.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var showMultiNewPlaylistDialog by remember { mutableStateOf(false) }
    var confirmDeleteTrack by remember { mutableStateOf<Track?>(null) }
    var confirmDeleteSelected by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val pendingDeleteSender by viewModel.pendingDeleteSender.collectAsState()
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.onDeleteConfirmed()
        else viewModel.onDeleteDismissed()
    }
    LaunchedEffect(pendingDeleteSender) {
        pendingDeleteSender?.let {
            deleteRequestLauncher.launch(IntentSenderRequest.Builder(it.first).build())
        }
    }

    val filteredTracks by viewModel.filteredTracks.collectAsState()
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsState()
    val playlistTracks by viewModel.playlistTracks.collectAsState()
    val multiPlaylistTracks by viewModel.multiPlaylistSheetTracks.collectAsState()

    val currentPlaylistName = favoritesLists.find { it.id == selectedPlaylistId }?.name

    var selectedIds by remember(selectedTab) { mutableStateOf<Set<String>>(emptySet()) }
    val isSelecting = selectedIds.isNotEmpty()
    fun toggleSelect(track: Track) {
        selectedIds = if (track.id in selectedIds) selectedIds - track.id else selectedIds + track.id
    }

    val sorted = when (prefs.sortOrder) {
        SortOrder.NAME -> filteredTracks.sortedBy { it.displayTitle.lowercase() }
        SortOrder.DATE -> filteredTracks.sortedByDescending { it.lastModified }
        SortOrder.SIZE -> filteredTracks.sortedByDescending { it.fileSizeBytes }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = { Text("Literal Player", style = MaterialTheme.typography.titleMedium) },
                    actions = {
                        Box {
                            IconButton(onClick = { showFolderMenu = true }) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = "Folders",
                                    tint = if (selectedFolder != null) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            DropdownMenu(expanded = showFolderMenu, onDismissRequest = { showFolderMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("All folders", style = MaterialTheme.typography.bodyMedium) },
                                    onClick = { viewModel.selectFolder(null); showFolderMenu = false }
                                )
                                prefs.rootFolders.sorted().forEach { folder ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                folder.substringAfterLast("/"),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (selectedFolder == folder) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurface,
                                            )
                                        },
                                        onClick = { viewModel.selectFolder(folder); showFolderMenu = false }
                                    )
                                }
                            }
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                listOf(SortOrder.NAME, SortOrder.DATE, SortOrder.SIZE).forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                        onClick = { viewModel.setSortOrder(order); showSortMenu = false }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                TabRow(selectedTabIndex = selectedTab) {
                    TABS.forEachIndexed { i, title ->
                        Tab(selected = selectedTab == i, onClick = { selectedTab = i },
                            text = { Text(title, style = MaterialTheme.typography.bodyMedium) })
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.imePadding()) {
                playerState.track?.let { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToPlayer)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.displayTitle, style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(track.displayArtist, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        IconButton(onClick = playerViewModel::playPause) {
                            Icon(
                                if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                            )
                        }
                    }
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (isSelecting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${selectedIds.size} selected",
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = {
                            val tracks = sorted.filter { it.id in selectedIds }
                            viewModel.showMultiPlaylistSheet(tracks)
                        }) { Text("Playlist") }
                        TextButton(onClick = { confirmDeleteTrack = null; confirmDeleteSelected = true }) {
                            Text("Delete", color = Color(0xFFCF6679))
                        }
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    }
                } else {
                    BottomAppBar(containerColor = Color.Transparent) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = viewModel::setSearch,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text("Search tracks...", style = TextStyle(fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    }
                                    inner()
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> ArtistTab(tracks = sorted, onArtistClick = onArtistClick, padding = padding)
                1 -> AlbumTab(
                    tracks = sorted,
                    onTrackClick = { albumTracks, track -> viewModel.updatePlaylist(albumTracks); onTrackClick(track) },
                    onToggle = ::toggleSelect,
                    onAction = { track -> viewModel.showTrackAction(track) },
                    onClearSelection = { selectedIds = emptySet() },
                    currentTrackId = playerState.track?.id,
                    selectedIds = selectedIds,
                    padding = padding,
                )
                2 -> AllTab(
                    tracks = sorted,
                    onTrackClick = { track -> viewModel.updatePlaylist(sorted); onTrackClick(track) },
                    onToggle = ::toggleSelect,
                    onAction = { track -> viewModel.showTrackAction(track) },
                    currentTrackId = playerState.track?.id,
                    selectedIds = selectedIds,
                    padding = padding,
                )
                3 -> PlaylistTab(
                    favoritesLists = favoritesLists,
                    selectedPlaylistId = selectedPlaylistId,
                    playlistTracks = playlistTracks,
                    currentTrackId = playerState.track?.id,
                    selectedIds = selectedIds,
                    onSelectPlaylist = viewModel::selectPlaylist,
                    onDeselectPlaylist = { viewModel.selectPlaylist(null); selectedIds = emptySet() },
                    onTrackClick = { track -> viewModel.updatePlaylist(playlistTracks); onTrackClick(track) },
                    onToggle = ::toggleSelect,
                    onAction = { track -> viewModel.showTrackAction(track, selectedPlaylistId, currentPlaylistName) },
                    onDeletePlaylist = viewModel::deletePlaylist,
                    padding = padding,
                )
            }
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
        }
    }

    trackAction?.let { action ->
        TrackActionSheet(
            action = action,
            trackMemberOf = trackMemberOf,
            favoritesLists = favoritesLists,
            onDismiss = viewModel::hideTrackAction,
            onNewPlaylist = { showNewPlaylistDialog = true },
            onTogglePlaylist = { viewModel.toggleTrackInPlaylist(it, action.track) },
            onSelectMultiple = { selectedIds = setOf(action.track.id); viewModel.hideTrackAction() },
            onDeleteFile = { viewModel.hideTrackAction(); confirmDeleteTrack = action.track },
            onRemoveFromPlaylist = if (action.playlistId != null) {
                { viewModel.removeFromPlaylist(action.playlistId, action.track.id); viewModel.hideTrackAction() }
            } else null,
        )
    }

    if (showNewPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showNewPlaylistDialog = false },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    placeholder = { Text("List name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newListName.isNotBlank()) {
                        trackAction?.let { viewModel.createPlaylistAndAdd(newListName.trim(), it.track) }
                        newListName = ""
                        showNewPlaylistDialog = false
                        viewModel.hideTrackAction()
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewPlaylistDialog = false }) { Text("Cancel") }
            }
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
            dismissButton = {
                TextButton(onClick = { confirmDeleteTrack = null }) { Text("Cancel") }
            }
        )
    }

    if (confirmDeleteSelected) {
        AlertDialog(
            onDismissRequest = { confirmDeleteSelected = false },
            title = { Text("Delete ${selectedIds.size} files?") },
            confirmButton = {
                TextButton(onClick = {
                    val tracks = sorted.filter { it.id in selectedIds }
                    viewModel.deleteFiles(tracks)
                    selectedIds = emptySet()
                    confirmDeleteSelected = false
                }) { Text("Delete", color = Color(0xFFCF6679)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteSelected = false }) { Text("Cancel") }
            }
        )
    }

    if (multiPlaylistTracks.isNotEmpty()) {
        MultiPlaylistSheet(
            trackCount = multiPlaylistTracks.size,
            favoritesLists = favoritesLists,
            onDismiss = viewModel::hideMultiPlaylistSheet,
            onNewPlaylist = { showMultiNewPlaylistDialog = true },
            onAddToPlaylist = {
                viewModel.addAllToPlaylist(it, multiPlaylistTracks)
                viewModel.hideMultiPlaylistSheet()
                selectedIds = emptySet()
            },
        )
    }

    if (showMultiNewPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showMultiNewPlaylistDialog = false },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    placeholder = { Text("List name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newListName.isNotBlank()) {
                        viewModel.createPlaylistAndAddAll(newListName.trim(), multiPlaylistTracks)
                        newListName = ""
                        showMultiNewPlaylistDialog = false
                        viewModel.hideMultiPlaylistSheet()
                        selectedIds = emptySet()
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showMultiNewPlaylistDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ArtistTab(
    tracks: List<Track>,
    onArtistClick: (String) -> Unit,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    val artists = tracks.groupBy { it.displayArtist }.keys.sorted()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
        items(artists) { artist ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onArtistClick(artist) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(artist, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumTab(
    tracks: List<Track>,
    onTrackClick: (albumTracks: List<Track>, track: Track) -> Unit,
    onToggle: (Track) -> Unit,
    onAction: (Track) -> Unit,
    onClearSelection: () -> Unit,
    currentTrackId: String?,
    selectedIds: Set<String>,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    var selectedAlbumName by remember { mutableStateOf<String?>(null) }
    val albums = tracks.groupBy { it.displayAlbum }.entries.sortedBy { it.key }

    BackHandler(enabled = selectedAlbumName != null) {
        selectedAlbumName = null
        onClearSelection()
    }

    if (selectedAlbumName == null) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(albums) { (album, albumTracks) ->
                Column(
                    modifier = Modifier.fillMaxWidth().clickable { selectedAlbumName = album }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(album, style = MaterialTheme.typography.bodyLarge)
                    Text(albumTracks.first().displayArtist, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    } else {
        val albumTracks = tracks.filter { it.displayAlbum == selectedAlbumName }
        if (albumTracks.isEmpty()) { selectedAlbumName = null; return }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { selectedAlbumName = null; onClearSelection() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text("← Back", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                        Text(selectedAlbumName!!, style = MaterialTheme.typography.titleMedium)
                        Text(albumTracks.first().displayArtist, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onTrackClick(albumTracks, albumTracks.first()) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play album",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            items(albumTracks) { track ->
                TrackRow(
                    track = track,
                    isPlaying = track.id == currentTrackId,
                    isSelected = track.id in selectedIds,
                    isSelecting = selectedIds.isNotEmpty(),
                    onClick = { if (selectedIds.isNotEmpty()) onToggle(track) else onTrackClick(albumTracks, track) },
                    onLongClick = { if (selectedIds.isNotEmpty()) onToggle(track) else onAction(track) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllTab(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    onToggle: (Track) -> Unit,
    onAction: (Track) -> Unit,
    currentTrackId: String?,
    selectedIds: Set<String>,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (tracks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${tracks.size} tracks",
                        modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = { onTrackClick(tracks.first()) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play all",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
        items(tracks) { track ->
            TrackRow(
                track = track,
                isPlaying = track.id == currentTrackId,
                isSelected = track.id in selectedIds,
                isSelecting = selectedIds.isNotEmpty(),
                onClick = { if (selectedIds.isNotEmpty()) onToggle(track) else onTrackClick(track) },
                onLongClick = { if (selectedIds.isNotEmpty()) onToggle(track) else onAction(track) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistTab(
    favoritesLists: List<FavoritesList>,
    selectedPlaylistId: Long?,
    playlistTracks: List<Track>,
    currentTrackId: String?,
    selectedIds: Set<String>,
    onSelectPlaylist: (Long) -> Unit,
    onDeselectPlaylist: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onToggle: (Track) -> Unit,
    onAction: (Track) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

    BackHandler(enabled = selectedPlaylistId != null) { onDeselectPlaylist() }

    if (selectedPlaylistId == null) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(favoritesLists) { list ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onSelectPlaylist(list.id) },
                            onLongClick = { confirmDeleteId = list.id },
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(list.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    } else {
        val playlistName = favoritesLists.find { it.id == selectedPlaylistId }?.name ?: ""
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDeselectPlaylist)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("← Back", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        playlistName,
                        modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (playlistTracks.isNotEmpty()) {
                        IconButton(onClick = { onTrackClick(playlistTracks.first()) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play playlist",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            items(playlistTracks) { track ->
                TrackRow(
                    track = track,
                    isPlaying = track.id == currentTrackId,
                    isSelected = track.id in selectedIds,
                    isSelecting = selectedIds.isNotEmpty(),
                    onClick = { if (selectedIds.isNotEmpty()) onToggle(track) else onTrackClick(track) },
                    onLongClick = { if (selectedIds.isNotEmpty()) onToggle(track) else onAction(track) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }

    confirmDeleteId?.let { id ->
        val name = favoritesLists.find { it.id == id }?.name ?: ""
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Delete \"$name\"?") },
            confirmButton = {
                TextButton(onClick = { onDeletePlaylist(id); confirmDeleteId = null }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: Track,
    isPlaying: Boolean = false,
    isSelected: Boolean = false,
    isSelecting: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        isPlaying -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
        Text(track.displayTitle, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(track.durationMs.toDisplayDuration(), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

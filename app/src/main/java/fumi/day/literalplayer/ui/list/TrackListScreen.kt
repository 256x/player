package fumi.day.literalplayer.ui.list

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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
    onAlbumPlay: (List<Track>) -> Unit,
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
    val favoritesSheetTrack by viewModel.favoritesSheetTrack.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var newFavListName by remember { mutableStateOf("") }
    var showNewFavDialog by remember { mutableStateOf(false) }
    var confirmDeleteTrack by remember { mutableStateOf<Track?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current

    val filteredTracks by viewModel.filteredTracks.collectAsState()
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsState()
    val playlistTracks by viewModel.playlistTracks.collectAsState()
    val playlistActionTrack by viewModel.playlistActionTrack.collectAsState()
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
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> ArtistTab(sorted, onArtistClick, padding)
                1 -> AlbumTab(sorted, { tracks -> viewModel.updatePlaylist(tracks); onAlbumPlay(tracks) }, padding)
                2 -> AllTab(sorted, { track -> viewModel.updatePlaylist(sorted); onTrackClick(track) },
                    viewModel::showFavoritesSheet, playerState.track?.id, padding)
                3 -> PlaylistTab(
                    favoritesLists = favoritesLists,
                    selectedPlaylistId = selectedPlaylistId,
                    playlistTracks = playlistTracks,
                    currentTrackId = playerState.track?.id,
                    onSelectPlaylist = viewModel::selectPlaylist,
                    onDeselectPlaylist = { viewModel.selectPlaylist(null) },
                    onTrackClick = { track -> viewModel.updatePlaylist(playlistTracks); onTrackClick(track) },
                    onTrackLongPress = viewModel::showPlaylistActionSheet,
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

    playlistActionTrack?.let { track ->
        val currentListName = favoritesLists.find { it.id == selectedPlaylistId }?.name ?: ""
        ModalBottomSheet(
            onDismissRequest = viewModel::hidePlaylistActionSheet,
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(track.displayTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                TextButton(
                    onClick = {
                        viewModel.removeFromCurrentPlaylist(track.id)
                        viewModel.hidePlaylistActionSheet()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("✕ Remove from \"$currentListName\"") }
                HorizontalDivider()
                TextButton(
                    onClick = {
                        viewModel.hidePlaylistActionSheet()
                        viewModel.showFavoritesSheet(track)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("+ Add to another playlist") }
                HorizontalDivider()
                TextButton(
                    onClick = {
                        viewModel.hidePlaylistActionSheet()
                        confirmDeleteTrack = track
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Delete file", color = androidx.compose.ui.graphics.Color(0xFFCF6679)) }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    favoritesSheetTrack?.let { track ->
        ModalBottomSheet(
            onDismissRequest = viewModel::hideFavoritesSheet,
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(track.displayTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                TextButton(
                    onClick = { showNewFavDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("+ New favorites list") }
                HorizontalDivider()
                favoritesLists.forEach { list ->
                    TextButton(
                        onClick = {
                            viewModel.addToFavorites(list.id, track)
                            viewModel.hideFavoritesSheet()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("♥ ${list.name}") }
                }
                HorizontalDivider()
                TextButton(
                    onClick = {
                        viewModel.hideFavoritesSheet()
                        confirmDeleteTrack = track
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Delete file", color = androidx.compose.ui.graphics.Color(0xFFCF6679)) }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showNewFavDialog) {
        AlertDialog(
            onDismissRequest = { showNewFavDialog = false },
            title = { Text("New favorites list") },
            text = {
                OutlinedTextField(
                    value = newFavListName,
                    onValueChange = { newFavListName = it },
                    placeholder = { Text("List name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFavListName.isNotBlank()) {
                        favoritesSheetTrack?.let {
                            viewModel.createFavoritesListAndAdd(newFavListName.trim(), it)
                        }
                        newFavListName = ""
                        showNewFavDialog = false
                        viewModel.hideFavoritesSheet()
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFavDialog = false }) { Text("Cancel") }
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
                }) { Text("Delete", color = androidx.compose.ui.graphics.Color(0xFFCF6679)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteTrack = null }) { Text("Cancel") }
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
                Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun AlbumTab(
    tracks: List<Track>,
    onAlbumPlay: (List<Track>) -> Unit,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    val albums = tracks.groupBy { it.displayAlbum }
        .entries.sortedBy { it.key }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
        items(albums) { (album, albumTracks) ->
            Column(
                modifier = Modifier.fillMaxWidth().clickable { onAlbumPlay(albumTracks) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(album, style = MaterialTheme.typography.bodyLarge)
                Text(albumTracks.first().displayArtist, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllTab(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    onLongPress: (Track) -> Unit,
    currentTrackId: String?,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
        items(tracks) { track ->
            TrackRow(
                track = track,
                isPlaying = track.id == currentTrackId,
                onClick = { onTrackClick(track) },
                onLongClick = { onLongPress(track) },
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
    onSelectPlaylist: (Long) -> Unit,
    onDeselectPlaylist: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onTrackLongPress: (Track) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

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
                    Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    } else {
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
            items(playlistTracks) { track ->
                TrackRow(
                    track = track,
                    isPlaying = track.id == currentTrackId,
                    onClick = { onTrackClick(track) },
                    onLongClick = { onTrackLongPress(track) },
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
fun TrackRow(track: Track, isPlaying: Boolean = false, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(track.displayTitle, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(track.durationMs.toDisplayDuration(), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

package fumi.day.literalplayer.ui.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
    onBack: () -> Unit,
    viewModel: ArtistDetailViewModel = hiltViewModel(),
) {
    val favoritesLists by viewModel.favoritesLists.collectAsState()
    val favoritesSheetTrack by viewModel.favoritesSheetTrack.collectAsState()
    val albumMap by viewModel.albumMap.collectAsState()
    var newFavListName by remember { mutableStateOf("") }
    var showNewFavDialog by remember { mutableStateOf(false) }
    var confirmDeleteTrack by remember { mutableStateOf<Track?>(null) }

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
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            albumMap.forEach { (album, tracks) ->
                item {
                    Text(
                        text = album,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = { viewModel.updatePlaylist(tracks); onAlbumPlay(tracks) })
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(tracks) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { viewModel.updatePlaylist(tracks); onTrackClick(track) },
                                onLongClick = { viewModel.showFavoritesSheet(track) },
                            )
                            .background(if (track.id == currentTrackId) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
                            .padding(start = 32.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
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

    favoritesSheetTrack?.let { track ->
        ModalBottomSheet(
            onDismissRequest = viewModel::hideFavoritesSheet,
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(track.displayTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                TextButton(onClick = { showNewFavDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ New favorites list")
                }
                HorizontalDivider()
                favoritesLists.forEach { list ->
                    TextButton(
                        onClick = { viewModel.addToFavorites(list.id, track); viewModel.hideFavoritesSheet() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("♥ ${list.name}") }
                }
                HorizontalDivider()
                TextButton(
                    onClick = { viewModel.hideFavoritesSheet(); confirmDeleteTrack = track },
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
                OutlinedTextField(value = newFavListName, onValueChange = { newFavListName = it },
                    placeholder = { Text("List name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFavListName.isNotBlank()) {
                        favoritesSheetTrack?.let { viewModel.createFavoritesListAndAdd(newFavListName.trim(), it) }
                        newFavListName = ""; showNewFavDialog = false; viewModel.hideFavoritesSheet()
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewFavDialog = false }) { Text("Cancel") } }
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
            dismissButton = { TextButton(onClick = { confirmDeleteTrack = null }) { Text("Cancel") } }
        )
    }
}

package fumi.day.literalplayer.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import fumi.day.literalplayer.domain.model.displayAlbum
import fumi.day.literalplayer.domain.model.displayArtist
import fumi.day.literalplayer.domain.model.displayTitle
import fumi.day.literalplayer.domain.model.toDisplayDuration
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    trackId: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel,
) {
    val state by viewModel.state.collectAsState()
    val favoritesLists by viewModel.favoritesLists.collectAsState()
    val showFavSheet by viewModel.showFavSheet.collectAsState()
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showNewFavDialog by remember { mutableStateOf(false) }
    var newFavListName by remember { mutableStateOf("") }

    LaunchedEffect(trackId) { viewModel.init(trackId) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        if (!state.isPlaying) {
            IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            // Title zone: weight(1f) so it absorbs all slack.
            // BottomStart pins the text to the bottom edge — title grows upward,
            // so the seekbar's position never shifts when lines wrap.
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.BottomStart,
            ) {
                state.track?.let { track ->
                    Column {
                        Text(
                            track.displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            track.displayArtist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${track.displayAlbum}${if (track.year.isNotBlank()) " · ${track.year}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Slider(
                value = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f,
                onValueChange = { viewModel.seekTo((it * state.durationMs).toLong()) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.onBackground,
                    activeTrackColor = MaterialTheme.colorScheme.onBackground,
                    inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                ),
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = remember { MutableInteractionSource() },
                        modifier = Modifier.size(24.dp),
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.onBackground),
                    )
                }
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(state.positionMs.toDisplayDuration(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(state.durationMs.toDisplayDuration(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HoldableIconButton(
                    onAction = { viewModel.skipBackward(false) },
                    onLongAction = { viewModel.skipBackward(true) },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Rewind",
                        modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = viewModel::prevTrack, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous",
                        modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = viewModel::playPause, modifier = Modifier.size(64.dp)) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(44.dp),
                    )
                }
                IconButton(onClick = viewModel::nextTrack, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next",
                        modifier = Modifier.size(32.dp))
                }
                HoldableIconButton(
                    onAction = { viewModel.skipForward(false) },
                    onLongAction = { viewModel.skipForward(true) },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = "Forward",
                        modifier = Modifier.size(36.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row {
                    IconButton(onClick = viewModel::openFavSheet, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.StarBorder, contentDescription = "Add to playlist",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = viewModel::toggleShuffle, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            modifier = Modifier.size(28.dp),
                            tint = if (state.shuffle) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = viewModel::cycleRepeat, modifier = Modifier.size(48.dp)) {
                        Icon(
                            if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne
                            else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            modifier = Modifier.size(28.dp),
                            tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Box {
                    TextButton(onClick = { showSpeedMenu = true }) {
                        Text(SPEED_LABELS[state.speedIndex], style = MaterialTheme.typography.bodyMedium)
                    }
                    DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                        SPEED_LABELS.forEachIndexed { i, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { viewModel.setSpeed(i); showSpeedMenu = false },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showFavSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeFavSheet,
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                state.track?.let { Text(it.displayTitle, style = MaterialTheme.typography.titleMedium) }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                TextButton(onClick = { showNewFavDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ New favorites list")
                }
                HorizontalDivider()
                favoritesLists.forEach { list ->
                    TextButton(
                        onClick = { viewModel.addTrackToFavorites(list.id); viewModel.closeFavSheet() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("♥ ${list.name}") }
                }
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
                        viewModel.createListAndAddTrack(newFavListName.trim())
                        newFavListName = ""
                        showNewFavDialog = false
                        viewModel.closeFavSheet()
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFavDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HoldableIconButton(
    onAction: () -> Unit,
    onLongAction: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.pointerInput(Unit) {
            coroutineScope {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        down.consume()
                        onAction()
                        val job = launch {
                            delay(400)
                            while (isActive) {
                                onLongAction()
                                delay(300)
                            }
                        }
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        job.cancel()
                    }
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

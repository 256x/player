package fumi.day.literalplayer.ui.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fumi.day.literalplayer.domain.model.FavoritesList
import fumi.day.literalplayer.domain.model.displayTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiPlaylistSheet(
    trackCount: Int,
    favoritesLists: List<FavoritesList>,
    onDismiss: () -> Unit,
    onNewPlaylist: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("$trackCount tracks", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            TextButton(onClick = onNewPlaylist, modifier = Modifier.fillMaxWidth()) {
                Text("+ New playlist")
            }
            HorizontalDivider()
            favoritesLists.forEach { list ->
                TextButton(
                    onClick = { onAddToPlaylist(list.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(list.name) }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

package fumi.day.literalplayer.ui.setup

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fumi.day.literalplayer.util.safUriToPath

@Composable
fun SetupScreen(
    onComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val folders by viewModel.folders.collectAsState()
    val pendingSubfolders by viewModel.pendingSubfolders.collectAsState()
    val checkedSubfolders by viewModel.checkedSubfolders.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { safUriToPath(it) }?.let { viewModel.scanAndShowSubfolders(it) }
    }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
    ) {
        Text("Literal Player", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("Select folders to scan for audio files.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        HorizontalDivider()

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(folders.toList().sorted()) { folder ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(folder.substringAfterLast("/"),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { viewModel.removeFolder(folder) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
                HorizontalDivider()
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                        } else {
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        folderPicker.launch(null)
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add folder")
                    }
                    Text("Add folder", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Button(
            onClick = { viewModel.save(onComplete) },
            enabled = folders.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start")
        }
    }

    if (pendingSubfolders.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSubfolders,
            title = { Text("Select folders") },
            text = {
                LazyColumn {
                    items(pendingSubfolders) { sub ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { viewModel.toggleSubfolder(sub) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = sub in checkedSubfolders,
                                onCheckedChange = { viewModel.toggleSubfolder(sub) },
                            )
                            Text(sub.substringAfterLast("/"),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmSubfolders) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSubfolders) { Text("Cancel") }
            }
        )
    }
}

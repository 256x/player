package fumi.day.literalplayer.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt
import fumi.day.literalplayer.data.prefs.AppFont
import fumi.day.literalplayer.ui.theme.parseColor
import fumi.day.literalplayer.util.safUriToPath

enum class ColorPickerTarget { BACKGROUND, TEXT, ACCENT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val pendingSubfolders by viewModel.pendingSubfolders.collectAsState()
    val checkedSubfolders by viewModel.checkedSubfolders.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    var shortSkipText by remember(state.shortSkipSec) { mutableStateOf(state.shortSkipSec.toString()) }
    var longSkipText by remember(state.longSkipSec) { mutableStateOf(state.longSkipSec.toString()) }
    var showColorPicker by remember { mutableStateOf<ColorPickerTarget?>(null) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            safUriToPath(it)?.let { path -> viewModel.scanAndShowSubfolders(path) }
        }
    }

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
                    title = { Text("Settings", style = MaterialTheme.typography.titleMedium) },
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionHeader("Folders")
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.rootFolders.sorted().forEach { folder ->
                    InputChip(
                        selected = false,
                        onClick = { viewModel.removeFolder(folder) },
                        label = {
                            Text(
                                text = folder.substringAfterLast("/"),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Remove",
                                modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.widthIn(max = 160.dp),
                    )
                }
                InputChip(
                    selected = false,
                    onClick = { folderPicker.launch(null) },
                    label = { Text("+") },
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = viewModel::rescan) {
                    Text("Rescan media", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionHeader("Skip (seconds)")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = shortSkipText,
                    onValueChange = {
                        shortSkipText = it
                        it.toIntOrNull()?.let { v -> if (v > 0) viewModel.setShortSkip(v) }
                    },
                    label = { Text("Short") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = longSkipText,
                    onValueChange = {
                        longSkipText = it
                        it.toIntOrNull()?.let { v -> if (v > 0) viewModel.setLongSkip(v) }
                    },
                    label = { Text("Long press") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionHeader("Font")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    AppFont.DEFAULT to "Default",
                    AppFont.SERIF to "Serif",
                    AppFont.MONOSPACE to "Mono",
                    AppFont.SCOPE_ONE to "Scope",
                ).forEach { (font, label) ->
                    FilterChip(
                        selected = state.font == font,
                        onClick = { viewModel.setFont(font) },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Size", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = state.fontSize,
                    onValueChange = viewModel::setFontSize,
                    valueRange = 12f..24f,
                    steps = 5,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Text("${state.fontSize.roundToInt()}sp", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionHeader("Playback")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Volume normalization", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Boost quiet audio to a consistent level",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.normalize, onCheckedChange = viewModel::setNormalize)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Album art", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Show embedded artwork in player",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.showAlbumArt, onCheckedChange = viewModel::setShowAlbumArt)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionHeader("Colors")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ColorDot(label = "BG",
                    color = parseColor(state.backgroundColorHex) ?: Color.Black,
                    onClick = { showColorPicker = ColorPickerTarget.BACKGROUND })
                ColorDot(label = "Text",
                    color = parseColor(state.textColorHex) ?: Color.White,
                    onClick = { showColorPicker = ColorPickerTarget.TEXT })
                ColorDot(label = "Accent",
                    color = parseColor(state.accentColorHex) ?: Color(0xFF6650A4),
                    onClick = { showColorPicker = ColorPickerTarget.ACCENT })
            }

            Spacer(Modifier.height(32.dp))
            Text(
                text = "Literal Player v${fumi.day.literalplayer.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { showPrivacyPolicy = true },
            )
            Spacer(Modifier.height(8.dp))
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

    if (isScanning) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Scanning…") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("Reading media files…", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {},
        )
    }

    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            title = { Text("Privacy Policy") },
            text = {
                androidx.compose.foundation.lazy.LazyColumn {
                    item {
                        Text(
                            text = """We do not collect, store, or share any personal data. All app data is stored locally on your device. We have no servers and no backend.

This app does not require internet access and functions entirely offline.

We do not use any analytics, advertising, crash reporting, or third-party SDKs.

Since we do not collect any data, there is nothing to retain or delete on our end. Uninstalling the app removes all locally stored data from your device.

Our apps do not collect any personal information from anyone, including children under the age of 13.

Contact: literalapps@proton.me""",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicy = false }) { Text("Close") }
            },
        )
    }

    showColorPicker?.let { target ->
        ColorPickerDialog(
            initialColor = when (target) {
                ColorPickerTarget.BACKGROUND -> parseColor(state.backgroundColorHex) ?: Color.Black
                ColorPickerTarget.TEXT -> parseColor(state.textColorHex) ?: Color.White
                ColorPickerTarget.ACCENT -> parseColor(state.accentColorHex) ?: Color(0xFF6650A4)
            },
            onColorSelected = { color ->
                val hex = colorToHex(color)
                when (target) {
                    ColorPickerTarget.BACKGROUND -> viewModel.setBackgroundColor(hex)
                    ColorPickerTarget.TEXT -> viewModel.setTextColor(hex)
                    ColorPickerTarget.ACCENT -> viewModel.setAccentColor(hex)
                }
                showColorPicker = null
            },
            onDismiss = { showColorPicker = null },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun ColorDot(label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2]
    }

    val currentColor = Color.hsv(hue, saturation, brightness)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(hue) {
                            detectTapGestures { offset ->
                                saturation = (offset.x / size.width).coerceIn(0f, 1f)
                                brightness = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(hue) {
                            detectDragGestures { change, _ ->
                                saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                                brightness = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
                        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                        val cx = saturation * size.width
                        val cy = (1f - brightness) * size.height
                        drawCircle(Color.White, 12f, Offset(cx, cy))
                        drawCircle(Color.Black, 10f, Offset(cx, cy), style = Stroke(2f))
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(brush = Brush.horizontalGradient((0..360 step 30).map { Color.hsv(it.toFloat(), 1f, 1f) }))
                        val cx = hue / 360f * size.width
                        drawCircle(Color.White, 14f, Offset(cx, size.height / 2))
                        drawCircle(Color.Black, 12f, Offset(cx, size.height / 2), style = Stroke(2f))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Text(colorToHex(currentColor), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onColorSelected(currentColor) }) { Text("Select") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun colorToHex(color: Color): String =
    String.format("#%06X", 0xFFFFFF and color.toArgb())

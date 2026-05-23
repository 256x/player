package fumi.day.literalplayer.data.prefs

import android.content.Context
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fumi.day.literalplayer.domain.model.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AppFont { DEFAULT, SERIF, MONOSPACE, SCOPE_ONE }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class UserPrefs(
    val rootFolders: Set<String> = emptySet(),
    val sortOrder: SortOrder = SortOrder.NAME,
    val shortSkipSec: Int = 10,
    val longSkipSec: Int = 30,
    val accentColorHex: String = "",
    val textColorHex: String = "",
    val backgroundColorHex: String = "",
    val font: AppFont = AppFont.DEFAULT,
    val fontSize: Float = 16f,
    val normalize: Boolean = false,
    val showAlbumArt: Boolean = false,
)

@Singleton
class UserPreferences @Inject constructor(private val context: Context) {
    private val rootFoldersKey = stringSetPreferencesKey("root_folders")
    private val sortOrderKey = stringPreferencesKey("sort_order")
    private val shortSkipKey = intPreferencesKey("short_skip_sec")
    private val longSkipKey = intPreferencesKey("long_skip_sec")
    private val accentColorKey = stringPreferencesKey("accent_color")
    private val textColorKey = stringPreferencesKey("text_color")
    private val bgColorKey = stringPreferencesKey("bg_color")
    private val fontKey = stringPreferencesKey("font")
    private val fontSizeKey = floatPreferencesKey("font_size")
    private val normalizeKey = booleanPreferencesKey("normalize")
    private val showAlbumArtKey = booleanPreferencesKey("show_album_art")

    val prefs: Flow<UserPrefs> = context.dataStore.data.map { p ->
        UserPrefs(
            rootFolders = (p[rootFoldersKey] ?: emptySet()).map { migrateSafPath(it) }.toSet(),
            sortOrder = SortOrder.entries.find { it.name == p[sortOrderKey] } ?: SortOrder.NAME,
            shortSkipSec = p[shortSkipKey] ?: 10,
            longSkipSec = p[longSkipKey] ?: 30,
            accentColorHex = p[accentColorKey] ?: "",
            textColorHex = p[textColorKey] ?: "",
            backgroundColorHex = p[bgColorKey] ?: "",
            font = AppFont.entries.find { it.name == p[fontKey] } ?: AppFont.DEFAULT,
            fontSize = p[fontSizeKey] ?: 16f,
            normalize = p[normalizeKey] ?: false,
            showAlbumArt = p[showAlbumArtKey] ?: false,
        )
    }

    private fun migrateSafPath(path: String): String {
        if (!path.startsWith("/tree/")) return path
        val after = path.removePrefix("/tree/")
        val parts = after.split(":", limit = 2)
        if (parts.size < 2) return path
        val volume = parts[0]
        val rel = parts[1]
        return if (volume.equals("primary", ignoreCase = true))
            "${Environment.getExternalStorageDirectory().absolutePath}/$rel"
        else "/storage/$volume/$rel"
    }

    suspend fun setRootFolders(folders: Set<String>) {
        context.dataStore.edit { it[rootFoldersKey] = folders }
    }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { it[sortOrderKey] = order.name }
    }

    suspend fun setShortSkipSec(sec: Int) {
        context.dataStore.edit { it[shortSkipKey] = sec }
    }

    suspend fun setLongSkipSec(sec: Int) {
        context.dataStore.edit { it[longSkipKey] = sec }
    }

    suspend fun setAccentColor(hex: String) {
        context.dataStore.edit { it[accentColorKey] = hex }
    }

    suspend fun setTextColor(hex: String) {
        context.dataStore.edit { it[textColorKey] = hex }
    }

    suspend fun setBackgroundColor(hex: String) {
        context.dataStore.edit { it[bgColorKey] = hex }
    }

    suspend fun setFont(font: AppFont) {
        context.dataStore.edit { it[fontKey] = font.name }
    }

    suspend fun setFontSize(size: Float) {
        context.dataStore.edit { it[fontSizeKey] = size }
    }

    suspend fun setNormalize(enabled: Boolean) {
        context.dataStore.edit { it[normalizeKey] = enabled }
    }

    suspend fun setShowAlbumArt(enabled: Boolean) {
        context.dataStore.edit { it[showAlbumArtKey] = enabled }
    }
}

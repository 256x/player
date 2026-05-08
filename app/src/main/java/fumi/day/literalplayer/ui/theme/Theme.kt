package fumi.day.literalplayer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fumi.day.literalplayer.R
import fumi.day.literalplayer.data.prefs.AppFont
import fumi.day.literalplayer.data.prefs.UserPrefs
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat

val ScopeOneFamily = FontFamily(Font(R.font.scopeone))

fun AppFont.toFontFamily(): FontFamily = when (this) {
    AppFont.DEFAULT -> FontFamily.Default
    AppFont.SERIF -> FontFamily.Serif
    AppFont.MONOSPACE -> FontFamily.Monospace
    AppFont.SCOPE_ONE -> ScopeOneFamily
}

data class AppThemeState(
    val accentColor: Color = Color(0xFF6650A4),
)

val LocalAppTheme = compositionLocalOf { AppThemeState() }

fun parseColor(hex: String): Color? {
    if (hex.isBlank()) return null
    return try { Color(hex.toColorInt()) } catch (e: Exception) { null }
}

@Composable
fun LiteralPlayerTheme(
    userPrefs: UserPrefs = UserPrefs(),
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val accentColor = parseColor(userPrefs.accentColorHex) ?: Color(0xFF6650A4)
    val textColor = parseColor(userPrefs.textColorHex) ?: Color.White
    val bgColor = parseColor(userPrefs.backgroundColorHex) ?: Color.Black

    val baseColorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    val colorScheme = baseColorScheme.copy(
        primary = accentColor,
        secondary = accentColor,
        tertiary = accentColor,
        background = bgColor,
        surface = bgColor,
        onBackground = textColor,
        onSurface = textColor,
        onSurfaceVariant = textColor.copy(alpha = 0.6f),
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val fontFamily = userPrefs.font.toFontFamily()
    val fs = userPrefs.fontSize
    val typography = Typography(
        bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = fs.sp, lineHeight = (fs * 1.5f).sp),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = (fs * 0.875f).sp, lineHeight = (fs * 1.4f).sp),
        bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = (fs * 0.75f).sp, lineHeight = (fs * 1.33f).sp),
        titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = fs.sp, lineHeight = (fs * 1.5f).sp),
        labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = (fs * 0.6875f).sp, lineHeight = (fs * 1.45f).sp),
    )

    CompositionLocalProvider(LocalAppTheme provides AppThemeState(accentColor = accentColor)) {
        MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
    }
}

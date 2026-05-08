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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import fumi.day.literalplayer.data.prefs.UserPrefs

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

    val typography = Typography(
        bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
        labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
    )

    CompositionLocalProvider(LocalAppTheme provides AppThemeState(accentColor = accentColor)) {
        MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
    }
}

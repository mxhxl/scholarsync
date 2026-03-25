package com.scholarsync.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = SurfaceLight,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = SurfaceLight,
    secondary = AccentTeal,
    onSecondary = SurfaceLight,
    secondaryContainer = AccentTeal.copy(alpha = 0.1f),
    onSecondaryContainer = AccentTeal,
    tertiary = AccentGold,
    onTertiary = Primary,
    tertiaryContainer = AccentGold.copy(alpha = 0.1f),
    onTertiaryContainer = AccentGold,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = Gray100,
    onSurfaceVariant = TextSecondary,
    outline = Gray300,
    outlineVariant = Gray200
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = SurfaceLight,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = SurfaceLight,
    secondary = AccentTeal,
    onSecondary = SurfaceLight,
    secondaryContainer = AccentTeal.copy(alpha = 0.2f),
    onSecondaryContainer = AccentTeal,
    tertiary = AccentGold,
    onTertiary = Primary,
    tertiaryContainer = AccentGold.copy(alpha = 0.2f),
    onTertiaryContainer = AccentGold,
    background = BackgroundDark,
    onBackground = SurfaceLight,
    surface = SurfaceDark,
    onSurface = SurfaceLight,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray400,
    outline = Gray600,
    outlineVariant = Gray700
)

@Composable
fun ScholarSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

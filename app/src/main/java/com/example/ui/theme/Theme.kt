package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Focus Classes Brand Palette - Consistent high-contrast Blue and White theme
private val FocusColorScheme = lightColorScheme(
    primary = FocusNavy,
    onPrimary = Color.White,
    primaryContainer = FocusPrimaryBlue,
    onPrimaryContainer = Color.White,
    secondary = FocusRoyalBlue,
    onSecondary = Color.White,
    secondaryContainer = FocusSurfaceVariant,
    onSecondaryContainer = FocusTextDark,
    tertiary = FocusGold,
    onTertiary = Color.Black,
    background = FocusBackground,
    onBackground = FocusTextDark,
    surface = FocusCardBackground,
    onSurface = FocusTextDark,
    surfaceVariant = FocusSurfaceVariant,
    onSurfaceVariant = FocusTextSecondary,
    error = FocusErrorRed,
    onError = Color.White,
    outline = FocusDivider
)

@Composable
fun FocusClassesTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = FocusNavy.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = FocusColorScheme,
        typography = Typography,
        content = content
    )
}

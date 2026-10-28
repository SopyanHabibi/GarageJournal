package com.ian.garagejournal.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val StealthGarageDark = darkColorScheme(
    primary = Lime,
    onPrimary = OnLime,
    primaryContainer = Color(0xFF2A3800),
    onPrimaryContainer = Lime,

    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = Color(0xFF222E0A),
    onSecondaryContainer = LimeDim,

    background = Background,
    onBackground = OnSurface,

    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,

    outline = Outline,
    outlineVariant = Color(0xFF2A2E33),

    error = ErrorRed,
    onError = OnError,
    errorContainer = Color(0xFF3D0000),
    onErrorContainer = ErrorRed,

    tertiary = Amber,
    onTertiary = OnAmber,
    tertiaryContainer = Color(0xFF3D2800),
    onTertiaryContainer = Amber,
)

@Composable
fun GarageJournalTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(LocalContext.current)
        }
        else -> StealthGarageDark
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
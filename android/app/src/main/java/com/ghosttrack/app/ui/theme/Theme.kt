package com.ghosttrack.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AppleBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = BlueTint,
    onPrimaryContainer = AppleBlue,
    secondary = AppleOrange,
    onSecondary = SurfaceWhite,
    background = SystemGroupedBackground,
    onBackground = LabelPrimary,
    surface = SurfaceWhite,
    onSurface = LabelPrimary,
    surfaceVariant = SystemGroupedBackground,
    onSurfaceVariant = SystemGray,
    error = AppleRed,
    onError = SurfaceWhite,
    errorContainer = RedTint,
    onErrorContainer = AppleRed,
    outline = SystemGray5,
    outlineVariant = Color(0x0A000000),
)

@Composable
fun GhostTrackTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = GhostTrackTypography,
        content = content
    )
}

package com.proshot.camera.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentOrange,
    onPrimary = PureWhite,
    primaryContainer = DarkGray,
    onPrimaryContainer = OffWhite,

    secondary = AccentYellow,
    onSecondary = PureBlack,
    secondaryContainer = MediumGray,
    onSecondaryContainer = OffWhite,

    tertiary = AccentBlue,
    onTertiary = PureWhite,

    background = PureBlack,
    onBackground = PureWhite,

    surface = DarkGray,
    onSurface = PureWhite,
    surfaceVariant = MediumGray,
    onSurfaceVariant = OffWhite,

    error = AccentRed,
    onError = PureWhite,

    outline = LightGray,
    outlineVariant = MediumGray
)

@Composable
fun ProShotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // ProShot is always dark themed for professional photography
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

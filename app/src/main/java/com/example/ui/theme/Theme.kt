package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryNeon,
    secondary = SecondaryNeon,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color(0xFF031006),
    onSecondary = Color.White,
    onBackground = OnDarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = TextGray,
    error = DangerRed,
    errorContainer = Color(0xFF3F1717),
    onErrorContainer = Color(0xFFFFDAD6),
    tertiary = PremiumGold
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

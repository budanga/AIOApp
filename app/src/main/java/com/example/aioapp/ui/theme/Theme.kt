package com.example.aioapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable


private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    background = LightBackground,
    surface = LightBackground, // Cards and backgrounds are the same color
    onBackground = OnLightBackground, // For main text
    onSurface = OnLightBackground, // For text on cards
    onSurfaceVariant = OnLightSurfaceVariant // For secondary text and icons
)
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    background = DarkBackground,
    surface = DarkBackground,
    onBackground = OnDarkBackground,
    onSurface = OnDarkBackground,
    onSurfaceVariant = OnDarkSurfaceVariant
)

@Composable
fun AIOAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

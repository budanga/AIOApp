package com.example.aioapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Blue,
    background = Black,
    surface = Black,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = Blue,
    onSurface = Blue
)

private val LightColorScheme = lightColorScheme(
    primary = Blue,
    background = White,
    surface = White,
    onPrimary = Black,
    onSecondary = Black,
    onTertiary = Black,
    onBackground = Blue,
    onSurface = Blue
)

@Composable
fun AIOAppTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

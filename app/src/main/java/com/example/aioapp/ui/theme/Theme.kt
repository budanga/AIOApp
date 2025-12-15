package com.example.aioapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = Blue,
    background = Black,
    surface = Black,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = Blue,
    onSurface = Blue
)

@Composable
fun AIOAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}

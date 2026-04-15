package com.example.aioapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color


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

val LocalAppGradient = staticCompositionLocalOf {
    listOf(DarkGradient1, DarkGradient2, DarkGradient3, DarkGradient4, DarkGradient5)
}

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun AIOAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val darkColors = listOf(DarkGradient1, DarkGradient2, DarkGradient3, DarkGradient4, DarkGradient5)
    val lightColors = listOf(LightGradient1, LightGradient2, LightGradient3, LightGradient4, LightGradient5)
    
    val gradientColors = if (darkTheme) darkColors else lightColors

    CompositionLocalProvider(
        LocalAppGradient provides gradientColors,
        LocalDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

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
    listOf(GradientStart, GradientMid1, GradientMid2, GradientMid3, GradientEnd)
}

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun AIOAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val baseColors = listOf(GradientStart, GradientMid1, GradientMid2, GradientMid3, GradientEnd)
    val gradientColors = if (darkTheme) {
        baseColors
    } else {
        baseColors.map { Color(1f - it.red, 1f - it.green, 1f - it.blue, it.alpha) }
    }

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

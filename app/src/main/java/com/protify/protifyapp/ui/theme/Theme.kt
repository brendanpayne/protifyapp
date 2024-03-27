@file:OptIn(ExperimentalFoundationApi::class)

package com.protify.protifyapp.ui.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    primaryContainer = PrimaryVariant,
    secondary = DarkMuted,
    background = DarkBackground,
    surface = DarkSurface,
    error = Error,
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    primaryContainer = PrimaryVariant,
    secondary = LightMuted,
    background = LightBackground,
    surface = LightSurface,
    error = Error,
)

@Composable
fun ProtifyTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography,
            content = content
        )
    }
}
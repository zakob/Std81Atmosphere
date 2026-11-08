package org.std81atmosphere.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFF2B33D),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF1A1400),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF4A3A05),
    background = androidx.compose.ui.graphics.Color(0xFF0F1216),
    surface = androidx.compose.ui.graphics.Color(0xFF171B21),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE6E8EB),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB0B6BF),
    error = androidx.compose.ui.graphics.Color(0xFFFF8A80),
)

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF8A6D1C),
    background = androidx.compose.ui.graphics.Color(0xFFFBFBFA),
)

@Composable
fun Std81Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
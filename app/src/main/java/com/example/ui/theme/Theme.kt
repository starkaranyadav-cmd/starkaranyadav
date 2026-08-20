package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = VibrantLavenderTrack,
    onPrimary = VibrantPurpleDark,
    primaryContainer = VibrantPurple,
    onPrimaryContainer = VibrantLavenderContainer,
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    background = VibrantDarkBackground,
    onBackground = VibrantDarkTextPrimary,
    surface = VibrantDarkSurface,
    onSurface = VibrantDarkTextPrimary,
    surfaceVariant = VibrantDarkSurfaceVariant,
    onSurfaceVariant = VibrantDarkTextSecondary,
    outline = VibrantDarkOutline,
    error = RoseDanger
)

private val LightColorScheme = lightColorScheme(
    primary = VibrantPurple,
    onPrimary = Color.White,
    primaryContainer = VibrantLavenderContainer,
    onPrimaryContainer = VibrantPurpleDark,
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = VibrantPillActive,
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    background = VibrantLightBackground,
    onBackground = VibrantLightTextPrimary,
    surface = VibrantLightSurface,
    onSurface = VibrantLightTextPrimary,
    surfaceVariant = VibrantLightSurfaceVariant,
    onSurfaceVariant = VibrantLightTextSecondary,
    outline = VibrantLightOutline,
    outlineVariant = VibrantLightOutlineVariant,
    error = RoseDanger
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our Vibrant Palette by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

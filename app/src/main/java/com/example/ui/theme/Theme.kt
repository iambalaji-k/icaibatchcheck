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
    primary = PolishPrimaryContainer,
    onPrimary = PolishOnPrimaryContainer,
    primaryContainer = PolishPrimary,
    onPrimaryContainer = Color.White,
    secondary = EmeraldOpenSeats,
    tertiary = AmberAlert,
    background = PolishDarkBackground,
    surface = PolishDarkSurface,
    surfaceContainer = PolishDarkSurfaceContainer,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F96)
)

private val LightColorScheme = lightColorScheme(
    primary = PolishPrimary,
    onPrimary = PolishOnPrimary,
    primaryContainer = PolishPrimaryContainer,
    onPrimaryContainer = PolishOnPrimaryContainer,
    secondary = EmeraldOpenSeats,
    tertiary = AmberAlert,
    background = PolishBackground,
    surface = PolishSurface,
    surfaceContainer = PolishSurfaceContainer,
    onBackground = PolishOnSurface,
    onSurface = PolishOnSurface,
    onSurfaceVariant = PolishOnSurfaceVariant,
    outline = PolishOutline
)

@Composable
fun IcaiBatchCheckerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
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


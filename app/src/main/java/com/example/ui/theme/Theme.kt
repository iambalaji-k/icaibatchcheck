package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

enum class AppThemeMode(val title: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED Black")
}

private val LightColorScheme = lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = TealOnPrimaryLight,
    primaryContainer = TealPrimaryContainerLight,
    onPrimaryContainer = TealOnPrimaryContainerLight,
    secondary = EmeraldOpenSeats,
    tertiary = AmberAlert,
    background = PolishBackgroundLight,
    surface = PolishSurfaceLight,
    surfaceContainer = PolishSurfaceContainerLight,
    surfaceContainerHigh = PolishSurfaceContainerHighLight,
    onBackground = PolishOnSurfaceLight,
    onSurface = PolishOnSurfaceLight,
    onSurfaceVariant = PolishOnSurfaceVariantLight,
    outline = PolishOutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealOnPrimaryDark,
    primaryContainer = TealPrimaryContainerDark,
    onPrimaryContainer = TealOnPrimaryContainerDark,
    secondary = EmeraldOpenSeatsBright,
    tertiary = AmberAlertBright,
    background = PolishBackgroundDark,
    surface = PolishSurfaceDark,
    surfaceContainer = PolishSurfaceContainerDark,
    surfaceContainerHigh = PolishSurfaceContainerHighDark,
    onBackground = PolishOnSurfaceDark,
    onSurface = PolishOnSurfaceDark,
    onSurfaceVariant = PolishOnSurfaceVariantDark,
    outline = PolishOutlineDark
)

private val AmoledColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF042F2E),
    onPrimaryContainer = Color(0xFF5EEAD4),
    secondary = EmeraldOpenSeatsBright,
    tertiary = AmberAlertBright,
    background = PolishAmoledBackground,
    surface = PolishAmoledSurface,
    surfaceContainer = PolishAmoledSurfaceContainer,
    surfaceContainerHigh = PolishAmoledSurfaceContainerHigh,
    onBackground = PolishAmoledOnSurface,
    onSurface = PolishAmoledOnSurface,
    onSurfaceVariant = PolishAmoledOnSurfaceVariant,
    outline = PolishAmoledOutline
)

@Composable
fun IcaiBatchCheckerTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val colorScheme: ColorScheme = when (themeMode) {
        AppThemeMode.SYSTEM -> if (systemInDark) DarkColorScheme else LightColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.AMOLED -> AmoledColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Status colors that adapt to the active scheme (light palettes clash on dark/AMOLED).

@Composable
private fun schemeIsDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun StatusOpen(): Color =
    if (schemeIsDark()) EmeraldOpenSeatsBright else EmeraldOpenSeats

@Composable
fun StatusOpenContainer(): Color =
    if (schemeIsDark()) Color(0xFF064E3B) else EmeraldContainer

@Composable
fun StatusFull(): Color =
    if (schemeIsDark()) Color(0xFFFB7185) else RedFullSeats

@Composable
fun StatusFullContainer(): Color =
    if (schemeIsDark()) Color(0xFF4C051A) else RedBg

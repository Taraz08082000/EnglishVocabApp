package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryIndigo,
    onPrimary = OnPrimaryWhite,
    secondary = SecondaryTeal,
    onSecondary = OnPrimaryWhite,
    tertiary = AccentAmber,
    background = LightBackground,
    onBackground = TextDark,
    surface = LightSurface,
    onSurface = TextDark,
    surfaceVariant = LightBackground,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryIndigoDark,
    onPrimary = TextDark,
    secondary = SecondaryTealDark,
    onSecondary = TextDark,
    tertiary = AccentAmber,
    background = DarkBackground,
    onBackground = TextLight,
    surface = DarkSurface,
    onSurface = TextLight,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = TextSecondaryDark,
    outline = CardBorderDark,
    error = ErrorRed
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

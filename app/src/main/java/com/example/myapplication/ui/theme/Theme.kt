package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
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

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}

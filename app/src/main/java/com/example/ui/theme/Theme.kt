package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StudyDarkColorScheme = darkColorScheme(
    primary = PrimaryTeal,
    onPrimary = DarkSlateBg,
    primaryContainer = SurfaceDark,
    onPrimaryContainer = OnSurfaceText,
    secondary = SecondaryBlue,
    onSecondary = OnSurfaceText,
    tertiary = TertiaryViolet,
    background = DarkSlateBg,
    onBackground = OnSurfaceText,
    surface = SurfaceDark,
    onSurface = OnSurfaceText,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = OnSurfaceMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // We default entirely to academic dark mode as requested
    dynamicColor: Boolean = false, // Use our gorgeous custom palette for cohesive study branding
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudyDarkColorScheme,
        typography = Typography,
        content = content
    )
}

package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CalSnapDarkColorScheme = darkColorScheme(
    primary = CalorieCoral,
    onPrimary = Color(0xFF140704),
    primaryContainer = CalorieCoralSoft,
    onPrimaryContainer = Color(0xFFFFDAD2),
    secondary = ProteinBlue,
    onSecondary = Color(0xFF041428),
    secondaryContainer = ProteinBlueSoft,
    onSecondaryContainer = Color(0xFFD6E6FF),
    tertiary = FatEmerald,
    onTertiary = Color(0xFF031F14),
    tertiaryContainer = FatEmeraldSoft,
    onTertiaryContainer = Color(0xFFCEFCE9),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = GraphiteElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = GraphiteElevated,
    surfaceContainerHigh = SurfaceMuted,
    outline = SubtleStroke,
    outlineVariant = Color(0xFF1F2636),
    error = ErrorRose,
    onError = Color(0xFF28060E),
    errorContainer = ErrorRoseSoft,
    onErrorContainer = Color(0xFFFFD9DF)
)

private val CalSnapShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CalSnapTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CalSnapDarkColorScheme,
        typography = CalSnapTypography,
        shapes = CalSnapShapes,
        content = content
    )
}

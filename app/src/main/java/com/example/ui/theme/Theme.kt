package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ClipForgeDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = ElectricVioletDeep,
    onPrimaryContainer = Color.White,
    secondary = ElectricBlue,
    onSecondary = ObsidianCanvas,
    secondaryContainer = Color(0xFF102A42),
    onSecondaryContainer = ElectricBlue,
    tertiary = EmeraldSuccess,
    onTertiary = ObsidianCanvas,
    background = ObsidianCanvas,
    onBackground = TextPrimary,
    surface = CharcoalPanel,
    onSurface = TextPrimary,
    surfaceVariant = SlateCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianCanvas,
    surfaceContainerLow = CharcoalPanel,
    surfaceContainer = SlateCard,
    surfaceContainerHigh = SlateElevated,
    outline = SubtleBorder,
    outlineVariant = FocusBorder,
    error = RoseError,
    onError = Color.White
)

private val ClipForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ClipForgeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClipForgeDarkColorScheme,
        typography = ClipForgeTypography,
        shapes = ClipForgeShapes,
        content = content
    )
}

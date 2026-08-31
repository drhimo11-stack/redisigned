package com.shield.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark is the primary, designed-for experience (a "control room" you check
// in on), matching the emerald/gold brand identity. Light is a faithful,
// legible inversion of the same palette rather than a separate look.
private val DarkColorScheme = darkColorScheme(
    primary = ShieldEmerald,
    onPrimary = ShieldNightBg,
    primaryContainer = ShieldEmeraldDeep,
    onPrimaryContainer = ShieldIvoryText,
    secondary = ShieldGold,
    onSecondary = ShieldNightBg,
    tertiary = ShieldGold,
    onTertiary = ShieldNightBg,
    error = ShieldMaroon,
    onError = ShieldNightBg,
    errorContainer = ShieldMaroonDeep,
    onErrorContainer = ShieldIvoryText,
    background = ShieldNightBg,
    onBackground = ShieldIvoryText,
    surface = ShieldNightSurface,
    onSurface = ShieldIvoryText,
    surfaceVariant = ShieldNightSurfaceElevated,
    onSurfaceVariant = ShieldMutedText,
    outline = ShieldNightOutline
)

private val LightColorScheme = lightColorScheme(
    primary = ShieldEmeraldDeep,
    onPrimary = ShieldParchmentSurface,
    primaryContainer = Color(0xFFBFE6D3),
    onPrimaryContainer = ShieldInkText,
    secondary = ShieldGoldDeep,
    onSecondary = ShieldParchmentSurface,
    tertiary = ShieldGoldDeep,
    onTertiary = ShieldParchmentSurface,
    error = ShieldMaroonDeep,
    onError = ShieldParchmentSurface,
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = ShieldMaroonDeep,
    background = ShieldParchmentBg,
    onBackground = ShieldInkText,
    surface = ShieldParchmentSurface,
    onSurface = ShieldInkText,
    surfaceVariant = ShieldParchmentSurfaceVariant,
    onSurfaceVariant = ShieldMutedInk,
    outline = ShieldMutedInk
)

@Composable
fun ShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color (Material You) is intentionally not used — it would
    // replace this deliberate emerald/gold brand identity with whatever
    // the user's wallpaper happens to produce.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ShieldShapes,
        content = content
    )
}

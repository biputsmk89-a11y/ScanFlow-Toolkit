package com.scanflow.app.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1D4ED8),             // Royal Sapphire Blue (Vibrant & Commercial)
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),    // Soft Blue Container
    onPrimaryContainer = Color(0xFF1E3A8A),  // Deep Blue on Container
    secondary = Color(0xFF475569),          // Slate 600
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1F5F9),  // Slate 100
    onSecondaryContainer = Color(0xFF334155),
    tertiary = Color(0xFF0D9488),           // Teal Accent
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF115E59),
    background = Color(0xFFF8FAFC),         // Modern Slate 50 Canvas
    onBackground = Color(0xFF0F172A),       // Slate 900 Typography
    surface = Color(0xFFFFFFFF),            // Clean White Surface
    onSurface = Color(0xFF0F172A),          // High-contrast Dark Typography
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),   // Slate 500 Subtitle
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFCBD5E1),
    surfaceTint = Color(0xFF1D4ED8),
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color(0xFFF8FAFC),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),             // Electric Sapphire (High Luminance, Crisp Contrast)
    onPrimary = Color(0xFF0A0F1D),
    primaryContainer = Color(0xFF1E3A8A),    // Deep Navy Container
    onPrimaryContainer = Color(0xFFBFDBFE),  // Light Blue on Navy
    secondary = Color(0xFF94A3B8),          // Slate 400
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E293B),  // Slate 800
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF2DD4BF),           // Electric Mint
    onTertiary = Color(0xFF042F2E),
    tertiaryContainer = Color(0xFF134E4A),
    onTertiaryContainer = Color(0xFF99F6E4),
    background = Color(0xFF0A0F1D),         // Deep Obsidian Navy (Ultra-Modern, Battery-Efficient)
    onBackground = Color(0xFFF8FAFC),       // Crisp High-Contrast Off-White
    surface = Color(0xFF111827),            // Elevated Card & Bar Surface
    onSurface = Color(0xFFF8FAFC),          // Sharp, Readable Off-White Text
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFF94A3B8),   // Clear Muted Text (No Blur/Washed Out)
    surfaceContainerLowest = Color(0xFF080D1A),
    surfaceContainerLow = Color(0xFF131B2E), // Primary Card Background in Dark Mode
    surfaceContainer = Color(0xFF1A243B),   // Interactive Elevated Items
    surfaceContainerHigh = Color(0xFF222F4C),// Search Bar & Input Pills
    surfaceContainerHighest = Color(0xFF2C3C60),
    surfaceTint = Color(0xFF3B82F6),
    inverseSurface = Color(0xFFF1F5F9),
    inverseOnSurface = Color(0xFF0F172A),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    outline = Color(0xFF374151),            // Subtle Sleek Outline for Cards
    outlineVariant = Color(0xFF1F293D)
)

@Composable
fun ScanFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

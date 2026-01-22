package com.hooked.theme

import androidx.compose.ui.graphics.Color

object Colors {
    // Primary Brand Colors (based on #050505, #1B9AAA muted, #DDDBCB, #F5F1E3, #FFFFFF)
    val mutedTeal = Color(0xFF157A87)        // Primary - muted version of #1B9AAA
    val warmBeige = Color(0xFFDDDBCB)        // Secondary accent
    val darkerBeige = Color(0xFFB8B6A8)      // Tertiary accent
    val lightCream = Color(0xFFF5F1E3)       // Light neutral
    val white = Color(0xFFFFFFFF)            // Pure white
    val almostBlack = Color(0xFF050505)      // Almost black background

    // Legacy names mapped to new colors (for compatibility)
    val deepOcean = mutedTeal
    val coral = warmBeige
    val seaFoam = darkerBeige
    val aqua = mutedTeal
    val navy = Color(0xFF1A1A1A)
    val sand = lightCream
    val sunsetOrange = Color(0xFFF59E0B)
    val kelp = Color(0xFF4CAF50)
    val pearl = white
    val anchorGray = Color(0xFF3A3A36)

    // Semantic Colors
    val red = Color(0xFFEF4444)              // Error/danger
    val yellow = Color(0xFFF59E0B)           // Warning
    val green = Color(0xFF4CAF50)            // Success
    val teal = mutedTeal                     // Info
    val blue = mutedTeal                     // Info

    // Chart Colors Palette
    val chartColors = listOf(
        mutedTeal,                            // #157A87
        warmBeige,                            // #DDDBCB
        Color(0xFFF59E0B),                   // Amber
        green,                                // #4CAF50
        red,                                  // #EF4444
        Color(0xFF8A8780),                   // Gray beige
        darkerBeige,                          // #B8B6A8
        Color(0xFF178B99)                    // Lighter teal variant
    )

    // Text Colors (high contrast on dark backgrounds)
    val text = lightCream                     // #F5F1E3 - Primary text
    val subtext1 = Color(0xFFE0DDD0)         // Secondary text
    val subtext0 = Color(0xFFC8C5BA)         // Tertiary text
    val disabledText = Color(0xFF8A8780)     // Disabled text

    // Surface & Overlay Colors (gradations for depth)
    val overlay2 = Color(0xFF4A4A44)         // Strong overlay
    val overlay1 = Color(0xFF3A3A36)         // Medium overlay
    val overlay0 = Color(0xFF2A2A28)         // Subtle overlay
    val surface2 = Color(0xFF1A1A1A)         // Most elevated surface
    val surface1 = Color(0xFF141414)         // Elevated surface
    val surface0 = Color(0xFF0F0F0F)         // Base surface
    val base = almostBlack                    // #050505 - Background base
    val mantle = Color(0xFF080808)           // Background layer
    val crust = Color(0xFF0C0C0C)            // Background darkest layer

    // Convenience aliases for semantic usage
    val error = red
    val success = green
    val warning = yellow
    val info = teal
}
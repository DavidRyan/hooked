package com.hooked.theme

import androidx.compose.ui.graphics.Color

object Colors {
    // Primary Brand Colors (based on coolors.co/bee9e8-62b6cb-1b4965-cae9ff-5fa8d3)
    val mediumTeal = Color(0xFF62B6CB)       // Primary
    val lightMint = Color(0xFFBEE9E8)        // Secondary accent
    val mediumBlue = Color(0xFF5FA8D3)       // Tertiary accent
    val lightBlue = Color(0xFFCAE9FF)        // Light neutral
    val white = Color(0xFFFFFFFF)            // Pure white
    val darkNavy = Color(0xFF050505)         // Black background

    // Legacy names mapped to new colors (for compatibility)
    val mutedTeal = mediumTeal
    val warmBeige = lightMint
    val darkerBeige = lightMint
    val lightCream = lightBlue
    val almostBlack = darkNavy
    val deepOcean = mediumTeal
    val coral = lightMint
    val seaFoam = lightMint
    val aqua = mediumTeal
    val navy = Color(0xFF1A1A1A)
    val sand = lightBlue
    val sunsetOrange = Color(0xFFF59E0B)
    val kelp = Color(0xFF4CAF50)
    val pearl = white
    val anchorGray = Color(0xFF3A3A3A)

    // Semantic Colors
    val red = Color(0xFFEF4444)              // Error/danger
    val yellow = Color(0xFFF59E0B)           // Warning
    val green = Color(0xFF4CAF50)            // Success
    val teal = mediumTeal                    // Info
    val blue = mediumBlue                    // Info

    // Chart Colors Palette
    val chartColors = listOf(
        mediumTeal,                           // #62B6CB
        lightMint,                            // #BEE9E8
        Color(0xFFF59E0B),                   // Amber
        green,                                // #4CAF50
        red,                                  // #EF4444
        Color(0xFF6A8FA6),                   // Muted blue-gray
        mediumBlue,                           // #5FA8D3
        Color(0xFF7AC4D8)                    // Lighter teal variant
    )

    // Text Colors (high contrast on dark backgrounds)
    val text = lightBlue                      // #CAE9FF - Primary text
    val subtext1 = Color(0xFFB0D4EC)         // Secondary text
    val subtext0 = Color(0xFF96BFD9)         // Tertiary text
    val disabledText = Color(0xFF6A8FA6)     // Disabled text

    // Surface & Overlay Colors (gradations for depth)
    val overlay2 = Color(0xFF4A4A4A)         // Strong overlay
    val overlay1 = Color(0xFF3A3A3A)         // Medium overlay
    val overlay0 = Color(0xFF2A2A2A)         // Subtle overlay
    val surface2 = Color(0xFF1A1A1A)         // Most elevated surface
    val surface1 = Color(0xFF141414)         // Elevated surface
    val surface0 = Color(0xFF0F0F0F)         // Base surface
    val base = darkNavy                       // #050505 - Background base
    val mantle = Color(0xFF080808)           // Background layer
    val crust = Color(0xFF0C0C0C)            // Background darkest layer

    // Convenience aliases for semantic usage
    val error = red
    val success = green
    val warning = yellow
    val info = teal
}
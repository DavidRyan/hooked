package com.hooked.theme

import androidx.compose.ui.graphics.Color

object Colors {
    // Brand
    val primary = Color(0xFFF5A85C)          // Sunrise amber — primary CTA, FAB
    val onPrimary = Color(0xFF1A0F00)        // Warm dark for text on amber
    val secondary = Color(0xFF7BA188)        // Sage — skunks, muted states
    val onSecondary = Color(0xFF0F1B14)
    val tertiary = Color(0xFF5FA8D3)         // Medium blue — links, info, accents
    val onTertiary = Color(0xFF071421)

    // Surfaces (deep-water layered)
    val base = Color(0xFF0A1628)             // Background
    val surface0 = Color(0xFF0F1B2E)         // Below cards
    val surface1 = Color(0xFF152238)         // Card base
    val surface2 = Color(0xFF1E3050)         // Card elevated / sheet
    val overlay0 = Color(0x14FFFFFF)         // Hairline / subtle stroke (~8%)
    val overlay1 = Color(0x29FFFFFF)         // Stronger divider (~16%)

    // Text
    val text = Color(0xFFFFFFFF)             // Primary text
    val subtext1 = Color(0xFFB8C4D6)         // Secondary text
    val subtext0 = Color(0xFF7A8AA0)         // Tertiary / hint
    val disabledText = Color(0xFF50607A)

    // Semantic
    val error = Color(0xFFF47171)
    val success = Color(0xFF7BC47F)
    val warning = Color(0xFFE8B654)
    val info = tertiary

    // Convenience
    val white = Color(0xFFFFFFFF)
    val red = error
    val green = success
    val yellow = warning

    // Chart palette — coherent with brand, distinct enough at a glance
    val chartColors = listOf(
        primary,                              // amber
        tertiary,                             // blue
        secondary,                            // sage
        Color(0xFFD98AB6),                    // dusty rose
        Color(0xFFE8B654),                    // warm gold
        Color(0xFF8FB8E0),                    // soft sky
        Color(0xFFC8B8A0),                    // sand
        Color(0xFF9D7FBF)                     // muted violet
    )
}

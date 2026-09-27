package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Liquid Glass Palette - Deep, Minimal & Cinematic
val DarkBg = Color(0xFF06080C)
val DarkBgSecondary = Color(0xFF0B0F17)
val DarkSurface = Color(0xFF10141E)

// Liquid Glass Surface Tints
val GlassSurfaceUltraLight = Color(0x18FFFFFF) // 9% White
val GlassSurfaceLight = Color(0x24FFFFFF)      // 14% White
val GlassSurfaceMedium = Color(0x38FFFFFF)     // 22% White
val GlassSurfaceElevated = Color(0x4DFFFFFF)   // 30% White

val GlassIcyWhite = Color(0xFFF1F5F9)
val GlassIcyBlue = Color(0xFFE0F2FE)
val GlassIcyTint = Color(0x1A38BDF8) // subtle cyan/blue refraction

// Glass Borders & Specular Highlights
val GlassBorderTop = Color(0x73FFFFFF)        // 45% White top shine
val GlassBorderMiddle = Color(0x26FFFFFF)     // 15% White
val GlassBorderBottom = Color(0x0DFFFFFF)     // 5% White shadow
val GlassBorderActive = Color(0xA6FFFFFF)     // 65% White active

// Refined Accents (Subtle & Luxurious)
val AmberStudio = Color(0xFFF59E0B)
val AmberStudioLight = Color(0xFFFCD34D)
val AmberStudioGlow = Color(0x33F59E0B)
val CyanOptical = Color(0xFF38BDF8)
val EmeraldSuccess = Color(0xFF10B981)

// Typography
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFFCBD5E1)
val TextMuted = Color(0xFF64748B)

// Gradients for Liquid Glass surfaces
val LiquidGlassGradient = Brush.linearGradient(
    colors = listOf(
        Color(0x33FFFFFF), // top-left glow
        Color(0x14FFFFFF), // subtle mid
        Color(0x0DFFFFFF)  // dark corner
    )
)

val LiquidGlassActiveGradient = Brush.linearGradient(
    colors = listOf(
        Color(0x4DFFFFFF),
        Color(0x24FFFFFF),
        Color(0x1AFFFFFF)
    )
)

val LiquidGlassShimmerGradient = Brush.linearGradient(
    colors = listOf(
        Color.Transparent,
        Color(0x33FFFFFF),
        Color(0x80FFFFFF),
        Color(0x33FFFFFF),
        Color.Transparent
    )
)

val LiquidGlassBorderBrush = Brush.linearGradient(
    colors = listOf(
        Color(0x80FFFFFF),
        Color(0x26FFFFFF),
        Color(0x0DFFFFFF)
    )
)

package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

enum class LiquidEnvironment(val title: String) {
    OBSIDIAN("Obsidian"),
    MIDNIGHT("Midnight"),
    DEEP_OCEAN("Deep Ocean"),
    LIQUID_SILVER("Liquid Silver"),
    AURORA("Aurora"),
    MINIMAL("Minimal")
}

enum class PerformanceMode {
    STANDARD,  // Full animated liquid layers
    ECONOMY,   // Reduced shader layers
    DISABLED   // Static dark background
}

/**
 * Multi-layer animated Liquid Background simulating organic fluid depth.
 */
@Composable
fun LiquidBackground(
    environment: LiquidEnvironment = LiquidEnvironment.OBSIDIAN,
    performanceMode: PerformanceMode = PerformanceMode.STANDARD,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isAnimated = performanceMode != PerformanceMode.DISABLED
    val isFullQuality = performanceMode == PerformanceMode.STANDARD

    val infiniteTransition = rememberInfiniteTransition(label = "liquidMotion")

    val phase1 by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 14000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase1"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val phase2 by if (isAnimated && isFullQuality) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 19000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase2"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    // Color palettes for environments
    val (baseColor, blob1Color, blob2Color, blob3Color) = when (environment) {
        LiquidEnvironment.OBSIDIAN -> Quad(
            Color(0xFF05070B),
            Color(0x181E293B),
            Color(0x1238BDF8),
            Color(0x0C0F172A)
        )
        LiquidEnvironment.MIDNIGHT -> Quad(
            Color(0xFF060612),
            Color(0x221E1B4B),
            Color(0x18312E81),
            Color(0x104C1D95)
        )
        LiquidEnvironment.DEEP_OCEAN -> Quad(
            Color(0xFF040A10),
            Color(0x200E3A52),
            Color(0x1A0891B2),
            Color(0x0D164E63)
        )
        LiquidEnvironment.LIQUID_SILVER -> Quad(
            Color(0xFF090A0C),
            Color(0x24334155),
            Color(0x1A64748B),
            Color(0x1494A3B8)
        )
        LiquidEnvironment.AURORA -> Quad(
            Color(0xFF040B09),
            Color(0x1F064E3B),
            Color(0x16059669),
            Color(0x120D9488)
        )
        LiquidEnvironment.MINIMAL -> Quad(
            Color(0xFF040507),
            Color(0x0CFFFFFF),
            Color(0x08FFFFFF),
            Color(0x05FFFFFF)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        if (performanceMode != PerformanceMode.DISABLED) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Liquid Blob 1
                val x1 = w * 0.4f + cos(phase1) * w * 0.22f
                val y1 = h * 0.28f + sin(phase1 * 0.8f) * h * 0.14f
                val r1 = w * 0.75f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blob1Color, Color.Transparent),
                        center = Offset(x1, y1),
                        radius = r1
                    ),
                    center = Offset(x1, y1),
                    radius = r1
                )

                // Liquid Blob 2
                val x2 = w * 0.65f + sin(phase2) * w * 0.25f
                val y2 = h * 0.72f + cos(phase2 * 0.9f) * h * 0.16f
                val r2 = w * 0.85f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blob2Color, Color.Transparent),
                        center = Offset(x2, y2),
                        radius = r2
                    ),
                    center = Offset(x2, y2),
                    radius = r2
                )

                if (isFullQuality) {
                    // Liquid Blob 3
                    val x3 = w * 0.2f + cos(phase1 * 1.2f) * w * 0.18f
                    val y3 = h * 0.85f + sin(phase2 * 0.7f) * h * 0.12f
                    val r3 = w * 0.65f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(blob3Color, Color.Transparent),
                            center = Offset(x3, y3),
                            radius = r3
                        ),
                        center = Offset(x3, y3),
                        radius = r3
                    )
                }
            }
        }

        content()
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

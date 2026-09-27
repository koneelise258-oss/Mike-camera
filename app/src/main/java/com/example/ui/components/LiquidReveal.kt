package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CyanOptical
import com.example.ui.theme.DarkBg

/**
 * Liquid Reveal transition overlay that ripples and dissolves to reveal underlying content.
 */
@Composable
fun LiquidReveal(
    trigger: Boolean,
    onRevealComplete: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val revealProgress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            revealProgress.snapTo(0f)
            revealProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
            onRevealComplete()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (revealProgress.value < 0.99f && trigger) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val p = revealProgress.value
                val w = size.width
                val h = size.height

                // Liquid wave clearing from center outward
                val maxRadius = kotlin.math.hypot(w, h)
                val currentRadius = p * maxRadius

                // Frosted veil fading out
                val alpha = (1f - p).coerceIn(0f, 1f)

                drawRect(
                    color = DarkBg.copy(alpha = alpha * 0.7f)
                )

                // Luminous refractive edge wave
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = alpha * 0.4f),
                            CyanOptical.copy(alpha = alpha * 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(w / 2f, h / 2f),
                        radius = (currentRadius + 80f).coerceAtLeast(10f)
                    ),
                    center = Offset(w / 2f, h / 2f),
                    radius = (currentRadius + 80f).coerceAtLeast(10f)
                )
            }
        }
    }
}

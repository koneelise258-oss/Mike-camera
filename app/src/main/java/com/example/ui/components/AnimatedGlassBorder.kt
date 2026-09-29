package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.CyanOptical

/**
 * Calm, refined liquid glass border with subtle organic depth breathing.
 *
 * Replaces the rotating LED beam with a tranquil, non-distracting glass edge:
 * - Subtle vertical light bevel reflecting environmental light.
 * - Extremely gentle, slow organic breathing when active (no spinning spotlights).
 */
fun Modifier.animatedGlassBorder(
    shape: Shape,
    borderWidth: Dp = 1.dp,
    isActive: Boolean = false,
    durationMillis: Int = 6000
): Modifier = composed {
    val breathingAlpha = if (isActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "glassBorderBreathing")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.75f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "borderBreathing"
        )
        alpha
    } else {
        0.35f
    }

    val borderBrush = if (isActive) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = breathingAlpha),
                AmberStudio.copy(alpha = breathingAlpha * 0.7f),
                CyanOptical.copy(alpha = breathingAlpha * 0.4f),
                Color.White.copy(alpha = 0.12f)
            ),
            start = Offset(0f, 0f),
            end = Offset(200f, 500f)
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.40f),
            0.5f to Color.White.copy(alpha = 0.10f),
            1.0f to Color.White.copy(alpha = 0.18f)
        )
    }

    this.border(borderWidth, borderBrush, shape)
}

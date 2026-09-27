package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.GlassBorderBottom
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassIcyTint
import com.example.ui.theme.GlassSurfaceLight

/**
 * Premium Liquid Glass container with specular highlights, depth gradient,
 * and subtle optical refraction.
 *
 * Adheres strictly to "LIQUID, NOT SHINY":
 * - No continuous sweeping light streaks or infinite shine loops.
 * - Single-shot intro sheen on initial launch if [playIntroSheen] is true.
 * - Clean, calm, translucent liquid depth during normal usage.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    backgroundColor: Color = GlassSurfaceLight,
    borderTopColor: Color = GlassBorderTop,
    borderBottomColor: Color = GlassBorderBottom,
    borderWidth: Dp = 1.dp,
    hasRefractionGlow: Boolean = true,
    hasIdleShine: Boolean = false, // Preserved for API compatibility; mapped to single intro sheen
    playIntroSheen: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shouldPlaySheen = playIntroSheen || hasIdleShine

    // Single-shot intro reflection sweep (plays ONCE on mount, then permanently stops)
    val introSheenProgress = remember { Animatable(if (shouldPlaySheen) -0.5f else 2f) }
    LaunchedEffect(shouldPlaySheen) {
        if (shouldPlaySheen) {
            introSheenProgress.animateTo(
                targetValue = 1.8f,
                animationSpec = tween(durationMillis = 1400)
            )
        }
    }

    // Calm, elegant border gradient that mimics light hitting the top edge of translucent glass
    val borderBrush = remember(borderTopColor, borderBottomColor) {
        Brush.verticalGradient(
            0.0f to borderTopColor.copy(alpha = 0.65f),
            0.5f to GlassBorderActive.copy(alpha = 0.15f),
            1.0f to borderBottomColor.copy(alpha = 0.25f)
        )
    }

    // Subtle liquid refraction depth gradient
    val glassBackground = remember(backgroundColor) {
        Brush.linearGradient(
            colors = listOf(
                backgroundColor.copy(alpha = (backgroundColor.alpha * 1.25f).coerceAtMost(0.85f)),
                backgroundColor,
                backgroundColor.copy(alpha = (backgroundColor.alpha * 0.55f))
            ),
            start = Offset(0f, 0f),
            end = Offset(400f, 800f)
        )
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(glassBackground)
            .border(borderWidth, borderBrush, shape)
            .then(
                if (hasRefractionGlow) {
                    Modifier.background(
                        Brush.radialGradient(
                            colors = listOf(
                                GlassIcyTint.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(80f, 80f),
                            radius = 320f
                        )
                    )
                } else Modifier
            )
            .then(
                if (introSheenProgress.value in -0.4f..1.6f) {
                    Modifier.drawWithContent {
                        drawContent()
                        val width = size.width
                        val height = size.height
                        val startX = introSheenProgress.value * width
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.05f),
                                    Color.White.copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                start = Offset(startX - 90f, 0f),
                                end = Offset(startX + 90f, height)
                            )
                        )
                    }
                } else Modifier
            )
            .then(clickModifier),
        content = content
    )
}

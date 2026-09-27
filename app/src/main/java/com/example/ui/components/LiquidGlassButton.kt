package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.GlassBorderBottom
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.GlassSurfaceMedium
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LiquidGlassButtonVariant {
    PRIMARY,    // Highlighted glass with subtle white core
    SECONDARY,  // Frosted translucent glass
    SUBTLE,     // Very light glass
    ACCENT      // Amber studio accent glass
}

/**
 * Liquid Glass button featuring organic liquid press physics,
 * calm translucent depth, and tactile response strictly on user action.
 *
 * Adheres strictly to "LIQUID, NOT SHINY":
 * - No continuous sweeping light streaks or infinite sheen loops.
 * - Reactive specular light flare ONLY on tap interaction.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: LiquidGlassButtonVariant = LiquidGlassButtonVariant.PRIMARY,
    text: String? = null,
    icon: ImageVector? = null,
    shape: Shape = CircleShape,
    isHero: Boolean = false,
    enabled: Boolean = true,
    height: Dp = if (isHero) 64.dp else 52.dp,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }
    var flashShimmer by remember { mutableStateOf(false) }

    val currentScale = pressScale.value

    val (bgGradient, borderBrush, textColor) = when (variant) {
        LiquidGlassButtonVariant.PRIMARY -> {
            Triple(
                Brush.linearGradient(
                    listOf(
                        Color(0x40FFFFFF),
                        Color(0x22FFFFFF),
                        Color(0x14FFFFFF)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(300f, 150f)
                ),
                Brush.verticalGradient(
                    listOf(
                        Color(0x80FFFFFF),
                        Color(0x26FFFFFF),
                        Color(0x10FFFFFF)
                    )
                ),
                TextPrimary
            )
        }
        LiquidGlassButtonVariant.ACCENT -> {
            Triple(
                Brush.linearGradient(
                    listOf(
                        AmberStudio.copy(alpha = 0.35f),
                        Color(0x20F59E0B),
                        Color(0x10FFFFFF)
                    )
                ),
                Brush.verticalGradient(
                    listOf(
                        Color(0xB3FDE68A),
                        AmberStudio.copy(alpha = 0.45f),
                        Color(0x10FFFFFF)
                    )
                ),
                Color(0xFFFEF3C7)
            )
        }
        LiquidGlassButtonVariant.SECONDARY -> {
            Triple(
                Brush.linearGradient(
                    listOf(
                        GlassSurfaceMedium.copy(alpha = 0.7f),
                        GlassSurfaceLight.copy(alpha = 0.5f),
                        Color(0x0AFFFFFF)
                    )
                ),
                Brush.verticalGradient(
                    listOf(
                        GlassBorderTop.copy(alpha = 0.6f),
                        GlassBorderActive.copy(alpha = 0.2f),
                        GlassBorderBottom.copy(alpha = 0.25f)
                    )
                ),
                TextPrimary
            )
        }
        LiquidGlassButtonVariant.SUBTLE -> {
            Triple(
                Brush.linearGradient(
                    listOf(
                        Color(0x14FFFFFF),
                        Color(0x08FFFFFF)
                    )
                ),
                Brush.verticalGradient(
                    listOf(
                        Color(0x28FFFFFF),
                        Color(0x0AFFFFFF)
                    )
                ),
                TextPrimary.copy(alpha = 0.85f)
            )
        }
    }

    Box(
        modifier = modifier
            .scale(currentScale)
            .clip(shape)
            .background(bgGradient)
            .border(if (isHero) 1.2.dp else 1.dp, borderBrush, shape)
            .defaultMinSize(minHeight = height)
            .drawWithContent {
                drawContent()
                // Action-driven tactile sheen (ONLY during press/tap, never looping)
                if (flashShimmer) {
                    val w = size.width
                    val h = size.height
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            start = Offset(w * 0.2f, 0f),
                            end = Offset(w * 0.8f, h)
                        )
                    )
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        coroutineScope.launch {
                            pressScale.animateTo(0.95f, animationSpec = tween(70, easing = FastOutSlowInEasing))
                        }
                        tryAwaitRelease()
                        coroutineScope.launch {
                            flashShimmer = true
                            pressScale.animateTo(
                                1.0f,
                                animationSpec = spring(
                                    dampingRatio = 0.65f,
                                    stiffness = 500f
                                )
                            )
                            delay(160)
                            flashShimmer = false
                        }
                    },
                    onTap = {
                        if (enabled) {
                            onClick()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (isHero) 28.dp else 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (content != null) {
                content()
            } else {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(if (isHero) 24.dp else 20.dp)
                    )
                }
                if (icon != null && !text.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(10.dp))
                }
                if (!text.isNullOrEmpty()) {
                    Text(
                        text = text,
                        color = textColor,
                        fontSize = if (isHero) 17.sp else 15.sp,
                        fontWeight = if (isHero) FontWeight.SemiBold else FontWeight.Medium,
                        letterSpacing = if (isHero) 0.6.sp else 0.3.sp
                    )
                }
            }
        }
    }
}

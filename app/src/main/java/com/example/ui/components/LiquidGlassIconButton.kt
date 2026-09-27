package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorderBottom
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.GlassSurfaceMedium
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.launch

/**
 * Circular Liquid Glass icon button with physical glass reflection and spring feedback.
 */
@Composable
fun LiquidGlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = TextPrimary,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    isActive: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }

    val bgGradient = if (isActive) {
        Brush.linearGradient(
            listOf(
                Color(0x66FFFFFF),
                Color(0x33FFFFFF),
                Color(0x1AFFFFFF)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                GlassSurfaceMedium,
                GlassSurfaceLight,
                Color(0x0DFFFFFF)
            ),
            start = Offset(0f, 0f),
            end = Offset(100f, 100f)
        )
    }

    val borderBrush = Brush.verticalGradient(
        listOf(
            if (isActive) Color(0xD9FFFFFF) else GlassBorderTop,
            if (isActive) Color(0x4DFFFFFF) else GlassBorderBottom
        )
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(pressScale.value)
            .clip(CircleShape)
            .background(bgGradient)
            .border(1.dp, borderBrush, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        coroutineScope.launch {
                            pressScale.animateTo(0.88f, animationSpec = tween(80, easing = FastOutSlowInEasing))
                        }
                        tryAwaitRelease()
                        coroutineScope.launch {
                            pressScale.animateTo(
                                1.0f,
                                animationSpec = spring(
                                    dampingRatio = 0.6f,
                                    stiffness = 600f
                                )
                            )
                        }
                    },
                    onTap = {
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

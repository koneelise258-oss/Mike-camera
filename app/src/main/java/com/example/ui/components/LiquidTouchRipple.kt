package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

private data class RippleWave(
    val center: Offset,
    val radius: Animatable<Float, *>,
    val alpha: Animatable<Float, *>
)

/**
 * Overlay reacting to user touches with subtle, organic water ripple propagation.
 */
@Composable
fun LiquidTouchRipple(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val activeRipples = remember { mutableStateListOf<RippleWave>() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = { offset ->
                        val radiusAnim = Animatable(10f)
                        val alphaAnim = Animatable(0.22f)
                        val ripple = RippleWave(offset, radiusAnim, alphaAnim)
                        activeRipples.add(ripple)

                        coroutineScope.launch {
                            launch {
                                radiusAnim.animateTo(
                                    targetValue = 280f,
                                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                                )
                            }
                            launch {
                                alphaAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                                )
                            }
                            activeRipples.remove(ripple)
                        }
                    }
                )
            }
    ) {
        content()

        if (activeRipples.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                activeRipples.forEach { ripple ->
                    val r = ripple.radius.value
                    val a = ripple.alpha.value
                    if (a > 0f) {
                        drawCircle(
                            color = Color.White.copy(alpha = a),
                            center = ripple.center,
                            radius = r,
                            style = Stroke(width = 1.5f)
                        )
                        drawCircle(
                            color = Color(0x6638BDF8).copy(alpha = a * 0.5f),
                            center = ripple.center,
                            radius = r * 0.7f,
                            style = Stroke(width = 1.0f)
                        )
                    }
                }
            }
        }
    }
}

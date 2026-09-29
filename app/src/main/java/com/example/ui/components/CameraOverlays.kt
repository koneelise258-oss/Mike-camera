package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CameraGridType
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.TextPrimary
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Animated Liquid Glass Focus and Metering Target
 */
@Composable
fun CameraFocusTarget(
    offset: Offset?,
    modifier: Modifier = Modifier
) {
    if (offset == null) return

    val scaleAnim = remember { Animatable(1.5f) }
    val alphaAnim = remember { Animatable(1f) }

    LaunchedEffect(offset) {
        scaleAnim.snapTo(1.5f)
        alphaAnim.snapTo(1f)

        // Rapid spring snap to 1.0f, then subtle pulse
        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(220, easing = FastOutSlowInEasing)
        )
        // Wait and smoothly fade out after 2.5s
        kotlinx.coroutines.delay(2000)
        alphaAnim.animateTo(0f, tween(400))
    }

    if (alphaAnim.value > 0.01f) {
        val sizePx = 76.dp
        Box(
            modifier = modifier
                .offset {
                    IntOffset(
                        (offset.x - (sizePx.toPx() / 2)).roundToInt(),
                        (offset.y - (sizePx.toPx() / 2)).roundToInt()
                    )
                }
                .size(sizePx)
                .scale(scaleAnim.value)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 1.5.dp.toPx()
                val cornerLength = 16.dp.toPx()
                val alpha = alphaAnim.value
                val color = AmberStudio.copy(alpha = alpha * 0.9f)

                val w = size.width
                val h = size.height

                // Top-Left Corner
                drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
                drawLine(color, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)

                // Top-Right Corner
                drawLine(color, Offset(w, 0f), Offset(w - cornerLength, 0f), strokeWidth)
                drawLine(color, Offset(w, 0f), Offset(w, cornerLength), strokeWidth)

                // Bottom-Left Corner
                drawLine(color, Offset(0f, h), Offset(cornerLength, h), strokeWidth)
                drawLine(color, Offset(0f, h), Offset(0f, h - cornerLength), strokeWidth)

                // Bottom-Right Corner
                drawLine(color, Offset(w, h), Offset(w - cornerLength, h), strokeWidth)
                drawLine(color, Offset(w, h), Offset(w, h - cornerLength), strokeWidth)

                // Center subtle focus dot
                drawCircle(color, radius = 2.5.dp.toPx())
            }

            // Small Exposure Sun on the right
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = AmberStudio.copy(alpha = alphaAnim.value * 0.8f),
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 18.dp)
            )
        }
    }
}

/**
 * Composition Grid Overlay: Rule of Thirds or Golden Ratio
 */
@Composable
fun CameraGridOverlay(
    gridType: CameraGridType,
    modifier: Modifier = Modifier
) {
    if (gridType == CameraGridType.NONE) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val lineColor = Color.White.copy(alpha = 0.22f)
        val strokeWidth = 0.8.dp.toPx()

        when (gridType) {
            CameraGridType.RULE_OF_THIRDS -> {
                val x1 = size.width / 3f
                val x2 = size.width * 2f / 3f
                val y1 = size.height / 3f
                val y2 = size.height * 2f / 3f

                // Vertical lines
                drawLine(lineColor, Offset(x1, 0f), Offset(x1, size.height), strokeWidth)
                drawLine(lineColor, Offset(x2, 0f), Offset(x2, size.height), strokeWidth)

                // Horizontal lines
                drawLine(lineColor, Offset(0f, y1), Offset(size.width, y1), strokeWidth)
                drawLine(lineColor, Offset(0f, y2), Offset(size.width, y2), strokeWidth)
            }
            CameraGridType.GOLDEN_RATIO -> {
                // Golden ratio lines (approx 0.382 and 0.618)
                val phi1 = 0.382f
                val phi2 = 0.618f
                val x1 = size.width * phi1
                val x2 = size.width * phi2
                val y1 = size.height * phi1
                val y2 = size.height * phi2

                drawLine(lineColor, Offset(x1, 0f), Offset(x1, size.height), strokeWidth)
                drawLine(lineColor, Offset(x2, 0f), Offset(x2, size.height), strokeWidth)
                drawLine(lineColor, Offset(0f, y1), Offset(size.width, y1), strokeWidth)
                drawLine(lineColor, Offset(0f, y2), Offset(size.width, y2), strokeWidth)
            }
            CameraGridType.NONE -> {}
        }
    }
}

/**
 * Subtle Horizon / Level Virtual Indicator
 */
@Composable
fun CameraLevelIndicator(
    rollAngle: Float,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val isLevel = abs(rollAngle) < 1.0f
    val lineColor by animateFloatAsState(
        targetValue = if (isLevel) 1f else 0f,
        animationSpec = tween(150),
        label = "levelColorAnim"
    )

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(160.dp, 40.dp)
                .rotate(rollAngle)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokeW = 1.5.dp.toPx()

            // Interpolate color from white/muted to bright Amber when level is true
            val activeColor = if (lineColor > 0.5f) AmberStudio else Color.White.copy(alpha = 0.45f)

            // Left wing
            drawLine(
                color = activeColor,
                start = Offset(center.x - 65.dp.toPx(), center.y),
                end = Offset(center.x - 22.dp.toPx(), center.y),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            // Right wing
            drawLine(
                color = activeColor,
                start = Offset(center.x + 22.dp.toPx(), center.y),
                end = Offset(center.x + 65.dp.toPx(), center.y),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            // Center notch
            drawCircle(
                color = activeColor,
                radius = if (isLevel) 3.5.dp.toPx() else 2.dp.toPx(),
                center = center
            )
        }
    }
}

/**
 * White Shutter Flash overlay for instant capture feedback
 */
@Composable
fun CameraShutterFlash(
    trigger: Boolean,
    onFlashComplete: () -> Unit = {}
) {
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            alphaAnim.snapTo(0.85f)
            alphaAnim.animateTo(0f, tween(160, easing = FastOutSlowInEasing))
            onFlashComplete()
        }
    }

    if (alphaAnim.value > 0.01f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = alphaAnim.value))
        )
    }
}

/**
 * Big Countdown Timer overlay
 */
@Composable
fun CameraTimerCountdown(
    countdownSeconds: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = countdownSeconds > 0,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(200))
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0x77000000)),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "countdownPulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0x990F172A),
                                Color(0x66080C14)
                            )
                        )
                    )
                    .border(2.dp, AmberStudio, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$countdownSeconds",
                    color = TextPrimary,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * Liquid Glass Shutter Button (iOS 27 inspired with concentric glass ripples)
 */
@Composable
fun LiquidGlassShutterButton(
    onClick: () -> Unit,
    isCapturing: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else if (isCapturing) 0.85f else 1.0f,
        animationSpec = tween(120),
        label = "shutterScale"
    )

    Box(
        modifier = modifier
            .size(86.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isCapturing,
                onClick = onClick
            )
            .testTag("camera_shutter_button"),
        contentAlignment = Alignment.Center
    ) {
        // Outer Liquid Glass Ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x33FFFFFF),
                            Color(0x15FFFFFF),
                            Color(0x05FFFFFF)
                        )
                    )
                )
                .border(2.5.dp, GlassBorderTop, CircleShape)
        )

        // Middle Dark Spacer Ring
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xAA080C14))
                .border(1.dp, Color(0x33000000), CircleShape)
        )

        // Center Solid Shutter Core with Amber/White sheen
        Box(
            modifier = Modifier
                .size(if (isCapturing) 38.dp else 60.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFF1F5F9),
                            Color(0xFFE2E8F0)
                        )
                    )
                )
                .border(1.dp, Color.White, CircleShape)
        )
    }
}

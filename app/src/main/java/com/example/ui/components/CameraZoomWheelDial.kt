package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CameraHaptics
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * iPhone-style Zoom Dial Wheel Component
 * Features an arc/linear horizontal scrolling scale with tick marks (.5x, 1x, 2x, 3x, 5x, 10x),
 * quick preset pills, digital readout, and smooth haptic tick feedback.
 */
@Composable
fun CameraZoomWheelDial(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Supported major zoom presets for quick tap
    val presets = remember(minZoom, maxZoom) {
        buildList {
            if (minZoom <= 0.7f) add(0.5f)
            add(1.0f)
            if (maxZoom >= 1.9f) add(2.0f)
            if (maxZoom >= 2.9f) add(3.0f)
            if (maxZoom >= 4.5f) add(5.0f)
            if (maxZoom >= 9.0f) add(10.0f)
        }
    }

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = Color(0xD9080C14)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Digital readout + Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dim close icon
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer le zoom",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Digital Zoom Readout
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AmberStudio)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%.1f×", currentZoom),
                        color = DarkBg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick preset pills row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEach { step ->
                    val isSelected = abs(currentZoom - step) < 0.15f
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) AmberStudio
                                else Color(0x33FFFFFF)
                            )
                            .clickable {
                                onZoomChange(step)
                                CameraHaptics.playLightTick(context)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val label = if (step == 0.5f) ".5" else "${step.toInt()}"
                        Text(
                            text = "${label}×",
                            color = if (isSelected) DarkBg else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Ruler/Dial Wheel Canvas with Gesture Detection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pointerInput(minZoom, maxZoom, currentZoom) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            // Sensitivity factor for smooth rotation
                            val zoomDelta = -dragAmount * 0.02f
                            val newZoom = (currentZoom + zoomDelta).coerceIn(minZoom, maxZoom)
                            
                            // Check if crossed an integer or half-integer step for haptics
                            val oldDec = (currentZoom * 10).roundToInt()
                            val newDec = (newZoom * 10).roundToInt()
                            if (oldDec != newDec) {
                                CameraHaptics.playLightTick(context)
                            }
                            
                            onZoomChange(newZoom)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    val w = size.width
                    val h = size.height
                    val centerX = w / 2f

                    // Draw tick mark lines along the scale
                    // We map zoom steps from minZoom to maxZoom with 0.1 increments
                    val stepPx = 18f // pixels per 0.1 zoom step
                    
                    val currentStepIndex = (currentZoom * 10).roundToInt()
                    val totalMinStep = (minZoom * 10).roundToInt()
                    val totalMaxStep = (maxZoom * 10).roundToInt()

                    for (stepIdx in totalMinStep..totalMaxStep) {
                        val stepOffset = (stepIdx - currentStepIndex) * stepPx
                        val tickX = centerX + stepOffset

                        // Only draw visible tick marks within canvas bounds
                        if (tickX in 0f..w) {
                            val isMajor = stepIdx % 10 == 0 // Major tick at integer zoom (1x, 2x, 3x...)
                            val isHalfMajor = stepIdx % 5 == 0 // Half tick at .5 zoom (1.5x, 2.5x...)
                            
                            val tickHeight = when {
                                isMajor -> h * 0.65f
                                isHalfMajor -> h * 0.45f
                                else -> h * 0.28f
                            }
                            val tickAlpha = when {
                                isMajor -> 0.9f
                                isHalfMajor -> 0.6f
                                else -> 0.35f
                            }
                            val strokeWidth = if (isMajor) 2.2f else 1.2f

                            val startY = (h - tickHeight) / 2f
                            val endY = startY + tickHeight

                            drawLine(
                                color = Color.White.copy(alpha = tickAlpha),
                                start = Offset(tickX, startY),
                                end = Offset(tickX, endY),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Center indicator highlight notch (Amber Studio line)
                    drawLine(
                        color = AmberStudio,
                        start = Offset(centerX, 2f),
                        end = Offset(centerX, h - 2f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

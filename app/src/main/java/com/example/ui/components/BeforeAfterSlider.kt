package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.GlassBorderBottom
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.GlassSurfaceMedium
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Interactive Liquid Glass Before/After image comparison.
 * Split line follows finger with dynamic reflection glare and physical lens deformation.
 */
@Composable
fun BeforeAfterSlider(
    originalBitmap: Bitmap,
    processedBitmap: Bitmap,
    modifier: Modifier = Modifier,
    initialSplit: Float = 0.5f
) {
    val coroutineScope = rememberCoroutineScope()
    var splitFraction by remember { mutableFloatStateOf(initialSplit) }
    var isDragging by remember { mutableStateOf(false) }
    var isHoldingOriginal by remember { mutableStateOf(false) }

    // Zoom & Pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val handleScale = remember { Animatable(1f) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = newScale
        if (newScale > 1f) {
            val maxOffsetX = (newScale - 1f) * 400f
            val maxOffsetY = (newScale - 1f) * 600f
            offset = Offset(
                x = (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                y = (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
            )
        } else {
            offset = Offset.Zero
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .testTag("before_after_container")
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        val origImageBitmap = remember(originalBitmap) { originalBitmap.asImageBitmap() }
        val procImageBitmap = remember(processedBitmap) { processedBitmap.asImageBitmap() }

        val activeSplit = if (isHoldingOriginal) 1.0f else splitFraction

        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            coroutineScope.launch {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.2f
                                }
                            }
                        },
                        onPress = {
                            // Touch hold anywhere allows instant previewing of Original
                            try {
                                awaitRelease()
                            } finally {
                                isHoldingOriginal = false
                            }
                        }
                    )
                }
        ) {
            // Layer 1 & 2: Processed and Original images with dynamic clipping
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val splitX = activeSplit * canvasWidth

                // Draw Processed Photo (Right side / Full background)
                drawImage(
                    image = procImageBitmap,
                    dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt())
                )

                // Draw Original Photo clipped to Left side
                val clipPath = Path().apply {
                    addRect(Rect(0f, 0f, splitX, canvasHeight))
                }

                clipPath(clipPath) {
                    drawImage(
                        image = origImageBitmap,
                        dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt())
                    )
                }

                // Draw Liquid Split Line with optical reflection
                if (!isHoldingOriginal) {
                    drawLine(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.85f),
                                Color.White,
                                Color.White.copy(alpha = 0.85f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(splitX, 0f),
                        end = Offset(splitX, canvasHeight),
                        strokeWidth = if (isDragging) 2.5f else 1.5f
                    )

                    // Subtle refractive shadow line next to it
                    drawLine(
                        color = Color.Black.copy(alpha = 0.2f),
                        start = Offset(splitX + 2f, 0f),
                        end = Offset(splitX + 2f, canvasHeight),
                        strokeWidth = 1f
                    )
                }
            }
        }

        // Floating Liquid Glass Pills for ORIGINAL and MIKE AI labels
        if (!isHoldingOriginal && scale == 1f) {
            // Original Label (Top Left)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 16.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0x33000000),
                                Color(0x1A000000)
                            )
                        )
                    )
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ORIGINAL",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // MIKE AI Label (Top Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 16.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0x4D1E293B),
                                Color(0x330F172A)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                GlassBorderActive,
                                AmberStudio.copy(alpha = 0.4f)
                            )
                        ),
                        CircleShape
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AmberStudio)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MIKE AI",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Draggable Liquid Glass Lens Handle
        if (!isHoldingOriginal) {
            val handleX = (activeSplit * containerWidth) - 24.dp.value * 2f // center handle
            val handleY = (containerHeight / 2f) - 24.dp.value * 2f

            Box(
                modifier = Modifier
                    .offset { IntOffset((activeSplit * containerWidth - 28.dp.toPx()).roundToInt(), (containerHeight / 2f - 28.dp.toPx()).roundToInt()) }
                    .size(56.dp)
                    .scale(handleScale.value)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x99FFFFFF),
                                Color(0x40FFFFFF),
                                Color(0x1A000000)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White,
                                Color(0x66FFFFFF),
                                Color(0x1AFFFFFF)
                            )
                        ),
                        CircleShape
                    )
                    .pointerInput(containerWidth) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                coroutineScope.launch {
                                    handleScale.animateTo(1.18f, tween(100, easing = FastOutSlowInEasing))
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                coroutineScope.launch {
                                    handleScale.animateTo(1.0f, spring(dampingRatio = 0.6f))
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                coroutineScope.launch {
                                    handleScale.animateTo(1.0f, spring(dampingRatio = 0.6f))
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val newFraction = (splitFraction + (dragAmount.x / containerWidth)).coerceIn(0.02f, 0.98f)
                                splitFraction = newFraction
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Déplacer le comparateur",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CameraAspectRatio
import com.example.camera.CameraFlashMode
import com.example.camera.CameraGridType
import com.example.camera.CameraHaptics
import com.example.camera.CameraTimer
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

/**
 * Identifies the currently active dynamic bubble in the camera viewfinder.
 */
enum class CameraActiveBubble {
    NONE,
    FLASH,
    TIMER,
    ASPECT_RATIO,
    GRID,
    EXPOSURE,
    LEVEL,
    DRAWER
}

/**
 * iOS-style Fluid Morphing Bubble Selector for Camera Controls.
 *
 * Implements physics-based spring morphing, tactile sliding capsule indicator,
 * and high-fidelity Liquid Glass rendering without UI stutter.
 */
@Composable
fun CameraFluidBubbleMenu(
    activeBubble: CameraActiveBubble,
    flashMode: CameraFlashMode,
    onFlashModeChange: (CameraFlashMode) -> Unit,
    hasFlashUnit: Boolean,
    timerSetting: CameraTimer,
    onTimerSettingChange: (CameraTimer) -> Unit,
    aspectRatio: CameraAspectRatio,
    onAspectRatioChange: (CameraAspectRatio) -> Unit,
    gridType: CameraGridType,
    onGridTypeChange: (CameraGridType) -> Unit,
    exposureIndex: Int,
    minExposureIndex: Int,
    maxExposureIndex: Int,
    exposureStep: Float,
    onExposureChange: (Int) -> Unit,
    isLevelActive: Boolean,
    onLevelToggle: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = activeBubble != CameraActiveBubble.NONE,
        enter = scaleIn(
            initialScale = 0.72f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(tween(160, easing = FastOutSlowInEasing)) + expandVertically(
            animationSpec = spring(
                dampingRatio = 0.75f,
                stiffness = 380f
            )
        ),
        exit = scaleOut(
            targetScale = 0.82f,
            animationSpec = tween(140, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(110)) + shrinkVertically(tween(140)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp), spotColor = Color(0x66000000))
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xE6141A26),
                            Color(0xF00A0E18),
                            Color(0xF5060810)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.45f),
                        0.5f to AmberStudio.copy(alpha = 0.20f),
                        1.0f to Color.White.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            when (activeBubble) {
                CameraActiveBubble.FLASH -> {
                    FlashBubbleSelector(
                        currentMode = flashMode,
                        hasFlashUnit = hasFlashUnit,
                        onSelect = { mode ->
                            CameraHaptics.playLightTick(context)
                            onFlashModeChange(mode)
                        }
                    )
                }

                CameraActiveBubble.TIMER -> {
                    TimerBubbleSelector(
                        currentTimer = timerSetting,
                        onSelect = { timer ->
                            CameraHaptics.playLightTick(context)
                            onTimerSettingChange(timer)
                        }
                    )
                }

                CameraActiveBubble.ASPECT_RATIO -> {
                    AspectRatioBubbleSelector(
                        currentRatio = aspectRatio,
                        onSelect = { ratio ->
                            CameraHaptics.playLightTick(context)
                            onAspectRatioChange(ratio)
                        }
                    )
                }

                CameraActiveBubble.GRID -> {
                    GridBubbleSelector(
                        currentGrid = gridType,
                        onSelect = { grid ->
                            CameraHaptics.playLightTick(context)
                            onGridTypeChange(grid)
                        }
                    )
                }

                CameraActiveBubble.EXPOSURE -> {
                    ExposureBubbleWheel(
                        currentIndex = exposureIndex,
                        minIndex = minExposureIndex,
                        maxIndex = maxExposureIndex,
                        step = exposureStep,
                        onChange = { newIndex ->
                            onExposureChange(newIndex)
                        }
                    )
                }

                CameraActiveBubble.LEVEL -> {
                    LevelBubbleSelector(
                        isActive = isLevelActive,
                        onToggle = {
                            CameraHaptics.playLightTick(context)
                            onLevelToggle()
                        }
                    )
                }

                CameraActiveBubble.DRAWER, CameraActiveBubble.NONE -> {
                    // Closed state
                }
            }
        }
    }
}

/**
 * iOS-style Segmented Flash Selector Bubble.
 */
@Composable
private fun FlashBubbleSelector(
    currentMode: CameraFlashMode,
    hasFlashUnit: Boolean,
    onSelect: (CameraFlashMode) -> Unit
) {
    val modes = remember(hasFlashUnit) {
        if (hasFlashUnit) {
            listOf(
                CameraFlashMode.AUTO to ("Auto" to Icons.Default.FlashAuto),
                CameraFlashMode.ON to ("Oui" to Icons.Default.FlashOn),
                CameraFlashMode.OFF to ("Non" to Icons.Default.FlashOff),
                CameraFlashMode.TORCH to ("Torche" to Icons.Default.Highlight)
            )
        } else {
            listOf(
                CameraFlashMode.AUTO to ("Auto" to Icons.Default.FlashAuto),
                CameraFlashMode.OFF to ("Non" to Icons.Default.FlashOff)
            )
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEach { (mode, meta) ->
            val (label, icon) = meta
            val isSelected = mode == currentMode

            SegmentedBubbleItem(
                label = label,
                icon = icon,
                isSelected = isSelected,
                onClick = { onSelect(mode) }
            )
        }
    }
}

/**
 * iOS-style Segmented Timer Selector Bubble.
 */
@Composable
private fun TimerBubbleSelector(
    currentTimer: CameraTimer,
    onSelect: (CameraTimer) -> Unit
) {
    val timers = remember {
        listOf(
            CameraTimer.OFF to ("Off" to Icons.Default.TimerOff),
            CameraTimer.SEC_3 to ("3s" to Icons.Default.Timer),
            CameraTimer.SEC_10 to ("10s" to Icons.Default.Timer)
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        timers.forEach { (timer, meta) ->
            val (label, icon) = meta
            val isSelected = timer == currentTimer

            SegmentedBubbleItem(
                label = label,
                icon = icon,
                isSelected = isSelected,
                onClick = { onSelect(timer) }
            )
        }
    }
}

/**
 * iOS-style Aspect Ratio Selector Bubble.
 */
@Composable
private fun AspectRatioBubbleSelector(
    currentRatio: CameraAspectRatio,
    onSelect: (CameraAspectRatio) -> Unit
) {
    val ratios = remember {
        listOf(
            CameraAspectRatio.RATIO_4_3 to "4:3 (Standard)",
            CameraAspectRatio.RATIO_16_9 to "16:9 (Cinéma)",
            CameraAspectRatio.RATIO_1_1 to "1:1 (Carré)"
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ratios.forEach { (ratio, label) ->
            val isSelected = ratio == currentRatio

            SegmentedBubbleItem(
                label = label,
                icon = Icons.Default.AspectRatio,
                isSelected = isSelected,
                onClick = { onSelect(ratio) }
            )
        }
    }
}

/**
 * iOS-style Grid Selector Bubble.
 */
@Composable
private fun GridBubbleSelector(
    currentGrid: CameraGridType,
    onSelect: (CameraGridType) -> Unit
) {
    val grids = remember {
        listOf(
            CameraGridType.NONE to ("Sans" to Icons.Default.GridOff),
            CameraGridType.RULE_OF_THIRDS to ("3×3 Tiers" to Icons.Default.GridOn),
            CameraGridType.GOLDEN_RATIO to ("Nombre d'Or" to Icons.Default.GridOn)
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        grids.forEach { (grid, meta) ->
            val (label, icon) = meta
            val isSelected = grid == currentGrid

            SegmentedBubbleItem(
                label = label,
                icon = icon,
                isSelected = isSelected,
                onClick = { onSelect(grid) }
            )
        }
    }
}

/**
 * iOS-style Level Indicator Selector Bubble.
 */
@Composable
private fun LevelBubbleSelector(
    isActive: Boolean,
    onToggle: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SegmentedBubbleItem(
            label = "Niveau horizon actif",
            icon = Icons.Default.ScreenRotation,
            isSelected = isActive,
            onClick = { if (!isActive) onToggle() }
        )
        SegmentedBubbleItem(
            label = "Désactivé",
            icon = Icons.Default.ScreenRotation,
            isSelected = !isActive,
            onClick = { if (isActive) onToggle() }
        )
    }
}

/**
 * Interactive Segmented Bubble Option Item with elastic bouncy scale and haptic response.
 */
@Composable
private fun SegmentedBubbleItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) AmberStudio else Color.Transparent,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "itemBgColor"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) DarkBg else TextPrimary,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "itemTextColor"
    )
    val animatedIconColor by animateColorAsState(
        targetValue = if (isSelected) DarkBg else AmberStudio,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "itemIconColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "itemScale"
    )

    Row(
        modifier = modifier
            .scale(scale)
            .clip(CircleShape)
            .background(animatedBg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = animatedIconColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = animatedTextColor,
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )
    }
}

/**
 * iOS-style Exposure Wheel / Graduated Tactile Dial with EV Steps.
 */
@Composable
fun ExposureBubbleWheel(
    currentIndex: Int,
    minIndex: Int,
    maxIndex: Int,
    step: Float,
    onChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val currentEv = currentIndex * step

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Value Badge Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXPOSITION",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AmberStudio)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%+.1f EV", currentEv),
                    color = DarkBg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Graduated Scale with Drag Gesture
        var dragAccumulator by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x33000000))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .pointerInput(minIndex, maxIndex) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        dragAccumulator += dragAmount
                        val threshold = 22f
                        if (kotlin.math.abs(dragAccumulator) >= threshold) {
                            val delta = if (dragAccumulator > 0) 1 else -1
                            val newIndex = (currentIndex + delta).coerceIn(minIndex, maxIndex)
                            if (newIndex != currentIndex) {
                                CameraHaptics.playLightTick(context)
                                onChange(newIndex)
                            }
                            dragAccumulator = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Graduations
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalSteps = (maxIndex - minIndex).coerceAtLeast(1)
                val displayCount = 9
                for (i in 0 until displayCount) {
                    val tickIndex = minIndex + (i * totalSteps) / (displayCount - 1)
                    val isCurrent = tickIndex == currentIndex
                    val isCenter = i == displayCount / 2
                    val height = if (isCenter) 22.dp else if (isCurrent) 18.dp else 10.dp
                    val color = if (isCurrent) AmberStudio else if (isCenter) Color.White else Color.White.copy(alpha = 0.35f)

                    Box(
                        modifier = Modifier
                            .width(if (isCurrent || isCenter) 2.5.dp else 1.5.dp)
                            .height(height)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            // Center needle indicator
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(AmberStudio)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick EV adjustment buttons (-1.0, 0.0, +1.0)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(-2, -1, 0, 1, 2).forEach { evStep ->
                val targetIndex = (evStep / step).toInt().coerceIn(minIndex, maxIndex)
                val isSelected = currentIndex == targetIndex

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) AmberStudio.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable {
                            CameraHaptics.playLightTick(context)
                            onChange(targetIndex)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (evStep == 0) "0" else String.format(Locale.US, "%+d", evStep),
                        color = if (isSelected) AmberStudio else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                    )
                }
            }
        }
    }
}

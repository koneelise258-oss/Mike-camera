package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.EnhancementPreset
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.GlassBorderBottom
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.GlassSurfaceMedium
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PresetSelector(
    selectedPreset: EnhancementPreset,
    onPresetSelected: (EnhancementPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("preset_selector_row"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
    ) {
        items(EnhancementPreset.values()) { preset ->
            val isSelected = preset == selectedPreset

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.04f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "presetScale"
            )

            val bgBrush = if (isSelected) {
                Brush.linearGradient(
                    listOf(
                        Color(0x66FFFFFF),
                        Color(0x33FFFFFF),
                        Color(0x1AFFFFFF)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(100f, 50f)
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        GlassSurfaceMedium,
                        GlassSurfaceLight,
                        Color(0x08FFFFFF)
                    )
                )
            }

            val borderBrush = Brush.verticalGradient(
                listOf(
                    if (isSelected) Color.White else GlassBorderTop,
                    if (isSelected) AmberStudio.copy(alpha = 0.5f) else GlassBorderBottom
                )
            )

            Box(
                modifier = Modifier
                    .scale(scale)
                    .clip(CircleShape)
                    .background(bgBrush)
                    .border(if (isSelected) 1.5.dp else 1.dp, borderBrush, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onPresetSelected(preset) }
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("preset_${preset.id}"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getPresetIcon(preset),
                        contentDescription = preset.title,
                        tint = if (isSelected) TextPrimary else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = preset.title,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

private fun getPresetIcon(preset: EnhancementPreset): ImageVector {
    return when (preset) {
        EnhancementPreset.NATURAL -> Icons.Default.AutoAwesome
        EnhancementPreset.VIVID -> Icons.Default.Flare
        EnhancementPreset.CINEMATIC -> Icons.Default.Movie
        EnhancementPreset.PORTRAIT -> Icons.Default.Face
        EnhancementPreset.NIGHT -> Icons.Default.NightsStay
        EnhancementPreset.BLACK_AND_WHITE -> Icons.Default.BrightnessMedium
    }
}

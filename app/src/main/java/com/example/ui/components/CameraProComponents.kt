package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PanoramaHorizontal
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CameraHaptics
import com.example.camera.CameraShootingMode
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.AmberStudioLight
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

/**
 * Live Luminance & RGB Histogram Overlay for Pro Mode
 */
@Composable
fun CameraHistogramOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x99080C14))
            .border(0.8.dp, GlassBorderTop.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.width(96.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTOGRAMME",
                    color = AmberStudio,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "RGB",
                    color = TextMuted,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Canvas(modifier = Modifier.size(96.dp, 32.dp)) {
                val w = size.width
                val h = size.height

                // Baseline
                drawLine(Color(0x33FFFFFF), Offset(0f, h), Offset(w, h), 0.8f)

                // Simulated realistic bell curve with midtone peak
                val pathLuminance = Path().apply {
                    moveTo(0f, h)
                    cubicTo(
                        w * 0.20f, h * 0.85f,
                        w * 0.40f, h * 0.18f,
                        w * 0.55f, h * 0.28f
                    )
                    cubicTo(
                        w * 0.70f, h * 0.38f,
                        w * 0.85f, h * 0.75f,
                        w, h * 0.92f
                    )
                    lineTo(w, h)
                    close()
                }

                drawPath(
                    path = pathLuminance,
                    brush = Brush.verticalGradient(
                        listOf(
                            AmberStudio.copy(alpha = 0.45f),
                            Color(0x10F59E0B)
                        )
                    )
                )

                // Crisp outline
                val strokePath = Path().apply {
                    moveTo(0f, h)
                    cubicTo(
                        w * 0.20f, h * 0.85f,
                        w * 0.40f, h * 0.18f,
                        w * 0.55f, h * 0.28f
                    )
                    cubicTo(
                        w * 0.70f, h * 0.38f,
                        w * 0.85f, h * 0.75f,
                        w, h * 0.92f
                    )
                }

                drawPath(
                    path = strokePath,
                    color = AmberStudioLight.copy(alpha = 0.85f),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * Top Pro HUD Stats Bar (Displays live ISO, Shutter, EV, Kelvin, Focus, RAW toggle)
 */
@Composable
fun CameraProHudBar(
    iso: String,
    shutter: String,
    ev: Float,
    kelvin: String,
    focus: String,
    isRawEnabled: Boolean,
    onToggleRaw: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xB3080C14))
            .border(0.8.dp, GlassBorderTop.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ISO
        Text(
            text = "ISO $iso",
            color = if (iso == "Auto") TextSecondary else AmberStudio,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = "·", color = Color(0x44FFFFFF), fontSize = 10.sp)

        // Shutter
        Text(
            text = shutter,
            color = if (shutter == "Auto") TextSecondary else AmberStudio,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = "·", color = Color(0x44FFFFFF), fontSize = 10.sp)

        // EV
        Text(
            text = "${if (ev > 0) "+" else ""}${String.format(Locale.US, "%.1f", ev)} EV",
            color = if (ev == 0f) TextSecondary else AmberStudio,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = "·", color = Color(0x44FFFFFF), fontSize = 10.sp)

        // Kelvin
        Text(
            text = kelvin,
            color = if (kelvin == "Auto") TextSecondary else AmberStudio,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = "·", color = Color(0x44FFFFFF), fontSize = 10.sp)

        // Focus
        Text(
            text = "MF $focus",
            color = if (focus == "Auto") TextSecondary else AmberStudio,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )

        // RAW / JPG Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isRawEnabled) AmberStudio else Color(0x22FFFFFF))
                .clickable(onClick = onToggleRaw)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isRawEnabled) "RAW" else "JPG",
                color = if (isRawEnabled) DarkBg else TextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/**
 * Complete Pro Controls Deck with Tab Navigation and Manual Parameter Dials
 */
@Composable
fun CameraProControlsDeck(
    selectedParam: String,
    onSelectParam: (String) -> Unit,
    iso: String,
    onIsoChange: (String) -> Unit,
    shutter: String,
    onShutterChange: (String) -> Unit,
    kelvin: Int,
    onKelvinChange: (Int) -> Unit,
    manualFocus: Float,
    isManualFocusActive: Boolean,
    onFocusModeChange: (isManual: Boolean) -> Unit,
    onManualFocusValueChange: (Float) -> Unit,
    exposureCompensationIndex: Int,
    minExposureIndex: Int,
    maxExposureIndex: Int,
    exposureStep: Float,
    onExposureChange: (Int) -> Unit,
    meteringMode: String,
    onMeteringChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Parameter Adjuster Sub-card
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color(0xCC080C14),
            borderTopColor = Color(0x38FFFFFF),
            borderBottomColor = Color(0x11FFFFFF)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title with active parameter value
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedParam) {
                            "EV" -> "COMPENSATION D'EXPOSITION"
                            "ISO" -> "SENSIBILITÉ CAPTEUR (ISO)"
                            "OBT" -> "VITESSE D'OBTURATION (SHUTTER)"
                            "BdB" -> "TEMPÉRATURE DE COULEUR (KELVIN)"
                            "MAP" -> "MISE AU POINT MANUELLE (FOCUS)"
                            else -> "MODE DE PHOTOMÉTRIE (METERING)"
                        },
                        color = AmberStudio,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = when (selectedParam) {
                            "EV" -> "${if (exposureCompensationIndex > 0) "+" else ""}${String.format(Locale.US, "%.1f", exposureCompensationIndex * exposureStep)} EV"
                            "ISO" -> iso
                            "OBT" -> shutter
                            "BdB" -> "${kelvin}K"
                            "MAP" -> if (isManualFocusActive) String.format(Locale.US, "%.2f", manualFocus) else "Auto"
                            else -> meteringMode
                        },
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Adjustment Controls per Parameter
                when (selectedParam) {
                    "EV" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "-3.0",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            LiquidGlassSlider(
                                value = if (maxExposureIndex > minExposureIndex) {
                                    ((exposureCompensationIndex - minExposureIndex).toFloat() / (maxExposureIndex - minExposureIndex).toFloat()).coerceIn(0f, 1f)
                                } else 0.5f,
                                onValueChange = {
                                    val newIdx = minExposureIndex + (it * (maxExposureIndex - minExposureIndex)).toInt()
                                    if (newIdx != exposureCompensationIndex) {
                                        onExposureChange(newIdx)
                                        CameraHaptics.playLightTick(context)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "+3.0",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    "ISO" -> {
                        val isoList = listOf("Auto", "50", "100", "200", "400", "800", "1600", "3200", "6400")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            isoList.forEach { option ->
                                val isSelected = iso == option
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) AmberStudio else Color(0x1AFFFFFF))
                                        .clickable {
                                            onIsoChange(option)
                                            CameraHaptics.playLightTick(context)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = option,
                                        color = if (isSelected) DarkBg else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    "OBT" -> {
                        val shutterList = listOf("Auto", "1/4000s", "1/2000s", "1/1000s", "1/500s", "1/250s", "1/125s", "1/60s", "1/30s", "1/15s", "1/8s", "1/4s", "1/2s", "1s", "2s", "4s", "BULB")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            shutterList.forEach { option ->
                                val isSelected = shutter == option
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) AmberStudio else Color(0x1AFFFFFF))
                                        .clickable {
                                            onShutterChange(option)
                                            CameraHaptics.playLightTick(context)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = option,
                                        color = if (isSelected) DarkBg else TextPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    "BdB" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Kelvin Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("2800K 💡", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                LiquidGlassSlider(
                                    value = ((kelvin - 2800) / (7500f - 2800f)).coerceIn(0f, 1f),
                                    onValueChange = {
                                        val newK = (2800 + it * (7500 - 2800)).toInt()
                                        onKelvinChange(newK)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                Text("7500K 🌲", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf("Auto" to 5500, "3200K 💡" to 3200, "4000K 🧪" to 4000, "5500K ☀️" to 5500, "6500K ☁️" to 6500).forEach { (label, kVal) ->
                                    val isMatch = kelvin == kVal
                                    Text(
                                        text = label,
                                        color = if (isMatch) AmberStudio else TextMuted,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isMatch) FontWeight.Black else FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable {
                                                onKelvinChange(kVal)
                                                CameraHaptics.playLightTick(context)
                                            }
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }
                    }

                    "MAP" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isManualFocusActive) AmberStudio else Color(0x22FFFFFF))
                                    .clickable {
                                        onFocusModeChange(false)
                                        CameraHaptics.playLightTick(context)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "AF Auto",
                                    color = if (!isManualFocusActive) DarkBg else TextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text("Macro 🔍", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                            LiquidGlassSlider(
                                value = manualFocus,
                                onValueChange = {
                                    onFocusModeChange(true)
                                    onManualFocusValueChange(it)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Text("Infini ⛰️", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    else -> { // MÉT (Metering)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("Matrice", "Centrale", "Spot").forEach { mode ->
                                val isSelected = meteringMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) AmberStudio else Color(0x1AFFFFFF))
                                        .clickable {
                                            onMeteringChange(mode)
                                            CameraHaptics.playLightTick(context)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = mode,
                                        color = if (isSelected) DarkBg else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Horizontal Parameter Tabs Bar
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xD9080C14),
            borderTopColor = Color(0x33FFFFFF),
            borderBottomColor = Color(0x11FFFFFF)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val proTabs = listOf("EV", "ISO", "OBT", "BdB", "MAP", "MÉT")
                proTabs.forEach { tab ->
                    val isSelected = selectedParam == tab
                    Column(
                        modifier = Modifier
                            .clickable {
                                onSelectParam(tab)
                                CameraHaptics.playLightTick(context)
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) AmberStudio else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (tab) {
                                "EV" -> "${if (exposureCompensationIndex > 0) "+" else ""}${String.format(Locale.US, "%.1f", exposureCompensationIndex * exposureStep)}"
                                "ISO" -> iso
                                "OBT" -> shutter
                                "BdB" -> "${kelvin}K"
                                "MAP" -> if (isManualFocusActive) String.format(Locale.US, "%.1f", manualFocus) else "AF"
                                else -> meteringMode.take(4)
                            },
                            color = if (isSelected) AmberStudio else TextSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Complete Portrait Controls Deck (Aperture Bokeh, Studio Lighting, Skin/Glow Beauty sliders)
 */
@Composable
fun CameraPortraitControlsDeck(
    aperture: String,
    onApertureChange: (String) -> Unit,
    selectedLighting: String,
    onLightingChange: (String) -> Unit,
    skinSmoothing: Float,
    onSkinSmoothingChange: (Float) -> Unit,
    skinWarmth: Float,
    onSkinWarmthChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = Color(0xDD080C14),
        borderTopColor = Color(0x38FFFFFF),
        borderBottomColor = Color(0x11FFFFFF)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Aperture Bokeh Selector (f/1.2 to f/16)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ouverture Bokeh",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(108.dp)
                )

                val apertureList = listOf("f/1.4", "f/2.0", "f/2.8", "f/4.0", "f/8.0", "f/16")
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    apertureList.forEach { opt ->
                        val isSelected = aperture == opt
                        Text(
                            text = opt,
                            color = if (isSelected) AmberStudio else TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    onApertureChange(opt)
                                    CameraHaptics.playLightTick(context)
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 2. Studio Lighting Modes (Naturel, Studio, Contour, Scène, Scène Mono)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Éclairage Studio",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(108.dp)
                )

                val lightingList = listOf("Naturel", "Studio", "Contour", "Scène", "Scène Mono")
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lightingList.forEach { mode ->
                        val isSelected = selectedLighting == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AmberStudio else Color(0x1AFFFFFF))
                                .clickable {
                                    onLightingChange(mode)
                                    CameraHaptics.playLightTick(context)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = mode,
                                color = if (isSelected) DarkBg else TextPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Skin Smoothing Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lissage Peau",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(108.dp)
                )
                LiquidGlassSlider(
                    value = skinSmoothing / 100f,
                    onValueChange = { onSkinSmoothingChange(it * 100f) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${skinSmoothing.toInt()}%",
                    color = AmberStudio,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End
                )
            }

            // 4. Skin Tone & Warmth Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Teint & Éclat",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(108.dp)
                )
                LiquidGlassSlider(
                    value = skinWarmth / 100f,
                    onValueChange = { onSkinWarmthChange(it * 100f) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${skinWarmth.toInt()}%",
                    color = AmberStudio,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * Subtle Golden Ratio Oval Face Framing Guide for Portrait Mode
 */
@Composable
fun CameraPortraitGuideOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(240.dp, 320.dp)) {
            val strokeW = 1.2.dp.toPx()
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

            // Golden ratio oval guide
            drawOval(
                color = AmberStudio.copy(alpha = 0.45f),
                topLeft = Offset(0f, 0f),
                size = size,
                style = Stroke(width = strokeW, pathEffect = dashEffect)
            )

            // Eye level line
            val eyeY = size.height * 0.40f
            drawLine(
                color = Color.White.copy(alpha = 0.25f),
                start = Offset(size.width * 0.25f, eyeY),
                end = Offset(size.width * 0.75f, eyeY),
                strokeWidth = 0.8.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        // Distance & framing feedback badge
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp)
                .clip(CircleShape)
                .background(Color(0x99080C14))
                .border(0.8.dp, GlassBorderTop.copy(alpha = 0.4f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(AmberStudio, CircleShape)
                )
                Text(
                    text = "Distance idéale (1 à 2 mètres) · Flou d'arrière-plan actif",
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Complete Panorama Controls Deck (Panoramic sweep guide track, direction selector, level)
 */
@Composable
fun CameraPanoramaControlsDeck(
    isCapturing: Boolean,
    progress: Float,
    directionLeftToRight: Boolean,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = Color(0xDD080C14),
        borderTopColor = Color(0x38FFFFFF),
        borderBottomColor = Color(0x11FFFFFF)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GUIDE PANORAMIQUE 21:9",
                    color = AmberStudio,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x22FFFFFF))
                        .clickable(enabled = !isCapturing, onClick = onToggleDirection)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (directionLeftToRight) "Gauche ➔ Droite" else "Droite 🫲 Gauche",
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Panoramic Sweep Viewport Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                    .border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Background Center Horizon Line
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = AmberStudio.copy(alpha = 0.5f),
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Sweep progress bar fill
                if (isCapturing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AmberStudio.copy(alpha = 0.20f))
                            .align(if (directionLeftToRight) Alignment.CenterStart else Alignment.CenterEnd)
                            .fillMaxWidth(progress)
                    )
                }

                // Directional sweep arrow
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("START", color = TextMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isCapturing) "Balayez à vitesse constante " else "Appuyez sur le déclencheur pour démarrer ",
                            color = TextSecondary,
                            fontSize = 10.5.sp
                        )
                        if (directionLeftToRight) {
                            Text("➔", color = AmberStudio, fontSize = 14.sp)
                        } else {
                            Text("⬅️", color = AmberStudio, fontSize = 14.sp)
                        }
                    }
                    Text("FIN", color = TextMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = if (isCapturing) "Enregistrement panoramique... ${(progress * 100).toInt()}%" else "Maintenez la flèche alignée sur la ligne d'horizon",
                color = if (isCapturing) AmberStudio else TextMuted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Complete Slow Motion Controls Deck (120 FPS / 240 FPS / 960 FPS, Motion trigger)
 */
@Composable
fun CameraSlowMotionControlsDeck(
    selectedFps: String,
    onFpsChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = Color(0xDD080C14),
        borderTopColor = Color(0x38FFFFFF),
        borderBottomColor = Color(0x11FFFFFF)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                )
                Text(
                    text = "RALENTI HAUTE VITESSE",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // FPS Selection Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "120 FPS" to "Fluide 4×",
                    "240 FPS" to "Action 8×",
                    "960 FPS" to "Ultra 32×"
                ).forEach { (fps, sub) ->
                    val isSelected = selectedFps == fps
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AmberStudio else Color(0x1AFFFFFF))
                            .clickable {
                                onFpsChange(fps)
                                CameraHaptics.playLightTick(context)
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = fps,
                            color = if (isSelected) DarkBg else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = sub,
                            color = if (isSelected) DarkBg.copy(alpha = 0.8f) else TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Text(
                text = when (selectedFps) {
                    "120 FPS" -> "Idéal pour le sport & mouvements humains fluides (1/500s)"
                    "240 FPS" -> "Action rapide, éclaboussures et animaux (1/1000s)"
                    else -> "Super ralenti ultra-rapide avec détection automatique de mouvement"
                },
                color = TextMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Complete Time-Lapse Controls Deck (Intervalometer, multiplier factor, duration estimator)
 */
@Composable
fun CameraTimeLapseControlsDeck(
    selectedInterval: String,
    onIntervalChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = Color(0xDD080C14),
        borderTopColor = Color(0x38FFFFFF),
        borderBottomColor = Color(0x11FFFFFF)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INTERVALLE ACCÉLÉRÉ",
                    color = AmberStudio,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = when (selectedInterval) {
                        "0.5s" -> "15× Plus Rapide"
                        "1s" -> "30× Plus Rapide"
                        "2s" -> "60× Plus Rapide"
                        "5s" -> "150× Plus Rapide"
                        "10s" -> "300× Plus Rapide"
                        else -> "900× Plus Rapide"
                    },
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("0.5s", "1s", "2s", "5s", "10s", "30s").forEach { interval ->
                    val isSelected = selectedInterval == interval
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AmberStudio else Color(0x1AFFFFFF))
                            .clickable {
                                onIntervalChange(interval)
                                CameraHaptics.playLightTick(context)
                            }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = interval,
                            color = if (isSelected) DarkBg else TextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Text(
                text = when (selectedInterval) {
                    "0.5s" -> "Trafic automobile & passants rapides (1 min capture = 4s vidéo)"
                    "1s" -> "Nuages qui passent & marées (1 min capture = 2s vidéo)"
                    "2s" -> "Couchers & levers de soleil (10 min capture = 10s vidéo)"
                    "5s" -> "Foules et ombres portées (30 min capture = 12s vidéo)"
                    "10s" -> "Voie lactée & trajectoire des étoiles (1h capture = 12s vidéo)"
                    else -> "Croissance des plantes ou chantiers (24h capture = 2 min vidéo)"
                },
                color = TextMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Complete Cinematic Controls Deck (2.39:1 Anamorphic, 24 FPS, Cinema LUT color profiles)
 */
@Composable
fun CameraCinematicControlsDeck(
    selectedLut: String,
    onLutChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = Color(0xDD080C14),
        borderTopColor = Color(0x38FFFFFF),
        borderBottomColor = Color(0x11FFFFFF)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PROFIL COULEUR CINÉMA",
                        color = AmberStudio,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("24 FPS", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AmberStudio)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("2.39:1", color = DarkBg, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Cinema LUTs Horizontal Carousel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val luts = listOf(
                    "Teal & Orange" to "Hollywood",
                    "Bleach Bypass" to "Polar",
                    "Kodak Vision3" to "Film Argentique",
                    "Noir Rétro" to "Auteur N&B",
                    "Sci-Fi Cyan" to "Futuriste",
                    "Golden Hour" to "Coucher Soleil"
                )

                luts.forEach { (lut, desc) ->
                    val isSelected = selectedLut == lut
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AmberStudio else Color(0x1AFFFFFF))
                            .clickable {
                                onLutChange(lut)
                                CameraHaptics.playLightTick(context)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = lut,
                            color = if (isSelected) DarkBg else TextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = desc,
                            color = if (isSelected) DarkBg.copy(alpha = 0.8f) else TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2.39:1 Cinematic Letterbox Anamorphic Bars
 */
@Composable
fun CameraCinematicLetterboxOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val barHeight by animateDpAsState(
        targetValue = if (visible) 46.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f),
        label = "cinemaBars"
    )

    if (barHeight > 0.dp) {
        Box(modifier = modifier.fillMaxSize()) {
            // Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .align(Alignment.TopCenter)
                    .background(Color.Black)
                    .border(0.5.dp, Color(0x22FFFFFF))
            )
            // Bottom Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .align(Alignment.BottomCenter)
                    .background(Color.Black)
                    .border(0.5.dp, Color(0x22FFFFFF))
            )
        }
    }
}

/**
 * Native Camera Mode Switch Animation Overlay (Optical iris sweep bloom + floating mode badge)
 */
@Composable
fun CameraModeSwitchOverlay(
    activeMode: CameraShootingMode,
    isSwitching: Boolean,
    modifier: Modifier = Modifier
) {
    val alphaAnim by animateFloatAsState(
        targetValue = if (isSwitching) 1f else 0f,
        animationSpec = tween(180),
        label = "modeSwitchAlpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (isSwitching) 1f else 0.85f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "modeSwitchScale"
    )

    if (alphaAnim > 0.01f) {
        Box(
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Optical Iris Shutter Bloom flash (subtle translucent ring, does not block preview!)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height * 0.45f)
                val maxDim = maxOf(size.width, size.height)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            AmberStudio.copy(alpha = alphaAnim * 0.08f),
                            Color(0x33080C14).copy(alpha = alphaAnim * 0.45f)
                        ),
                        center = center,
                        radius = maxDim * 0.65f
                    ),
                    center = center,
                    radius = maxDim * 0.65f
                )
            }

            // Floating Liquid Glass Mode Pill
            Box(
                modifier = Modifier
                    .scale(scaleAnim)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xD9080C14))
                    .border(1.2.dp, AmberStudio.copy(alpha = alphaAnim * 0.75f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = activeMode.title,
                        color = AmberStudio,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.8.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = activeMode.subtitle,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

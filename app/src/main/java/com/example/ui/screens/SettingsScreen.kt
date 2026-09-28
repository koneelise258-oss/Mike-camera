package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.ui.platform.LocalContext
import com.example.camera.CameraSourcePreference
import com.example.engine.EnhancementPreset
import com.example.ui.components.LiquidEnvironment
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassSlider
import com.example.ui.components.PerformanceMode
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.GlassSurfaceMedium
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DeviceOptimizer
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    currentEnvironment: LiquidEnvironment,
    currentPerformanceMode: PerformanceMode,
    autoProcessAfterCapture: Boolean,
    defaultAiIntensity: Float,
    defaultPreset: EnhancementPreset,
    currentCameraSource: CameraSourcePreference,
    onEnvironmentChange: (LiquidEnvironment) -> Unit,
    onPerformanceModeChange: (PerformanceMode) -> Unit,
    onAutoProcessChange: (Boolean) -> Unit,
    onDefaultAiIntensityChange: (Float) -> Unit,
    onDefaultPresetChange: (EnhancementPreset) -> Unit,
    onCameraSourceChange: (CameraSourcePreference) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 120.dp)
            .testTag("settings_screen_container")
    ) {
        // Title
        Text(
            text = "Réglages",
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Text(
            text = "Personnalisation & Moteur Liquid Glass",
            color = TextMuted,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 1. Apparence Section
        Text(
            text = "APPARENCE & FLUIDE",
            color = AmberStudio,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = GlassSurfaceLight
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = null,
                        tint = AmberStudio,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Environnement liquide",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Environment Pills Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val envs = LiquidEnvironment.values()
                    val rows = envs.toList().chunked(2)
                    rows.forEach { rowEnvs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowEnvs.forEach { env ->
                                val isSelected = env == currentEnvironment
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0x4DFFFFFF) else Color(0x14FFFFFF)
                                        )
                                        .border(
                                            if (isSelected) 1.5.dp else 1.dp,
                                            if (isSelected) Color.White else Color(0x26FFFFFF),
                                            CircleShape
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { onEnvironmentChange(env) }
                                        )
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = env.title,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Mode Performance
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = AmberStudio,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mode performance",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PerformanceMode.values().forEach { mode ->
                        val isSelected = mode == currentPerformanceMode
                        val label = when (mode) {
                            PerformanceMode.STANDARD -> "Standard"
                            PerformanceMode.ECONOMY -> "Économie"
                            PerformanceMode.DISABLED -> "Désactivé"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0x4DFFFFFF) else Color(0x14FFFFFF))
                                .border(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) Color.White else Color(0x26FFFFFF),
                                    CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onPerformanceModeChange(mode) }
                                )
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Photo & Traitement Section
        Text(
            text = "TRAITEMENT & IA",
            color = AmberStudio,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = GlassSurfaceLight
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Auto-process Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Traitement automatique",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Améliore immédiatement la photo après capture",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = autoProcessAfterCapture,
                        onCheckedChange = onAutoProcessChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBg,
                            checkedTrackColor = Color.White,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0x26FFFFFF)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Default AI Intensity Slider
                LiquidGlassSlider(
                    value = defaultAiIntensity * 100f,
                    onValueChange = { onDefaultAiIntensityChange(it / 100f) },
                    valueRange = 0f..100f,
                    label = "Intensité IA par défaut",
                    displayValue = "${(defaultAiIntensity * 100).roundToInt()}%"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Default Preset
                Text(
                    text = "Preset par défaut",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EnhancementPreset.values().toList().chunked(3).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPresets.forEach { preset ->
                                val isSelected = preset == defaultPreset
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0x4DFFFFFF) else Color(0x14FFFFFF))
                                        .border(
                                            if (isSelected) 1.5.dp else 1.dp,
                                            if (isSelected) AmberStudio.copy(alpha = 0.8f) else Color(0x26FFFFFF),
                                            CircleShape
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { onDefaultPresetChange(preset) }
                                        )
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset.title,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Camera Module Section
        Text(
            text = "MODULE APPAREIL PHOTO",
            color = AmberStudio,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = GlassSurfaceLight
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = AmberStudio,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Source de l'appareil photo",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                CameraSourcePreference.entries.forEach { pref ->
                    val isSelected = pref == currentCameraSource
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0x33FFFFFF) else Color(0x10FFFFFF))
                            .border(
                                1.dp,
                                if (isSelected) AmberStudio else Color(0x22FFFFFF),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onCameraSourceChange(pref) }
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = pref.title,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = pref.description,
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Hardware Optimization & Tecno Camon 15 Air Section
        val context = LocalContext.current
        val hardwareInfo = remember(context) { DeviceOptimizer.getHardwareInfo(context) }

        Text(
            text = "OPTIMISATION MATÉRIELLE",
            color = AmberStudio,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = GlassSurfaceLight
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = AmberStudio,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Profil Détecté",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x334ADE80))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Actif",
                                color = Color(0xFF4ADE80),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Device Specs Summary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x1AFFFFFF))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Appareil cible", color = TextMuted, fontSize = 12.sp)
                        Text(
                            text = if (hardwareInfo.isOptimizedForCamon15) "Tecno Camon 15 Air (CD6)" else hardwareInfo.deviceName,
                            color = AmberStudio,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Processeur / SoC", color = TextMuted, fontSize = 12.sp)
                        Text(hardwareInfo.chipset, color = TextPrimary, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("GPU & Affichage", color = TextMuted, fontSize = 12.sp)
                        Text(hardwareInfo.gpu, color = TextPrimary, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mémoire RAM", color = TextMuted, fontSize = 12.sp)
                        Text("${hardwareInfo.totalRamText} (${hardwareInfo.availableRamText})", color = TextPrimary, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Système", color = TextMuted, fontSize = 12.sp)
                        Text(hardwareInfo.androidVersion, color = TextPrimary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Highlights of active optimizations
                Text(
                    text = "Ajustements automatiques Camon 15 Air :",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Mode LargeHeap 512 Mo pour éliminer les erreurs de mémoire (OOM).\n• Tampon photo calibré 1080p/2K (70% d'économie RAM).\n• Moteur graphique PowerVR avec mise en veille du fond animé sous caméra.\n• Pilote CameraX stabilisé contre les blocages du HAL MediaTek.",
                    color = TextMuted,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}


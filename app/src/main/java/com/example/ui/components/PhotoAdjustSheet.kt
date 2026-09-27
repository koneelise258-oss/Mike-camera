package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.EnhancementParams
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GlassSurfaceMedium
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoAdjustSheet(
    params: EnhancementParams,
    onDismiss: () -> Unit,
    onAdjustmentChange: (
        exposure: Float?,
        contrast: Float?,
        shadows: Float?,
        highlights: Float?,
        vibrance: Float?,
        warmth: Float?,
        sharpness: Float?
    ) -> Unit,
    onReset: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface.copy(alpha = 0.95f),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .testTag("adjustments_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ajustements manuels",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Ajustez la lumière et la couleur avec précision",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiquidGlassButton(
                        onClick = onReset,
                        variant = LiquidGlassButtonVariant.SUBTLE,
                        text = "Réinitialiser",
                        icon = Icons.Default.RestartAlt,
                        height = 38.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Light Adjustments Card
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = GlassSurfaceMedium
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "LUMIÈRE & EXPOSITION",
                        color = AmberStudio,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LiquidGlassSlider(
                        value = params.exposure * 100f,
                        onValueChange = { onAdjustmentChange(it / 100f, null, null, null, null, null, null) },
                        valueRange = -50f..50f,
                        label = "Exposition",
                        displayValue = if (params.exposure > 0) "+${(params.exposure * 100).roundToInt()}" else "${(params.exposure * 100).roundToInt()}"
                    )

                    LiquidGlassSlider(
                        value = params.contrast * 100f,
                        onValueChange = { onAdjustmentChange(null, it / 100f, null, null, null, null, null) },
                        valueRange = -50f..50f,
                        label = "Contraste",
                        displayValue = if (params.contrast > 0) "+${(params.contrast * 100).roundToInt()}" else "${(params.contrast * 100).roundToInt()}"
                    )

                    LiquidGlassSlider(
                        value = params.shadows * 100f,
                        onValueChange = { onAdjustmentChange(null, null, it / 100f, null, null, null, null) },
                        valueRange = -50f..100f,
                        label = "Débouchage des ombres",
                        displayValue = "${(params.shadows * 100).roundToInt()}%"
                    )

                    LiquidGlassSlider(
                        value = params.highlights * 100f,
                        onValueChange = { onAdjustmentChange(null, null, null, it / 100f, null, null, null) },
                        valueRange = -100f..50f,
                        label = "Récupération des hautes lumières",
                        displayValue = "${(params.highlights * 100).roundToInt()}%"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Color & Detail Card
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = GlassSurfaceMedium
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "COULEURS & NETTETÉ",
                        color = AmberStudio,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LiquidGlassSlider(
                        value = params.vibrance * 100f,
                        onValueChange = { onAdjustmentChange(null, null, null, null, it / 100f, null, null) },
                        valueRange = -100f..100f,
                        label = "Vibrance naturelle",
                        displayValue = if (params.vibrance > 0) "+${(params.vibrance * 100).roundToInt()}" else "${(params.vibrance * 100).roundToInt()}"
                    )

                    LiquidGlassSlider(
                        value = params.warmth * 100f,
                        onValueChange = { onAdjustmentChange(null, null, null, null, null, it / 100f, null) },
                        valueRange = -50f..50f,
                        label = "Température",
                        displayValue = if (params.warmth > 0) "+${(params.warmth * 100).roundToInt()}" else "${(params.warmth * 100).roundToInt()}"
                    )

                    LiquidGlassSlider(
                        value = params.sharpness * 100f,
                        onValueChange = { onAdjustmentChange(null, null, null, null, null, null, it / 100f) },
                        valueRange = 0f..100f,
                        label = "Netteté & Micro-contraste",
                        displayValue = "${(params.sharpness * 100).roundToInt()}%"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LiquidGlassButton(
                onClick = onDismiss,
                text = "Terminé",
                modifier = Modifier.fillMaxWidth(),
                variant = LiquidGlassButtonVariant.PRIMARY
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.CyanOptical
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.GlassSurfaceMedium
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Premium Liquid Glass AI processing overlay with organic light sweeps,
 * depth refractions, and live visual feedback.
 */
@Composable
fun LiquidGlassProgress(
    message: String = "Amélioration de votre photo…",
    photoBitmap: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "processingAnim")

    val beamPosition by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beam"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val waveAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        // Background photo if available
        if (photoBitmap != null) {
            Image(
                bitmap = photoBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(16.dp)
            )
        }

        // Dark frosted layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DarkBg.copy(alpha = 0.75f),
                            DarkBg.copy(alpha = 0.85f),
                            DarkBg.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // Glass scanning light line over the whole screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val h = size.height
                    val w = size.width
                    val currentY = beamPosition * h

                    // Soft horizontal optical scan wave
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                CyanOptical.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.25f),
                                AmberStudio.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            startY = currentY - 160f,
                            endY = currentY + 160f
                        )
                    )
                }
        )

        // Center Glass Capsule Card
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .padding(24.dp),
            shape = RoundedCornerShape(32.dp),
            backgroundColor = GlassSurfaceMedium,
            hasRefractionGlow = true,
            hasIdleShine = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Liquid Glowing Aperture Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0x66FFFFFF),
                                    Color(0x2638BDF8),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.sweepGradient(
                                listOf(
                                    GlassBorderActive,
                                    AmberStudio.copy(alpha = 0.7f),
                                    CyanOptical.copy(alpha = 0.7f),
                                    GlassBorderActive
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = TextPrimary.copy(alpha = pulseGlow),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "MIKE AI",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.2.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Organic progress track in Liquid Glass
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                val w = size.width
                                val progressX = beamPosition * w
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            CyanOptical.copy(alpha = 0.6f),
                                            Color.White,
                                            AmberStudio.copy(alpha = 0.8f),
                                            Color.Transparent
                                        ),
                                        startX = progressX - 100f,
                                        endX = progressX + 100f
                                    )
                                )
                            }
                    )
                }
            }
        }
    }
}

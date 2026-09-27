package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassButtonVariant
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassIconButton
import com.example.ui.components.LiquidReveal
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun PhotoPreviewScreen(
    bitmap: Bitmap,
    imageFile: File?,
    onEnhance: () -> Unit,
    onRetake: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    LiquidReveal(trigger = true) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBg)
                .testTag("photo_preview_screen_container")
        ) {
            // Full Screen Raw Photo Display
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Photo capturée",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack,
                    contentDescription = "Retour",
                    size = 46.dp
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${bitmap.width} × ${bitmap.height}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Bottom Actions Card
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    backgroundColor = Color(0x59080C14),
                    hasIdleShine = true
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Photo capturée avec succès",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Prête pour le traitement photographique MIKE AI",
                            color = TextMuted,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Retake button
                            LiquidGlassButton(
                                onClick = onRetake,
                                text = "Reprendre",
                                icon = Icons.Default.CameraAlt,
                                variant = LiquidGlassButtonVariant.SECONDARY,
                                shape = RoundedCornerShape(26.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("preview_retake_button")
                            )

                            // Enhance button (Hero action)
                            LiquidGlassButton(
                                onClick = onEnhance,
                                text = "Améliorer",
                                icon = Icons.Default.AutoAwesome,
                                variant = LiquidGlassButtonVariant.PRIMARY,
                                shape = RoundedCornerShape(26.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("preview_enhance_button")
                            )
                        }
                    }
                }
            }
        }
    }
}

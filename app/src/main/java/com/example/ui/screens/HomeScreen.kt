package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ProcessedPhoto
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassButtonVariant
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassIconButton
import com.example.ui.components.animatedGlassBorder
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.GlassBorderBottom
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.GlassSurfaceMedium
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    photos: List<ProcessedPhoto>,
    onTakePhotoClick: () -> Unit,
    onImportPhotoClick: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenPhoto: (ProcessedPhoto) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestPhoto = photos.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 110.dp)
            .testTag("home_screen_container"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Top Header: MIKE AI + STUDIO
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MIKE AI",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AmberStudio.copy(alpha = 0.2f))
                            .border(1.dp, AmberStudio.copy(alpha = 0.5f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PHOTO STUDIO",
                            color = AmberStudio,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
                Text(
                    text = "Photographie naturelle de précision",
                    color = TextMuted,
                    fontSize = 12.sp,
                    letterSpacing = 0.3.sp
                )
            }

            LiquidGlassIconButton(
                icon = Icons.Default.Collections,
                onClick = onOpenGallery,
                contentDescription = "Mes photos",
                size = 44.dp,
                iconSize = 20.dp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Center: Large Liquid Glass Preview Card
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .aspectRatio(0.92f)
                .animatedGlassBorder(shape = RoundedCornerShape(32.dp), isActive = latestPhoto != null),
            shape = RoundedCornerShape(32.dp),
            backgroundColor = GlassSurfaceLight,
            hasRefractionGlow = true,
            hasIdleShine = true,
            onClick = {
                if (latestPhoto != null) {
                    onOpenPhoto(latestPhoto)
                } else {
                    onTakePhotoClick()
                }
            }
        ) {
            if (latestPhoto != null && File(latestPhoto.processedPath).exists()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = File(latestPhoto.processedPath),
                        contentDescription = "Dernière photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Bottom glass card info badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0x99000000),
                                        Color(0xCC000000)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = AmberStudio,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = latestPhoto.presetName,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = formatTime(latestPhoto.timestamp),
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                                    .border(1.dp, Color(0x66FFFFFF), CircleShape)
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Ouvrir",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else {
                // Empty state: "Votre prochaine photo commence ici"
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x33FFFFFF),
                                        Color(0x1038BDF8),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.dp, GlassBorderTop, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = TextPrimary.copy(alpha = 0.85f),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Votre prochaine photo\ncommence ici",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 24.sp,
                        letterSpacing = 0.4.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Touchez pour capturer en haute précision",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Bottom Actions: Main Hero Button & Secondary Import Button
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero Action: "Prendre une photo" (Liquid Glass Hero with Breathing & Sheen)
            LiquidGlassButton(
                onClick = onTakePhotoClick,
                text = "Prendre une photo",
                icon = Icons.Default.CameraAlt,
                variant = LiquidGlassButtonVariant.PRIMARY,
                isHero = true,
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animatedGlassBorder(shape = RoundedCornerShape(32.dp), isActive = true)
                    .testTag("take_photo_hero_button")
            )

            // Secondary Action: "Importer une photo"
            LiquidGlassButton(
                onClick = onImportPhotoClick,
                text = "Importer une photo",
                icon = Icons.Default.AddPhotoAlternate,
                variant = LiquidGlassButtonVariant.SECONDARY,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_photo_button")
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

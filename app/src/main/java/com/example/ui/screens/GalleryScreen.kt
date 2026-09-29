package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ProcessedPhoto
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassButtonVariant
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassIconButton
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.GlassSurfaceMedium
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.GalleryFilter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun GalleryScreen(
    photos: List<ProcessedPhoto>,
    favoritePhotos: List<ProcessedPhoto>,
    recentPhotos: List<ProcessedPhoto>,
    currentFilter: GalleryFilter,
    onFilterChange: (GalleryFilter) -> Unit,
    onBack: () -> Unit,
    onOpenPhoto: (ProcessedPhoto) -> Unit,
    onToggleFavorite: (ProcessedPhoto) -> Unit,
    onDeletePhoto: (ProcessedPhoto) -> Unit,
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    var photoToDelete by remember { mutableStateOf<ProcessedPhoto?>(null) }

    val displayedPhotos = when (currentFilter) {
        GalleryFilter.ALL -> photos
        GalleryFilter.FAVORITES -> favoritePhotos
        GalleryFilter.RECENTS -> recentPhotos
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .testTag("gallery_screen_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiquidGlassIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBack,
                        contentDescription = "Retour",
                        size = 44.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Mes photos",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "${displayedPhotos.size} photo${if (displayedPhotos.size > 1) "s" else ""}",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                LiquidGlassIconButton(
                    icon = Icons.Default.CameraAlt,
                    onClick = onTakePhoto,
                    contentDescription = "Prendre une photo",
                    size = 44.dp
                )
            }

            // Filter Tabs: Toutes, Favoris, Récentes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    GalleryFilter.ALL to "Toutes",
                    GalleryFilter.FAVORITES to "Favoris",
                    GalleryFilter.RECENTS to "Récentes"
                ).forEach { (filter, title) ->
                    val isSelected = filter == currentFilter
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
                                onClick = { onFilterChange(filter) }
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Photos Grid
            if (displayedPhotos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(0.88f),
                        shape = RoundedCornerShape(32.dp),
                        backgroundColor = GlassSurfaceLight,
                        hasIdleShine = true
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x26FFFFFF))
                                    .border(1.dp, GlassBorderTop, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Collections,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (currentFilter == GalleryFilter.FAVORITES) "Aucun favori" else "Aucune photo",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (currentFilter == GalleryFilter.FAVORITES) "Ajoutez des photos en favoris pour les retrouver ici" else "Prenez votre première photo pour la voir ici",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            LiquidGlassButton(
                                onClick = onTakePhoto,
                                text = "Prendre une photo",
                                icon = Icons.Default.CameraAlt,
                                variant = LiquidGlassButtonVariant.PRIMARY
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(displayedPhotos, key = { it.id }) { photo ->
                        GalleryPhotoCard(
                            photo = photo,
                            onClick = { onOpenPhoto(photo) },
                            onToggleFavorite = { onToggleFavorite(photo) },
                            onDelete = { photoToDelete = photo }
                        )
                    }
                }
            }
        }

        // Delete Dialog
        photoToDelete?.let { targetPhoto ->
            BasicAlertDialog(
                onDismissRequest = { photoToDelete = null }
            ) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    backgroundColor = Color(0xEB0D121D)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Supprimer cette photo ?",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cette action supprimera définitivement la photo de l'historique.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LiquidGlassButton(
                                onClick = { photoToDelete = null },
                                text = "Annuler",
                                variant = LiquidGlassButtonVariant.SUBTLE,
                                modifier = Modifier.weight(1f)
                            )
                            LiquidGlassButton(
                                onClick = {
                                    onDeletePhoto(targetPhoto)
                                    photoToDelete = null
                                },
                                text = "Supprimer",
                                variant = LiquidGlassButtonVariant.PRIMARY,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryPhotoCard(
    photo: ProcessedPhoto,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val file = File(photo.processedPath)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(24.dp))
            .background(GlassSurfaceLight)
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        GlassBorderTop.copy(alpha = 0.4f),
                        Color(0x14FFFFFF)
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
            .testTag("gallery_item_${photo.id}")
    ) {
        if (file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = "Photo traitée",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // If the item represents a Video capture (Cinematic, SlowMo, Timelapse, Video), show a beautiful play button overlay
        val isVideo = photo.isVideo || photo.presetName in listOf("CINÉMA", "RALENTI", "ACCÉLÉRÉ", "VIDÉO") || photo.sceneType.startsWith("Vidéo")
        if (isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .border(1.2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Lire la vidéo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Duration badge at bottom-left, right above the date row
            val durationText = if (photo.videoDurationSeconds > 0) {
                val mins = photo.videoDurationSeconds / 60
                val secs = photo.videoDurationSeconds % 60
                String.format(Locale.US, "%02d:%02d", mins, secs)
            } else {
                "00:05" // Fallback duration
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x80000000))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = durationText,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Overlay Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x33000000),
                        0.5f to Color.Transparent,
                        1.0f to Color(0xCC000000)
                    )
                )
        )

        // Preset badge on top-left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(CircleShape)
                .background(Color(0x66000000))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AmberStudio,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = photo.presetName,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Top right favorite heart button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(if (photo.isFavorite) Color(0x66F43F5E) else Color(0x66000000))
                .clickable { onToggleFavorite() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (photo.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favori",
                tint = if (photo.isFavorite) Color(0xFFF43F5E) else Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // Date and quick delete at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatPhotoDate(photo.timestamp),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun formatPhotoDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

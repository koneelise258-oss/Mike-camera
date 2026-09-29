package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import android.widget.MediaController
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.EnhancementParams
import com.example.engine.EnhancementPreset
import com.example.engine.WallpaperTarget
import com.example.ui.components.BeforeAfterSlider
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassButtonVariant
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassIconButton
import com.example.ui.components.LiquidGlassSlider
import com.example.ui.components.LiquidGlassToast
import com.example.ui.components.LiquidReveal
import com.example.ui.components.PhotoAdjustSheet
import com.example.ui.components.PresetSelector
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.GlassSurfaceMedium
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    originalBitmap: Bitmap,
    processedBitmap: Bitmap,
    currentParams: EnhancementParams,
    videoFile: java.io.File? = null,
    isProcessing: Boolean,
    isSavedToGallery: Boolean,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onSaveToGallery: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetAsWallpaper: (WallpaperTarget) -> Unit,
    onPrint: () -> Unit,
    onDelete: () -> Unit,
    onRetake: () -> Unit,
    onPresetSelected: (EnhancementPreset) -> Unit,
    onAiIntensityChange: (Float) -> Unit,
    onAdjustmentsChange: (
        exposure: Float?,
        contrast: Float?,
        shadows: Float?,
        highlights: Float?,
        vibrance: Float?,
        warmth: Float?,
        sharpness: Float?
    ) -> Unit,
    onResetAdjustments: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var showAdjustSheet by remember { mutableStateOf(false) }
    var showSaveToast by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val heartScale = remember { Animatable(1f) }

    BackHandler {
        onBack()
    }

    LaunchedEffect(isSavedToGallery) {
        if (isSavedToGallery) {
            showSaveToast = true
            delay(2600)
            showSaveToast = false
        }
    }

    LiquidReveal(trigger = true) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBg)
                .testTag("comparison_screen_container")
        ) {
            // 1. Full Screen Interactive Image Comparator (ORIGINAL ↔ MIKE AI)
            BeforeAfterSlider(
                originalBitmap = originalBitmap,
                processedBitmap = processedBitmap,
                modifier = Modifier.fillMaxSize()
            )

            // If it's a recorded video, display a centered play button overlay and handle in-app playback dialog
            if (currentParams.isVideo) {
                var isVideoPlaying by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                        .clickable { isVideoPlaying = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Lire la vidéo",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                if (isVideoPlaying && videoFile != null && videoFile.exists()) {
                    BasicAlertDialog(
                        onDismissRequest = { isVideoPlaying = false }
                    ) {
                        LiquidGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            backgroundColor = Color.Black
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AndroidView(
                                    factory = { ctx ->
                                        VideoView(ctx).apply {
                                            setVideoPath(videoFile.absolutePath)
                                            val mediaController = MediaController(ctx)
                                            mediaController.setAnchorView(this)
                                            setMediaController(mediaController)
                                            setOnPreparedListener { mp ->
                                                mp.isLooping = true
                                                start()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // 2. Discrete Liquid Glass Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                LiquidGlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack,
                    contentDescription = "Retour",
                    size = 46.dp
                )

                // Title Pill with current Preset & Intensity
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x73000000))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${currentParams.preset.title} • ${(currentParams.aiIntensity * 100).roundToInt()}%",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Favorite Heart Button with liquid spring
                    Box(
                        modifier = Modifier
                            .scale(heartScale.value)
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isFavorite) Color(0x33F43F5E) else Color(0x33FFFFFF))
                            .border(
                                1.dp,
                                if (isFavorite) Color(0x80F43F5E) else GlassBorderTop,
                                CircleShape
                            )
                            .clickable {
                                coroutineScope.launch {
                                    heartScale.animateTo(1.35f, tween(120, easing = FastOutSlowInEasing))
                                    heartScale.animateTo(1.0f, spring(dampingRatio = 0.5f))
                                }
                                onToggleFavorite()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (isFavorite) Color(0xFFF43F5E) else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // More Menu Button (Wallpaper, Print, Delete)
                    Box {
                        LiquidGlassIconButton(
                            icon = Icons.Default.MoreVert,
                            onClick = { showMoreMenu = true },
                            contentDescription = "Plus d'options",
                            size = 46.dp
                        )

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(DarkSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Définir comme fond d'écran", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Wallpaper, null, tint = TextSecondary) },
                                onClick = {
                                    showMoreMenu = false
                                    showWallpaperDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Imprimer", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Print, null, tint = TextSecondary) },
                                onClick = {
                                    showMoreMenu = false
                                    onPrint()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Supprimer", color = Color(0xFFEF4444)) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444)) },
                                onClick = {
                                    showMoreMenu = false
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                }
            }

            // 3. Processing Indicator if user adjusts in real-time
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .padding(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 4. Floating Bottom Control Stack
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // AI Intensity Slider Capsule
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = Color(0x66080C14)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        LiquidGlassSlider(
                            value = currentParams.aiIntensity * 100f,
                            onValueChange = { onAiIntensityChange(it / 100f) },
                            valueRange = 0f..100f,
                            label = "Intensité AI",
                            displayValue = "${(currentParams.aiIntensity * 100).roundToInt()}%"
                        )
                    }
                }

                // Presets Carousel
                PresetSelector(
                    selectedPreset = currentParams.preset,
                    onPresetSelected = onPresetSelected
                )

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Retake photo button
                    LiquidGlassIconButton(
                        icon = Icons.Default.CameraAlt,
                        onClick = onRetake,
                        contentDescription = "Reprendre",
                        size = 54.dp,
                        iconSize = 22.dp
                    )

                    // Manual Tune adjustments button
                    LiquidGlassIconButton(
                        icon = Icons.Default.Tune,
                        onClick = { showAdjustSheet = true },
                        contentDescription = "Ajustements",
                        size = 54.dp,
                        iconSize = 22.dp
                    )

                    // Save to gallery button (Hero Action)
                    LiquidGlassButton(
                        onClick = onSaveToGallery,
                        icon = if (isSavedToGallery) Icons.Default.Check else Icons.Default.Download,
                        text = if (isSavedToGallery) "Enregistré" else "Enregistrer",
                        variant = if (isSavedToGallery) LiquidGlassButtonVariant.SECONDARY else LiquidGlassButtonVariant.PRIMARY,
                        shape = RoundedCornerShape(28.dp),
                        height = 54.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Toast Confirmation Overlay
            LiquidGlassToast(
                visible = showSaveToast,
                message = "Photo enregistrée dans la galerie",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 70.dp)
            )

            // 6. Manual Adjustments Sheet
            if (showAdjustSheet) {
                PhotoAdjustSheet(
                    params = currentParams,
                    onDismiss = { showAdjustSheet = false },
                    onAdjustmentChange = onAdjustmentsChange,
                    onReset = onResetAdjustments
                )
            }

            // 7. Wallpaper Target Selection Dialog
            if (showWallpaperDialog) {
                BasicAlertDialog(
                    onDismissRequest = { showWallpaperDialog = false }
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
                                text = "Définir comme fond d'écran",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                LiquidGlassButton(
                                    onClick = {
                                        showWallpaperDialog = false
                                        onSetAsWallpaper(WallpaperTarget.HOME_SCREEN)
                                    },
                                    text = "Écran d'accueil",
                                    variant = LiquidGlassButtonVariant.SECONDARY,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                LiquidGlassButton(
                                    onClick = {
                                        showWallpaperDialog = false
                                        onSetAsWallpaper(WallpaperTarget.LOCK_SCREEN)
                                    },
                                    text = "Écran de verrouillage",
                                    variant = LiquidGlassButtonVariant.SECONDARY,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                LiquidGlassButton(
                                    onClick = {
                                        showWallpaperDialog = false
                                        onSetAsWallpaper(WallpaperTarget.BOTH)
                                    },
                                    text = "Les deux écrans",
                                    variant = LiquidGlassButtonVariant.PRIMARY,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // 8. Delete Confirmation Dialog
            if (showDeleteConfirm) {
                BasicAlertDialog(
                    onDismissRequest = { showDeleteConfirm = false }
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
                                text = "Cette photo sera retirée de l'historique MIKE AI.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LiquidGlassButton(
                                    onClick = { showDeleteConfirm = false },
                                    text = "Annuler",
                                    variant = LiquidGlassButtonVariant.SUBTLE,
                                    modifier = Modifier.weight(1f)
                                )
                                LiquidGlassButton(
                                    onClick = {
                                        showDeleteConfirm = false
                                        onDelete()
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
}

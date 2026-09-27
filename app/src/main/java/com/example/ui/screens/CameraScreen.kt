package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.camera.CameraAspectRatio
import com.example.camera.CameraFlashMode
import com.example.camera.CameraGridType
import com.example.camera.CameraHaptics
import com.example.camera.CameraShootingMode
import com.example.camera.CameraTimer
import com.example.camera.rememberDeviceRollAngle
import com.example.data.model.ProcessedPhoto
import com.example.engine.MediaManager
import com.example.ui.components.CameraFocusTarget
import com.example.ui.components.CameraGridOverlay
import com.example.ui.components.CameraLevelIndicator
import com.example.ui.components.CameraShutterFlash
import com.example.ui.components.CameraTimerCountdown
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassIconButton
import com.example.ui.components.LiquidGlassShutterButton
import com.example.ui.components.LiquidGlassSlider
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun CameraScreen(
    latestPhoto: ProcessedPhoto?,
    onPhotoCaptured: (File, CameraShootingMode) -> Unit,
    onOpenGallery: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Camera Hardware state
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var hasFrontCamera by remember { mutableStateOf(true) }
    var hasFlashUnit by remember { mutableStateOf(true) }

    // Controls and user preferences
    var shootingMode by remember { mutableStateOf(CameraShootingMode.PHOTO) }
    var flashMode by remember { mutableStateOf(CameraFlashMode.AUTO) }
    var timerSetting by remember { mutableStateOf(CameraTimer.OFF) }
    var gridType by remember { mutableStateOf(CameraGridType.RULE_OF_THIRDS) }
    var aspectRatio by remember { mutableStateOf(CameraAspectRatio.RATIO_4_3) }
    var isLevelIndicatorVisible by remember { mutableStateOf(true) }
    var showProControls by remember { mutableStateOf(false) }

    // Pro Exposure Compensation
    var exposureCompensationIndex by remember { mutableIntStateOf(0) }
    var minExposureIndex by remember { mutableIntStateOf(-4) }
    var maxExposureIndex by remember { mutableIntStateOf(4) }
    var exposureStep by remember { mutableFloatStateOf(0.5f) }

    // Zoom state
    var currentZoomRatio by remember { mutableFloatStateOf(1.0f) }
    var minZoomRatio by remember { mutableFloatStateOf(1.0f) }
    var maxZoomRatio by remember { mutableFloatStateOf(8.0f) }

    // Dynamic camera instances
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Overlays state
    var tapFocusPoint by remember { mutableStateOf<Offset?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var triggerFlashAnimation by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(0) }
    var timerJob by remember { mutableStateOf<Job?>(null) }

    // Device Tilt Roll Sensor for Virtual Level
    val deviceRollAngle by rememberDeviceRollAngle(enabled = isLevelIndicatorVisible)

    // Flip animation for lens switch
    var flipRotation by remember { mutableFloatStateOf(0f) }
    val animatedFlipRotation by animateFloatAsState(
        targetValue = flipRotation,
        animationSpec = tween(380),
        label = "flipCameraRotation"
    )

    // Check available cameras once on load
    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        hasFrontCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
    }

    // Function to perform the actual capture after optional countdown
    val executePhotoCapture: () -> Unit = {
        if (!isCapturing) {
            val capture = imageCapture
            if (capture != null) {
                isCapturing = true
                CameraHaptics.playShutterTick(context)
                triggerFlashAnimation = true

                val (photoFile, _) = MediaManager.createTempCaptureFile(context)

                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            isCapturing = false
                            onPhotoCaptured(photoFile, shootingMode)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            isCapturing = false
                            android.widget.Toast.makeText(
                                context,
                                "Erreur de capture: ${exception.localizedMessage}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }
        }
    }

    // Shutter button clicked (handles countdown if timer is set)
    val onShutterClick: () -> Unit = {
        if (!isCapturing) {
            if (timerSetting != CameraTimer.OFF && countdownSeconds == 0) {
                timerJob?.cancel()
                timerJob = coroutineScope.launch {
                    var remaining = timerSetting.seconds
                    while (remaining > 0) {
                        countdownSeconds = remaining
                        CameraHaptics.playLightTick(context)
                        delay(1000)
                        remaining--
                    }
                    countdownSeconds = 0
                    executePhotoCapture()
                }
            } else {
                executePhotoCapture()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .testTag("camera_screen_container")
    ) {
        // 1. Full Screen CameraX Preview
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(aspectRatio) {
                    // Pinch-to-zoom gesture
                    detectTransformGestures { _, _, zoom, _ ->
                        val targetZoom = (currentZoomRatio * zoom).coerceIn(minZoomRatio, maxZoomRatio)
                        currentZoomRatio = targetZoom
                        cameraControl?.setZoomRatio(targetZoom)
                    }
                }
                .pointerInput(aspectRatio) {
                    // Tap-to-focus & metering gesture
                    detectTapGestures { offset ->
                        tapFocusPoint = offset
                        val pView = previewView ?: return@detectTapGestures
                        val factory = SurfaceOrientedMeteringPointFactory(
                            pView.width.toFloat(),
                            pView.height.toFloat()
                        )
                        val point = factory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                            .build()
                        cameraControl?.startFocusAndMetering(action)
                        CameraHaptics.playLightTick(context)
                    }
                }
        ) {
            // AndroidView CameraX View
            AndroidView(
                factory = { ctx ->
                    val frameLayout = FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val pView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    frameLayout.addView(pView)
                    previewView = pView

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()
                            .setTargetAspectRatio(aspectRatio.ratioValue)
                            .build()
                            .also {
                                it.surfaceProvider = pView.surfaceProvider
                            }

                        val capture = ImageCapture.Builder()
                            .setTargetAspectRatio(aspectRatio.ratioValue)
                            .setCaptureMode(
                                if (shootingMode == CameraShootingMode.NIGHT) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                                else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                            )
                            .setFlashMode(
                                if (flashMode == CameraFlashMode.TORCH) ImageCapture.FLASH_MODE_OFF
                                else flashMode.modeValue
                            )
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )

                            cameraControl = cam.cameraControl
                            cameraInfo = cam.cameraInfo

                            // Update hardware capabilities
                            hasFlashUnit = cam.cameraInfo.hasFlashUnit()
                            val zoomState = cam.cameraInfo.zoomState.value
                            if (zoomState != null) {
                                minZoomRatio = zoomState.minZoomRatio
                                maxZoomRatio = zoomState.maxZoomRatio.coerceAtMost(10f)
                                currentZoomRatio = zoomState.zoomRatio
                            }

                            val expState = cam.cameraInfo.exposureState
                            minExposureIndex = expState.exposureCompensationRange.lower
                            maxExposureIndex = expState.exposureCompensationRange.upper
                            exposureStep = expState.exposureCompensationStep.toFloat()
                            exposureCompensationIndex = expState.exposureCompensationIndex

                            // Enable torch if selected
                            if (flashMode == CameraFlashMode.TORCH && hasFlashUnit) {
                                cam.cameraControl.enableTorch(true)
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                ctx,
                                "Erreur d'initialisation caméra: ${e.localizedMessage}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    frameLayout
                },
                update = {
                    // Update flash mode on capture instance if changed
                    imageCapture?.flashMode = if (flashMode == CameraFlashMode.TORCH) {
                        ImageCapture.FLASH_MODE_OFF
                    } else {
                        flashMode.modeValue
                    }
                    if (hasFlashUnit) {
                        cameraControl?.enableTorch(flashMode == CameraFlashMode.TORCH)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 1:1 Aspect ratio square crop visual mask if 1:1 selected
            if (aspectRatio == CameraAspectRatio.RATIO_1_1) {
                val availableWidth = maxWidth
                val topBottomPad = (maxHeight - availableWidth) / 2
                if (topBottomPad > 0.dp) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(topBottomPad)
                                .background(Color(0xCC000000))
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(topBottomPad)
                                .background(Color(0xCC000000))
                        )
                    }
                }
            }

            // Grid Overlay (Rule of Thirds or Golden Ratio)
            CameraGridOverlay(gridType = gridType)

            // Horizon / Level Virtual Indicator
            CameraLevelIndicator(
                rollAngle = deviceRollAngle,
                visible = isLevelIndicatorVisible
            )

            // Animated Focus & Metering Target
            CameraFocusTarget(offset = tapFocusPoint)

            // White Flash Transition Animation
            CameraShutterFlash(
                trigger = triggerFlashAnimation,
                onFlashComplete = { triggerFlashAnimation = false }
            )

            // Timer Countdown Overlay
            CameraTimerCountdown(countdownSeconds = countdownSeconds)
        }

        // 2. Top Bar: Floating Liquid Glass Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            LiquidGlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBack,
                contentDescription = "Retour",
                size = 42.dp,
                iconSize = 20.dp
            )

            // Top Quick Settings Pill
            LiquidGlassCard(
                modifier = Modifier.clip(CircleShape),
                shape = CircleShape,
                backgroundColor = Color(0x66080C14)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flash Mode Toggle
                    CameraTopPillButton(
                        icon = when (flashMode) {
                            CameraFlashMode.AUTO -> Icons.Default.FlashAuto
                            CameraFlashMode.ON -> Icons.Default.FlashOn
                            CameraFlashMode.OFF -> Icons.Default.FlashOff
                            CameraFlashMode.TORCH -> Icons.Default.Highlight
                        },
                        label = flashMode.label,
                        isActive = flashMode != CameraFlashMode.OFF,
                        onClick = {
                            flashMode = when (flashMode) {
                                CameraFlashMode.AUTO -> CameraFlashMode.ON
                                CameraFlashMode.ON -> if (hasFlashUnit) CameraFlashMode.TORCH else CameraFlashMode.OFF
                                CameraFlashMode.TORCH -> CameraFlashMode.OFF
                                CameraFlashMode.OFF -> CameraFlashMode.AUTO
                            }
                            CameraHaptics.playLightTick(context)
                        }
                    )

                    // Timer Toggle
                    CameraTopPillButton(
                        icon = Icons.Default.Timer,
                        label = timerSetting.label,
                        isActive = timerSetting != CameraTimer.OFF,
                        onClick = {
                            timerSetting = when (timerSetting) {
                                CameraTimer.OFF -> CameraTimer.SEC_3
                                CameraTimer.SEC_3 -> CameraTimer.SEC_10
                                CameraTimer.SEC_10 -> CameraTimer.OFF
                            }
                            CameraHaptics.playLightTick(context)
                        }
                    )

                    // Aspect Ratio Toggle
                    CameraTopPillButton(
                        icon = Icons.Default.AspectRatio,
                        label = aspectRatio.label,
                        isActive = aspectRatio != CameraAspectRatio.RATIO_4_3,
                        onClick = {
                            aspectRatio = when (aspectRatio) {
                                CameraAspectRatio.RATIO_4_3 -> CameraAspectRatio.RATIO_16_9
                                CameraAspectRatio.RATIO_16_9 -> CameraAspectRatio.RATIO_1_1
                                CameraAspectRatio.RATIO_1_1 -> CameraAspectRatio.RATIO_4_3
                            }
                            CameraHaptics.playLightTick(context)
                        }
                    )

                    // Grid Toggle
                    CameraTopPillButton(
                        icon = Icons.Default.GridOn,
                        label = gridType.label,
                        isActive = gridType != CameraGridType.NONE,
                        onClick = {
                            gridType = when (gridType) {
                                CameraGridType.NONE -> CameraGridType.RULE_OF_THIRDS
                                CameraGridType.RULE_OF_THIRDS -> CameraGridType.GOLDEN_RATIO
                                CameraGridType.GOLDEN_RATIO -> CameraGridType.NONE
                            }
                            CameraHaptics.playLightTick(context)
                        }
                    )
                }
            }

            // Level toggle / Pro controls button
            LiquidGlassIconButton(
                icon = Icons.Default.Tune,
                onClick = {
                    showProControls = !showProControls
                    CameraHaptics.playLightTick(context)
                },
                contentDescription = "Contrôles avancés",
                size = 42.dp,
                iconSize = 20.dp,
                tint = if (showProControls) AmberStudio else TextPrimary
            )
        }

        // 3. Pro Controls Floating Slider (when active)
        AnimatedVisibility(
            visible = showProControls,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 62.dp, start = 20.dp, end = 20.dp)
        ) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = Color(0xCC080C14)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exposition (EV)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        val evVal = String.format(Locale.US, "%+.1f EV", exposureCompensationIndex * exposureStep)
                        Text(
                            text = evVal,
                            color = AmberStudio,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LiquidGlassSlider(
                        value = exposureCompensationIndex.toFloat(),
                        onValueChange = { newVal ->
                            val rounded = newVal.toInt()
                            exposureCompensationIndex = rounded
                            cameraControl?.setExposureCompensationIndex(rounded)
                        },
                        valueRange = minExposureIndex.toFloat()..maxExposureIndex.toFloat(),
                        label = "EV",
                        displayValue = String.format(Locale.US, "%+.1f", exposureCompensationIndex * exposureStep)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Niveau horizon virtuel",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isLevelIndicatorVisible) AmberStudio.copy(alpha = 0.2f) else Color(0x22FFFFFF))
                                .border(1.dp, if (isLevelIndicatorVisible) AmberStudio else Color(0x33FFFFFF), CircleShape)
                                .clickable {
                                    isLevelIndicatorVisible = !isLevelIndicatorVisible
                                    CameraHaptics.playLightTick(context)
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isLevelIndicatorVisible) "Activé" else "Désactivé",
                                color = if (isLevelIndicatorVisible) AmberStudio else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 4. Bottom Controls Complex (Zoom Steps + Shooting Modes + Shutter + Lens switch)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom Selector Buttons (0.5x, 1x, 2x, 5x depending on capabilities)
            val zoomSteps = remember(minZoomRatio, maxZoomRatio) {
                buildList {
                    if (minZoomRatio <= 0.7f) add(0.5f)
                    add(1.0f)
                    if (maxZoomRatio >= 1.9f) add(2.0f)
                    if (maxZoomRatio >= 4.5f) add(5.0f)
                }
            }

            LiquidGlassCard(
                modifier = Modifier.clip(CircleShape),
                shape = CircleShape,
                backgroundColor = Color(0x88080C14)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    zoomSteps.forEach { step ->
                        val isSelected = kotlin.math.abs(currentZoomRatio - step) < 0.25f
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) AmberStudio
                                    else Color.Transparent
                                )
                                .clickable {
                                    currentZoomRatio = step
                                    cameraControl?.setZoomRatio(step)
                                    CameraHaptics.playLightTick(context)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val label = if (step == 0.5f) ".5" else "${step.toInt()}"
                            Text(
                                text = "${label}×",
                                color = if (isSelected) DarkBg else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Shooting Modes Carousel (PHOTO, PORTRAIT, NUIT, PRO)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CameraShootingMode.entries.forEach { mode ->
                    val isSelected = mode == shootingMode
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                shootingMode = mode
                                CameraHaptics.playLightTick(context)
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.title,
                            color = if (isSelected) AmberStudio else TextMuted,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Shutter Row: Gallery Thumb | Shutter Button | Lens Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Gallery Thumbnail / Shortcut
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x55080C14))
                        .border(1.5.dp, Color(0x44FFFFFF), CircleShape)
                        .clickable { onOpenGallery() },
                    contentAlignment = Alignment.Center
                ) {
                    if (latestPhoto != null && File(latestPhoto.processedPath).exists()) {
                        AsyncImage(
                            model = File(latestPhoto.processedPath),
                            contentDescription = "Galerie",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = "Galerie",
                            tint = TextPrimary.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Center: Liquid Glass Shutter Button
                LiquidGlassShutterButton(
                    onClick = onShutterClick,
                    isCapturing = isCapturing
                )

                // Right: Lens Switch (Rear <-> Front)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .rotate(animatedFlipRotation)
                        .clip(CircleShape)
                        .background(Color(0x55080C14))
                        .border(1.5.dp, Color(0x44FFFFFF), CircleShape)
                        .clickable(enabled = hasFrontCamera) {
                            flipRotation += 180f
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                            CameraHaptics.playLightTick(context)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Changer de caméra",
                        tint = if (hasFrontCamera) TextPrimary else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraTopPillButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isActive) AmberStudio.copy(alpha = 0.22f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) AmberStudio else TextPrimary.copy(alpha = 0.85f),
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            color = if (isActive) AmberStudio else TextPrimary.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

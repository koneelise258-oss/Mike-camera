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
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import com.example.ui.components.CameraActiveBubble
import com.example.ui.components.CameraFluidBubbleMenu
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
import androidx.compose.ui.draw.scale
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
import com.example.engine.EnhancementParams
import com.example.engine.EnhancementPreset
import com.example.engine.MediaManager
import com.example.engine.MikeAIEngine
import com.example.engine.MikeImageAnalyzer
import com.example.engine.SceneType
import com.example.ui.components.CameraCinematicControlsDeck
import com.example.ui.components.CameraCinematicLetterboxOverlay
import com.example.ui.components.CameraFocusTarget
import com.example.ui.components.CameraGridOverlay
import com.example.ui.components.CameraHistogramOverlay
import com.example.ui.components.CameraLevelIndicator
import com.example.ui.components.CameraModeSwitchOverlay
import com.example.ui.components.CameraPanoramaControlsDeck
import com.example.ui.components.CameraPortraitControlsDeck
import com.example.ui.components.CameraPortraitGuideOverlay
import com.example.ui.components.CameraProControlsDeck
import com.example.ui.components.CameraProHudBar
import com.example.ui.components.CameraShutterFlash
import com.example.ui.components.CameraSlowMotionControlsDeck
import com.example.ui.components.CameraTimeLapseControlsDeck
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

@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun CameraScreen(
    latestPhoto: ProcessedPhoto?,
    onPhotoCaptured: (File, CameraShootingMode, EnhancementParams) -> Unit,
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
    var activeBubble by remember { mutableStateOf(CameraActiveBubble.NONE) }

    // Pro Exposure Compensation
    var exposureCompensationIndex by remember { mutableIntStateOf(0) }
    var minExposureIndex by remember { mutableIntStateOf(-4) }
    var maxExposureIndex by remember { mutableIntStateOf(4) }
    var exposureStep by remember { mutableFloatStateOf(0.5f) }

    // Pro Mode Manual Settings
    var proIso by remember { mutableStateOf("Auto") }
    var proShutterSpeed by remember { mutableStateOf("Auto") }
    var proKelvin by remember { mutableIntStateOf(5500) }
    var proFocusManualValue by remember { mutableFloatStateOf(0.5f) }
    var isManualFocusActive by remember { mutableStateOf(false) }
    var proMeteringMode by remember { mutableStateOf("Matrice") }
    var isRawCapture by remember { mutableStateOf(false) }

    // Portrait Mode Manual Settings
    var portraitAperture by remember { mutableStateOf("f/2.8") }
    var portraitLighting by remember { mutableStateOf("Naturel") }
    var portraitSkinSmoothing by remember { mutableFloatStateOf(35f) }
    var portraitWarmth by remember { mutableFloatStateOf(25f) }

    // Panorama Mode State
    var isPanoramaCapturing by remember { mutableStateOf(false) }
    var panoramaProgress by remember { mutableFloatStateOf(0f) }
    var panoramaDirectionLeftToRight by remember { mutableStateOf(true) }

    // Slow Motion Mode State
    var slowMotionFps by remember { mutableStateOf("120 FPS") }

    // Time-Lapse Mode State
    var timeLapseInterval by remember { mutableStateOf("5s") }

    // Cinematic Mode State
    var cinematicColorProfile by remember { mutableStateOf("LUT Or") }

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

    // Background Frame Analysis executor (Offline Local Live Diagnosis)
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val isAnalyzingFrame = remember { AtomicBoolean(false) }
    var lastAnalysisTime by remember { mutableStateOf(0L) }
    var liveDetectedScene by remember { mutableStateOf(SceneType.GENERAL) }
    var sceneHistory by remember { mutableStateOf(listOf<SceneType>()) }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    // Device Tilt Roll Sensor for Virtual Level
    val deviceRollAngle by rememberDeviceRollAngle(enabled = isLevelIndicatorVisible)

    // Flip animation for lens switch
    var flipRotation by remember { mutableFloatStateOf(0f) }
    val animatedFlipRotation by animateFloatAsState(
        targetValue = flipRotation,
        animationSpec = tween(380),
        label = "flipCameraRotation"
    )

    // Mode Transition & Pro State
    var isSwitchingMode by remember { mutableStateOf(false) }
    var selectedProParam by remember { mutableStateOf("ISO") }

    LaunchedEffect(shootingMode) {
        isSwitchingMode = true
        CameraHaptics.playLightTick(context)
        delay(380)
        isSwitchingMode = false
    }

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

                val currentModeParams = when (shootingMode) {
                    CameraShootingMode.PRO -> {
                        val kelvinWarmth = ((proKelvin - 5500) / 4000f).coerceIn(-0.35f, 0.35f)
                        val evExp = (exposureCompensationIndex * exposureStep) * 0.15f
                        EnhancementParams(
                            preset = EnhancementPreset.NATURAL,
                            exposure = evExp,
                            warmth = kelvinWarmth,
                            proIso = proIso,
                            proShutterSpeed = proShutterSpeed,
                            proWhiteBalance = "${proKelvin}K"
                        )
                    }
                    CameraShootingMode.PORTRAIT -> {
                        val apertureFloat = when (portraitAperture) {
                            "f/1.4" -> 1.4f
                            "f/2.0" -> 2.0f
                            "f/2.8" -> 2.8f
                            "f/4.0" -> 4.0f
                            "f/8.0" -> 8.0f
                            else -> 16.0f
                        }
                        EnhancementParams(
                            preset = EnhancementPreset.PORTRAIT,
                            portraitAperture = apertureFloat,
                            portraitSkinSmoothing = portraitSkinSmoothing,
                            portraitLighting = portraitLighting,
                            warmth = (portraitWarmth / 100f) * 0.12f
                        )
                    }
                    CameraShootingMode.CINEMATIC -> {
                        EnhancementParams(
                            preset = EnhancementPreset.CINEMATIC,
                            cinematicLut = cinematicColorProfile
                        )
                    }
                    CameraShootingMode.PANORAMA -> {
                        EnhancementParams(
                            preset = EnhancementPreset.NATURAL,
                            isPanorama = true
                        )
                    }
                    CameraShootingMode.SLOW_MOTION -> {
                        EnhancementParams(
                            preset = EnhancementPreset.CINEMATIC,
                            sharpness = 0.45f
                        )
                    }
                    CameraShootingMode.TIME_LAPSE -> {
                        EnhancementParams(
                            preset = EnhancementPreset.VIVID,
                            contrast = 0.22f
                        )
                    }
                    CameraShootingMode.NIGHT -> {
                        EnhancementParams(
                            preset = EnhancementPreset.NIGHT
                        )
                    }
                    else -> {
                        EnhancementParams(
                            preset = EnhancementPreset.NATURAL
                        )
                    }
                }

                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            isCapturing = false
                            onPhotoCaptured(photoFile, shootingMode, currentModeParams)
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
            if (shootingMode == CameraShootingMode.PANORAMA) {
                isPanoramaCapturing = !isPanoramaCapturing
                CameraHaptics.playShutterTick(context)
            } else if (timerSetting != CameraTimer.OFF && countdownSeconds == 0) {
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
                        try {
                            cameraControl?.setZoomRatio(targetZoom)
                        } catch (_: Exception) {}
                    }
                }
                .pointerInput(aspectRatio, activeBubble) {
                    // Tap-to-focus & metering gesture (or dismiss active bubble)
                    detectTapGestures { offset ->
                        if (activeBubble != CameraActiveBubble.NONE) {
                            activeBubble = CameraActiveBubble.NONE
                            CameraHaptics.playLightTick(context)
                            return@detectTapGestures
                        }
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
                        try {
                            cameraControl?.startFocusAndMetering(action)
                        } catch (_: Exception) {}
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

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetAspectRatio(aspectRatio.ratioValue)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()

                        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            try {
                                val now = System.currentTimeMillis()
                                if (now - lastAnalysisTime >= 250 && isAnalyzingFrame.compareAndSet(false, true)) {
                                    lastAnalysisTime = now
                                    val bmp = imageProxy.toBitmap()
                                    val scaled = if (bmp.width > 120 || bmp.height > 120) {
                                        Bitmap.createScaledBitmap(bmp, 100, 100, false)
                                    } else {
                                        bmp
                                    }
                                    val analysis = MikeImageAnalyzer.analyzeDirect(scaled)
                                    val liveModeParams = when (shootingMode) {
                                        CameraShootingMode.PORTRAIT -> EnhancementParams(preset = EnhancementPreset.PORTRAIT)
                                        CameraShootingMode.NIGHT -> EnhancementParams(preset = EnhancementPreset.NIGHT)
                                        CameraShootingMode.CINEMATIC -> EnhancementParams(preset = EnhancementPreset.CINEMATIC)
                                        else -> EnhancementParams(preset = EnhancementPreset.NATURAL)
                                    }
                                    val liveEnhanced = MikeAIEngine.processLivePreview(scaled, liveModeParams, analysis)
                                    liveEnhanced.recycle()
                                    if (scaled != bmp) scaled.recycle()
                                    bmp.recycle()

                                    val newScene = analysis.sceneType
                                    val updatedHistory = (sceneHistory + newScene).takeLast(3)
                                    sceneHistory = updatedHistory
                                    val matchesCount = updatedHistory.filter { scene -> scene == newScene }.size
                                    if (matchesCount >= 2) {
                                        liveDetectedScene = newScene
                                    }
                                    isAnalyzingFrame.set(false)
                                }
                            } catch (_: Exception) {
                                isAnalyzingFrame.set(false)
                            } finally {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture,
                                imageAnalysis
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
                        try {
                            cameraControl?.enableTorch(flashMode == CameraFlashMode.TORCH)
                        } catch (_: Exception) {}
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

            // 2.39:1 Cinemascope Letterbox overlay if Cinematic mode selected
            if (shootingMode == CameraShootingMode.CINEMATIC) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.12f)
                            .background(Color.Black)
                    )
                    Spacer(modifier = Modifier.weight(0.76f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.12f)
                            .background(Color.Black)
                    )
                }
            }

            // Grid Overlay (Rule of Thirds or Golden Ratio)
            CameraGridOverlay(gridType = gridType)

            // 2.39:1 Anamorphic Cinematic Letterbox Bars
            CameraCinematicLetterboxOverlay(visible = shootingMode == CameraShootingMode.CINEMATIC)

            // Portrait Mode Framing Guide (Golden Ratio Face Oval)
            if (shootingMode == CameraShootingMode.PORTRAIT) {
                CameraPortraitGuideOverlay()
            }

            // Pro Mode Live Histogram
            if (shootingMode == CameraShootingMode.PRO) {
                CameraHistogramOverlay(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 68.dp, end = 16.dp)
                )
            }

            // Live MIKE AI Scene Diagnosis Badge (Real-time intelligent detection)
            AnimatedVisibility(
                visible = liveDetectedScene != SceneType.GENERAL,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 74.dp)
            ) {
                LiquidGlassCard(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AmberStudio)
                        )
                        Text(
                            text = when (liveDetectedScene) {
                                SceneType.PORTRAIT -> "MIKE AI • PORTRAIT"
                                SceneType.NIGHT, SceneType.LOW_LIGHT -> "MIKE AI • NUIT"
                                SceneType.LANDSCAPE -> "MIKE AI • PAYSAGE"
                                SceneType.DOCUMENT -> "MIKE AI • DOCUMENT"
                                else -> "MIKE AI • OPTIMISÉ"
                            },
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

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
                        isBubbleOpen = activeBubble == CameraActiveBubble.FLASH,
                        onClick = {
                            activeBubble = if (activeBubble == CameraActiveBubble.FLASH) CameraActiveBubble.NONE else CameraActiveBubble.FLASH
                            CameraHaptics.playLightTick(context)
                        }
                    )

                    // Timer Toggle
                    CameraTopPillButton(
                        icon = Icons.Default.Timer,
                        label = timerSetting.label,
                        isActive = timerSetting != CameraTimer.OFF,
                        isBubbleOpen = activeBubble == CameraActiveBubble.TIMER,
                        onClick = {
                            activeBubble = if (activeBubble == CameraActiveBubble.TIMER) CameraActiveBubble.NONE else CameraActiveBubble.TIMER
                            CameraHaptics.playLightTick(context)
                        }
                    )

                    // Aspect Ratio Toggle
                    CameraTopPillButton(
                        icon = Icons.Default.AspectRatio,
                        label = aspectRatio.label,
                        isActive = aspectRatio != CameraAspectRatio.RATIO_4_3,
                        isBubbleOpen = activeBubble == CameraActiveBubble.ASPECT_RATIO,
                        onClick = {
                            activeBubble = if (activeBubble == CameraActiveBubble.ASPECT_RATIO) CameraActiveBubble.NONE else CameraActiveBubble.ASPECT_RATIO
                            CameraHaptics.playLightTick(context)
                        }
                    )

                    // Grid Toggle
                    CameraTopPillButton(
                        icon = Icons.Default.GridOn,
                        label = gridType.label,
                        isActive = gridType != CameraGridType.NONE,
                        isBubbleOpen = activeBubble == CameraActiveBubble.GRID,
                        onClick = {
                            activeBubble = if (activeBubble == CameraActiveBubble.GRID) CameraActiveBubble.NONE else CameraActiveBubble.GRID
                            CameraHaptics.playLightTick(context)
                        }
                    )
                }
            }

            // Level toggle / Pro controls button
            LiquidGlassIconButton(
                icon = Icons.Default.Tune,
                onClick = {
                    activeBubble = if (activeBubble == CameraActiveBubble.EXPOSURE) CameraActiveBubble.NONE else CameraActiveBubble.EXPOSURE
                    CameraHaptics.playLightTick(context)
                },
                contentDescription = "Contrôles d'exposition",
                size = 42.dp,
                iconSize = 20.dp,
                tint = if (activeBubble == CameraActiveBubble.EXPOSURE) AmberStudio else TextPrimary
            )
        }

        // 3. iOS-style Fluid Morphing Bubble Selector (Flash, Timer, Ratio, Grid, Exposure EV Wheel, Level)
        CameraFluidBubbleMenu(
            activeBubble = activeBubble,
            flashMode = flashMode,
            onFlashModeChange = { newMode ->
                flashMode = newMode
                activeBubble = CameraActiveBubble.NONE
            },
            hasFlashUnit = hasFlashUnit,
            timerSetting = timerSetting,
            onTimerSettingChange = { newTimer ->
                timerSetting = newTimer
                activeBubble = CameraActiveBubble.NONE
            },
            aspectRatio = aspectRatio,
            onAspectRatioChange = { newRatio ->
                aspectRatio = newRatio
                activeBubble = CameraActiveBubble.NONE
            },
            gridType = gridType,
            onGridTypeChange = { newGrid ->
                gridType = newGrid
                activeBubble = CameraActiveBubble.NONE
            },
            exposureIndex = exposureCompensationIndex,
            minExposureIndex = minExposureIndex,
            maxExposureIndex = maxExposureIndex,
            exposureStep = exposureStep,
            onExposureChange = { newIndex ->
                exposureCompensationIndex = newIndex
                try {
                    cameraControl?.setExposureCompensationIndex(newIndex)
                } catch (_: Exception) {}
            },
            isLevelActive = isLevelIndicatorVisible,
            onLevelToggle = {
                isLevelIndicatorVisible = !isLevelIndicatorVisible
            },
            onClose = { activeBubble = CameraActiveBubble.NONE },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 58.dp)
        )

        // 3.2 Pro Mode Top HUD Bar
        if (shootingMode == CameraShootingMode.PRO && activeBubble == CameraActiveBubble.NONE) {
            CameraProHudBar(
                iso = proIso,
                shutter = proShutterSpeed,
                ev = exposureCompensationIndex * exposureStep,
                kelvin = "${proKelvin}K",
                focus = if (isManualFocusActive) String.format(Locale.US, "%.1f", proFocusManualValue) else "AF",
                isRawEnabled = isRawCapture,
                onToggleRaw = {
                    isRawCapture = !isRawCapture
                    CameraHaptics.playLightTick(context)
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 58.dp)
            )
        }

        // 3.5 Mode-specific Liquid Glass Overlay controls (PRO, Portrait, Pano, SlowMo, Timelapse, Cinematic)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 195.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = shootingMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) with fadeOut(animationSpec = tween(180))
                },
                label = "modeOverlayTransition"
            ) { activeMode ->
                when (activeMode) {
                    CameraShootingMode.PRO -> {
                        CameraProControlsDeck(
                            selectedParam = selectedProParam,
                            onSelectParam = { selectedProParam = it },
                            iso = proIso,
                            onIsoChange = { proIso = it },
                            shutter = proShutterSpeed,
                            onShutterChange = { proShutterSpeed = it },
                            kelvin = proKelvin,
                            onKelvinChange = { proKelvin = it },
                            manualFocus = proFocusManualValue,
                            isManualFocusActive = isManualFocusActive,
                            onFocusModeChange = { isManualFocusActive = it },
                            onManualFocusValueChange = { proFocusManualValue = it },
                            exposureCompensationIndex = exposureCompensationIndex,
                            minExposureIndex = minExposureIndex,
                            maxExposureIndex = maxExposureIndex,
                            exposureStep = exposureStep,
                            onExposureChange = { newIndex ->
                                exposureCompensationIndex = newIndex
                                try {
                                    cameraControl?.setExposureCompensationIndex(newIndex)
                                } catch (_: Exception) {}
                            },
                            meteringMode = proMeteringMode,
                            onMeteringChange = { proMeteringMode = it }
                        )
                    }

                    CameraShootingMode.PORTRAIT -> {
                        CameraPortraitControlsDeck(
                            aperture = portraitAperture,
                            onApertureChange = { portraitAperture = it },
                            selectedLighting = portraitLighting,
                            onLightingChange = { portraitLighting = it },
                            skinSmoothing = portraitSkinSmoothing,
                            onSkinSmoothingChange = { portraitSkinSmoothing = it },
                            skinWarmth = portraitWarmth,
                            onSkinWarmthChange = { portraitWarmth = it }
                        )
                    }

                    CameraShootingMode.PANORAMA -> {
                        CameraPanoramaControlsDeck(
                            isCapturing = isPanoramaCapturing,
                            progress = panoramaProgress,
                            directionLeftToRight = panoramaDirectionLeftToRight,
                            onToggleDirection = {
                                panoramaDirectionLeftToRight = !panoramaDirectionLeftToRight
                                CameraHaptics.playLightTick(context)
                            }
                        )
                    }

                    CameraShootingMode.SLOW_MOTION -> {
                        CameraSlowMotionControlsDeck(
                            selectedFps = slowMotionFps,
                            onFpsChange = { slowMotionFps = it }
                        )
                    }

                    CameraShootingMode.TIME_LAPSE -> {
                        CameraTimeLapseControlsDeck(
                            selectedInterval = timeLapseInterval,
                            onIntervalChange = { timeLapseInterval = it }
                        )
                    }

                    CameraShootingMode.CINEMATIC -> {
                        CameraCinematicControlsDeck(
                            selectedLut = cinematicColorProfile,
                            onLutChange = { cinematicColorProfile = it }
                        )
                    }

                    else -> {}
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
            // iOS Dynamic Drawer Chevron Button
            val isAnyBubbleOpen = activeBubble != CameraActiveBubble.NONE
            val chevronRotation by animateFloatAsState(
                targetValue = if (isAnyBubbleOpen) 180f else 0f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
                label = "chevronRotation"
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isAnyBubbleOpen) AmberStudio.copy(alpha = 0.25f) else Color(0x44080C14))
                    .clickable {
                        activeBubble = if (activeBubble == CameraActiveBubble.NONE) CameraActiveBubble.FLASH else CameraActiveBubble.NONE
                        CameraHaptics.playLightTick(context)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Options appareil",
                    tint = if (isAnyBubbleOpen) AmberStudio else TextPrimary.copy(alpha = 0.75f),
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

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

            // Shooting Modes Carousel (PHOTO, PORTRAIT, NUIT, PRO...)
            // Centers the selected mode horizontally and displays a high-visibility active indicator
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shooting_modes_carousel")
            ) {
                val screenWidth = maxWidth
                val itemEstimatedWidth = 84.dp
                val sidePadding = ((screenWidth - itemEstimatedWidth) / 2).coerceAtLeast(0.dp)
                val modeListState = rememberLazyListState()

                // Smoothly scroll and center the selected mode whenever it changes
                LaunchedEffect(shootingMode) {
                    val targetIndex = CameraShootingMode.entries.indexOf(shootingMode)
                    if (targetIndex >= 0) {
                        modeListState.animateScrollToItem(
                            index = targetIndex,
                            scrollOffset = 0
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LazyRow(
                        state = modeListState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = sidePadding),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(CameraShootingMode.entries) { index, mode ->
                            val isSelected = mode == shootingMode
                            val modeScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.08f else 0.92f,
                                animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f),
                                label = "modeScale"
                            )
                            val textColor by animateColorAsState(
                                targetValue = if (isSelected) AmberStudio else TextMuted,
                                animationSpec = tween(200),
                                label = "modeTextColor"
                            )

                            Box(
                                modifier = Modifier
                                    .scale(modeScale)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color(0x38FFB020)
                                        else Color(0x1A080C14)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) AmberStudio.copy(alpha = 0.85f) else Color(0x22FFFFFF),
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (shootingMode != mode) {
                                            shootingMode = mode
                                            CameraHaptics.playLightTick(context)
                                        }
                                        coroutineScope.launch {
                                            modeListState.animateScrollToItem(index, 0)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = mode.title,
                                        color = textColor,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    // High-visibility selection indicator bar/dot
                                    Box(
                                        modifier = Modifier
                                            .width(if (isSelected) 14.dp else 0.dp)
                                            .height(3.dp)
                                            .background(
                                                color = if (isSelected) AmberStudio else Color.Transparent,
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Luminous center chevron / pointer aligning the active mode to the viewfinder center
                    Box(
                        modifier = Modifier
                            .size(width = 6.dp, height = 4.dp)
                            .background(AmberStudio.copy(alpha = 0.85f), CircleShape)
                    )
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

        // Seamless Native Mode Switch Iris Bloom & Floating Badge Animation
        CameraModeSwitchOverlay(
            activeMode = shootingMode,
            isSwitching = isSwitchingMode
        )
    }
}

@Composable
private fun CameraTopPillButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isBubbleOpen: Boolean = false,
    onClick: () -> Unit
) {
    val bgModifier = if (isBubbleOpen) {
        Modifier
            .background(AmberStudio.copy(alpha = 0.35f))
            .border(1.dp, AmberStudio, CircleShape)
    } else if (isActive) {
        Modifier.background(AmberStudio.copy(alpha = 0.20f))
    } else {
        Modifier.background(Color.Transparent)
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .then(bgModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isBubbleOpen || isActive) AmberStudio else TextPrimary.copy(alpha = 0.85f),
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            color = if (isBubbleOpen || isActive) AmberStudio else TextPrimary.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = if (isBubbleOpen) FontWeight.Black else FontWeight.Bold
        )
    }
}

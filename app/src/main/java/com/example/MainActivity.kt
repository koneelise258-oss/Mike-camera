package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.camera.CameraSourcePreference
import com.example.ui.components.CameraPermissionDialog
import com.example.ui.components.LiquidBackground
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassNavigation
import com.example.ui.components.LiquidTouchRipple
import com.example.ui.components.NavigationTabItem
import com.example.ui.components.PerformanceMode
import com.example.ui.components.ProcessingAnimation
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.ComparisonScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PhotoPreviewScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MikeAiPhotoTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.PhotoViewModel
import com.example.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: PhotoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MikeAiPhotoTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: PhotoViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allPhotos by viewModel.allPhotos.collectAsStateWithLifecycle()
    val favoritePhotos by viewModel.favoritePhotos.collectAsStateWithLifecycle()
    val recentPhotos by viewModel.recentPhotos.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCameraPermissionDialog by remember { mutableStateOf(false) }

    // Request permission on initial launch if on CAMERA screen
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            showCameraPermissionDialog = true
        }
    }

    // Launcher for Native System Camera App fallback using FileProvider URI
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.onCameraCaptureResult(context, success)
    }

    // Permission Launcher for Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        showCameraPermissionDialog = false
        if (isGranted) {
            if (uiState.cameraSource == CameraSourcePreference.MIKE_AI_CAMERA) {
                viewModel.navigateTo(Screen.CAMERA)
            } else {
                try {
                    val captureUri = viewModel.prepareCameraCapture(context)
                    takePictureLauncher.launch(captureUri)
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Impossible de préparer la capture: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                context,
                "Permission caméra refusée. Vous pouvez aussi importer une photo existante.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Launcher for Photo Picker (zero permission modern gallery picker)
    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhotoImported(context, uri)
        }
    }

    val launchCamera: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            if (uiState.cameraSource == CameraSourcePreference.MIKE_AI_CAMERA) {
                viewModel.navigateTo(Screen.CAMERA)
            } else {
                try {
                    val captureUri = viewModel.prepareCameraCapture(context)
                    takePictureLauncher.launch(captureUri)
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Impossible d'ouvrir l'appareil photo: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            showCameraPermissionDialog = true
        }
    }

    val launchPhotoPicker: () -> Unit = {
        try {
            pickPhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Impossible d'ouvrir la galerie: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val navTabs = remember {
        listOf(
            NavigationTabItem("camera", "Caméra", Icons.Default.PhotoCamera),
            NavigationTabItem("gallery", "Galerie", Icons.Default.Collections),
            NavigationTabItem("studio", "AI", Icons.Default.AutoAwesome),
            NavigationTabItem("settings", "Réglages", Icons.Default.Settings)
        )
    }

    val currentTabId = when (uiState.currentScreen) {
        Screen.CAMERA -> "camera"
        Screen.GALLERY -> "gallery"
        Screen.STUDIO -> "studio"
        Screen.SETTINGS -> "settings"
        else -> "camera"
    }

    val showBottomNav = uiState.currentScreen in listOf(
        Screen.GALLERY,
        Screen.STUDIO,
        Screen.SETTINGS
    )

    val isCameraActive = uiState.currentScreen == Screen.CAMERA || uiState.currentScreen == Screen.HOME

    LiquidTouchRipple(enabled = !isCameraActive && uiState.performanceMode != PerformanceMode.DISABLED) {
        LiquidBackground(
            environment = uiState.environment,
            performanceMode = if (isCameraActive) PerformanceMode.DISABLED else uiState.performanceMode
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (showBottomNav) {
                        LiquidGlassNavigation(
                            tabs = navTabs,
                            selectedTabId = currentTabId,
                            onTabSelected = { tabId ->
                                when (tabId) {
                                    "camera" -> viewModel.navigateTo(Screen.CAMERA)
                                    "gallery" -> viewModel.navigateTo(Screen.GALLERY)
                                    "studio" -> viewModel.navigateTo(Screen.STUDIO)
                                    "settings" -> viewModel.navigateTo(Screen.SETTINGS)
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = uiState.currentScreen,
                        animationSpec = tween(280),
                        label = "screenTransition"
                    ) { screen ->
                        when (screen) {
                            Screen.CAMERA -> {
                                CameraScreen(
                                    latestPhoto = allPhotos.firstOrNull(),
                                    onPhotoCaptured = { photoFile, shootingMode ->
                                        viewModel.onCustomCameraCapture(context, photoFile, shootingMode)
                                    },
                                    onOpenGallery = { viewModel.navigateTo(Screen.GALLERY) },
                                    onBack = { viewModel.navigateTo(Screen.GALLERY) }
                                )
                            }

                            Screen.HOME -> {
                                // Fallback redirects directly to camera in camera-first architecture
                                CameraScreen(
                                    latestPhoto = allPhotos.firstOrNull(),
                                    onPhotoCaptured = { photoFile, shootingMode ->
                                        viewModel.onCustomCameraCapture(context, photoFile, shootingMode)
                                    },
                                    onOpenGallery = { viewModel.navigateTo(Screen.GALLERY) },
                                    onBack = { viewModel.navigateTo(Screen.GALLERY) }
                                )
                            }

                            Screen.PREVIEW -> {
                                val origBmp = uiState.originalBitmap
                                if (origBmp != null) {
                                    PhotoPreviewScreen(
                                        bitmap = origBmp,
                                        imageFile = uiState.originalFile,
                                        onEnhance = { viewModel.startEnhanceCapturedPhoto(context) },
                                        onRetake = { viewModel.navigateTo(Screen.CAMERA) },
                                        onBack = { viewModel.navigateTo(Screen.CAMERA) }
                                    )
                                } else {
                                    viewModel.navigateTo(Screen.CAMERA)
                                }
                            }

                            Screen.PROCESSING -> {
                                ProcessingAnimation(
                                    message = uiState.processingMessage,
                                    photoBitmap = uiState.originalBitmap
                                )
                            }

                            Screen.COMPARISON -> {
                                val original = uiState.originalBitmap
                                val processed = uiState.processedBitmap

                                if (original != null && processed != null) {
                                    ComparisonScreen(
                                        originalBitmap = original,
                                        processedBitmap = processed,
                                        currentParams = uiState.currentParams,
                                        isProcessing = uiState.isProcessing,
                                        isSavedToGallery = uiState.isSavedToGallery,
                                        isFavorite = uiState.isCurrentFavorite,
                                        onBack = { viewModel.navigateTo(Screen.CAMERA) },
                                        onSaveToGallery = { viewModel.saveCurrentToGallery(context) },
                                        onToggleFavorite = { viewModel.toggleCurrentFavorite() },
                                        onSetAsWallpaper = { target -> viewModel.setAsWallpaper(context, target) },
                                        onPrint = { viewModel.printCurrentPhoto(context) },
                                        onDelete = {
                                            val photo = allPhotos.find { it.id == uiState.currentPhotoId }
                                            if (photo != null) {
                                                viewModel.deletePhoto(photo)
                                            }
                                            viewModel.navigateTo(Screen.CAMERA)
                                        },
                                        onRetake = { viewModel.navigateTo(Screen.CAMERA) },
                                        onPresetSelected = { preset -> viewModel.applyPreset(preset) },
                                        onAiIntensityChange = { intensity -> viewModel.updateAiIntensity(intensity) },
                                        onAdjustmentsChange = { exp, cont, shd, hgl, vib, wrm, shp ->
                                            viewModel.updateAdjustments(exp, cont, shd, hgl, vib, wrm, shp)
                                        }
                                    )
                                } else {
                                    ProcessingAnimation(
                                        message = "Chargement de la photo…",
                                        photoBitmap = original
                                    )
                                }
                            }

                            Screen.GALLERY -> {
                                GalleryScreen(
                                    photos = allPhotos,
                                    favoritePhotos = favoritePhotos,
                                    recentPhotos = recentPhotos,
                                    currentFilter = uiState.galleryFilter,
                                    onFilterChange = { filter -> viewModel.setGalleryFilter(filter) },
                                    onBack = { viewModel.navigateTo(Screen.CAMERA) },
                                    onOpenPhoto = { photo -> viewModel.openGalleryPhoto(photo) },
                                    onToggleFavorite = { photo -> viewModel.togglePhotoFavorite(photo) },
                                    onDeletePhoto = { photo -> viewModel.deletePhoto(photo) },
                                    onTakePhoto = { viewModel.navigateTo(Screen.CAMERA) }
                                )
                            }

                            Screen.STUDIO -> {
                                StudioScreen(
                                    photos = allPhotos,
                                    onOpenPhoto = { photo -> viewModel.openGalleryPhoto(photo) }
                                )
                            }

                            Screen.SETTINGS -> {
                                SettingsScreen(
                                    currentEnvironment = uiState.environment,
                                    currentPerformanceMode = uiState.performanceMode,
                                    autoProcessAfterCapture = uiState.autoProcessAfterCapture,
                                    defaultAiIntensity = uiState.defaultAiIntensity,
                                    defaultPreset = uiState.defaultPreset,
                                    currentCameraSource = uiState.cameraSource,
                                    onEnvironmentChange = { env -> viewModel.setEnvironment(env) },
                                    onPerformanceModeChange = { mode -> viewModel.setPerformanceMode(mode) },
                                    onAutoProcessChange = { auto -> viewModel.setAutoProcess(auto) },
                                    onDefaultAiIntensityChange = { int -> viewModel.setDefaultAiIntensity(int) },
                                    onDefaultPresetChange = { p -> viewModel.setDefaultPreset(p) },
                                    onCameraSourceChange = { src -> viewModel.setCameraSource(src) }
                                )
                            }
                        }
                    }

                    // Camera Permission Liquid Glass Dialog
                    CameraPermissionDialog(
                        visible = showCameraPermissionDialog,
                        onRequestPermission = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onDismiss = { showCameraPermissionDialog = false }
                    )

                    // Liquid Glass styled Error Dialog
                    uiState.errorMessage?.let { errorMsg ->
                        BasicAlertDialog(
                            onDismissRequest = { viewModel.clearError() }
                        ) {
                            LiquidGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .padding(16.dp),
                                shape = RoundedCornerShape(28.dp),
                                backgroundColor = DarkSurface
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Information",
                                        color = TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = errorMsg,
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { viewModel.clearError() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("OK", color = DarkBg)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

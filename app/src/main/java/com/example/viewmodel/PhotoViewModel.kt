package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PhotoDatabase
import com.example.data.model.ProcessedPhoto
import com.example.data.repository.PhotoRepository
import com.example.engine.EnhancementParams
import com.example.engine.EnhancementPreset
import com.example.engine.ImageAnalysis
import com.example.engine.MediaManager
import com.example.engine.MikeAIEngine
import com.example.engine.SceneType
import com.example.engine.WallpaperTarget
import com.example.ui.components.LiquidEnvironment
import com.example.ui.components.PerformanceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

import com.example.ai.gemini.GeminiEngineBackend
import com.example.ai.gemini.GeminiProxyBackend
import com.example.camera.CameraShootingMode
import com.example.camera.CameraSourcePreference

enum class Screen {
    HOME,
    CAMERA,
    GALLERY,
    STUDIO,
    SETTINGS,
    PREVIEW,
    PROCESSING,
    COMPARISON
}

enum class GalleryFilter {
    ALL,
    FAVORITES,
    RECENTS
}

data class UiState(
    val currentScreen: Screen = Screen.CAMERA,
    val originalBitmap: Bitmap? = null,
    val processedBitmap: Bitmap? = null,
    val originalFile: File? = null,
    val currentPhotoId: Long = 0,
    val currentParams: EnhancementParams = EnhancementParams(preset = EnhancementPreset.NATURAL),
    val currentAnalysis: ImageAnalysis? = null,
    val isProcessing: Boolean = false,
    val processingMessage: String = "Analyse & traitement de votre photo…",
    val isSavedToGallery: Boolean = false,
    val isCurrentFavorite: Boolean = false,
    val errorMessage: String? = null,
    val selectedGalleryPhoto: ProcessedPhoto? = null,
    val detectedScene: SceneType = SceneType.GENERAL,
    val isGeminiPowered: Boolean = false,
    val geminiStatusMessage: String? = null,
    // Settings state
    val environment: LiquidEnvironment = LiquidEnvironment.OBSIDIAN,
    val performanceMode: PerformanceMode = PerformanceMode.STANDARD,
    val autoProcessAfterCapture: Boolean = true,
    val defaultAiIntensity: Float = 1.0f,
    val defaultPreset: EnhancementPreset = EnhancementPreset.NATURAL,
    val galleryFilter: GalleryFilter = GalleryFilter.ALL,
    val cameraSource: CameraSourcePreference = CameraSourcePreference.MIKE_AI_CAMERA
)

class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PhotoRepository
    private val geminiBackend = GeminiProxyBackend(GeminiEngineBackend(application))

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var pendingCaptureFile: File? = null
    private var processingJob: Job? = null

    init {
        MikeAIEngine.activeBackend = geminiBackend
        val db = PhotoDatabase.getInstance(application)
        repository = PhotoRepository(db.photoDao())

        val recommendedMode = com.example.util.DeviceOptimizer.getRecommendedPerformanceMode(application)
        _uiState.update { it.copy(performanceMode = recommendedMode) }
    }

    val allPhotos: StateFlow<List<ProcessedPhoto>> = repository.allPhotos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoritePhotos: StateFlow<List<ProcessedPhoto>> = repository.favoritePhotos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentPhotos: StateFlow<List<ProcessedPhoto>> = repository.recentPhotos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun prepareCameraCapture(context: Context): Uri {
        val (file, uri) = MediaManager.createTempCaptureFile(context)
        pendingCaptureFile = file
        return uri
    }

    fun onCameraCaptureResult(context: Context, success: Boolean) {
        if (!success) {
            // User cancelled camera capture: silently return to previous screen without errors
            pendingCaptureFile = null
            return
        }

        val capturedFile = pendingCaptureFile ?: return
        if (!capturedFile.exists() || capturedFile.length() == 0L) {
            _uiState.update { it.copy(errorMessage = "La photo n'a pas pu être enregistrée par l'appareil photo.") }
            pendingCaptureFile = null
            return
        }

        viewModelScope.launch {
            try {
                // 1. Decode high-resolution bitmap safely handling EXIF orientation in-memory without altering original file
                val maxDim = com.example.util.DeviceOptimizer.getMaxBitmapDimension(context)
                val originalBmp = MikeAIEngine.decodeSampledBitmap(capturedFile, maxWidth = maxDim, maxHeight = maxDim)

                // 2. Complete diagnostic analysis & scene detection
                val analysis = MikeAIEngine.analyzeImage(originalBmp)

                _uiState.update {
                    it.copy(
                        currentScreen = Screen.PREVIEW,
                        originalBitmap = originalBmp,
                        originalFile = capturedFile,
                        detectedScene = analysis.sceneType,
                        currentAnalysis = analysis,
                        currentParams = EnhancementParams(
                            preset = it.defaultPreset,
                            aiIntensity = it.defaultAiIntensity
                        ),
                        isProcessing = false,
                        errorMessage = null
                    )
                }

                if (_uiState.value.autoProcessAfterCapture) {
                    startEnhanceCapturedPhoto(context)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        currentScreen = Screen.CAMERA,
                        errorMessage = "Erreur lors de la récupération de la photo: ${e.localizedMessage}"
                    )
                }
            } finally {
                pendingCaptureFile = null
            }
        }
    }

    fun onCustomCameraCapture(
        context: Context,
        capturedFile: File,
        mode: CameraShootingMode,
        modeParams: EnhancementParams? = null
    ) {
        if (!capturedFile.exists() || capturedFile.length() == 0L) {
            _uiState.update { it.copy(errorMessage = "La photo n'a pas pu être enregistrée.") }
            return
        }

        viewModelScope.launch {
            try {
                // 1. Decode high-resolution bitmap safely handling EXIF orientation in-memory without altering original file
                val maxDim = com.example.util.DeviceOptimizer.getMaxBitmapDimension(context)
                val originalBmp = MikeAIEngine.decodeSampledBitmap(capturedFile, maxWidth = maxDim, maxHeight = maxDim)

                // 2. Complete diagnostic analysis & scene detection
                val analysis = MikeAIEngine.analyzeImage(originalBmp)

                // 4. Determine initial preset based on shooting mode
                val targetPreset = mode.targetPreset
                val effectiveParams = modeParams ?: EnhancementParams(
                    preset = targetPreset,
                    aiIntensity = _uiState.value.defaultAiIntensity
                )

                _uiState.update {
                    it.copy(
                        currentScreen = Screen.PREVIEW,
                        originalBitmap = originalBmp,
                        originalFile = capturedFile,
                        detectedScene = analysis.sceneType,
                        currentAnalysis = analysis,
                        currentParams = effectiveParams,
                        isProcessing = false,
                        errorMessage = null
                    )
                }

                if (_uiState.value.autoProcessAfterCapture) {
                    startEnhanceCapturedPhoto(context)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        currentScreen = Screen.CAMERA,
                        errorMessage = "Erreur lors du traitement de la capture: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun onPhotoImported(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        currentScreen = Screen.PROCESSING,
                        isProcessing = true,
                        processingMessage = "Importation et analyse de la photo…",
                        errorMessage = null
                    )
                }
                val localFile = MediaManager.copyUriToInternalStorage(context, uri)
                val maxDim = com.example.util.DeviceOptimizer.getMaxBitmapDimension(context)
                val originalBmp = MikeAIEngine.decodeSampledBitmap(localFile, maxWidth = maxDim, maxHeight = maxDim)
                val analysis = MikeAIEngine.analyzeImage(originalBmp)

                _uiState.update {
                    it.copy(
                        currentScreen = Screen.PREVIEW,
                        originalBitmap = originalBmp,
                        originalFile = localFile,
                        detectedScene = analysis.sceneType,
                        currentAnalysis = analysis,
                        currentParams = EnhancementParams(
                            preset = it.defaultPreset,
                            aiIntensity = it.defaultAiIntensity
                        ),
                        isProcessing = false
                    )
                }

                if (_uiState.value.autoProcessAfterCapture) {
                    startEnhanceCapturedPhoto(context)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        currentScreen = Screen.CAMERA,
                        errorMessage = "Erreur lors de l'importation: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun startEnhanceCapturedPhoto(context: Context) {
        val file = _uiState.value.originalFile ?: return
        val originalBmp = _uiState.value.originalBitmap ?: return

        viewModelScope.launch {
            val defaultPreset = _uiState.value.defaultPreset
            val defaultIntensity = _uiState.value.defaultAiIntensity
            val scene = _uiState.value.detectedScene

            _uiState.update {
                it.copy(
                    currentScreen = Screen.PROCESSING,
                    isProcessing = true,
                    processingMessage = "Analyse de la scène ${scene.displayName.lowercase()} & dynamique…",
                    errorMessage = null,
                    isSavedToGallery = false,
                    isCurrentFavorite = false,
                    currentParams = EnhancementParams(preset = defaultPreset, aiIntensity = defaultIntensity)
                )
            }

            try {
                delay(150)
                val params = _uiState.value.currentParams
                val result = MikeAIEngine.processImage(
                    sourceBitmap = originalBmp,
                    params = params,
                    onProgress = { step, _ ->
                        _uiState.update { it.copy(processingMessage = step) }
                    }
                )

                val processedBmp = result.processedBitmap

                // Permanently persist the original file to filesDir if it's currently in cacheDir (temp capture)
                val finalOriginalFile = if (file.absolutePath.contains(context.cacheDir.absolutePath)) {
                    try {
                        MediaManager.persistOriginalFile(context, file)
                    } catch (e: Exception) {
                        file // Fallback to current temporary file on any error
                    }
                } else {
                    file
                }

                val processedFile = MediaManager.saveProcessedBitmapToFile(
                    context,
                    processedBmp,
                    prefix = "ENHANCED",
                    originalFile = finalOriginalFile
                )

                val photoRecord = ProcessedPhoto(
                    originalPath = finalOriginalFile.absolutePath,
                    processedPath = processedFile.absolutePath,
                    timestamp = System.currentTimeMillis(),
                    width = processedBmp.width,
                    height = processedBmp.height,
                    presetName = params.preset.title,
                    isSavedToGallery = false,
                    isFavorite = false,
                    aiIntensity = params.aiIntensity,
                    sceneType = result.analysis.sceneType.displayName,
                    exposureAdj = params.exposure,
                    contrastAdj = params.contrast,
                    shadowsAdj = params.shadows,
                    highlightsAdj = params.highlights,
                    vibranceAdj = params.vibrance,
                    warmthAdj = params.warmth,
                    sharpnessAdj = params.sharpness,
                    isVideo = params.isVideo,
                    videoDurationSeconds = params.videoDurationSeconds,
                    shootingModeName = params.shootingModeName
                )
                val insertedId = repository.insertPhoto(photoRecord)

                _uiState.update {
                    it.copy(
                        currentScreen = Screen.COMPARISON,
                        originalBitmap = originalBmp,
                        processedBitmap = processedBmp,
                        originalFile = finalOriginalFile,
                        currentPhotoId = insertedId,
                        currentAnalysis = result.analysis,
                        isProcessing = false,
                        isSavedToGallery = false,
                        isCurrentFavorite = false,
                        isGeminiPowered = geminiBackend.wasLastRunGemini,
                        geminiStatusMessage = geminiBackend.lastProcessingStatus
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        currentScreen = Screen.PREVIEW,
                        errorMessage = "Échec du traitement: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun applyPreset(preset: EnhancementPreset) {
        val current = _uiState.value.currentParams
        val newParams = EnhancementParams(
            preset = preset,
            exposure = preset.defaultExposure,
            contrast = preset.defaultContrast,
            shadows = preset.defaultShadows,
            highlights = preset.defaultHighlights,
            vibrance = preset.defaultVibrance,
            warmth = preset.defaultWarmth,
            sharpness = preset.defaultSharpness,
            aiIntensity = current.aiIntensity
        )
        reprocessWithParams(newParams)
    }

    fun updateAiIntensity(intensity: Float) {
        val current = _uiState.value.currentParams
        val updated = current.copy(aiIntensity = intensity.coerceIn(0f, 1f))
        reprocessWithParams(updated)
    }

    fun updateAdjustments(
        exposure: Float? = null,
        contrast: Float? = null,
        shadows: Float? = null,
        highlights: Float? = null,
        vibrance: Float? = null,
        warmth: Float? = null,
        sharpness: Float? = null
    ) {
        val current = _uiState.value.currentParams
        val updated = current.copy(
            exposure = exposure?.coerceIn(-0.50f, 0.50f) ?: current.exposure,
            contrast = contrast?.coerceIn(-0.50f, 0.50f) ?: current.contrast,
            shadows = shadows?.coerceIn(-0.50f, 1.00f) ?: current.shadows,
            highlights = highlights?.coerceIn(-1.00f, 0.50f) ?: current.highlights,
            vibrance = vibrance?.coerceIn(-1.00f, 1.00f) ?: current.vibrance,
            warmth = warmth?.coerceIn(-0.50f, 0.50f) ?: current.warmth,
            sharpness = sharpness?.coerceIn(0.00f, 1.00f) ?: current.sharpness
        )
        reprocessWithParams(updated)
    }

    fun resetAdjustments() {
        val current = _uiState.value.currentParams
        val defaultPreset = current.preset
        val resetParams = EnhancementParams(
            preset = defaultPreset,
            exposure = defaultPreset.defaultExposure,
            contrast = defaultPreset.defaultContrast,
            shadows = defaultPreset.defaultShadows,
            highlights = defaultPreset.defaultHighlights,
            vibrance = defaultPreset.defaultVibrance,
            warmth = defaultPreset.defaultWarmth,
            sharpness = defaultPreset.defaultSharpness,
            aiIntensity = 1.0f
        )
        reprocessWithParams(resetParams)
    }

    private fun reprocessWithParams(params: EnhancementParams) {
        val origBmp = _uiState.value.originalBitmap ?: return

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.update { it.copy(currentParams = params, isProcessing = true) }
            val result = MikeAIEngine.processImage(origBmp, params)
            val newProcessedBmp = result.processedBitmap

            val context = getApplication<Application>()
            val processedFile = MediaManager.saveProcessedBitmapToFile(
                context,
                newProcessedBmp,
                prefix = "ENHANCED",
                originalFile = _uiState.value.originalFile
            )

            val currentId = _uiState.value.currentPhotoId
            if (currentId != 0L) {
                val record = repository.getPhotoById(currentId)
                if (record != null) {
                    val oldPath = record.processedPath
                    if (oldPath.isNotEmpty() && oldPath != processedFile.absolutePath && oldPath != record.originalPath) {
                        try { File(oldPath).delete() } catch (_: Exception) {}
                    }
                    repository.updatePhoto(
                        record.copy(
                            processedPath = processedFile.absolutePath,
                            width = newProcessedBmp.width,
                            height = newProcessedBmp.height,
                            presetName = params.preset.title,
                            aiIntensity = params.aiIntensity,
                            exposureAdj = params.exposure,
                            contrastAdj = params.contrast,
                            shadowsAdj = params.shadows,
                            highlightsAdj = params.highlights,
                            vibranceAdj = params.vibrance,
                            warmthAdj = params.warmth,
                            sharpnessAdj = params.sharpness
                        )
                    )
                }
            }

            _uiState.update {
                it.copy(
                    processedBitmap = newProcessedBmp,
                    isProcessing = false
                )
            }
        }
    }

    fun saveCurrentToGallery(context: Context) {
        val currentBmp = _uiState.value.processedBitmap ?: return
        val origFile = _uiState.value.originalFile
        val params = _uiState.value.currentParams

        viewModelScope.launch {
            val bitmapToSave = if (origFile != null && origFile.exists() && origFile.length() > 0L) {
                try {
                    // Full resolution processing for export & permanent gallery saving
                    val fullResult = MikeAIEngine.processFullResolution(origFile, params)
                    fullResult.processedBitmap
                } catch (_: Exception) {
                    currentBmp
                }
            } else {
                currentBmp
            }

            val uri = MediaManager.saveToDeviceGallery(context, bitmapToSave, originalFile = origFile)
            if (uri != null) {
                val currentId = _uiState.value.currentPhotoId
                if (currentId != 0L) {
                    val record = repository.getPhotoById(currentId)
                    if (record != null) {
                        val fullProcessedFile = MediaManager.saveProcessedBitmapToFile(
                            context,
                            bitmapToSave,
                            prefix = "ENHANCED",
                            originalFile = origFile
                        )
                        if (record.processedPath.isNotEmpty() && record.processedPath != fullProcessedFile.absolutePath && record.processedPath != record.originalPath) {
                            try { File(record.processedPath).delete() } catch (_: Exception) {}
                        }
                        repository.updatePhoto(
                            record.copy(
                                processedPath = fullProcessedFile.absolutePath,
                                width = bitmapToSave.width,
                                height = bitmapToSave.height,
                                isSavedToGallery = true
                            )
                        )
                    } else {
                        repository.markAsSaved(currentId)
                    }
                }
                _uiState.update { it.copy(isSavedToGallery = true, processedBitmap = bitmapToSave) }
            } else {
                Toast.makeText(context, "Erreur lors de l'enregistrement.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleCurrentFavorite() {
        val currentId = _uiState.value.currentPhotoId
        if (currentId == 0L) return
        viewModelScope.launch {
            val record = repository.getPhotoById(currentId) ?: return@launch
            repository.toggleFavorite(record)
            _uiState.update { it.copy(isCurrentFavorite = !it.isCurrentFavorite) }
        }
    }

    fun togglePhotoFavorite(photo: ProcessedPhoto) {
        viewModelScope.launch {
            repository.toggleFavorite(photo)
        }
    }

    fun setAsWallpaper(context: Context, target: WallpaperTarget) {
        val bmp = _uiState.value.processedBitmap ?: return
        viewModelScope.launch {
            val success = MediaManager.setAsWallpaper(context, bmp, target)
            if (success) {
                Toast.makeText(context, "Fond d'écran appliqué avec succès !", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Impossible d'appliquer le fond d'écran.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun printCurrentPhoto(context: Context) {
        val bmp = _uiState.value.processedBitmap ?: return
        MediaManager.printPhoto(context, bmp)
    }

    fun openGalleryPhoto(photo: ProcessedPhoto) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentScreen = Screen.PROCESSING,
                    isProcessing = true,
                    processingMessage = "Chargement de la photo…",
                    selectedGalleryPhoto = photo
                )
            }

            val origFile = File(photo.originalPath)
            val procFile = File(photo.processedPath)

            val context = getApplication<Application>()
            val maxDim = com.example.util.DeviceOptimizer.getMaxBitmapDimension(context)
            val origBmp = if (origFile.exists()) {
                try {
                    MikeAIEngine.decodeSampledBitmap(origFile, maxWidth = maxDim, maxHeight = maxDim)
                } catch (_: Exception) { null }
            } else null

            val procBmp = if (procFile.exists()) {
                try {
                    MikeAIEngine.decodeSampledBitmap(procFile, maxWidth = maxDim, maxHeight = maxDim)
                } catch (_: Exception) { null }
            } else null

            if (procBmp != null) {
                val matchedPreset = EnhancementPreset.entries.find { p -> p.title.equals(photo.presetName, ignoreCase = true) }
                    ?: EnhancementPreset.NATURAL

                _uiState.update {
                    it.copy(
                        currentScreen = Screen.COMPARISON,
                        originalBitmap = origBmp ?: procBmp,
                        processedBitmap = procBmp,
                        originalFile = origFile,
                        currentPhotoId = photo.id,
                        isProcessing = false,
                        isSavedToGallery = photo.isSavedToGallery,
                        isCurrentFavorite = photo.isFavorite,
                        currentParams = EnhancementParams(
                            preset = matchedPreset,
                            exposure = photo.exposureAdj,
                            contrast = photo.contrastAdj,
                            shadows = photo.shadowsAdj,
                            highlights = photo.highlightsAdj,
                            vibrance = photo.vibranceAdj,
                            warmth = photo.warmthAdj,
                            sharpness = photo.sharpnessAdj,
                            aiIntensity = photo.aiIntensity
                        )
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        currentScreen = Screen.GALLERY,
                        isProcessing = false,
                        errorMessage = "Fichier image introuvable."
                    )
                }
            }
        }
    }

    fun deletePhoto(photo: ProcessedPhoto) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
            if (_uiState.value.currentPhotoId == photo.id) {
                _uiState.update {
                    it.copy(
                        currentScreen = Screen.GALLERY,
                        originalBitmap = null,
                        processedBitmap = null,
                        currentPhotoId = 0
                    )
                }
            }
        }
    }

    // Settings actions
    fun setEnvironment(env: LiquidEnvironment) {
        _uiState.update { it.copy(environment = env) }
    }

    fun setPerformanceMode(mode: PerformanceMode) {
        _uiState.update { it.copy(performanceMode = mode) }
    }

    fun setAutoProcess(enabled: Boolean) {
        _uiState.update { it.copy(autoProcessAfterCapture = enabled) }
    }

    fun setDefaultAiIntensity(intensity: Float) {
        _uiState.update { it.copy(defaultAiIntensity = intensity) }
    }

    fun setDefaultPreset(preset: EnhancementPreset) {
        _uiState.update { it.copy(defaultPreset = preset) }
    }

    fun setCameraSource(source: CameraSourcePreference) {
        _uiState.update { it.copy(cameraSource = source) }
    }

    fun setGalleryFilter(filter: GalleryFilter) {
        _uiState.update { it.copy(galleryFilter = filter) }
    }

    fun navigateTo(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

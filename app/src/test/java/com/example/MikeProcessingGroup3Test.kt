package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.ai.gemini.GeminiEngineBackend
import com.example.ai.gemini.GeminiEnhancementResult
import com.example.ai.gemini.GeminiProxyBackend
import com.example.data.local.PhotoDatabase
import com.example.data.model.ProcessedPhoto
import com.example.data.repository.PhotoRepository
import com.example.engine.ClassicEngineBackend
import com.example.engine.EnhancementParams
import com.example.engine.EnhancementPreset
import com.example.engine.ImageAnalysis
import com.example.engine.MediaManager
import com.example.engine.MikeAIEngine
import com.example.engine.SceneType
import com.example.viewmodel.PhotoViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MikeProcessingGroup3Test {

    private lateinit var context: Application
    private lateinit var repository: PhotoRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val db = PhotoDatabase.getInstance(context)
        repository = PhotoRepository(db.photoDao())
    }

    private fun createTestBitmap(width: Int = 120, height: Int = 120, colorR: Int = 120, colorG: Int = 140, colorB: Int = 160): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bmp.setPixel(x, y, Color.rgb(colorR, colorG, colorB))
            }
        }
        return bmp
    }

    private fun createTestImageFile(width: Int = 120, height: Int = 120): File {
        val file = File(context.cacheDir, "post_capture_g3_${System.nanoTime()}.jpg")
        val bmp = createTestBitmap(width, height)
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        bmp.recycle()
        return file
    }

    // TEST A & B : Capture sans Internet & traitement local après capture
    @Test
    fun testPostCaptureProcessingOffline() {
        runBlocking {
            val file = createTestImageFile(120, 120)
            val decodedBmp = MikeAIEngine.decodeSampledBitmap(file)
            assertNotNull(decodedBmp)

            val params = EnhancementParams(preset = EnhancementPreset.NATURAL, aiIntensity = 0.8f)
            val result = MikeAIEngine.processImage(decodedBmp, params)

            assertNotNull(result.processedBitmap)
            assertEquals(120, result.processedBitmap.width)
            assertEquals(120, result.processedBitmap.height)

            file.delete()
        }
    }

    // TEST C à H : Presets (Natural, Vivid, Cinematic, Portrait, Night, Black & White)
    @Test
    fun testPostCaptureAllPresets() {
        runBlocking {
            val file = createTestImageFile(100, 100)
            val decodedBmp = MikeAIEngine.decodeSampledBitmap(file)
            assertNotNull(decodedBmp)

            val presets = listOf(
                EnhancementPreset.NATURAL,
                EnhancementPreset.VIVID,
                EnhancementPreset.CINEMATIC,
                EnhancementPreset.PORTRAIT,
                EnhancementPreset.NIGHT,
                EnhancementPreset.BLACK_AND_WHITE
            )

            for (preset in presets) {
                val params = EnhancementParams(preset = preset, aiIntensity = 1.0f)
                val result = MikeAIEngine.processImage(decodedBmp, params)
                assertNotNull("Result for preset ${preset.title} must be valid", result.processedBitmap)
                assertEquals(preset, result.appliedParams.preset)
            }

            file.delete()
        }
    }

    // TEST I : Intensité 0 / 50 / 100
    @Test
    fun testPostCaptureIntensities() {
        runBlocking {
            val file = createTestImageFile(100, 100)
            val decodedBmp = MikeAIEngine.decodeSampledBitmap(file)
            assertNotNull(decodedBmp)

            val localBackend = ClassicEngineBackend()
            val analysis = ImageAnalysis(sceneType = SceneType.GENERAL)

            val res0 = localBackend.process(decodedBmp, analysis, EnhancementParams(aiIntensity = 0.0f))
            assertEquals("0% intensity must yield identical pixel", decodedBmp.getPixel(50, 50), res0.getPixel(50, 50))

            val res50 = localBackend.process(decodedBmp, analysis, EnhancementParams(preset = EnhancementPreset.VIVID, aiIntensity = 0.5f))
            assertNotNull(res50)

            val res100 = localBackend.process(decodedBmp, analysis, EnhancementParams(preset = EnhancementPreset.VIVID, aiIntensity = 1.0f))
            assertNotNull(res100)

            file.delete()
        }
    }

    // TEST J : Original inchangé
    @Test
    fun testOriginalFileImmutableDuringPostCapture() {
        runBlocking {
            val file = createTestImageFile(100, 100)
            val originalBytes = file.readBytes()

            val decodedBmp = MikeAIEngine.decodeSampledBitmap(file)
            val result = MikeAIEngine.processImage(decodedBmp, EnhancementParams(preset = EnhancementPreset.CINEMATIC))
            assertNotNull(result.processedBitmap)

            val afterBytes = file.readBytes()
            assertEquals("File size must remain identical", originalBytes.size, afterBytes.size)
            assertTrue("File content must remain bit-for-bit identical", originalBytes.contentEquals(afterBytes))

            file.delete()
        }
    }

    // TEST K : Résultat pleine résolution
    @Test
    fun testFullResolutionProcessing() {
        runBlocking {
            val file = createTestImageFile(200, 200)
            val params = EnhancementParams(preset = EnhancementPreset.NATURAL, aiIntensity = 0.9f)

            val fullResult = MikeAIEngine.processFullResolution(file, params)
            assertNotNull(fullResult.processedBitmap)
            assertTrue("Full resolution result must be marked isFullResolution", fullResult.isFullResolution)
            assertEquals(200, fullResult.processedBitmap.width)
            assertEquals(200, fullResult.processedBitmap.height)

            file.delete()
        }
    }

    // TEST FALLBACK : Fallback local automatique si Gemini est indisponible ou non configuré
    @Test
    fun testGeminiFallbackToLocalEngine() {
        runBlocking {
            val geminiEngine = GeminiEngineBackend(context)
            val proxyBackend = GeminiProxyBackend(geminiEngine)

            val bmp = createTestBitmap(100, 100)
            val analysis = ImageAnalysis(sceneType = SceneType.GENERAL)
            val params = EnhancementParams(preset = EnhancementPreset.NATURAL)

            val result = proxyBackend.process(bmp, analysis, params)
            assertNotNull(result)
            assertFalse("Fallback to local must set wasLastRunGemini to false", proxyBackend.wasLastRunGemini)
            assertTrue("Status message must explain fallback reason", proxyBackend.lastProcessingStatus.isNotEmpty())
        }
    }

    // TEST ViewModel Flow & Repository Persistence
    @Test
    fun testViewModelPostCaptureFlowAndRepositoryInsert() {
        runBlocking {
            val viewModel = PhotoViewModel(context)
            val file = createTestImageFile(120, 120)

            viewModel.onCustomCameraCapture(context, file, com.example.camera.CameraShootingMode.PHOTO)
            var attempts = 0
            while (viewModel.uiState.value.currentScreen != Screen.COMPARISON && attempts < 50) {
                org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
                Thread.sleep(50)
                attempts++
            }

            val state = viewModel.uiState.value
            assertNull("Error message should be null but was: ${state.errorMessage}", state.errorMessage)
            assertEquals("Screen must be COMPARISON", Screen.COMPARISON, state.currentScreen)
            assertNotNull(state.originalBitmap)
            assertNotNull(state.processedBitmap)
            assertTrue("Photo record ID must be created in DB", state.currentPhotoId > 0L)

            val savedPhoto = repository.getPhotoById(state.currentPhotoId)
            assertNotNull(savedPhoto)
            assertTrue("Original file path in DB must exist", File(savedPhoto!!.originalPath).exists())
            assertTrue("Processed file path in DB must exist", File(savedPhoto.processedPath).exists())

            file.delete()
        }
    }
}

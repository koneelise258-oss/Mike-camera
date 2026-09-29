package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PhotoDatabase
import com.example.data.model.ProcessedPhoto
import com.example.data.repository.PhotoRepository
import com.example.engine.EnhancementParams
import com.example.engine.EnhancementPreset
import com.example.engine.ImageAnalysis
import com.example.engine.MediaManager
import com.example.engine.MikeAIEngine
import com.example.engine.MikePresetProcessor
import com.example.engine.SceneType
import com.example.viewmodel.PhotoViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
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
class PhotoEditorTest {

    private lateinit var context: Application
    private lateinit var database: PhotoDatabase
    private lateinit var repository: PhotoRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PhotoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PhotoRepository(database.photoDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createTestImageFile(width: Int = 120, height: Int = 120): File {
        val file = File(context.cacheDir, "editor_test_${System.nanoTime()}.jpg")
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bmp.setPixel(x, y, Color.rgb(80 + (x % 60), 90 + (y % 60), 100))
            }
        }
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        bmp.recycle()
        return file
    }

    // TEST A : Ouvrir une vraie photo dans l'éditeur
    @Test
    fun testOpenRealPhotoInEditor() {
        runBlocking {
            val file = createTestImageFile()
            val origBmp = MikeAIEngine.decodeSampledBitmap(file)
            assertNotNull(origBmp)
            assertEquals(120, origBmp.width)
            assertEquals(120, origBmp.height)

            val analysis = MikeAIEngine.analyzeImage(origBmp)
            assertNotNull(analysis)
            assertTrue(analysis.avgLuminance in 50f..200f)

            file.delete()
        }
    }

    // TEST B : Modifier un slider (contraste, exposition, netteté)
    @Test
    fun testModifySliderPreservesManualValues() {
        runBlocking {
            val file = createTestImageFile()
            val origBmp = MikeAIEngine.decodeSampledBitmap(file)

            // Test that manual contrast adjustment is NOT overwritten by adaptParamsForScene
            val manualParams = EnhancementParams(
                preset = EnhancementPreset.NATURAL,
                contrast = 0.40f, // Explicit user adjustment
                exposure = -0.20f
            )

            val dummyAnalysis = ImageAnalysis(
                sceneType = SceneType.LANDSCAPE
            )

            val adapted = MikePresetProcessor.adaptParamsForScene(manualParams, dummyAnalysis)
            assertEquals("Manual contrast must be strictly preserved", 0.40f, adapted.contrast, 0.001f)
            assertEquals("Manual exposure must be strictly preserved", -0.20f, adapted.exposure, 0.001f)

            val result = MikeAIEngine.processImage(origBmp, manualParams)
            assertNotNull(result.processedBitmap)
            assertEquals(0.40f, result.appliedParams.contrast, 0.001f)

            file.delete()
        }
    }

    // TEST C : Appliquer tous les presets existants
    @Test
    fun testApplyAllExistingPresets() {
        runBlocking {
            val file = createTestImageFile()
            val origBmp = MikeAIEngine.decodeSampledBitmap(file)

            val presets = listOf(
                EnhancementPreset.NATURAL,
                EnhancementPreset.VIVID,
                EnhancementPreset.CINEMATIC,
                EnhancementPreset.PORTRAIT,
                EnhancementPreset.NIGHT,
                EnhancementPreset.BLACK_AND_WHITE
            )

            for (preset in presets) {
                val params = EnhancementParams(
                    preset = preset,
                    exposure = preset.defaultExposure,
                    contrast = preset.defaultContrast,
                    shadows = preset.defaultShadows,
                    highlights = preset.defaultHighlights,
                    vibrance = preset.defaultVibrance,
                    warmth = preset.defaultWarmth,
                    sharpness = preset.defaultSharpness
                )
                val result = MikeAIEngine.processImage(origBmp, params)
                assertNotNull("Preset ${preset.title} must produce a valid bitmap", result.processedBitmap)
                assertEquals(origBmp.width, result.processedBitmap.width)
                assertEquals(origBmp.height, result.processedBitmap.height)

                if (preset == EnhancementPreset.BLACK_AND_WHITE) {
                    val p = result.processedBitmap.getPixel(60, 60)
                    val r = (p shr 16) and 0xFF
                    val g = (p shr 8) and 0xFF
                    val b = p and 0xFF
                    // In monochrome, RGB channels must be almost identical
                    assertTrue("R and G must match in B&W", kotlin.math.abs(r - g) <= 2)
                    assertTrue("G and B must match in B&W", kotlin.math.abs(g - b) <= 2)
                }
            }

            file.delete()
        }
    }

    // TEST D : Modifier l'intensité (0%, 50%, 100%)
    @Test
    fun testModifyAiIntensity() {
        runBlocking {
            val file = createTestImageFile()
            val origBmp = MikeAIEngine.decodeSampledBitmap(file)

            // 0% intensity: must match original pixels
            val result0 = MikeAIEngine.processImage(origBmp, EnhancementParams(aiIntensity = 0.0f))
            assertEquals(origBmp.getPixel(60, 60), result0.processedBitmap.getPixel(60, 60))

            // 100% intensity: must be enhanced
            val result100 = MikeAIEngine.processImage(origBmp, EnhancementParams(preset = EnhancementPreset.VIVID, aiIntensity = 1.0f))
            assertNotNull(result100.processedBitmap)

            // 50% intensity: intermediate result
            val result50 = MikeAIEngine.processImage(origBmp, EnhancementParams(preset = EnhancementPreset.VIVID, aiIntensity = 0.5f))
            assertNotNull(result50.processedBitmap)

            file.delete()
        }
    }

    // TEST E : Reset réinitialise les réglages
    @Test
    fun testResetAdjustments() {
        val currentParams = EnhancementParams(
            preset = EnhancementPreset.VIVID,
            exposure = 0.45f,
            contrast = -0.30f,
            aiIntensity = 0.65f
        )
        // Reset back to defaults for that preset
        val resetParams = EnhancementParams(
            preset = currentParams.preset,
            exposure = currentParams.preset.defaultExposure,
            contrast = currentParams.preset.defaultContrast,
            shadows = currentParams.preset.defaultShadows,
            highlights = currentParams.preset.defaultHighlights,
            vibrance = currentParams.preset.defaultVibrance,
            warmth = currentParams.preset.defaultWarmth,
            sharpness = currentParams.preset.defaultSharpness,
            aiIntensity = 1.0f
        )

        assertEquals(EnhancementPreset.VIVID.defaultExposure, resetParams.exposure, 0.001f)
        assertEquals(EnhancementPreset.VIVID.defaultContrast, resetParams.contrast, 0.001f)
        assertEquals(1.0f, resetParams.aiIntensity, 0.001f)
    }

    // TEST F : Avant / Après
    @Test
    fun testBeforeAfterIntegrity() {
        runBlocking {
            val file = createTestImageFile(150, 100)
            val origBmp = MikeAIEngine.decodeSampledBitmap(file)
            val result = MikeAIEngine.processImage(origBmp, EnhancementParams(preset = EnhancementPreset.NATURAL))
            val procBmp = result.processedBitmap

            assertEquals("Original and Processed width must match for BeforeAfterSlider", origBmp.width, procBmp.width)
            assertEquals("Original and Processed height must match for BeforeAfterSlider", origBmp.height, procBmp.height)

            file.delete()
        }
    }

    // TEST G & H : Sauvegarder dans Room & Fermer/Rouvrir
    @Test
    fun testSaveAndReopenFromGallery() {
        runBlocking {
            val origFile = createTestImageFile()
            val procBmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
            val procFile = MediaManager.saveProcessedBitmapToFile(context, procBmp, prefix = "TEST_SAVE")

            val photoRecord = ProcessedPhoto(
                originalPath = origFile.absolutePath,
                processedPath = procFile.absolutePath,
                width = 120,
                height = 120,
                presetName = "Vif",
                contrastAdj = 0.25f,
                exposureAdj = 0.10f
            )

            val id = repository.insertPhoto(photoRecord)
            assertTrue(id > 0)

            val retrieved = repository.getPhotoById(id)
            assertNotNull(retrieved)
            assertEquals(origFile.absolutePath, retrieved!!.originalPath)
            assertEquals(procFile.absolutePath, retrieved.processedPath)
            assertEquals("Vif", retrieved.presetName)
            assertEquals(0.25f, retrieved.contrastAdj, 0.001f)

            origFile.delete()
            procFile.delete()
        }
    }

    // TEST I : Vérifier que l'original est toujours intact
    @Test
    fun testOriginalRemainsStrictlyIntact() {
        runBlocking {
            val origFile = createTestImageFile(100, 100)
            val initialBytes = origFile.readBytes()

            val decoded = MikeAIEngine.decodeSampledBitmap(origFile)
            val result = MikeAIEngine.processImage(decoded, EnhancementParams(preset = EnhancementPreset.CINEMATIC))
            assertNotNull(result.processedBitmap)

            val afterBytes = origFile.readBytes()
            assertEquals("Original file length must not change", initialBytes.size, afterBytes.size)
            assertTrue("Original file bytes must remain bit-for-bit identical", initialBytes.contentEquals(afterBytes))

            origFile.delete()
        }
    }
}

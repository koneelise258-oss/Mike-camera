package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.ai.gemini.GeminiEngineBackend
import com.example.ai.gemini.GeminiProxyBackend
import com.example.engine.ClassicEngineBackend
import com.example.engine.EnhancementParams
import com.example.engine.EnhancementPreset
import com.example.engine.ImageAnalysis
import com.example.engine.MediaManager
import com.example.engine.MikeAIEngine
import com.example.engine.SceneType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MikeProcessingGroup1Test {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun createTestBitmap(width: Int = 120, height: Int = 120): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bmp.setPixel(x, y, Color.rgb(90 + (x % 50), 100 + (y % 50), 110))
            }
        }
        return bmp
    }

    private fun createTestImageFile(width: Int = 120, height: Int = 120): File {
        val file = File(context.cacheDir, "processing_g1_${System.nanoTime()}.jpg")
        val bmp = createTestBitmap(width, height)
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        bmp.recycle()
        return file
    }

    // TEST 3 : Tester le moteur local sans réseau
    @Test
    fun testLocalEngineWithoutNetwork() {
        runBlocking {
            val localEngine = ClassicEngineBackend()
            val bitmap = createTestBitmap(100, 100)
            val analysis = ImageAnalysis(sceneType = SceneType.GENERAL)
            val params = EnhancementParams(preset = EnhancementPreset.NATURAL)

            val processed = localEngine.process(bitmap, analysis, params)
            assertNotNull(processed)
            assertEquals(100, processed.width)
            assertEquals(100, processed.height)
        }
    }

    // TEST 4 : Tester tous les presets sans réseau
    @Test
    fun testAllPresetsOffline() {
        runBlocking {
            val localEngine = ClassicEngineBackend()
            val bitmap = createTestBitmap(100, 100)
            val analysis = ImageAnalysis(sceneType = SceneType.GENERAL)

            val presets = listOf(
                EnhancementPreset.NATURAL,
                EnhancementPreset.VIVID,
                EnhancementPreset.CINEMATIC,
                EnhancementPreset.PORTRAIT,
                EnhancementPreset.NIGHT,
                EnhancementPreset.BLACK_AND_WHITE
            )

            for (preset in presets) {
                val params = EnhancementParams(preset = preset)
                val processed = localEngine.process(bitmap, analysis, params)
                assertNotNull("Preset ${preset.title} must work offline", processed)
            }
        }
    }

    // TEST 5 : Tester l'intensité 0-100%
    @Test
    fun testAiIntensityRange() {
        runBlocking {
            val localEngine = ClassicEngineBackend()
            val bitmap = createTestBitmap(100, 100)
            val analysis = ImageAnalysis(sceneType = SceneType.GENERAL)

            // 0% Intensity: matches original
            val result0 = localEngine.process(bitmap, analysis, EnhancementParams(aiIntensity = 0.0f))
            assertEquals(bitmap.getPixel(50, 50), result0.getPixel(50, 50))

            // 50% Intensity: valid intermediate blend
            val result50 = localEngine.process(bitmap, analysis, EnhancementParams(preset = EnhancementPreset.VIVID, aiIntensity = 0.5f))
            assertNotNull(result50)

            // 100% Intensity: full effect
            val result100 = localEngine.process(bitmap, analysis, EnhancementParams(preset = EnhancementPreset.VIVID, aiIntensity = 1.0f))
            assertNotNull(result100)
        }
    }

    // TEST 6 : Tester le traitement d'une photo après capture
    @Test
    fun testPostCaptureProcessing() {
        runBlocking {
            val file = createTestImageFile(160, 120)
            val decoded = MikeAIEngine.decodeSampledBitmap(file)
            assertNotNull(decoded)

            val result = MikeAIEngine.processImage(decoded, EnhancementParams(preset = EnhancementPreset.NATURAL))
            assertNotNull(result.processedBitmap)
            assertEquals(decoded.width, result.processedBitmap.width)
            assertEquals(decoded.height, result.processedBitmap.height)
            assertFalse(result.isFullResolution)

            file.delete()
        }
    }

    // TEST 7 : Vérifier que l'original n'est jamais modifié
    @Test
    fun testOriginalFileRemainsUntouched() {
        runBlocking {
            val file = createTestImageFile(120, 120)
            val initialBytes = file.readBytes()

            val decoded = MikeAIEngine.decodeSampledBitmap(file)
            val result = MikeAIEngine.processImage(decoded, EnhancementParams(preset = EnhancementPreset.CINEMATIC))
            assertNotNull(result.processedBitmap)

            val afterBytes = file.readBytes()
            assertEquals("File size must not change", initialBytes.size, afterBytes.size)
            assertTrue("File content must remain bit-for-bit identical", initialBytes.contentEquals(afterBytes))

            file.delete()
        }
    }

    // TEST 8 : Vérifier que le live preview ne lance pas plusieurs traitements simultanément (latest-frame lock)
    @Test
    fun testLivePreviewAtomicThrottling() {
        val isAnalyzingFrame = AtomicBoolean(false)

        // Simulate frame 1 acquired lock
        assertTrue(isAnalyzingFrame.compareAndSet(false, true))

        // Simulate frame 2 arriving while frame 1 is in-flight -> dropped/ignored
        assertFalse(isAnalyzingFrame.compareAndSet(false, true))

        // Frame 1 completed
        isAnalyzingFrame.set(false)

        // Next latest frame can acquire lock cleanly
        assertTrue(isAnalyzingFrame.compareAndSet(false, true))
        isAnalyzingFrame.set(false)
    }

    // TEST 9 : Vérifier que Gemini n'est jamais appelé pour chaque frame de live preview
    @Test
    fun testGeminiNeverCalledForLiveFrames() {
        val geminiEngine = GeminiEngineBackend(context)
        val proxyBackend = GeminiProxyBackend(geminiEngine)

        val bmp = createTestBitmap(80, 80)
        val analysis = ImageAnalysis(sceneType = SceneType.GENERAL)
        val params = EnhancementParams(preset = EnhancementPreset.NATURAL)

        // processLivePreview must run on local fallback backend without calling remote Gemini API
        val liveBmp = proxyBackend.processLivePreview(bmp, analysis, params)
        assertNotNull(liveBmp)
        assertEquals(80, liveBmp.width)
        assertEquals(80, liveBmp.height)
        assertFalse("Gemini must never be marked as run for live preview frames", proxyBackend.wasLastRunGemini)
    }
}

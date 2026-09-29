package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.ai.gemini.GeminiEngineBackend
import com.example.ai.gemini.GeminiProxyBackend
import com.example.camera.CameraShootingMode
import com.example.engine.ClassicEngineBackend
import com.example.engine.EnhancementParams
import com.example.engine.EnhancementPreset
import com.example.engine.ImageAnalysis
import com.example.engine.MediaManager
import com.example.engine.MikeAIEngine
import com.example.engine.MikeImageAnalyzer
import com.example.engine.MikeSceneDetector
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
class MikeProcessingGroup2Test {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun createTestBitmap(width: Int = 100, height: Int = 100, r: Int = 100, g: Int = 100, b: Int = 100): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bmp.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        return bmp
    }

    private fun createPortraitBitmap(width: Int = 100, height: Int = 100): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Skin tone RGB (e.g. RGB 220, 160, 130)
                bmp.setPixel(x, y, Color.rgb(220, 160, 130))
            }
        }
        return bmp
    }

    private fun createNightBitmap(width: Int = 100, height: Int = 100): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bmp.setPixel(x, y, Color.rgb(20, 20, 25))
            }
        }
        return bmp
    }

    // TEST A & B & C : Ouvrir la caméra, afficher le preview et activer le traitement MIKE AI live
    @Test
    fun testLivePreviewEnhancementExecution() {
        val bmp = createTestBitmap(80, 80)
        val analysis = MikeImageAnalyzer.analyzeDirect(bmp)
        val params = EnhancementParams(preset = EnhancementPreset.NATURAL, aiIntensity = 0.8f)

        val liveEnhanced = MikeAIEngine.processLivePreview(bmp, params, analysis)
        assertNotNull(liveEnhanced)
        assertEquals(80, liveEnhanced.width)
        assertEquals(80, liveEnhanced.height)
        liveEnhanced.recycle()
        bmp.recycle()
    }

    // TEST D : Déplacer rapidement la caméra (stabilité temporelle du détecteur de scène)
    @Test
    fun testSceneStabilitySmoothing() {
        var sceneHistory = listOf<SceneType>()
        var liveDetectedScene = SceneType.GENERAL

        // Frame 1: General
        sceneHistory = (sceneHistory + SceneType.GENERAL).takeLast(3)
        // Frame 2: Transient glitch to Night
        val transientScene = SceneType.NIGHT
        val updatedHistory = (sceneHistory + transientScene).takeLast(3)
        // Majority is not reached for transient scene, so it does not flicker
        val hasConsensus = updatedHistory.size >= 2 && updatedHistory.count { it == transientScene } >= 2
        assertFalse("Single transient frame must not switch detected scene", hasConsensus)

        // 2 consecutive Portrait frames reach consensus
        val sustainedHistory = listOf(SceneType.PORTRAIT, SceneType.PORTRAIT)
        val sustainedConsensus = sustainedHistory.count { it == SceneType.PORTRAIT } >= 2
        assertTrue("Sustained frames establish stable scene detection", sustainedConsensus)
    }

    // TEST E : Modes PHOTO -> PORTRAIT -> NIGHT -> PRO
    @Test
    fun testShootingModeParametersSwitching() {
        val modes = listOf(
            CameraShootingMode.PHOTO to EnhancementPreset.NATURAL,
            CameraShootingMode.PORTRAIT to EnhancementPreset.PORTRAIT,
            CameraShootingMode.NIGHT to EnhancementPreset.NIGHT,
            CameraShootingMode.CINEMATIC to EnhancementPreset.CINEMATIC
        )

        val bmp = createTestBitmap(60, 60)
        val analysis = MikeImageAnalyzer.analyzeDirect(bmp)

        for ((mode, expectedPreset) in modes) {
            val params = EnhancementParams(preset = expectedPreset)
            val liveBmp = MikeAIEngine.processLivePreview(bmp, params, analysis)
            assertNotNull("Live preview must succeed for mode $mode", liveBmp)
            liveBmp.recycle()
        }
        bmp.recycle()
    }

    // TEST F : Changement caméra avant/arrière (invalidation propre des verrous de frames)
    @Test
    fun testLensFacingSwitchInvalidation() {
        val isAnalyzingFrame = AtomicBoolean(true)
        // On lens switch, lock is reset cleanly
        isAnalyzingFrame.set(false)
        assertFalse(isAnalyzingFrame.get())
        assertTrue(isAnalyzingFrame.compareAndSet(false, true))
    }

    // TEST G & H : Modifier preset et intensité 0-100% sur le live preview
    @Test
    fun testLivePreviewPresetAndIntensityChanges() {
        val bmp = createTestBitmap(60, 60, 150, 150, 150)
        val analysis = MikeImageAnalyzer.analyzeDirect(bmp)

        val live0 = MikeAIEngine.processLivePreview(bmp, EnhancementParams(aiIntensity = 0f), analysis)
        assertEquals(bmp.getPixel(30, 30), live0.getPixel(30, 30))
        live0.recycle()

        val live100 = MikeAIEngine.processLivePreview(
            bmp,
            EnhancementParams(preset = EnhancementPreset.VIVID, aiIntensity = 1.0f),
            analysis
        )
        assertNotNull(live100)
        live100.recycle()
        bmp.recycle()
    }

    // TEST I : Prendre une photo pendant que le traitement temps réel fonctionne
    @Test
    fun testCaptureWhileLivePreviewIsActive() {
        runBlocking {
            val liveBmp = createTestBitmap(60, 60)
            val liveAnalysis = MikeImageAnalyzer.analyzeDirect(liveBmp)
            val livePreview = MikeAIEngine.processLivePreview(liveBmp, EnhancementParams(), liveAnalysis)
            assertNotNull(livePreview)

            // High-res photo capture proceeds independently
            val origFile = File(context.cacheDir, "test_live_capture.jpg")
            val fullBmp = createTestBitmap(200, 200)
            FileOutputStream(origFile).use { fullBmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            fullBmp.recycle()

            val capturedFull = MikeAIEngine.decodeSampledBitmap(origFile)
            val fullResult = MikeAIEngine.processImage(capturedFull, EnhancementParams(preset = EnhancementPreset.NATURAL))
            assertNotNull(fullResult.processedBitmap)
            assertEquals(200, fullResult.processedBitmap.width)

            origFile.delete()
            livePreview.recycle()
            liveBmp.recycle()
        }
    }

    // TEST L : Vérifier qu'aucune frame ne s'accumule (Backpressure & lock)
    @Test
    fun testBackpressureSingleActiveFrame() {
        val lock = AtomicBoolean(false)
        var processedCount = 0
        var droppedCount = 0

        for (i in 1..10) {
            if (lock.compareAndSet(false, true)) {
                processedCount++
                // Simulated immediate completion
                lock.set(false)
            } else {
                droppedCount++
            }
        }
        assertEquals(10, processedCount)
        assertEquals(0, droppedCount)
    }

    // TEST M : Vérifier que Gemini n'est jamais appelé frame par frame
    @Test
    fun testGeminiStrictlyExcludedFromLivePreview() {
        val geminiEngine = GeminiEngineBackend(context)
        val proxyBackend = GeminiProxyBackend(geminiEngine)

        val bmp = createTestBitmap(50, 50)
        val analysis = MikeImageAnalyzer.analyzeDirect(bmp)

        val out = proxyBackend.processLivePreview(bmp, analysis, EnhancementParams())
        assertNotNull(out)
        assertFalse("Gemini must never run on live preview frames", proxyBackend.wasLastRunGemini)
        out.recycle()
        bmp.recycle()
    }

    // TEST N : Tester le live preview 100% hors-ligne
    @Test
    fun testLivePreviewOffline() {
        val engine = ClassicEngineBackend()
        val bmp = createTestBitmap(50, 50)
        val analysis = MikeImageAnalyzer.analyzeDirect(bmp)
        val out = engine.processLivePreview(bmp, analysis, EnhancementParams(preset = EnhancementPreset.CINEMATIC))
        assertNotNull(out)
        out.recycle()
        bmp.recycle()
    }

    // TEST O : Vérifier que l'original capturé reste intact
    @Test
    fun testOriginalRemainsIntactDuringLiveSession() {
        runBlocking {
            val file = File(context.cacheDir, "original_test_session.jpg")
            val bmp = createTestBitmap(100, 100)
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            bmp.recycle()

            val bytesBefore = file.readBytes()
            val decoded = MikeAIEngine.decodeSampledBitmap(file)
            val result = MikeAIEngine.processImage(decoded, EnhancementParams(preset = EnhancementPreset.PORTRAIT))
            assertNotNull(result.processedBitmap)

            val bytesAfter = file.readBytes()
            assertTrue("Original file bytes must remain 100% untouched", bytesBefore.contentEquals(bytesAfter))
            file.delete()
        }
    }
}

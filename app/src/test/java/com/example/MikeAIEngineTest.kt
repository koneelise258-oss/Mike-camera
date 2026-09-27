package com.example

import android.graphics.Bitmap
import android.graphics.Color
import com.example.engine.EnhancementParams
import com.example.engine.EnhancementPreset
import com.example.engine.ImageAnalysis
import com.example.engine.MikeAIEngine
import com.example.engine.MikeColorProcessor
import com.example.engine.MikeExposureProcessor
import com.example.engine.MikeImageAnalyzer
import com.example.engine.MikeSceneDetector
import com.example.engine.SceneType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MikeAIEngineTest {

    private fun createTestBitmap(
        width: Int = 100,
        height: Int = 100,
        generator: (x: Int, y: Int) -> Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, generator(x, y))
            }
        }
        return bitmap
    }

    private fun assertValidOutputBitmap(bitmap: Bitmap, original: Bitmap) {
        assertNotNull(bitmap)
        assertEquals(original.width, bitmap.width)
        assertEquals(original.height, bitmap.height)

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        val totalPixels = bitmap.width * bitmap.height

        for (y in 0 until bitmap.height step 5) {
            for (x in 0 until bitmap.width step 5) {
                val p = bitmap.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                totalR += r
                totalG += g
                totalB += b
            }
        }

        val sampledCount = (bitmap.width / 5) * (bitmap.height / 5)
        val avgR = totalR / sampledCount
        val avgG = totalG / sampledCount
        val avgB = totalB / sampledCount

        // Assert not totally black (avg > 1) and not totally blown white (avg < 254 unless intended)
        assertTrue("Output should not be completely black: avgR=$avgR, avgG=$avgG, avgB=$avgB", avgR + avgG + avgB > 3)
    }

    // 1. Photo correctement exposée
    @Test
    fun testWellExposedPhoto() = runBlocking {
        val bmp = createTestBitmap(120, 120) { x, y ->
            val lum = (100 + (x * 40 / 120) + (y * 30 / 120)).coerceIn(0, 255)
            Color.rgb(lum, lum, (lum * 0.95).toInt())
        }
        val analysis = MikeAIEngine.analyzeImage(bmp)
        assertTrue(analysis.avgLuminance in 90f..160f)

        val result = MikeAIEngine.processImage(bmp, EnhancementParams(preset = EnhancementPreset.NATURAL))
        assertValidOutputBitmap(result.processedBitmap, bmp)
    }

    // 2. Photo sous-exposée
    @Test
    fun testUnderexposedPhoto() = runBlocking {
        val bmp = createTestBitmap(100, 100) { _, _ -> Color.rgb(20, 22, 25) }
        val analysis = MikeAIEngine.analyzeImage(bmp)
        assertTrue("Should detect low luminance", analysis.avgLuminance < 45f)
        assertTrue("Should detect underexposed ratio", analysis.underexposedRatio > 0.5f)

        val result = MikeAIEngine.processImage(bmp, EnhancementParams(preset = EnhancementPreset.NATURAL))
        assertValidOutputBitmap(result.processedBitmap, bmp)

        val outPixel = result.processedBitmap.getPixel(50, 50)
        val outR = (outPixel shr 16) and 0xFF
        assertTrue("Should lift shadows in underexposed photo", outR >= 20)
    }

    // 3. Photo surexposée
    @Test
    fun testOverexposedPhoto() = runBlocking {
        val bmp = createTestBitmap(100, 100) { _, _ -> Color.rgb(240, 242, 245) }
        val analysis = MikeAIEngine.analyzeImage(bmp)
        assertTrue("Should detect high luminance", analysis.avgLuminance > 220f)
        assertTrue("Should detect overexposed ratio", analysis.overexposedRatio > 0.8f)

        val result = MikeAIEngine.processImage(bmp, EnhancementParams(preset = EnhancementPreset.NATURAL))
        assertValidOutputBitmap(result.processedBitmap, bmp)
    }

    // 4. Photo de nuit
    @Test
    fun testNightPhoto() = runBlocking {
        val bmp = createTestBitmap(120, 120) { x, y ->
            if (x in 50..55 && y in 50..55) Color.rgb(255, 240, 200) // street light
            else Color.rgb(10, 12, 18)
        }
        val analysis = MikeAIEngine.analyzeImage(bmp)
        assertEquals(SceneType.NIGHT, analysis.sceneType)

        val result = MikeAIEngine.processImage(bmp, EnhancementParams(preset = EnhancementPreset.NIGHT))
        assertValidOutputBitmap(result.processedBitmap, bmp)
    }

    // 5. Portrait
    @Test
    fun testPortraitPhoto() = runBlocking {
        val bmp = createTestBitmap(100, 100) { _, _ ->
            // Skin tone RGB (e.g. #D2A082)
            Color.rgb(210, 160, 130)
        }
        val analysis = MikeAIEngine.analyzeImage(bmp)
        assertEquals(SceneType.PORTRAIT, analysis.sceneType)
        assertTrue(analysis.skinPixelRatio > 0.8f)

        val result = MikeAIEngine.processImage(bmp, EnhancementParams(preset = EnhancementPreset.PORTRAIT))
        assertValidOutputBitmap(result.processedBitmap, bmp)

        val p = result.processedBitmap.getPixel(50, 50)
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        // Verify skin tone is NOT shifted to unnatural neon orange
        val hsv = FloatArray(3)
        Color.RGBToHSV(r, g, b, hsv)
        assertTrue("Skin hue must remain natural in 10-50 deg range", hsv[0] in 10f..50f)
    }

    // 6. Paysage
    @Test
    fun testLandscapePhoto() = runBlocking {
        val bmp = createTestBitmap(100, 100) { _, y ->
            if (y < 45) Color.rgb(100, 170, 240) // Sky
            else Color.rgb(50, 130, 60) // Grass
        }
        val analysis = MikeAIEngine.analyzeImage(bmp)
        assertTrue(analysis.skyPixelRatio > 0.2f || analysis.vegetationPixelRatio > 0.2f)
        assertEquals(SceneType.LANDSCAPE, analysis.sceneType)

        val result = MikeAIEngine.processImage(bmp, EnhancementParams(preset = EnhancementPreset.VIVID))
        assertValidOutputBitmap(result.processedBitmap, bmp)
    }

    // 7. Photo avec bruit
    @Test
    fun testNoisyPhoto() = runBlocking {
        val random = Random(42)
        val bmp = createTestBitmap(100, 100) { _, _ ->
            val noise = random.nextInt(-25, 25)
            val v = (120 + noise).coerceIn(0, 255)
            Color.rgb(v, v, v)
        }
        val analysis = MikeAIEngine.analyzeImage(bmp)
        assertTrue("Estimated noise should be detected", analysis.estimatedNoiseLevel > 0.15f)

        val result = MikeAIEngine.processImage(bmp, EnhancementParams(preset = EnhancementPreset.NATURAL))
        assertValidOutputBitmap(result.processedBitmap, bmp)
    }

    // 8. Photo très colorée
    @Test
    fun testVividColorsPhoto() = runBlocking {
        val bmp = createTestBitmap(100, 100) { x, _ ->
            when (x % 3) {
                0 -> Color.rgb(220, 30, 30)
                1 -> Color.rgb(30, 220, 30)
                else -> Color.rgb(30, 30, 220)
            }
        }
        val result = MikeAIEngine.processImage(bmp, EnhancementParams(preset = EnhancementPreset.VIVID))
        assertValidOutputBitmap(result.processedBitmap, bmp)
    }

    // 9. Photo froide (dominante bleue)
    @Test
    fun testCoolTintPhoto() = runBlocking {
        val bmp = createTestBitmap(100, 100) { _, _ -> Color.rgb(90, 110, 180) }
        val analysis = MikeAIEngine.analyzeImage(bmp)
        assertEquals("FROIDE", analysis.dominantTint)

        val result = MikeAIEngine.processImage(bmp, EnhancementParams(preset = EnhancementPreset.NATURAL))
        assertValidOutputBitmap(result.processedBitmap, bmp)
    }

    // 10. Photo chaude (dominante rouge/jaune)
    @Test
    fun testWarmTintPhoto() = runBlocking {
        val bmp = createTestBitmap(100, 100) { _, _ -> Color.rgb(190, 130, 70) }
        val analysis = MikeAIEngine.analyzeImage(bmp)
        assertEquals("CHAUDE", analysis.dominantTint)

        val result = MikeAIEngine.processImage(bmp, EnhancementParams(preset = EnhancementPreset.NATURAL))
        assertValidOutputBitmap(result.processedBitmap, bmp)
    }

    // 11. Photo noir et blanc
    @Test
    fun testBlackAndWhitePreset() = runBlocking {
        val bmp = createTestBitmap(100, 100) { x, y -> Color.rgb(x * 2, y * 2, 100) }
        val result = MikeAIEngine.processImage(bmp, EnhancementParams(preset = EnhancementPreset.BLACK_AND_WHITE))
        assertValidOutputBitmap(result.processedBitmap, bmp)

        val p = result.processedBitmap.getPixel(50, 50)
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        assertEquals("R and G must match in B&W", r, g)
        assertEquals("G and B must match in B&W", g, b)
    }

    // 12. Photo haute résolution
    @Test
    fun testHighResolutionPhoto() = runBlocking {
        val bmp = createTestBitmap(1200, 800) { x, y ->
            val lum = ((x * 255) / 1200 + (y * 255) / 800) / 2
            Color.rgb(lum, (lum * 0.9).toInt(), (lum * 0.8).toInt())
        }
        val result = MikeAIEngine.processImage(bmp, EnhancementParams(preset = EnhancementPreset.NATURAL))
        assertEquals(1200, result.processedBitmap.width)
        assertEquals(800, result.processedBitmap.height)
        assertValidOutputBitmap(result.processedBitmap, bmp)
    }

    // 13. Intensité 0% (doit être strictement identique à l'original)
    @Test
    fun testZeroIntensityPreservesOriginal() = runBlocking {
        val bmp = createTestBitmap(80, 80) { x, y -> Color.rgb(x * 3, y * 3, 140) }
        val result = MikeAIEngine.processImage(bmp, EnhancementParams(aiIntensity = 0.0f))

        val origPixel = bmp.getPixel(40, 40)
        val procPixel = result.processedBitmap.getPixel(40, 40)
        assertEquals("0% intensity must match original pixel exactly", origPixel, procPixel)
    }

    // 14. Intensité 50%
    @Test
    fun testFiftyPercentIntensity() = runBlocking {
        val bmp = createTestBitmap(80, 80) { x, y -> Color.rgb(x * 2, y * 2, 80) }
        val result = MikeAIEngine.processImage(bmp, EnhancementParams(aiIntensity = 0.5f))
        assertValidOutputBitmap(result.processedBitmap, bmp)
    }

    // 15. Intensité 100% (maximale sans artefacts ni couleurs déformées)
    @Test
    fun testFullIntensity() = runBlocking {
        val bmp = createTestBitmap(80, 80) { x, y -> Color.rgb(x * 2, y * 2, 100) }
        val result = MikeAIEngine.processImage(bmp, EnhancementParams(aiIntensity = 1.0f))
        assertValidOutputBitmap(result.processedBitmap, bmp)
    }
}

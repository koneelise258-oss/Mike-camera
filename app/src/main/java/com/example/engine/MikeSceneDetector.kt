package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

object MikeSceneDetector {

    /**
     * Cautiously detects the photographic scene type from statistical analysis.
     * Defaults to GENERAL if confidence is low.
     */
    suspend fun detect(bitmap: Bitmap): Pair<SceneType, Float> = withContext(Dispatchers.Default) {
        detectDirect(bitmap)
    }

    fun detectDirect(bitmap: Bitmap): Pair<SceneType, Float> {
        val width = bitmap.width
        val height = bitmap.height
        val totalSampledTarget = 6000
        val sampleStepX = max(1, width / 80)
        val sampleStepY = max(1, height / 80)

        var darkPixels = 0
        var brightPixels = 0
        var skinPixels = 0
        var skyPixels = 0
        var vegetationPixels = 0
        var documentContrastEdges = 0
        var totalSamples = 0
        var sumLum = 0.0

        val hsv = FloatArray(3)

        for (y in 0 until height step sampleStepY) {
            for (x in 0 until width step sampleStepX) {
                val p = bitmap.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF

                val lum = 0.299 * r + 0.587 * g + 0.114 * b
                sumLum += lum
                totalSamples++

                if (lum < 35.0) darkPixels++
                if (lum > 225.0) brightPixels++

                Color.RGBToHSV(r, g, b, hsv)
                val hue = hsv[0]
                val sat = hsv[1]
                val value = hsv[2]

                // Skin detection (Hue in 12-48 deg, natural saturation and brightness)
                if (hue in 12f..48f && sat in 0.14f..0.65f && value in 0.22f..0.95f) {
                    skinPixels++
                }

                // Sky detection (top 50% of the image, blueish)
                if (y < height * 0.5 && hue in 180f..240f && sat in 0.18f..0.90f && value > 0.35f) {
                    skyPixels++
                }

                // Vegetation detection (greenish hue)
                if (hue in 75f..165f && sat in 0.20f..0.90f && value in 0.20f..0.90f) {
                    vegetationPixels++
                }

                // Check for document-like high contrast (near black or near white with low saturation)
                if ((lum < 40.0 || lum > 215.0) && sat < 0.15f) {
                    documentContrastEdges++
                }
            }
        }

        if (totalSamples == 0) return Pair(SceneType.GENERAL, 0.5f)

        val avgLum = sumLum / totalSamples
        val darkRatio = darkPixels.toFloat() / totalSamples
        val skinRatio = skinPixels.toFloat() / totalSamples
        val skyRatio = skyPixels.toFloat() / totalSamples
        val vegRatio = vegetationPixels.toFloat() / totalSamples
        val docRatio = documentContrastEdges.toFloat() / totalSamples

        return when {
            // Night / Low light
            darkRatio > 0.55 || (avgLum < 45.0 && darkRatio > 0.40) -> {
                val conf = (darkRatio.coerceAtMost(0.95f))
                Pair(SceneType.NIGHT, conf)
            }
            avgLum < 65.0 && darkRatio > 0.30 -> {
                Pair(SceneType.LOW_LIGHT, 0.75f)
            }
            // Portrait (faces / skin tones prominent)
            skinRatio > 0.16 -> {
                val conf = (skinRatio * 3.5f).coerceIn(0.65f, 0.95f)
                Pair(SceneType.PORTRAIT, conf)
            }
            // Landscape (sky + vegetation or open outdoors)
            skyRatio > 0.18 || (skyRatio > 0.08 && vegRatio > 0.15) || vegRatio > 0.25 -> {
                val conf = ((skyRatio + vegRatio) * 1.8f).coerceIn(0.60f, 0.90f)
                Pair(SceneType.LANDSCAPE, conf)
            }
            // Document (high ratio of pure b/w text or scan page)
            docRatio > 0.75 && skinRatio < 0.02 && satCheckIsLow(avgLum, docRatio) -> {
                Pair(SceneType.DOCUMENT, 0.85f)
            }
            // Default General
            else -> {
                Pair(SceneType.GENERAL, 0.80f)
            }
        }
    }

    private fun satCheckIsLow(avgLum: Double, docRatio: Float): Boolean {
        return docRatio > 0.80 && avgLum in 100.0..220.0
    }
}

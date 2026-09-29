package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object MikeImageAnalyzer {

    suspend fun analyze(bitmap: Bitmap): ImageAnalysis = withContext(Dispatchers.Default) {
        analyzeDirect(bitmap)
    }

    fun analyzeDirect(bitmap: Bitmap): ImageAnalysis {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height

        val histR = IntArray(256)
        val histG = IntArray(256)
        val histB = IntArray(256)
        val histLum = IntArray(256)

        var sumLum = 0.0
        var sumLumSq = 0.0
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var sumSat = 0.0
        var sampleCount = 0

        var darkPixels = 0
        var brightPixels = 0
        var skinPixels = 0
        var skyPixels = 0
        var vegPixels = 0

        val step = max(1, (width * height) / 40000)
        val hsv = FloatArray(3)

        var minLum = 255
        var maxLum = 0

        for (y in 0 until height step (height / 200).coerceAtLeast(1)) {
            for (x in 0 until width step (width / 200).coerceAtLeast(1)) {
                val p = bitmap.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF

                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)

                histR[r]++
                histG[g]++
                histB[b]++
                histLum[lum]++

                minLum = min(minLum, lum)
                maxLum = max(maxLum, lum)

                sumLum += lum
                sumLumSq += (lum * lum)
                sumR += r
                sumG += g
                sumB += b

                if (lum < 22) darkPixels++
                if (lum > 235) brightPixels++

                Color.RGBToHSV(r, g, b, hsv)
                val hue = hsv[0]
                val sat = hsv[1]
                val value = hsv[2]

                sumSat += sat
                sampleCount++

                // Skin
                if (hue in 12f..48f && sat in 0.14f..0.65f && value in 0.22f..0.95f) {
                    skinPixels++
                }
                // Sky
                if (y < height * 0.5 && hue in 180f..240f && sat in 0.18f..0.90f) {
                    skyPixels++
                }
                // Vegetation
                if (hue in 75f..165f && sat in 0.20f..0.90f) {
                    vegPixels++
                }
            }
        }

        val totalSamples = max(1, sampleCount)
        val avgLum = (sumLum / totalSamples).toFloat()
        val varianceLum = max(0.0, (sumLumSq / totalSamples) - (avgLum * avgLum))
        val contrastStdDev = sqrt(varianceLum).toFloat()

        val avgR = (sumR / totalSamples).toFloat()
        val avgG = (sumG / totalSamples).toFloat()
        val avgB = (sumB / totalSamples).toFloat()
        val avgSat = (sumSat / totalSamples).toFloat()
        val grayMean = (avgR + avgG + avgB) / 3.0f

        val colorCastR = if (grayMean > 0f) avgR / grayMean else 1.0f
        val colorCastG = if (grayMean > 0f) avgG / grayMean else 1.0f
        val colorCastB = if (grayMean > 0f) avgB / grayMean else 1.0f

        val dominantTint = when {
            colorCastR > 1.12f && colorCastB < 0.90f -> "CHAUDE"
            colorCastB > 1.12f && colorCastR < 0.90f -> "FROIDE"
            colorCastG > 1.15f -> "VERDATRE"
            colorCastR > 1.10f && colorCastB > 1.10f && colorCastG < 0.90f -> "MAGENTA"
            else -> "NEUTRE"
        }

        // Noise and Sharpness estimation using Laplacian & Local Variance
        val (estimatedNoise, sharpness, detailLevel) = estimateNoiseAndSharpness(bitmap)

        // Scene detection
        val (sceneType, sceneConfidence) = MikeSceneDetector.detectDirect(bitmap)

        return ImageAnalysis(
            sceneType = sceneType,
            sceneConfidence = sceneConfidence,
            avgLuminance = avgLum,
            minLuminance = minLum,
            maxLuminance = maxLum,
            underexposedRatio = darkPixels.toFloat() / totalSamples,
            overexposedRatio = brightPixels.toFloat() / totalSamples,
            contrastStdDev = contrastStdDev,
            dynamicRange = (maxLum - minLum).toFloat(),
            avgSaturation = avgSat,
            colorCastR = colorCastR,
            colorCastG = colorCastG,
            colorCastB = colorCastB,
            dominantTint = dominantTint,
            estimatedNoiseLevel = estimatedNoise,
            detailLevel = detailLevel,
            sharpness = sharpness,
            skinPixelRatio = skinPixels.toFloat() / totalSamples,
            skyPixelRatio = skyPixels.toFloat() / totalSamples,
            vegetationPixelRatio = vegPixels.toFloat() / totalSamples,
            width = width,
            height = height
        )
    }

    private fun estimateNoiseAndSharpness(bitmap: Bitmap): Triple<Float, Float, Float> {
        val w = bitmap.width
        val h = bitmap.height

        var totalLaplacian = 0.0
        var totalLaplacianSq = 0.0
        var edgePixels = 0
        var flatPatchVarSum = 0.0
        var flatPatchesCount = 0

        val sampleStep = max(2, min(w, h) / 100)

        for (y in sampleStep until h - sampleStep step sampleStep) {
            for (x in sampleStep until w - sampleStep step sampleStep) {
                val pC = bitmap.getPixel(x, y)
                val pL = bitmap.getPixel(x - 1, y)
                val pR = bitmap.getPixel(x + 1, y)
                val pT = bitmap.getPixel(x, y - 1)
                val pB = bitmap.getPixel(x, y + 1)

                val lumC = (0.299 * ((pC shr 16) and 0xFF) + 0.587 * ((pC shr 8) and 0xFF) + 0.114 * (pC and 0xFF)).toInt()
                val lumL = (0.299 * ((pL shr 16) and 0xFF) + 0.587 * ((pL shr 8) and 0xFF) + 0.114 * (pL and 0xFF)).toInt()
                val lumR = (0.299 * ((pR shr 16) and 0xFF) + 0.587 * ((pR shr 8) and 0xFF) + 0.114 * (pR and 0xFF)).toInt()
                val lumT = (0.299 * ((pT shr 16) and 0xFF) + 0.587 * ((pT shr 8) and 0xFF) + 0.114 * (pT and 0xFF)).toInt()
                val lumB = (0.299 * ((pB shr 16) and 0xFF) + 0.587 * ((pB shr 8) and 0xFF) + 0.114 * (pB and 0xFF)).toInt()

                val lap = abs(4 * lumC - lumL - lumR - lumT - lumB)

                if (lap > 12) {
                    totalLaplacian += lap
                    totalLaplacianSq += (lap * lap)
                    edgePixels++
                } else {
                    // Flat region variance is a prime indicator of sensor noise
                    val diff = abs(lumC - lumL) + abs(lumC - lumR) + abs(lumC - lumT) + abs(lumC - lumB)
                    flatPatchVarSum += diff
                    flatPatchesCount++
                }
            }
        }

        val estimatedNoise = if (flatPatchesCount > 0) {
            ((flatPatchVarSum / flatPatchesCount) / 16.0).toFloat().coerceIn(0.0f, 1.0f)
        } else 0.05f

        val sharpness = if (edgePixels > 0) {
            ((totalLaplacian / edgePixels) / 45.0).toFloat().coerceIn(0.05f, 1.0f)
        } else 0.3f

        val detailLevel = (edgePixels.toFloat() / max(1, (w / sampleStep) * (h / sampleStep))).coerceIn(0.0f, 1.0f)

        return Triple(estimatedNoise, sharpness, detailLevel)
    }
}

package com.example.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object MikeNoiseReducer {

    /**
     * Edge-preserving noise reduction.
     * Selectively filters high-frequency grain in flat/low-gradient areas while
     * strictly preserving edges, fine details, and textures.
     */
    fun reduceNoise(
        pixels: IntArray,
        width: Int,
        height: Int,
        analysis: ImageAnalysis,
        intensity: Float
    ) {
        val estimatedNoise = analysis.estimatedNoiseLevel
        val isLowLight = analysis.sceneType == SceneType.NIGHT || analysis.sceneType == SceneType.LOW_LIGHT

        // If noise is minimal and not in low-light, skip processing to retain 100% micro-textures
        if (estimatedNoise < 0.12f && !isLowLight) {
            return
        }

        val strength = (if (isLowLight) 0.65f else 0.40f) * intensity * estimatedNoise.coerceIn(0.2f, 1.0f)
        if (strength < 0.05f) return

        // Gradient threshold below which pixels are considered noise rather than edges
        val edgeThreshold = (18f + estimatedNoise * 20f).toInt()

        val copy = pixels.clone()
        val stride = width

        for (y in 1 until height - 1) {
            val rowOffset = y * stride
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                val centerP = copy[idx]
                val cR = (centerP shr 16) and 0xFF
                val cG = (centerP shr 8) and 0xFF
                val cB = centerP and 0xFF
                val cLum = (cR * 77 + cG * 150 + cB * 29) shr 8

                // Sample 4 cardinal neighbors
                val neighbors = intArrayOf(
                    copy[idx - 1],
                    copy[idx + 1],
                    copy[idx - stride],
                    copy[idx + stride]
                )

                var sumR = cR * 2f
                var sumG = cG * 2f
                var sumB = cB * 2f
                var totalWeight = 2f

                for (nP in neighbors) {
                    val nR = (nP shr 16) and 0xFF
                    val nG = (nP shr 8) and 0xFF
                    val nB = nP and 0xFF
                    val nLum = (nR * 77 + nG * 150 + nB * 29) shr 8

                    val lumDiff = abs(cLum - nLum)
                    if (lumDiff < edgeThreshold) {
                        // Gaussian-like bilateral photometric weight
                        val weight = 1.0f - (lumDiff.toFloat() / edgeThreshold)
                        sumR += nR * weight
                        sumG += nG * weight
                        sumB += nB * weight
                        totalWeight += weight
                    }
                }

                val filteredR = (sumR / totalWeight).toInt().coerceIn(0, 255)
                val filteredG = (sumG / totalWeight).toInt().coerceIn(0, 255)
                val filteredB = (sumB / totalWeight).toInt().coerceIn(0, 255)

                // Blend filtered result with original according to strength
                val finalR = (cR + (filteredR - cR) * strength).toInt().coerceIn(0, 255)
                val finalG = (cG + (filteredG - cG) * strength).toInt().coerceIn(0, 255)
                val finalB = (cB + (filteredB - cB) * strength).toInt().coerceIn(0, 255)

                pixels[idx] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
            }
        }
    }
}

package com.example.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object MikeDetailProcessor {

    /**
     * Applies halo-free adaptive micro-contrast and edge enhancement.
     */
    fun enhanceDetails(
        pixels: IntArray,
        width: Int,
        height: Int,
        analysis: ImageAnalysis,
        sharpnessParam: Float,
        intensity: Float
    ) {
        val effectiveSharpness = sharpnessParam * intensity
        if (effectiveSharpness <= 0.04f) return

        val amount = min(0.55f, effectiveSharpness * 0.60f)
        val lowThreshold = 6
        val highThreshold = 55
        val stride = width

        // Precompute luminance plane
        val luminance = IntArray(pixels.size)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            luminance[i] = (r * 77 + g * 150 + b * 29) shr 8
        }

        for (y in 1 until height - 1) {
            val rowOffset = y * stride
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                val centerLum = luminance[idx]

                // Avoid sharpening deep shadow noise (lum < 15)
                if (centerLum < 15) continue

                val laplacian = (centerLum shl 2) -
                        luminance[idx - 1] -
                        luminance[idx + 1] -
                        luminance[idx - stride] -
                        luminance[idx + stride]

                val absLap = abs(laplacian)

                // Only enhance intermediate details; clamp high contrast to eliminate halos
                if (absLap > lowThreshold) {
                    val scaleFactor = if (absLap > highThreshold) {
                        // Dampen sharp borders to prevent haloing
                        (highThreshold.toFloat() / absLap)
                    } else {
                        1.0f
                    }

                    val delta = (laplacian * amount * scaleFactor).toInt().coerceIn(-35, 35)

                    val p = pixels[idx]
                    val r = (((p shr 16) and 0xFF) + delta).coerceIn(0, 255)
                    val g = (((p shr 8) and 0xFF) + delta).coerceIn(0, 255)
                    val b = ((p and 0xFF) + delta).coerceIn(0, 255)

                    pixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }
    }
}

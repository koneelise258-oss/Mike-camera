package com.example.engine

import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

object MikeColorProcessor {

    data class ColorGains(
        val scaleR: Float,
        val scaleG: Float,
        val scaleB: Float
    )

    /**
     * Calculates white balance gains, respecting intentional scene lighting (e.g. sunsets or night).
     */
    fun calculateWhiteBalanceGains(
        analysis: ImageAnalysis,
        warmth: Float,
        intensity: Float
    ): ColorGains {
        // Gray-world correction strength
        val isIntentionalWarmAtmosphere = analysis.dominantTint == "CHAUDE" && analysis.avgSaturation > 0.45f
        val isNightAtmosphere = analysis.sceneType == SceneType.NIGHT || analysis.sceneType == SceneType.LOW_LIGHT

        val autoWbStrength = when {
            isIntentionalWarmAtmosphere -> 0.10f * intensity // Preserve sunset warmth
            isNightAtmosphere -> 0.15f * intensity
            else -> 0.35f * intensity
        }

        val castR = analysis.colorCastR.coerceIn(0.70f, 1.40f)
        val castG = analysis.colorCastG.coerceIn(0.85f, 1.15f)
        val castB = analysis.colorCastB.coerceIn(0.70f, 1.40f)

        val targetScaleR = (1.0f + (1.0f / castR - 1.0f) * autoWbStrength)
        val targetScaleG = (1.0f + (1.0f / castG - 1.0f) * autoWbStrength * 0.5f)
        val targetScaleB = (1.0f + (1.0f / castB - 1.0f) * autoWbStrength)

        // Manual Warmth offset
        val effectiveWarmth = warmth * intensity
        val finalScaleR = (targetScaleR + (effectiveWarmth * 0.14f)).coerceIn(0.75f, 1.35f)
        val finalScaleG = targetScaleG.coerceIn(0.85f, 1.15f)
        val finalScaleB = (targetScaleB - (effectiveWarmth * 0.14f)).coerceIn(0.75f, 1.35f)

        return ColorGains(finalScaleR, finalScaleG, finalScaleB)
    }

    /**
     * Adjusts color saturation and vibrance while strictly guarding skin tones from over-saturation.
     */
    inline fun processPixelColor(
        r: Int,
        g: Int,
        b: Int,
        vibrance: Float,
        intensity: Float,
        isPortraitScene: Boolean,
        hsvBuffer: FloatArray
    ): Int {
        Color.RGBToHSV(r, g, b, hsvBuffer)
        val hue = hsvBuffer[0]
        val sat = hsvBuffer[1]
        val value = hsvBuffer[2]

        // Check if pixel belongs to human skin tone spectrum
        val isSkinTone = (hue in 12f..48f) && (sat in 0.12f..0.65f) && (value in 0.20f..0.95f)

        val vibranceBoost = vibrance * intensity
        val deltaSat: Float = if (isSkinTone) {
            // Very subtle saturation adaptation for skin (max 3-5%)
            if (isPortraitScene) {
                (vibranceBoost * 0.15f).coerceIn(-0.05f, 0.04f) * (1.0f - sat)
            } else {
                (vibranceBoost * 0.20f).coerceIn(-0.06f, 0.06f) * (1.0f - sat)
            }
        } else {
            // Adaptive vibrance: boosts desaturated colors more than already saturated colors
            vibranceBoost * (1.0f - sat)
        }

        hsvBuffer[1] = (sat + deltaSat).coerceIn(0.0f, 1.0f)

        return Color.HSVToColor(hsvBuffer)
    }

    /**
     * High dynamic range Monochrome (Black & White) conversion with argentic tonality.
     */
    inline fun convertToMonochrome(
        r: Int,
        g: Int,
        b: Int,
        origR: Int,
        origG: Int,
        origB: Int,
        intensity: Float
    ): Triple<Int, Int, Int> {
        val monoLum = ((2126 * r + 7152 * g + 722 * b) / 10000).coerceIn(0, 255)
        val finalR = (origR + (monoLum - origR) * intensity).toInt().coerceIn(0, 255)
        val finalG = (origG + (monoLum - origG) * intensity).toInt().coerceIn(0, 255)
        val finalB = (origB + (monoLum - origB) * intensity).toInt().coerceIn(0, 255)
        return Triple(finalR, finalG, finalB)
    }
}

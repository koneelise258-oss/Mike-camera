package com.example.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object MikeExposureProcessor {

    /**
     * Calculates the smart exposure, shadow lift, and highlight recovery parameters
     * based on the diagnostic analysis and user intensity.
     */
    fun calculateAutoExposureOffset(analysis: ImageAnalysis, scene: SceneType): Float {
        val avgLum = analysis.avgLuminance
        val underRatio = analysis.underexposedRatio
        val overRatio = analysis.overexposedRatio

        return when {
            // Night scene: gentle lift, do not overbrighten to preserve night aesthetic
            scene == SceneType.NIGHT || scene == SceneType.LOW_LIGHT -> {
                if (avgLum < 40f) 0.18f else 0.08f
            }
            // Severely underexposed daytime photo
            underRatio > 0.40f || avgLum < 60f -> {
                ((110f - avgLum) / 255f * 0.45f).coerceIn(0.08f, 0.28f)
            }
            // Overexposed photo: slightly pull down highlights/exposure
            overRatio > 0.15f || avgLum > 185f -> {
                -((avgLum - 150f) / 255f * 0.35f).coerceIn(-0.25f, -0.05f)
            }
            // Well-balanced photo: slight micro dynamic expansion
            else -> {
                0.03f
            }
        }
    }

    /**
     * Generates a 256-entry Tone Mapping Look-Up Table (LUT) ensuring high performance and
     * pristine natural tonal transitions.
     */
    fun buildToneLut(
        analysis: ImageAnalysis,
        params: EnhancementParams,
        intensity: Float
    ): IntArray {
        val lut = IntArray(256)

        val autoExposure = calculateAutoExposureOffset(analysis, analysis.sceneType)
        val targetExposure = (params.exposure + autoExposure * 0.5f) * intensity
        val expMultiplier = (1.0f + targetExposure).coerceIn(0.5f, 1.8f)

        val effectiveContrast = 1.0f + (params.contrast * 0.55f * intensity)
        val effectiveShadowLift = (params.shadows * 0.55f * intensity).coerceIn(-0.5f, 0.8f)
        val effectiveHighlightComp = (-params.highlights * 0.50f * intensity).coerceIn(-0.5f, 0.8f)

        for (i in 0..255) {
            var norm = i / 255.0f

            // 1. Exposure scaling
            norm *= expMultiplier

            // 2. Intelligent Shadow Lifting (protects true black anchor at 0)
            if (effectiveShadowLift > 0f) {
                // Smooth bell-curve focused on lower-mid tones (0.05 to 0.45)
                val shadowWeight = (1.0f - norm).coerceAtLeast(0f).pow(1.8f) * sqrt(norm.coerceAtLeast(0f)) * 2.0f
                norm += effectiveShadowLift * shadowWeight * 0.40f
            } else if (effectiveShadowLift < 0f) {
                val shadowWeight = (1.0f - norm).coerceAtLeast(0f) * norm
                norm += effectiveShadowLift * shadowWeight * 0.30f
            }

            // 3. Highlight Recovery / Compression (protects maximum whites without harsh clipping)
            if (effectiveHighlightComp > 0f) {
                val highlightWeight = norm.coerceAtLeast(0f).pow(2.2f)
                norm -= effectiveHighlightComp * highlightWeight * 0.28f
            } else if (effectiveHighlightComp < 0f) {
                val highlightWeight = norm.coerceAtLeast(0f).pow(1.5f)
                norm -= effectiveHighlightComp * highlightWeight * 0.20f
            }

            // 4. Contrast S-Curve around natural perceptual midtone (0.46)
            norm = (norm - 0.46f) * effectiveContrast + 0.46f

            // Clamp and store
            lut[i] = (norm * 255.0f).toInt().coerceIn(0, 255)
        }

        // Guarantee monotonicity / smooth gradient
        lut[0] = min(lut[0], 2)
        lut[255] = max(lut[255], 253)

        return lut
    }
}

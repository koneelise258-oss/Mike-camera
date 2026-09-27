package com.example.ai.gemini

import com.example.engine.EnhancementParams
import com.example.engine.EnhancementPreset
import com.example.engine.ImageAnalysis
import com.example.engine.SceneType

object GeminiPromptBuilder {

    fun buildSystemInstruction(): String {
        return """
You are the professional photograph post-processing and color grading engine for MIKE AI.
Your sole purpose is natural, authentic photographic image refinement.

MANDATORY RULES:
1. PRESERVE subject identity, facial features, proportions, composition, geometry, and original context.
2. DO NOT add objects, people, text, logos, or artificial elements.
3. DO NOT remove existing objects or subject parts.
4. DO NOT create AI-generated reinterpretation or plastic cartoon effects.
5. KEEP skin tones organic, natural, with authentic texture.
6. AVOID oversharpening, artificial HDR halos, and toxic oversaturation.
7. Return only the enhanced natural photograph.
""".trimIndent()
    }

    fun buildPhotoEnhancementPrompt(
        params: EnhancementParams,
        analysis: ImageAnalysis?
    ): String {
        val scene = analysis?.sceneType ?: SceneType.GENERAL
        val preset = params.preset
        val intensityPct = (params.aiIntensity * 100).toInt()

        val sceneInstructions = when (scene) {
            SceneType.PORTRAIT -> "Focus on gentle portrait exposure, realistic skin tonality, soft shadows, and natural depth without artificial skin smoothing."
            SceneType.NIGHT, SceneType.NUIT, SceneType.LOW_LIGHT -> "Perform intelligent low-light illumination: lift dark shadows naturally, suppress chroma and luma noise, protect highlight sources from blooming, maintain authentic nocturnal atmosphere."
            SceneType.LANDSCAPE, SceneType.PAYSAGE -> "Enhance atmospheric dynamic range, micro-contrast in foliage/sky/water, true-to-life organic colors without oversaturating greens or blues."
            SceneType.DOCUMENT -> "Maximize legibility, high contrast, clean white background, razor-sharp text clarity."
            SceneType.GENERAL, SceneType.CLASSIQUE -> "Balance global exposure, recover highlight and shadow detail, optimize white balance and contrast."
            else -> "Balance global exposure, recover highlight and shadow detail, optimize white balance and contrast."
        }

        val presetInstructions = when (preset) {
            EnhancementPreset.NATURAL -> "Apply true-to-life studio color accuracy, balanced natural contrast, and subtle dynamic range recovery."
            EnhancementPreset.VIVID -> "Enhance color depth and brilliance, deep clean blacks, and vivid dynamic contrast."
            EnhancementPreset.CINEMATIC -> "Apply cinematic 35mm film color grading: warm subtle highlight split-toning, rich shadows, and soft organic rolloff."
            EnhancementPreset.PORTRAIT -> "Flattering skin illumination, gentle soft-focus background rolloff, and studio lighting warmth."
            EnhancementPreset.NIGHT -> "Deep night recovery, balanced ambient streetlight glow, and crisp noise reduction."
            EnhancementPreset.BLACK_AND_WHITE -> "Classic monochromatic silver-gelatin black and white: rich tonal grayscale gradation, deep contrast, fine luminance detail."
        }

        return """
Refine this photograph with MIKE AI Engine at $intensityPct% intensity.

Scene Context: ${scene.displayName}
$sceneInstructions

Preset Style: ${preset.title}
$presetInstructions

Fine Adjustments:
- Exposure Offset: ${params.exposure}
- Contrast Factor: ${params.contrast}
- Shadow Recovery: ${params.shadows}
- Highlight Protection: ${params.highlights}
- Vibrance: ${params.vibrance}
- Warmth / White Balance: ${params.warmth}
- Sharpness / Clarity: ${params.sharpness}

Generate the refined photograph maintaining high resolution and authentic photographic realism.
""".trimIndent()
    }
}

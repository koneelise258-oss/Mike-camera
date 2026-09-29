package com.example.engine

enum class EnhancementPreset(
    val id: String,
    val title: String,
    val description: String,
    val defaultExposure: Float,
    val defaultContrast: Float,
    val defaultShadows: Float,
    val defaultHighlights: Float,
    val defaultVibrance: Float,
    val defaultWarmth: Float,
    val defaultSharpness: Float
) {
    NATURAL(
        id = "natural",
        title = "Naturel",
        description = "Rendu photographique équilibré, dynamique étendue et micro-contraste subtil.",
        defaultExposure = 0.05f,
        defaultContrast = 0.08f,
        defaultShadows = 0.28f,
        defaultHighlights = -0.18f,
        defaultVibrance = 0.16f,
        defaultWarmth = 0.02f,
        defaultSharpness = 0.28f
    ),
    VIVID(
        id = "vivid",
        title = "Vif",
        description = "Couleurs éclatantes, ciel renforcé et dynamique lumineuse accentuée.",
        defaultExposure = 0.05f,
        defaultContrast = 0.18f,
        defaultShadows = 0.32f,
        defaultHighlights = -0.28f,
        defaultVibrance = 0.35f,
        defaultWarmth = 0.03f,
        defaultSharpness = 0.42f
    ),
    CINEMATIC(
        id = "cinematic",
        title = "Cinématique",
        description = "Tons riches, ombres profondes et ambiance optique feutrée.",
        defaultExposure = -0.02f,
        defaultContrast = 0.20f,
        defaultShadows = 0.15f,
        defaultHighlights = -0.32f,
        defaultVibrance = 0.12f,
        defaultWarmth = 0.06f,
        defaultSharpness = 0.34f
    ),
    PORTRAIT(
        id = "portrait",
        title = "Portrait",
        description = "Teint de peau préservé, douceur des ombres et mise en valeur des regards.",
        defaultExposure = 0.08f,
        defaultContrast = 0.05f,
        defaultShadows = 0.24f,
        defaultHighlights = -0.14f,
        defaultVibrance = 0.08f,
        defaultWarmth = 0.06f,
        defaultSharpness = 0.20f
    ),
    NIGHT(
        id = "night",
        title = "Nuit",
        description = "Débouchage des zones sombres, réduction du bruit et maîtrise des sources lumineuses.",
        defaultExposure = 0.22f,
        defaultContrast = 0.10f,
        defaultShadows = 0.52f,
        defaultHighlights = -0.32f,
        defaultVibrance = 0.14f,
        defaultWarmth = -0.02f,
        defaultSharpness = 0.25f
    ),
    BLACK_AND_WHITE(
        id = "black_and_white",
        title = "Noir & Blanc",
        description = "Gradient argentique profond, gamme dynamique étendue et noirs intenses.",
        defaultExposure = 0.04f,
        defaultContrast = 0.26f,
        defaultShadows = 0.20f,
        defaultHighlights = -0.20f,
        defaultVibrance = -1.0f,
        defaultWarmth = 0.0f,
        defaultSharpness = 0.38f
    );

    companion object {
        // French alias support
        val NATUREL = NATURAL
        val VIF = VIVID
        val CINEMATIQUE = CINEMATIC
        val NUIT = NIGHT
        val NOIR_ET_BLANC = BLACK_AND_WHITE

        fun fromId(id: String): EnhancementPreset {
            return entries.find { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) }
                ?: NATURAL
        }
    }
}

data class EnhancementParams(
    val preset: EnhancementPreset = EnhancementPreset.NATURAL,
    val exposure: Float = preset.defaultExposure,
    val contrast: Float = preset.defaultContrast,
    val shadows: Float = preset.defaultShadows,
    val highlights: Float = preset.defaultHighlights,
    val vibrance: Float = preset.defaultVibrance,
    val warmth: Float = preset.defaultWarmth,
    val sharpness: Float = preset.defaultSharpness,
    val aiIntensity: Float = 1.0f,
    val portraitAperture: Float = 0f,
    val portraitSkinSmoothing: Float = 0f,
    val portraitLighting: String = "Naturel",
    val proIso: String = "Auto",
    val proShutterSpeed: String = "Auto",
    val proWhiteBalance: String = "Auto",
    val cinematicLut: String = "",
    val isPanorama: Boolean = false,
    val isVideo: Boolean = false,
    val videoDurationSeconds: Int = 0,
    val shootingModeName: String = "PHOTO"
)

object MikePresetProcessor {

    /**
     * Adapts parameters according to scene diagnosis if preset is NATURAL.
     */
    fun adaptParamsForScene(
        baseParams: EnhancementParams,
        analysis: ImageAnalysis
    ): EnhancementParams {
        if (baseParams.preset != EnhancementPreset.NATURAL) {
            return baseParams
        }

        // If the user has manually customized any adjustment, strictly preserve user's explicit values!
        val isDefaultNatural = baseParams.exposure == EnhancementPreset.NATURAL.defaultExposure &&
            baseParams.contrast == EnhancementPreset.NATURAL.defaultContrast &&
            baseParams.shadows == EnhancementPreset.NATURAL.defaultShadows &&
            baseParams.highlights == EnhancementPreset.NATURAL.defaultHighlights &&
            baseParams.vibrance == EnhancementPreset.NATURAL.defaultVibrance &&
            baseParams.warmth == EnhancementPreset.NATURAL.defaultWarmth &&
            baseParams.sharpness == EnhancementPreset.NATURAL.defaultSharpness

        if (!isDefaultNatural) {
            return baseParams
        }

        return when (analysis.sceneType) {
            SceneType.PORTRAIT -> baseParams.copy(
                exposure = 0.08f,
                contrast = 0.05f,
                shadows = 0.24f,
                vibrance = 0.08f,
                warmth = 0.05f,
                sharpness = 0.20f
            )
            SceneType.NIGHT, SceneType.LOW_LIGHT -> baseParams.copy(
                exposure = 0.20f,
                contrast = 0.10f,
                shadows = 0.48f,
                highlights = -0.30f,
                sharpness = 0.22f
            )
            SceneType.LANDSCAPE, SceneType.PAYSAGE -> baseParams.copy(
                exposure = 0.04f,
                contrast = 0.14f,
                shadows = 0.28f,
                highlights = -0.25f,
                vibrance = 0.26f,
                sharpness = 0.38f
            )
            SceneType.DOCUMENT -> baseParams.copy(
                exposure = 0.10f,
                contrast = 0.35f,
                shadows = 0.10f,
                highlights = -0.10f,
                vibrance = -0.20f,
                sharpness = 0.45f
            )
            else -> baseParams
        }
    }
}

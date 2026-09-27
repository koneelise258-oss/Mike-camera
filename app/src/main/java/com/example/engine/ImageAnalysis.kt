package com.example.engine

enum class SceneType(val displayName: String, val code: String) {
    GENERAL("Classique", "general"),
    CLASSIQUE("Classique", "general"),
    PORTRAIT("Portrait", "portrait"),
    PAYSAGE("Paysage", "landscape"),
    LANDSCAPE("Paysage", "landscape"),
    NUIT("Nuit", "night"),
    NIGHT("Nuit", "night"),
    LOW_LIGHT("Basse lumière", "low_light"),
    DOCUMENT("Document", "document");

    companion object {
        fun fromCode(code: String): SceneType {
            return entries.find { it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) }
                ?: GENERAL
        }
    }
}

data class ImageAnalysis(
    val sceneType: SceneType = SceneType.GENERAL,
    val sceneConfidence: Float = 0.85f,
    val avgLuminance: Float = 128f,
    val minLuminance: Int = 0,
    val maxLuminance: Int = 255,
    val underexposedRatio: Float = 0f,
    val overexposedRatio: Float = 0f,
    val contrastStdDev: Float = 50f,
    val dynamicRange: Float = 200f,
    val avgSaturation: Float = 0.3f,
    val colorCastR: Float = 1.0f,
    val colorCastG: Float = 1.0f,
    val colorCastB: Float = 1.0f,
    val dominantTint: String = "NEUTRE",
    val estimatedNoiseLevel: Float = 0.1f,
    val detailLevel: Float = 0.5f,
    val sharpness: Float = 0.5f,
    val skinPixelRatio: Float = 0f,
    val skyPixelRatio: Float = 0f,
    val vegetationPixelRatio: Float = 0f,
    val width: Int = 0,
    val height: Int = 0
)

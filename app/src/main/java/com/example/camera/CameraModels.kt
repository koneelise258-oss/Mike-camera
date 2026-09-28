package com.example.camera

import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import com.example.engine.EnhancementPreset

enum class CameraShootingMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val targetPreset: EnhancementPreset
) {
    PHOTO("photo", "PHOTO", "Capture naturelle haute précision", EnhancementPreset.NATURAL),
    PORTRAIT("portrait", "PORTRAIT", "Mise au point & teint naturel", EnhancementPreset.PORTRAIT),
    NIGHT("night", "NUIT", "Basse lumière & réduction de bruit", EnhancementPreset.NIGHT),
    PRO("pro", "PRO", "Contrôles manuels & dynamique étendue", EnhancementPreset.VIVID),
    PANORAMA("panorama", "PANO", "Champ visuel ultra-large", EnhancementPreset.NATURAL),
    SLOW_MOTION("slow_motion", "RALENTI", "Ralenti fluide haute vitesse", EnhancementPreset.CINEMATIC),
    TIME_LAPSE("time_lapse", "ACCÉLÉRÉ", "Accéléré temporel ultra-dynamique", EnhancementPreset.VIVID),
    CINEMATIC("cinematic", "CINÉMA", "Rendu cinéma & profondeur", EnhancementPreset.CINEMATIC)
}

enum class CameraTimer(val seconds: Int, val label: String) {
    OFF(0, "Off"),
    SEC_3(3, "3s"),
    SEC_10(10, "10s")
}

enum class CameraAspectRatio(val label: String, val ratioValue: Int, val heightFraction: Float) {
    RATIO_4_3("4:3", AspectRatio.RATIO_4_3, 4f / 3f),
    RATIO_16_9("16:9", AspectRatio.RATIO_16_9, 16f / 9f),
    RATIO_1_1("1:1", AspectRatio.RATIO_4_3, 1f) // Crop to square
}

enum class CameraGridType(val label: String) {
    NONE("Off"),
    RULE_OF_THIRDS("3×3"),
    GOLDEN_RATIO("Phi")
}

enum class CameraFlashMode(val modeValue: Int, val label: String) {
    AUTO(ImageCapture.FLASH_MODE_AUTO, "Auto"),
    ON(ImageCapture.FLASH_MODE_ON, "Oui"),
    OFF(ImageCapture.FLASH_MODE_OFF, "Non"),
    TORCH(-1, "Torche")
}

enum class CameraSourcePreference(val title: String, val description: String) {
    MIKE_AI_CAMERA("Caméra MIKE AI (Recommandé)", "Caméra native personnalisée intégrée avec Liquid Glass"),
    SYSTEM_CAMERA("Caméra Système Android", "Ouvre l'application appareil photo par défaut de l'appareil")
}

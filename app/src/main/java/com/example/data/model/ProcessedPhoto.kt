package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_photos")
data class ProcessedPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalPath: String,
    val processedPath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val width: Int = 0,
    val height: Int = 0,
    val presetName: String = "Naturel",
    val isSavedToGallery: Boolean = false,
    val isFavorite: Boolean = false,
    val aiIntensity: Float = 1.0f,
    val sceneType: String = "Classique",
    val exposureAdj: Float = 0f,
    val contrastAdj: Float = 0f,
    val shadowsAdj: Float = 0f,
    val highlightsAdj: Float = 0f,
    val vibranceAdj: Float = 0f,
    val warmthAdj: Float = 0f,
    val sharpnessAdj: Float = 0f,
    val isVideo: Boolean = false,
    val videoDurationSeconds: Int = 0,
    val shootingModeName: String = "PHOTO"
)

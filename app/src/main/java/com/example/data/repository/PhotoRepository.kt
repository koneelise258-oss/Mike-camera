package com.example.data.repository

import com.example.data.local.PhotoDao
import com.example.data.model.ProcessedPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class PhotoRepository(
    private val photoDao: PhotoDao
) {
    val allPhotos: Flow<List<ProcessedPhoto>> = photoDao.getAllPhotos()
    val favoritePhotos: Flow<List<ProcessedPhoto>> = photoDao.getFavoritePhotos()
    val recentPhotos: Flow<List<ProcessedPhoto>> = photoDao.getRecentPhotos(10)

    suspend fun getPhotoById(id: Long): ProcessedPhoto? = withContext(Dispatchers.IO) {
        photoDao.getPhotoById(id)
    }

    suspend fun insertPhoto(photo: ProcessedPhoto): Long = withContext(Dispatchers.IO) {
        photoDao.insertPhoto(photo)
    }

    suspend fun updatePhoto(photo: ProcessedPhoto) = withContext(Dispatchers.IO) {
        photoDao.updatePhoto(photo)
    }

    suspend fun toggleFavorite(photo: ProcessedPhoto) = withContext(Dispatchers.IO) {
        val newStatus = !photo.isFavorite
        photoDao.updateFavoriteStatus(photo.id, newStatus)
    }

    suspend fun deletePhoto(photo: ProcessedPhoto) = withContext(Dispatchers.IO) {
        // Delete files from storage
        try {
            val origFile = File(photo.originalPath)
            if (origFile.exists()) origFile.delete()
        } catch (_: Exception) {}

        try {
            val procFile = File(photo.processedPath)
            if (procFile.exists()) procFile.delete()
        } catch (_: Exception) {}

        photoDao.deletePhoto(photo)
    }

    suspend fun markAsSaved(id: Long) = withContext(Dispatchers.IO) {
        photoDao.markAsSaved(id)
    }
}

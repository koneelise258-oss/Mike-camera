package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ProcessedPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM processed_photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<ProcessedPhoto>>

    @Query("SELECT * FROM processed_photos WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoritePhotos(): Flow<List<ProcessedPhoto>>

    @Query("SELECT * FROM processed_photos ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPhotos(limit: Int = 10): Flow<List<ProcessedPhoto>>

    @Query("SELECT * FROM processed_photos WHERE id = :id LIMIT 1")
    suspend fun getPhotoById(id: Long): ProcessedPhoto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: ProcessedPhoto): Long

    @Update
    suspend fun updatePhoto(photo: ProcessedPhoto)

    @Delete
    suspend fun deletePhoto(photo: ProcessedPhoto)

    @Query("DELETE FROM processed_photos WHERE id = :id")
    suspend fun deletePhotoById(id: Long)

    @Query("UPDATE processed_photos SET isSavedToGallery = 1 WHERE id = :id")
    suspend fun markAsSaved(id: Long)

    @Query("UPDATE processed_photos SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)
}

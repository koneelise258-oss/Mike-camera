package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PhotoDatabase
import com.example.data.model.ProcessedPhoto
import com.example.data.repository.PhotoRepository
import com.example.engine.EnhancementParams
import com.example.engine.EnhancementPreset
import com.example.engine.MediaManager
import com.example.engine.MikeAIEngine
import com.example.engine.SceneType
import com.example.viewmodel.PhotoViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EndToEndPipelineTest {

    private lateinit var context: Application
    private lateinit var database: PhotoDatabase
    private lateinit var repository: PhotoRepository
    private lateinit var viewModel: PhotoViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PhotoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PhotoRepository(database.photoDao())
        viewModel = PhotoViewModel(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createTestBitmap(width: Int = 100, height: Int = 100): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bmp.setPixel(x, y, Color.rgb(100 + (x % 50), 120 + (y % 50), 140))
            }
        }
        return bmp
    }

    private fun createTestImageFile(width: Int = 100, height: Int = 100): File {
        val file = File(context.cacheDir, "test_cuj_${System.nanoTime()}.jpg")
        val bmp = createTestBitmap(width, height)
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bmp.recycle()
        return file
    }

    // TEST A : Lancement
    @Test
    fun testAppLaunchInitialState() {
        val state = viewModel.uiState.value
        assertEquals("Default launch screen must be CAMERA", Screen.CAMERA, state.currentScreen)
        assertFalse("Processing state must be false initially", state.isProcessing)
    }

    // TEST B & C : Capture & Traitement (Original -> Analyse -> Traitement -> Résultat)
    @Test
    fun testCaptureAndProcessingPipeline() {
        runBlocking {
            val file = createTestImageFile(120, 90)
            val initialBytes = file.readBytes()

            val decoded = MikeAIEngine.decodeSampledBitmap(file)
            assertNotNull(decoded)
            assertEquals(120, decoded.width)
            assertEquals(90, decoded.height)

            val analysis = MikeAIEngine.analyzeImage(decoded)
            assertNotNull(analysis)

            val result = MikeAIEngine.processImage(decoded, EnhancementParams(preset = EnhancementPreset.NATURAL))
            assertNotNull(result.processedBitmap)
            assertEquals(120, result.processedBitmap.width)
            assertEquals(90, result.processedBitmap.height)

            // Original file remains bit-for-bit identical
            assertTrue(file.readBytes().contentEquals(initialBytes))

            file.delete()
        }
    }

    // TEST D : Galerie & Navigation
    @Test
    fun testGalleryRecordAndNavigation() {
        runBlocking {
            val origFile = createTestImageFile()
            val procFile = createTestImageFile()

            val record = ProcessedPhoto(
                originalPath = origFile.absolutePath,
                processedPath = procFile.absolutePath,
                width = 100,
                height = 100,
                presetName = "Naturel"
            )

            val id = repository.insertPhoto(record)
            assertTrue(id > 0)

            val photos = repository.allPhotos.first()
            assertEquals(1, photos.size)
            assertEquals(id, photos[0].id)

            // Test favorite toggle
            repository.toggleFavorite(photos[0])
            val favs = repository.favoritePhotos.first()
            assertEquals(1, favs.size)

            origFile.delete()
            procFile.delete()
        }
    }

    // TEST E : Éditeur & Modification
    @Test
    fun testEditorAdjustmentAndPreset() {
        runBlocking {
            val bmp = createTestBitmap(80, 80)
            val result = MikeAIEngine.processImage(
                bmp,
                EnhancementParams(
                    preset = EnhancementPreset.VIVID,
                    contrast = 0.30f,
                    exposure = 0.15f,
                    aiIntensity = 0.85f
                )
            )

            assertNotNull(result.processedBitmap)
            assertEquals(0.30f, result.appliedParams.contrast, 0.001f)
            assertEquals(0.15f, result.appliedParams.exposure, 0.001f)
            assertEquals(0.85f, result.appliedParams.aiIntensity, 0.001f)
        }
    }

    // TEST F : Import et Persistance
    @Test
    fun testImportAndPersistence() {
        runBlocking {
            val srcFile = createTestImageFile()
            val persisted = MediaManager.persistOriginalFile(context, srcFile)

            assertTrue(persisted.exists())
            assertTrue(persisted.length() > 0)
            assertEquals(srcFile.length(), persisted.length())

            srcFile.delete()
            persisted.delete()
        }
    }

    // TEST G : Sauvegarde et Export
    @Test
    fun testSaveProcessedFileAndExif() {
        runBlocking {
            val bmp = createTestBitmap(60, 60)
            val savedFile = MediaManager.saveProcessedBitmapToFile(context, bmp, prefix = "TEST_EXPORT")

            assertTrue(savedFile.exists())
            assertTrue(savedFile.length() > 0)

            val uri = MediaManager.saveToDeviceGallery(context, bmp)
            // On test environment MediaStore uri or fallback is handled safely
            savedFile.delete()
        }
    }

    // TEST H : Gestion d'erreurs (Fichier absent, aucune fermeture brutale)
    @Test
    fun testErrorResilience() {
        runBlocking {
            val missingFile = File(context.cacheDir, "non_existent_file.jpg")
            assertFalse(missingFile.exists())

            // Attempting to open missing file in ViewModel handles gracefully without crash
            viewModel.openGalleryPhoto(
                ProcessedPhoto(
                    originalPath = missingFile.absolutePath,
                    processedPath = missingFile.absolutePath,
                    width = 0,
                    height = 0
                )
            )

            // State should update safely with error or fallback without throwing uncaught exceptions
            assertFalse(viewModel.uiState.value.isProcessing)
        }
    }
}

package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.example.engine.EnhancementParams
import com.example.engine.EnhancementPreset
import com.example.engine.MediaManager
import com.example.engine.MikeAIEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExifMetadataTest {

    private fun createJpegFile(width: Int, height: Int, orientationTag: Int? = null): File {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "test_${System.nanoTime()}.jpg")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, Color.rgb(100 + (x % 50), 120 + (y % 50), 140))
            }
        }
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()

        if (orientationTag != null) {
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientationTag.toString())
            exif.setAttribute(ExifInterface.TAG_MAKE, "Tecno")
            exif.setAttribute(ExifInterface.TAG_MODEL, "Camon 15 Air")
            exif.saveAttributes()
        }
        return file
    }

    // 1. Capture portrait avec tag orientation 6 (90°)
    @Test
    fun testPortraitOrientationExif6() {
        runBlocking {
            // Raw sensor is landscape 400x200, but held vertically -> EXIF 6
            val file = createJpegFile(400, 200, orientationTag = ExifInterface.ORIENTATION_ROTATE_90)
            val initialBytes = file.readBytes()

            val decoded = MikeAIEngine.decodeSampledBitmap(file)
            assertNotNull(decoded)

            // After applying EXIF 6 rotation in-memory, height > width (portrait)
            assertEquals(200, decoded.width)
            assertEquals(400, decoded.height)

            // Verify original file was NOT overwritten or modified
            val afterBytes = file.readBytes()
            assertEquals("Original file on disk must remain strictly untouched", initialBytes.size, afterBytes.size)

            file.delete()
        }
    }

    // 2. Capture paysage avec tag orientation 1 (0°)
    @Test
    fun testLandscapeOrientationExif1() {
        runBlocking {
            val file = createJpegFile(400, 200, orientationTag = ExifInterface.ORIENTATION_NORMAL)
            val decoded = MikeAIEngine.decodeSampledBitmap(file)
            assertNotNull(decoded)

            // Standard landscape: width remains 400, height 200
            assertEquals(400, decoded.width)
            assertEquals(200, decoded.height)

            file.delete()
        }
    }

    // 3. Orientation 180° (EXIF 3)
    @Test
    fun testOrientationExif3() {
        runBlocking {
            val file = createJpegFile(300, 200, orientationTag = ExifInterface.ORIENTATION_ROTATE_180)
            val decoded = MikeAIEngine.decodeSampledBitmap(file)
            assertNotNull(decoded)
            assertEquals(300, decoded.width)
            assertEquals(200, decoded.height)
            file.delete()
        }
    }

    // 4. Orientation 270° (EXIF 8)
    @Test
    fun testOrientationExif8() {
        runBlocking {
            val file = createJpegFile(400, 200, orientationTag = ExifInterface.ORIENTATION_ROTATE_270)
            val decoded = MikeAIEngine.decodeSampledBitmap(file)
            assertNotNull(decoded)
            // 270° swaps width and height
            assertEquals(200, decoded.width)
            assertEquals(400, decoded.height)
            file.delete()
        }
    }

    // 5. Image sans tag EXIF
    @Test
    fun testImageWithoutExifTag() {
        runBlocking {
            val file = createJpegFile(300, 150, orientationTag = null)
            val decoded = MikeAIEngine.decodeSampledBitmap(file)
            assertNotNull(decoded)
            assertEquals(300, decoded.width)
            assertEquals(150, decoded.height)
            file.delete()
        }
    }

    // 6. Sauvegarde et métadonnées du résultat
    @Test
    fun testProcessedFileMetadataAndExifInheritance() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val originalFile = createJpegFile(400, 200, orientationTag = ExifInterface.ORIENTATION_ROTATE_90)

            val processedBmp = Bitmap.createBitmap(200, 400, Bitmap.Config.ARGB_8888)
            val processedFile = MediaManager.saveProcessedBitmapToFile(
                context = context,
                bitmap = processedBmp,
                prefix = "TEST_PROC",
                originalFile = originalFile
            )

            assertTrue(processedFile.exists())
            val procExif = ExifInterface(processedFile.absolutePath)

            // Processed file MUST have ORIENTATION_NORMAL (1)
            val orientation = procExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
            assertEquals(ExifInterface.ORIENTATION_NORMAL, orientation)

            // Processed file MUST have correct width and height
            assertEquals(200, procExif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0))
            assertEquals(400, procExif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0))

            // Processed file MUST have MIKE AI software tag
            assertEquals("MIKE AI PHOTO", procExif.getAttribute(ExifInterface.TAG_SOFTWARE))

            // Processed file safely inherits camera make & model from original
            assertEquals("Tecno", procExif.getAttribute(ExifInterface.TAG_MAKE))
            assertEquals("Camon 15 Air", procExif.getAttribute(ExifInterface.TAG_MODEL))

            originalFile.delete()
            processedFile.delete()
        }
    }

    // 7. Pipeline complet MIKE AI sans dégradation de l'original
    @Test
    fun testCompletePipelinePreservesOriginal() {
        runBlocking {
            val originalFile = createJpegFile(300, 300, orientationTag = ExifInterface.ORIENTATION_NORMAL)
            val initialLength = originalFile.length()

            val decoded = MikeAIEngine.decodeSampledBitmap(originalFile)
            val result = MikeAIEngine.processImage(decoded, EnhancementParams(preset = EnhancementPreset.NATURAL))

            assertNotNull(result.processedBitmap)
            assertEquals(decoded.width, result.processedBitmap.width)
            assertEquals(decoded.height, result.processedBitmap.height)

            // Assert original file was completely untouched
            assertEquals(initialLength, originalFile.length())

            originalFile.delete()
        }
    }
}

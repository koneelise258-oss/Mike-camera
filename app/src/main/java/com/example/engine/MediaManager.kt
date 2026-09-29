package com.example.engine

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class WallpaperTarget {
    HOME_SCREEN,
    LOCK_SCREEN,
    BOTH
}

object MediaManager {

    private fun getPhotosDir(context: Context): File {
        val dir = File(context.filesDir, "photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getTempDir(context: Context): File {
        val dir = File(context.cacheDir, "photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun createTempCaptureFile(context: Context): Pair<File, Uri> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "CAPTURE_${timeStamp}.jpg"
        val storageDir = getTempDir(context)
        val file = File(storageDir, fileName)
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        return Pair(file, uri)
    }

    suspend fun copyUriToInternalStorage(context: Context, sourceUri: Uri): File = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val mimeType = context.contentResolver.getType(sourceUri)
        val ext = when {
            mimeType?.contains("png", ignoreCase = true) == true -> "png"
            mimeType?.contains("webp", ignoreCase = true) == true -> "webp"
            mimeType?.contains("heic", ignoreCase = true) == true || mimeType?.contains("heif", ignoreCase = true) == true -> "heic"
            else -> "jpg"
        }
        val destFile = File(getPhotosDir(context), "ORIGINAL_${timeStamp}.${ext}")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Impossible de lire le flux de l'image source.")

        // Original file is preserved 100% intact with its genuine bytes and EXIF tags.
        // Memory-safe EXIF orientation is handled during decode in MikeAIEngine.decodeSampledBitmap.
        destFile
    }

    suspend fun persistOriginalFile(context: Context, sourceFile: File): File = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val ext = sourceFile.extension.ifEmpty { "jpg" }
        val destFile = File(getPhotosDir(context), "ORIGINAL_${timeStamp}.${ext}")
        if (sourceFile.exists()) {
            sourceFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            throw IllegalStateException("Le fichier source n'existe pas : ${sourceFile.absolutePath}")
        }
        destFile
    }

    suspend fun saveProcessedBitmapToFile(
        context: Context,
        bitmap: Bitmap,
        prefix: String = "PROC",
        originalFile: File? = null
    ): File = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val file = File(getPhotosDir(context), "${prefix}_${timeStamp}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
        }

        // Write clear, consistent EXIF metadata on the processed result
        try {
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            exif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, bitmap.width.toString())
            exif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, bitmap.height.toString())
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "MIKE AI PHOTO")
            val dateStr = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
            exif.setAttribute(ExifInterface.TAG_DATETIME, dateStr)

            // Safely propagate non-spatial camera/device tags from original if available
            if (originalFile != null && originalFile.exists()) {
                val origExif = ExifInterface(originalFile.absolutePath)
                origExif.getAttribute(ExifInterface.TAG_MAKE)?.let { exif.setAttribute(ExifInterface.TAG_MAKE, it) }
                origExif.getAttribute(ExifInterface.TAG_MODEL)?.let { exif.setAttribute(ExifInterface.TAG_MODEL, it) }
                origExif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let { exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, it) }
                origExif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, it) }
                origExif.getAttribute(ExifInterface.TAG_FLASH)?.let { exif.setAttribute(ExifInterface.TAG_FLASH, it) }
                origExif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)?.let { exif.setAttribute(ExifInterface.TAG_WHITE_BALANCE, it) }
                origExif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, it) }
                origExif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { exif.setAttribute(ExifInterface.TAG_F_NUMBER, it) }
                origExif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)?.let { exif.setAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS, it) }
            }
            exif.saveAttributes()
        } catch (_: Exception) {}

        file
    }

    /**
     * Preserved for backward compatibility. In-memory orientation normalization is
     * handled non-destructively in MikeAIEngine.decodeSampledBitmap.
     */
    fun normalizeImageOrientation(file: File) {
        // Intentionally non-destructive to guarantee original files and EXIF metadata remain intact
    }

    suspend fun saveToDeviceGallery(
        context: Context,
        bitmap: Bitmap,
        titleSuffix: String = "MIKE_AI",
        originalFile: File? = null
    ): Uri? = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "MIKE_AI_${timeStamp}.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.ORIENTATION, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MikeAiPhoto")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = context.contentResolver.insert(collection, values) ?: return@withContext null

        try {
            context.contentResolver.openOutputStream(itemUri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 98, out)) {
                    throw IllegalStateException("Échec de la compression JPEG")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    context.contentResolver.openFileDescriptor(itemUri, "rw")?.use { pfd ->
                        val galleryExif = ExifInterface(pfd.fileDescriptor)
                        galleryExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                        galleryExif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, bitmap.width.toString())
                        galleryExif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, bitmap.height.toString())
                        galleryExif.setAttribute(ExifInterface.TAG_SOFTWARE, "MIKE AI PHOTO")
                        val dateStr = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
                        galleryExif.setAttribute(ExifInterface.TAG_DATETIME, dateStr)

                        if (originalFile != null && originalFile.exists()) {
                            val origExif = ExifInterface(originalFile.absolutePath)
                            origExif.getAttribute(ExifInterface.TAG_MAKE)?.let { galleryExif.setAttribute(ExifInterface.TAG_MAKE, it) }
                            origExif.getAttribute(ExifInterface.TAG_MODEL)?.let { galleryExif.setAttribute(ExifInterface.TAG_MODEL, it) }
                            origExif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let { galleryExif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, it) }
                            origExif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { galleryExif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, it) }
                            origExif.getAttribute(ExifInterface.TAG_FLASH)?.let { galleryExif.setAttribute(ExifInterface.TAG_FLASH, it) }
                            origExif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)?.let { galleryExif.setAttribute(ExifInterface.TAG_WHITE_BALANCE, it) }
                            origExif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { galleryExif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, it) }
                            origExif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { galleryExif.setAttribute(ExifInterface.TAG_F_NUMBER, it) }
                            origExif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)?.let { galleryExif.setAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS, it) }
                        }
                        galleryExif.saveAttributes()
                    }
                } catch (_: Exception) {}

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(itemUri, values, null, null)
            }
            itemUri
        } catch (e: Exception) {
            context.contentResolver.delete(itemUri, null, null)
            null
        }
    }

    suspend fun setAsWallpaper(context: Context, bitmap: Bitmap, target: WallpaperTarget): Boolean = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val whichFlag = when (target) {
                    WallpaperTarget.HOME_SCREEN -> WallpaperManager.FLAG_SYSTEM
                    WallpaperTarget.LOCK_SCREEN -> WallpaperManager.FLAG_LOCK
                    WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                wallpaperManager.setBitmap(bitmap, null, true, whichFlag)
            } else {
                wallpaperManager.setBitmap(bitmap)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun printPhoto(context: Context, bitmap: Bitmap, jobName: String = "MIKE AI Photo") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
            val printAdapter = object : android.print.PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = android.print.PrintDocumentInfo.Builder(jobName)
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                        .setPageCount(1)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onWriteCancelled()
                        return
                    }
                    try {
                        destination?.fileDescriptor?.let { fd ->
                            FileOutputStream(fd).use { out ->
                                val pdfDocument = android.graphics.pdf.PdfDocument()
                                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                                val page = pdfDocument.startPage(pageInfo)
                                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                                pdfDocument.finishPage(page)
                                pdfDocument.writeTo(out)
                                pdfDocument.close()
                            }
                        }
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }
            val printAttributes = PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
            printManager.print(jobName, printAdapter, printAttributes)
        } catch (_: Exception) {}
    }
}

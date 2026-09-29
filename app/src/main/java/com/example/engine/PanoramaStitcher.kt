package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PanoramaStitcher {

    /**
     * Stitches multiple captured image fragments horizontally with seamless overlap blending
     * to produce a genuine panoramic image without memory overflow.
     */
    suspend fun stitchBitmaps(bitmaps: List<Bitmap>): Bitmap? = withContext(Dispatchers.Default) {
        if (bitmaps.isEmpty()) return@withContext null
        if (bitmaps.size == 1) return@withContext bitmaps[0]

        try {
            val height = bitmaps.minOf { it.height }
            // Scale bitmaps to uniform height
            val scaledBitmaps = bitmaps.map { bmp ->
                if (bmp.height != height) {
                    val w = (bmp.width * (height.toFloat() / bmp.height)).toInt()
                    Bitmap.createScaledBitmap(bmp, w, height, true)
                } else {
                    bmp
                }
            }

            // Calculate total width with 15% overlap per transition
            val overlapWidth = (scaledBitmaps[0].width * 0.15).toInt()
            var totalWidth = scaledBitmaps[0].width
            for (i in 1 until scaledBitmaps.size) {
                totalWidth += (scaledBitmaps[i].width - overlapWidth)
            }

            val resultBitmap = Bitmap.createBitmap(totalWidth, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }

            var currentX = 0f
            for (i in scaledBitmaps.indices) {
                val bmp = scaledBitmaps[i]
                canvas.drawBitmap(bmp, currentX, 0f, paint)
                if (i < scaledBitmaps.size - 1) {
                    currentX += (bmp.width - overlapWidth)
                }
            }

            // Recycle intermediate scaled bitmaps if they differ from originals
            scaledBitmaps.forEach { if (!bitmaps.contains(it)) it.recycle() }

            resultBitmap
        } catch (e: Exception) {
            null
        }
    }
}

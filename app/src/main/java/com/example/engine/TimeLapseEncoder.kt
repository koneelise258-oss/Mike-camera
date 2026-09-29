package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object TimeLapseEncoder {
    private const val TAG = "TimeLapseEncoder"

    /**
     * Compiles a list of JPEG files into a real, high-quality hardware-accelerated MP4 video.
     * Uses hardware Surface encoding to minimize RAM and CPU usage, making it ideal for budget devices.
     */
    suspend fun encode(
        jpegFiles: List<File>,
        outputFile: File,
        width: Int = 1280,
        height: Int = 720,
        fps: Int = 30,
        bitrate: Int = 3_000_000 // 3 Mbps for crisp 720p
    ): Boolean = withContext(Dispatchers.IO) {
        if (jpegFiles.isEmpty()) return@withContext false

        var mediaCodec: MediaCodec? = null
        var mediaMuxer: MediaMuxer? = null
        var inputSurface: android.view.Surface? = null

        try {
            // 1. Setup encoder format
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // Keyframe every second
            }

            // 2. Initialize encoder and create input Surface
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = mediaCodec.createInputSurface()
            mediaCodec.start()

            // 3. Initialize Muxer
            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var isMuxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val frameDurationUs = 1_000_000L / fps
            val destRect = Rect(0, 0, width, height)

            // 4. Iterate frames
            for (i in jpegFiles.indices) {
                val jpegFile = jpegFiles[i]
                if (!jpegFile.exists() || jpegFile.length() == 0L) continue

                // Decode bitmap downsampled to target size to avoid OOM
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(jpegFile.absolutePath, options)
                
                var inSampleSize = 1
                while ((options.outHeight / inSampleSize) > height || (options.outWidth / inSampleSize) > width) {
                    inSampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bitmap = BitmapFactory.decodeFile(jpegFile.absolutePath, decodeOptions) ?: continue

                // Render frame onto encoder's input Surface using canvas
                val canvas: Canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    inputSurface.lockHardwareCanvas()
                } else {
                    inputSurface.lockCanvas(null)
                }

                try {
                    // Center-crop or scale-to-fit bitmap into destRect
                    val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                    canvas.drawBitmap(bitmap, srcRect, destRect, null)
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                bitmap.recycle()

                // Feed frame timestamp corresponding to target FPS
                val presentationTimeUs = i * frameDurationUs

                // Drain output buffer from encoder
                var doneDraining = false
                while (!doneDraining) {
                    val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 2000L)
                    if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        doneDraining = true
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (isMuxerStarted) {
                            throw IllegalStateException("Format change occurred after muxer started")
                        }
                        val newFormat = mediaCodec.outputFormat
                        videoTrackIndex = mediaMuxer.addTrack(newFormat)
                        mediaMuxer.start()
                        isMuxerStarted = true
                    } else if (outputBufferIndex >= 0) {
                        val encodedData = mediaCodec.getOutputBuffer(outputBufferIndex) ?: continue
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size > 0) {
                            if (!isMuxerStarted) {
                                throw IllegalStateException("Muxer has not started yet")
                            }
                            bufferInfo.presentationTimeUs = presentationTimeUs
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            mediaMuxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }

                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            doneDraining = true
                        }
                    }
                }
            }

            // 5. Signal EOS (End Of Stream)
            mediaCodec.signalEndOfInputStream()

            // 6. Final drain to read the remaining frames
            var finished = false
            while (!finished) {
                val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 5000L)
                if (outputBufferIndex >= 0) {
                    val encodedData = mediaCodec.getOutputBuffer(outputBufferIndex) ?: continue
                    if (bufferInfo.size > 0 && isMuxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                    }
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        finished = true
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    finished = true
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Encoding failed: ${e.localizedMessage}", e)
            false
        } finally {
            try {
                mediaCodec?.stop()
                mediaCodec?.release()
            } catch (_: Exception) {}
            try {
                mediaMuxer?.stop()
                mediaMuxer?.release()
            } catch (_: Exception) {}
            try {
                inputSurface?.release()
            } catch (_: Exception) {}
        }
    }
}

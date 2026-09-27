package com.example.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Haptic vibration feedback for camera shutter, mode switch, and timer countdown.
 */
object CameraHaptics {
    fun playShutterTick(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35)
            }
        } catch (_: Exception) {}
    }

    fun playLightTick(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(12, 100))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(12)
            }
        } catch (_: Exception) {}
    }

    fun playLevelLock(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, 150))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        } catch (_: Exception) {}
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}

/**
 * Real-time tilt and roll angle listener for camera horizon level.
 */
@Composable
fun rememberDeviceRollAngle(enabled: Boolean = true): State<Float> {
    val context = LocalContext.current
    val rollAngle = remember { mutableFloatStateOf(0f) }

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || accelerometer == null) {
            return@DisposableEffect onDispose {}
        }

        val listener = object : SensorEventListener {
            private var lastRoll = 0f

            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.values.size < 3) return
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]

                // Calculate roll angle around phone z-axis in degrees
                val angleRad = atan2(-ax.toDouble(), ay.toDouble())
                val rawDeg = Math.toDegrees(angleRad).toFloat()

                // Normalize around portrait 0, 90, 180, 270 degrees
                var normalized = rawDeg
                if (normalized > 45 && normalized <= 135) {
                    normalized -= 90f // Landscape right
                } else if (normalized < -45 && normalized >= -135) {
                    normalized += 90f // Landscape left
                } else if (normalized > 135) {
                    normalized -= 180f
                } else if (normalized < -135) {
                    normalized += 180f
                }

                // Smooth low-pass filter
                val smoothed = lastRoll * 0.8f + normalized * 0.2f
                lastRoll = smoothed
                rollAngle.floatValue = smoothed
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return rollAngle
}

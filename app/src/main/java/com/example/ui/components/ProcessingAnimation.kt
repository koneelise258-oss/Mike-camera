package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ProcessingAnimation(
    message: String = "Amélioration de votre photo…",
    photoBitmap: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    LiquidGlassProgress(
        message = message,
        photoBitmap = photoBitmap,
        modifier = modifier
    )
}

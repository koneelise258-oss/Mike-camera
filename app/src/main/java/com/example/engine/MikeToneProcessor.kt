package com.example.engine

object MikeToneProcessor {

    /**
     * Applies the tone Look-Up Table to an RGB pixel array in place or returns processed channels.
     */
    inline fun applyToneToChannel(channelValue: Int, lut: IntArray): Int {
        return lut[channelValue.coerceIn(0, 255)]
    }

    /**
     * Computes high-precision perceptual luminance (Rec. 709).
     */
    inline fun getLuminance(r: Int, g: Int, b: Int): Int {
        return ((2126 * r + 7152 * g + 722 * b) / 10000).coerceIn(0, 255)
    }
}

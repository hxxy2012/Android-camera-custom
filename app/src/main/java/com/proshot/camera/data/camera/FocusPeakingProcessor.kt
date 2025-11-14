package com.proshot.camera.data.camera

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Focus peaking processor for manual focus assistance
 * Highlights edges in the image to show what's in focus
 */
@Singleton
class FocusPeakingProcessor @Inject constructor() {

    /**
     * Process image and highlight focused areas
     * Uses edge detection to find high-contrast areas
     */
    suspend fun processFocusPeaking(
        bitmap: Bitmap,
        color: PeakingColor = PeakingColor.RED,
        sensitivity: Float = 0.5f // 0.0 - 1.0
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // Apply Sobel edge detection
            val edges = detectEdges(pixels, width, height, sensitivity)

            // Create output bitmap with focus peaking overlay
            val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val outputPixels = IntArray(width * height)
            output.getPixels(outputPixels, 0, width, 0, 0, width, height)

            // Overlay peaking color on edges
            val peakingColorInt = when (color) {
                PeakingColor.RED -> Color.argb(180, 255, 0, 0)
                PeakingColor.GREEN -> Color.argb(180, 0, 255, 0)
                PeakingColor.YELLOW -> Color.argb(180, 255, 255, 0)
                PeakingColor.WHITE -> Color.argb(180, 255, 255, 255)
            }

            for (i in outputPixels.indices) {
                if (edges[i]) {
                    // Blend peaking color with original pixel
                    outputPixels[i] = blendColors(outputPixels[i], peakingColorInt)
                }
            }

            output.setPixels(outputPixels, 0, width, 0, 0, width, height)
            output
        } catch (e: Exception) {
            Timber.e(e, "Failed to process focus peaking")
            bitmap
        }
    }

    /**
     * Detect edges using Sobel operator
     */
    private fun detectEdges(
        pixels: IntArray,
        width: Int,
        height: Int,
        sensitivity: Float
    ): BooleanArray {
        val edges = BooleanArray(width * height)

        // Sobel kernels
        val sobelX = arrayOf(
            intArrayOf(-1, 0, 1),
            intArrayOf(-2, 0, 2),
            intArrayOf(-1, 0, 1)
        )

        val sobelY = arrayOf(
            intArrayOf(-1, -2, -1),
            intArrayOf(0, 0, 0),
            intArrayOf(1, 2, 1)
        )

        // Threshold based on sensitivity (higher sensitivity = lower threshold)
        val threshold = (255 * (1.0f - sensitivity) * 0.5f).toInt()

        // Apply Sobel operator (skip borders)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var gx = 0
                var gy = 0

                // Apply kernels
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = pixels[(y + ky) * width + (x + kx)]
                        val gray = getGrayscale(pixel)

                        gx += gray * sobelX[ky + 1][kx + 1]
                        gy += gray * sobelY[ky + 1][kx + 1]
                    }
                }

                // Calculate gradient magnitude
                val magnitude = kotlin.math.sqrt((gx * gx + gy * gy).toDouble()).toInt()

                // Mark as edge if above threshold
                edges[y * width + x] = magnitude > threshold
            }
        }

        return edges
    }

    /**
     * Convert pixel to grayscale value
     */
    private fun getGrayscale(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)

        // Standard luminance formula
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    /**
     * Blend two colors
     */
    private fun blendColors(color1: Int, color2: Int): Int {
        val alpha2 = Color.alpha(color2) / 255f
        val alpha1 = 1f - alpha2

        val r = (Color.red(color1) * alpha1 + Color.red(color2) * alpha2).toInt()
        val g = (Color.green(color1) * alpha1 + Color.green(color2) * alpha2).toInt()
        val b = (Color.blue(color1) * alpha1 + Color.blue(color2) * alpha2).toInt()

        return Color.argb(255, r, g, b)
    }

    /**
     * Quick focus check - returns focus confidence (0.0 - 1.0)
     */
    fun calculateFocusConfidence(pixels: IntArray, width: Int, height: Int): Float {
        var edgeSum = 0
        var pixelCount = 0

        // Sample center region only
        val startX = width / 4
        val endX = 3 * width / 4
        val startY = height / 4
        val endY = 3 * height / 4

        for (y in startY until endY step 4) {
            for (x in startX until endX step 4) {
                if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                    val current = getGrayscale(pixels[y * width + x])
                    val right = getGrayscale(pixels[y * width + (x + 1)])
                    val down = getGrayscale(pixels[(y + 1) * width + x])

                    edgeSum += abs(current - right) + abs(current - down)
                    pixelCount++
                }
            }
        }

        if (pixelCount == 0) return 0f

        val avgEdge = edgeSum.toFloat() / pixelCount

        // Normalize to 0-1 range (empirical values)
        return (avgEdge / 50f).coerceIn(0f, 1f)
    }
}

/**
 * Focus peaking color options
 */
enum class PeakingColor {
    RED,
    GREEN,
    YELLOW,
    WHITE
}

package com.proshot.camera.data.camera

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicHistogram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Histogram analyzer for real-time exposure analysis
 * Provides RGB and luminance histograms for professional exposure control
 */
@Singleton
class HistogramAnalyzer @Inject constructor() {

    /**
     * Calculate histogram from image data
     * Returns IntArray with 256 bins for each channel
     */
    suspend fun analyzeImage(image: Image): HistogramData = withContext(Dispatchers.Default) {
        try {
            when (image.format) {
                ImageFormat.YUV_420_888 -> analyzeYUV(image)
                ImageFormat.JPEG -> analyzeJPEG(image)
                else -> {
                    Timber.w("Unsupported image format: ${image.format}")
                    HistogramData.empty()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to analyze histogram")
            HistogramData.empty()
        }
    }

    /**
     * Analyze YUV image (preview frames)
     */
    private fun analyzeYUV(image: Image): HistogramData {
        val yPlane = image.planes[0]
        val yBuffer = yPlane.buffer
        val ySize = yBuffer.remaining()

        val luminanceHistogram = IntArray(256)

        // Sample every 4th pixel for performance
        val step = 4
        var position = 0

        while (position < ySize) {
            yBuffer.position(position)
            val y = yBuffer.get().toInt() and 0xFF
            luminanceHistogram[y]++
            position += step
        }

        // For YUV, we use Y channel for all RGB channels (grayscale approximation)
        return HistogramData(
            red = luminanceHistogram.clone(),
            green = luminanceHistogram.clone(),
            blue = luminanceHistogram.clone(),
            luminance = luminanceHistogram
        )
    }

    /**
     * Analyze JPEG image
     */
    private fun analyzeJPEG(image: Image): HistogramData {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // For JPEG, we'd need to decode first
        // This is a simplified version
        val luminanceHistogram = IntArray(256)

        return HistogramData(
            red = luminanceHistogram.clone(),
            green = luminanceHistogram.clone(),
            blue = luminanceHistogram.clone(),
            luminance = luminanceHistogram
        )
    }

    /**
     * Analyze bitmap for histogram
     */
    fun analyzeBitmap(bitmap: Bitmap): HistogramData {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val redHistogram = IntArray(256)
        val greenHistogram = IntArray(256)
        val blueHistogram = IntArray(256)
        val luminanceHistogram = IntArray(256)

        // Sample every 4th pixel for performance
        val step = 4

        for (i in pixels.indices step step) {
            val pixel = pixels[i]

            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            redHistogram[r]++
            greenHistogram[g]++
            blueHistogram[b]++

            // Luminance = 0.299R + 0.587G + 0.114B
            val lum = ((0.299 * r + 0.587 * g + 0.114 * b).toInt()).coerceIn(0, 255)
            luminanceHistogram[lum]++
        }

        return HistogramData(
            red = redHistogram,
            green = greenHistogram,
            blue = blueHistogram,
            luminance = luminanceHistogram
        )
    }

    /**
     * Calculate exposure statistics from histogram
     */
    fun calculateExposureStats(histogram: HistogramData): ExposureStats {
        val lum = histogram.luminance
        val total = lum.sum().toFloat()

        if (total == 0f) {
            return ExposureStats(
                meanLuminance = 0f,
                clippedShadows = 0f,
                clippedHighlights = 0f,
                isUnderexposed = false,
                isOverexposed = false
            )
        }

        // Mean luminance
        var sum = 0f
        for (i in lum.indices) {
            sum += i * lum[i]
        }
        val mean = sum / total

        // Clipped shadows (first 5 bins)
        val shadows = (0..4).sumOf { lum[it] }.toFloat() / total

        // Clipped highlights (last 5 bins)
        val highlights = (251..255).sumOf { lum[it] }.toFloat() / total

        return ExposureStats(
            meanLuminance = mean / 255f,
            clippedShadows = shadows,
            clippedHighlights = highlights,
            isUnderexposed = mean < 64 && shadows > 0.05f,
            isOverexposed = mean > 192 && highlights > 0.05f
        )
    }
}

/**
 * Histogram data for all channels
 */
data class HistogramData(
    val red: IntArray,
    val green: IntArray,
    val blue: IntArray,
    val luminance: IntArray
) {
    companion object {
        fun empty() = HistogramData(
            red = IntArray(256),
            green = IntArray(256),
            blue = IntArray(256),
            luminance = IntArray(256)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistogramData

        if (!red.contentEquals(other.red)) return false
        if (!green.contentEquals(other.green)) return false
        if (!blue.contentEquals(other.blue)) return false
        if (!luminance.contentEquals(other.luminance)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = red.contentHashCode()
        result = 31 * result + green.contentHashCode()
        result = 31 * result + blue.contentHashCode()
        result = 31 * result + luminance.contentHashCode()
        return result
    }
}

/**
 * Exposure statistics from histogram analysis
 */
data class ExposureStats(
    val meanLuminance: Float,      // 0.0 - 1.0
    val clippedShadows: Float,     // Percentage of clipped shadows
    val clippedHighlights: Float,  // Percentage of clipped highlights
    val isUnderexposed: Boolean,
    val isOverexposed: Boolean
)

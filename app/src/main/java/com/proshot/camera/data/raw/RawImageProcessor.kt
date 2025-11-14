package com.proshot.camera.data.raw

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * RAW image processor for DNG files
 * Provides non-destructive editing capabilities
 */
@Singleton
class RawImageProcessor @Inject constructor() {

    /**
     * Load RAW (DNG) image
     */
    suspend fun loadRawImage(file: File): Result<RawImage> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Loading RAW image: ${file.absolutePath}")

            // Read EXIF data
            val exif = ExifInterface(file.absolutePath)
            val metadata = extractMetadata(exif)

            // For DNG files, we can extract the embedded JPEG preview
            // In a production app, you would use a proper RAW decoder like libraw
            val previewBitmap = loadDngPreview(file)

            if (previewBitmap != null) {
                val rawImage = RawImage(
                    file = file,
                    width = previewBitmap.width,
                    height = previewBitmap.height,
                    previewBitmap = previewBitmap,
                    metadata = metadata,
                    histogram = calculateHistogram(previewBitmap)
                )

                Timber.d("RAW image loaded: ${rawImage.width}x${rawImage.height}")
                Result.success(rawImage)
            } else {
                Result.failure(Exception("Failed to load DNG preview"))
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to load RAW image")
            Result.failure(e)
        }
    }

    /**
     * Apply non-destructive edits to RAW image
     */
    suspend fun applyEdits(
        rawImage: RawImage,
        edits: RawEditParameters
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            Timber.d("Applying edits to RAW image")

            var bitmap = rawImage.previewBitmap.copy(Bitmap.Config.ARGB_8888, true)

            // Apply edits in the correct order for best results
            // 1. Exposure
            if (edits.exposure != 0f) {
                bitmap = applyExposure(bitmap, edits.exposure)
            }

            // 2. White Balance
            if (edits.temperature != 0f || edits.tint != 0f) {
                bitmap = applyWhiteBalance(bitmap, edits.temperature, edits.tint)
            }

            // 3. Contrast
            if (edits.contrast != 0f) {
                bitmap = applyContrast(bitmap, edits.contrast)
            }

            // 4. Highlights & Shadows
            if (edits.highlights != 0f || edits.shadows != 0f) {
                bitmap = applyHighlightsShadows(bitmap, edits.highlights, edits.shadows)
            }

            // 5. Saturation & Vibrance
            if (edits.saturation != 0f || edits.vibrance != 0f) {
                bitmap = applySaturationVibrance(bitmap, edits.saturation, edits.vibrance)
            }

            // 6. Tone Curve
            if (edits.toneCurve != null) {
                bitmap = applyToneCurve(bitmap, edits.toneCurve)
            }

            // 7. Sharpness
            if (edits.sharpness != 0f) {
                bitmap = applySharpness(bitmap, edits.sharpness)
            }

            // 8. Denoise
            if (edits.denoise != 0f) {
                bitmap = applyDenoise(bitmap, edits.denoise)
            }

            // 9. Vignette
            if (edits.vignette != 0f) {
                bitmap = applyVignette(bitmap, edits.vignette)
            }

            Timber.d("Edits applied successfully")
            Result.success(bitmap)

        } catch (e: Exception) {
            Timber.e(e, "Failed to apply edits")
            Result.failure(e)
        }
    }

    /**
     * Apply exposure adjustment (-2.0 to +2.0 EV)
     */
    private fun applyExposure(bitmap: Bitmap, exposure: Float): Bitmap {
        val factor = 2f.pow(exposure)
        return adjustPixels(bitmap) { r, g, b ->
            Triple(
                (r * factor).toInt().coerceIn(0, 255),
                (g * factor).toInt().coerceIn(0, 255),
                (b * factor).toInt().coerceIn(0, 255)
            )
        }
    }

    /**
     * Apply white balance adjustment
     * Temperature: -100 to +100 (blue to yellow)
     * Tint: -100 to +100 (green to magenta)
     */
    private fun applyWhiteBalance(bitmap: Bitmap, temperature: Float, tint: Float): Bitmap {
        // Temperature adjustment (affects red and blue channels)
        val tempFactor = temperature / 100f
        val redAdjust = 1f + (tempFactor * 0.3f)
        val blueAdjust = 1f - (tempFactor * 0.3f)

        // Tint adjustment (affects green channel)
        val tintFactor = tint / 100f
        val greenAdjust = 1f - (tintFactor * 0.3f)

        return adjustPixels(bitmap) { r, g, b ->
            Triple(
                (r * redAdjust).toInt().coerceIn(0, 255),
                (g * greenAdjust).toInt().coerceIn(0, 255),
                (b * blueAdjust).toInt().coerceIn(0, 255)
            )
        }
    }

    /**
     * Apply contrast adjustment (-100 to +100)
     */
    private fun applyContrast(bitmap: Bitmap, contrast: Float): Bitmap {
        val factor = (259f * (contrast + 255f)) / (255f * (259f - contrast))

        return adjustPixels(bitmap) { r, g, b ->
            Triple(
                ((factor * (r - 128)) + 128).toInt().coerceIn(0, 255),
                ((factor * (g - 128)) + 128).toInt().coerceIn(0, 255),
                ((factor * (b - 128)) + 128).toInt().coerceIn(0, 255)
            )
        }
    }

    /**
     * Apply highlights and shadows adjustment (-100 to +100 each)
     */
    private fun applyHighlightsShadows(
        bitmap: Bitmap,
        highlights: Float,
        shadows: Float
    ): Bitmap {
        val highlightFactor = 1f - (highlights / 100f * 0.5f)
        val shadowFactor = 1f + (shadows / 100f * 0.5f)

        return adjustPixels(bitmap) { r, g, b ->
            val luminance = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

            // Apply different factors based on luminance
            val factor = if (luminance > 0.5f) {
                // Highlights
                1f + (luminance - 0.5f) * 2f * (highlightFactor - 1f)
            } else {
                // Shadows
                1f + (0.5f - luminance) * 2f * (shadowFactor - 1f)
            }

            Triple(
                (r * factor).toInt().coerceIn(0, 255),
                (g * factor).toInt().coerceIn(0, 255),
                (b * factor).toInt().coerceIn(0, 255)
            )
        }
    }

    /**
     * Apply saturation and vibrance
     * Saturation: -100 to +100 (affects all colors equally)
     * Vibrance: -100 to +100 (smart saturation, affects muted colors more)
     */
    private fun applySaturationVibrance(
        bitmap: Bitmap,
        saturation: Float,
        vibrance: Float
    ): Bitmap {
        val satFactor = 1f + (saturation / 100f)
        val vibFactor = vibrance / 100f

        return adjustPixels(bitmap) { r, g, b ->
            // Convert to HSL
            val max = max(max(r, g), b) / 255f
            val min = min(min(r, g), b) / 255f
            val luminance = (max + min) / 2f
            val currentSat = if (max == min) 0f else {
                val delta = max - min
                if (luminance > 0.5f) delta / (2f - max - min) else delta / (max + min)
            }

            // Apply saturation
            var finalSatFactor = satFactor

            // Add vibrance (affects less saturated colors more)
            if (vibFactor != 0f) {
                val vibranceBoost = (1f - currentSat) * vibFactor
                finalSatFactor += vibranceBoost
            }

            // Apply to RGB
            val gray = 0.299f * r + 0.587f * g + 0.114f * b
            Triple(
                (gray + (r - gray) * finalSatFactor).toInt().coerceIn(0, 255),
                (gray + (g - gray) * finalSatFactor).toInt().coerceIn(0, 255),
                (gray + (b - gray) * finalSatFactor).toInt().coerceIn(0, 255)
            )
        }
    }

    /**
     * Apply tone curve
     */
    private fun applyToneCurve(bitmap: Bitmap, curve: ToneCurve): Bitmap {
        // Create lookup table from curve
        val lut = IntArray(256) { i ->
            curve.evaluate(i / 255f).times(255f).toInt().coerceIn(0, 255)
        }

        return adjustPixels(bitmap) { r, g, b ->
            Triple(lut[r], lut[g], lut[b])
        }
    }

    /**
     * Apply sharpness (0 to +100)
     */
    private fun applySharpness(bitmap: Bitmap, sharpness: Float): Bitmap {
        if (sharpness <= 0f) return bitmap

        val amount = sharpness / 100f
        val width = bitmap.width
        val height = bitmap.height
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        // Unsharp mask kernel
        val kernel = arrayOf(
            floatArrayOf(0f, -amount, 0f),
            floatArrayOf(-amount, 1f + 4f * amount, -amount),
            floatArrayOf(0f, -amount, 0f)
        )

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0f
                var g = 0f
                var b = 0f

                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = bitmap.getPixel(x + kx, y + ky)
                        val weight = kernel[ky + 1][kx + 1]
                        r += ((pixel shr 16) and 0xFF) * weight
                        g += ((pixel shr 8) and 0xFF) * weight
                        b += (pixel and 0xFF) * weight
                    }
                }

                val newPixel = (0xFF shl 24) or
                        (r.toInt().coerceIn(0, 255) shl 16) or
                        (g.toInt().coerceIn(0, 255) shl 8) or
                        b.toInt().coerceIn(0, 255)

                output.setPixel(x, y, newPixel)
            }
        }

        return output
    }

    /**
     * Apply noise reduction (0 to +100)
     */
    private fun applyDenoise(bitmap: Bitmap, denoise: Float): Bitmap {
        if (denoise <= 0f) return bitmap

        // Simple box blur for noise reduction
        val radius = (denoise / 100f * 3f).toInt().coerceAtLeast(1)
        return applyBoxBlur(bitmap, radius)
    }

    /**
     * Apply vignette effect (-100 to +100)
     * Negative values lighten edges, positive values darken edges
     */
    private fun applyVignette(bitmap: Bitmap, vignette: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val centerX = width / 2f
        val centerY = height / 2f
        val maxDist = kotlin.math.sqrt((centerX * centerX + centerY * centerY).toDouble()).toFloat()
        val strength = vignette / 100f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - centerX
                val dy = y - centerY
                val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val factor = 1f - (dist / maxDist) * strength

                val pixel = bitmap.getPixel(x, y)
                val r = ((pixel shr 16) and 0xFF)
                val g = ((pixel shr 8) and 0xFF)
                val b = (pixel and 0xFF)

                val newPixel = (0xFF shl 24) or
                        ((r * factor).toInt().coerceIn(0, 255) shl 16) or
                        ((g * factor).toInt().coerceIn(0, 255) shl 8) or
                        (b * factor).toInt().coerceIn(0, 255)

                output.setPixel(x, y, newPixel)
            }
        }

        return output
    }

    /**
     * Helper function to adjust pixels
     */
    private inline fun adjustPixels(
        bitmap: Bitmap,
        transform: (r: Int, g: Int, b: Int) -> Triple<Int, Int, Int>
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val (newR, newG, newB) = transform(r, g, b)

                val newPixel = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
                output.setPixel(x, y, newPixel)
            }
        }

        return output
    }

    /**
     * Apply box blur
     */
    private fun applyBoxBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0

                for (ky in -radius..radius) {
                    for (kx in -radius..radius) {
                        val px = (x + kx).coerceIn(0, width - 1)
                        val py = (y + ky).coerceIn(0, height - 1)
                        val pixel = bitmap.getPixel(px, py)

                        r += (pixel shr 16) and 0xFF
                        g += (pixel shr 8) and 0xFF
                        b += pixel and 0xFF
                        count++
                    }
                }

                val newPixel = (0xFF shl 24) or
                        ((r / count) shl 16) or
                        ((g / count) shl 8) or
                        (b / count)

                output.setPixel(x, y, newPixel)
            }
        }

        return output
    }

    /**
     * Load DNG preview image
     */
    private fun loadDngPreview(file: File): Bitmap? {
        return try {
            // For DNG files, BitmapFactory can extract the embedded JPEG preview
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load DNG preview")
            null
        }
    }

    /**
     * Extract metadata from EXIF
     */
    private fun extractMetadata(exif: ExifInterface): RawMetadata {
        return RawMetadata(
            make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: "Unknown",
            model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: "Unknown",
            iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)?.toIntOrNull() ?: 0,
            exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) ?: "0",
            fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER) ?: "0",
            focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: "0",
            dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME) ?: "",
            width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0),
            height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
        )
    }

    /**
     * Calculate histogram for image
     */
    private fun calculateHistogram(bitmap: Bitmap): ImageHistogram {
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)
        val luminance = IntArray(256)

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt()

                red[r]++
                green[g]++
                blue[b]++
                luminance[lum]++
            }
        }

        return ImageHistogram(red, green, blue, luminance)
    }
}

/**
 * RAW image data
 */
data class RawImage(
    val file: File,
    val width: Int,
    val height: Int,
    val previewBitmap: Bitmap,
    val metadata: RawMetadata,
    val histogram: ImageHistogram
)

/**
 * RAW metadata
 */
data class RawMetadata(
    val make: String,
    val model: String,
    val iso: Int,
    val exposureTime: String,
    val fNumber: String,
    val focalLength: String,
    val dateTime: String,
    val width: Int,
    val height: Int
)

/**
 * Image histogram
 */
data class ImageHistogram(
    val red: IntArray,
    val green: IntArray,
    val blue: IntArray,
    val luminance: IntArray
)

/**
 * RAW edit parameters (non-destructive)
 */
data class RawEditParameters(
    val exposure: Float = 0f,           // -2.0 to +2.0 EV
    val contrast: Float = 0f,           // -100 to +100
    val highlights: Float = 0f,         // -100 to +100
    val shadows: Float = 0f,            // -100 to +100
    val temperature: Float = 0f,        // -100 to +100 (blue to yellow)
    val tint: Float = 0f,               // -100 to +100 (green to magenta)
    val saturation: Float = 0f,         // -100 to +100
    val vibrance: Float = 0f,           // -100 to +100
    val sharpness: Float = 0f,          // 0 to +100
    val denoise: Float = 0f,            // 0 to +100
    val vignette: Float = 0f,           // -100 to +100
    val toneCurve: ToneCurve? = null
)

/**
 * Tone curve for advanced tonal adjustments
 */
data class ToneCurve(
    val points: List<CurvePoint> = listOf(
        CurvePoint(0f, 0f),      // Black point
        CurvePoint(1f, 1f)       // White point
    )
) {
    /**
     * Evaluate curve at input value (0.0 - 1.0)
     */
    fun evaluate(input: Float): Float {
        if (points.size < 2) return input

        // Find surrounding points
        val sortedPoints = points.sortedBy { it.x }

        // Before first point
        if (input <= sortedPoints.first().x) {
            return sortedPoints.first().y
        }

        // After last point
        if (input >= sortedPoints.last().x) {
            return sortedPoints.last().y
        }

        // Find interpolation points
        for (i in 0 until sortedPoints.size - 1) {
            val p1 = sortedPoints[i]
            val p2 = sortedPoints[i + 1]

            if (input >= p1.x && input <= p2.x) {
                // Linear interpolation
                val t = (input - p1.x) / (p2.x - p1.x)
                return p1.y + t * (p2.y - p1.y)
            }
        }

        return input
    }
}

/**
 * Curve point
 */
data class CurvePoint(
    val x: Float,  // Input (0.0 - 1.0)
    val y: Float   // Output (0.0 - 1.0)
)

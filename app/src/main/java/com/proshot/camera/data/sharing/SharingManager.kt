package com.proshot.camera.data.sharing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sharing manager for exporting and sharing photos
 * Supports various export formats and sharing methods
 */
@Singleton
class SharingManager @Inject constructor(
    private val context: Context
) {
    private val exportDir = File(context.cacheDir, "exports")

    init {
        // Create export directory
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
    }

    /**
     * Export bitmap to file
     */
    suspend fun exportBitmap(
        bitmap: Bitmap,
        settings: ExportSettings
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Generate filename
            val timestamp = System.currentTimeMillis()
            val extension = when (settings.format) {
                ExportFormat.JPEG -> "jpg"
                ExportFormat.PNG -> "png"
                ExportFormat.WEBP -> "webp"
            }
            val filename = "ProShot_${timestamp}.${extension}"
            val file = File(exportDir, filename)

            // Compress and save
            FileOutputStream(file).use { output ->
                val success = when (settings.format) {
                    ExportFormat.JPEG -> bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        settings.quality,
                        output
                    )
                    ExportFormat.PNG -> bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,  // PNG is lossless
                        output
                    )
                    ExportFormat.WEBP -> bitmap.compress(
                        Bitmap.CompressFormat.WEBP,
                        settings.quality,
                        output
                    )
                }

                if (success) {
                    Timber.d("Image exported: ${file.absolutePath} (${file.length()} bytes)")
                    Result.success(file)
                } else {
                    Result.failure(Exception("Failed to compress image"))
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to export bitmap")
            Result.failure(e)
        }
    }

    /**
     * Share file using Android share sheet
     */
    fun shareFile(file: File, mimeType: String = "image/*"): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Share multiple files
     */
    fun shareMultipleFiles(files: List<File>, mimeType: String = "image/*"): Intent {
        val uris = files.map { file ->
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }

        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Share to specific app
     */
    fun shareToApp(file: File, packageName: String, mimeType: String = "image/*"): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            setPackage(packageName)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Copy file to MediaStore (save to gallery)
     */
    suspend fun saveToGallery(file: File): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // Use MediaStore to add to gallery
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, getMimeType(file))
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ProShot")
            }

            val uri = context.contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )

            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    file.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }

                Timber.d("File saved to gallery: $uri")
                Result.success(uri)
            } else {
                Result.failure(Exception("Failed to create MediaStore entry"))
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to save to gallery")
            Result.failure(e)
        }
    }

    /**
     * Get shareable apps
     */
    fun getShareableApps(mimeType: String = "image/*"): List<ShareTarget> {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
        }

        val activities = context.packageManager.queryIntentActivities(
            shareIntent,
            0
        )

        return activities.map { resolveInfo ->
            ShareTarget(
                packageName = resolveInfo.activityInfo.packageName,
                name = resolveInfo.loadLabel(context.packageManager).toString(),
                icon = resolveInfo.loadIcon(context.packageManager)
            )
        }
    }

    /**
     * Create export settings based on quality preset
     */
    fun createExportSettings(preset: ExportPreset): ExportSettings {
        return when (preset) {
            ExportPreset.ULTRA_HIGH -> ExportSettings(
                format = ExportFormat.PNG,
                quality = 100,
                maxDimension = null,
                watermark = null
            )
            ExportPreset.HIGH -> ExportSettings(
                format = ExportFormat.JPEG,
                quality = 95,
                maxDimension = 4096,
                watermark = null
            )
            ExportPreset.MEDIUM -> ExportSettings(
                format = ExportFormat.JPEG,
                quality = 85,
                maxDimension = 2048,
                watermark = null
            )
            ExportPreset.LOW -> ExportSettings(
                format = ExportFormat.JPEG,
                quality = 70,
                maxDimension = 1024,
                watermark = null
            )
            ExportPreset.SOCIAL_MEDIA -> ExportSettings(
                format = ExportFormat.JPEG,
                quality = 90,
                maxDimension = 2048,
                watermark = WatermarkSettings(
                    text = "ProShot",
                    position = WatermarkPosition.BOTTOM_RIGHT,
                    opacity = 0.5f
                )
            )
        }
    }

    /**
     * Add watermark to bitmap
     */
    fun addWatermark(
        bitmap: Bitmap,
        settings: WatermarkSettings
    ): Bitmap {
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(output)

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 48f
            alpha = (settings.opacity * 255).toInt()
            isAntiAlias = true
            textAlign = when (settings.position) {
                WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.TOP_LEFT -> android.graphics.Paint.Align.LEFT
                WatermarkPosition.BOTTOM_RIGHT, WatermarkPosition.TOP_RIGHT -> android.graphics.Paint.Align.RIGHT
                WatermarkPosition.CENTER -> android.graphics.Paint.Align.CENTER
            }
        }

        val padding = 32f
        val x = when (settings.position) {
            WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.TOP_LEFT -> padding
            WatermarkPosition.BOTTOM_RIGHT, WatermarkPosition.TOP_RIGHT -> bitmap.width - padding
            WatermarkPosition.CENTER -> bitmap.width / 2f
        }

        val y = when (settings.position) {
            WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> bitmap.height - padding
            WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> padding + 48f
            WatermarkPosition.CENTER -> bitmap.height / 2f
        }

        canvas.drawText(settings.text, x, y, paint)

        return output
    }

    /**
     * Resize bitmap if needed
     */
    fun resizeBitmap(bitmap: Bitmap, maxDimension: Int?): Bitmap {
        if (maxDimension == null) return bitmap

        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val scale = if (width > height) {
            maxDimension.toFloat() / width
        } else {
            maxDimension.toFloat() / height
        }

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Get MIME type from file extension
     */
    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "dng" -> "image/x-adobe-dng"
            "mp4" -> "video/mp4"
            else -> "application/octet-stream"
        }
    }

    /**
     * Clear export cache
     */
    fun clearExportCache() {
        exportDir.listFiles()?.forEach { it.delete() }
        Timber.d("Export cache cleared")
    }
}

/**
 * Export settings
 */
data class ExportSettings(
    val format: ExportFormat,
    val quality: Int = 95,         // 1-100 for JPEG/WebP
    val maxDimension: Int? = null,  // Max width or height (null = original size)
    val watermark: WatermarkSettings? = null
)

/**
 * Export format
 */
enum class ExportFormat {
    JPEG,
    PNG,
    WEBP
}

/**
 * Export preset
 */
enum class ExportPreset(val displayName: String) {
    ULTRA_HIGH("Ultra High (PNG)"),
    HIGH("High (95% JPEG)"),
    MEDIUM("Medium (85% JPEG)"),
    LOW("Low (70% JPEG)"),
    SOCIAL_MEDIA("Social Media (Optimized)")
}

/**
 * Watermark settings
 */
data class WatermarkSettings(
    val text: String,
    val position: WatermarkPosition,
    val opacity: Float = 0.5f  // 0.0 - 1.0
)

/**
 * Watermark position
 */
enum class WatermarkPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER
}

/**
 * Share target (app that can receive shared content)
 */
data class ShareTarget(
    val packageName: String,
    val name: String,
    val icon: android.graphics.drawable.Drawable
)

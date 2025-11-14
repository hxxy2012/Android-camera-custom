package com.proshot.camera.data.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.DngCreator
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Environment
import com.proshot.camera.domain.model.ImageFormat as AppImageFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles RAW image capture and DNG file creation
 * Professional photographers need RAW for maximum post-processing flexibility
 */
@Singleton
class RawCaptureController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val camera2Manager: Camera2Manager
) {
    private var rawImageReader: ImageReader? = null
    private var jpegImageReader: ImageReader? = null

    /**
     * Initialize image readers for RAW and JPEG
     */
    fun initializeImageReaders(cameraId: String) {
        val characteristics = camera2Manager.getCameraCharacteristics(cameraId) ?: return

        // Get largest available size for RAW
        val rawSizes = camera2Manager.getOutputSizes(cameraId, ImageFormat.RAW_SENSOR)
        val rawSize = rawSizes.maxByOrNull { it.width * it.height }

        if (rawSize != null) {
            rawImageReader = ImageReader.newInstance(
                rawSize.width,
                rawSize.height,
                ImageFormat.RAW_SENSOR,
                2 // Buffer for 2 images
            )
            Timber.d("RAW ImageReader initialized: ${rawSize.width}x${rawSize.height}")
        } else {
            Timber.w("RAW format not supported on this device")
        }

        // JPEG reader
        val jpegSizes = camera2Manager.getOutputSizes(cameraId, ImageFormat.JPEG)
        val jpegSize = jpegSizes.maxByOrNull { it.width * it.height }

        if (jpegSize != null) {
            jpegImageReader = ImageReader.newInstance(
                jpegSize.width,
                jpegSize.height,
                ImageFormat.JPEG,
                2
            )
            Timber.d("JPEG ImageReader initialized: ${jpegSize.width}x${jpegSize.height}")
        }
    }

    /**
     * Capture image in specified format
     */
    suspend fun captureImage(
        format: AppImageFormat,
        onComplete: (File?) -> Unit
    ) = withContext(Dispatchers.IO) {
        when (format) {
            AppImageFormat.RAW_ONLY -> captureRaw(onComplete)
            AppImageFormat.JPEG_ONLY -> captureJpeg(onComplete)
            AppImageFormat.RAW_JPEG -> {
                // Capture both RAW and JPEG
                captureRaw { rawFile ->
                    captureJpeg { jpegFile ->
                        // Return both files (could be improved with a data class)
                        onComplete(rawFile)
                    }
                }
            }
        }
    }

    /**
     * Capture RAW image and save as DNG
     */
    private suspend fun captureRaw(onComplete: (File?) -> Unit) = withContext(Dispatchers.IO) {
        val reader = rawImageReader
        val camera = camera2Manager.getCurrentCamera()
        val session = camera2Manager.getCurrentSession()

        if (reader == null || camera == null || session == null) {
            Timber.e("Cannot capture RAW: reader=$reader, camera=$camera, session=$session")
            onComplete(null)
            return@withContext
        }

        try {
            val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)

            // Configure for best quality
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF)
            captureBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY)
            captureBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY)

            reader.setOnImageAvailableListener({ imageReader ->
                val image = imageReader.acquireLatestImage()
                if (image != null) {
                    val file = saveRawImage(image, camera)
                    image.close()
                    onComplete(file)
                }
            }, null)

            session.capture(captureBuilder.build(), object : android.hardware.camera2.CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: android.hardware.camera2.CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    Timber.d("RAW capture completed")
                }
            }, null)

        } catch (e: Exception) {
            Timber.e(e, "Failed to capture RAW")
            onComplete(null)
        }
    }

    /**
     * Save RAW image as DNG file
     */
    private fun saveRawImage(image: Image, camera: CameraDevice): File? {
        val characteristics = camera2Manager.getCurrentCharacteristics() ?: return null

        try {
            // Create output file
            val file = createImageFile("DNG")

            FileOutputStream(file).use { outputStream ->
                // Create DNG from raw image
                val dngCreator = DngCreator(characteristics, null)

                // Set orientation (could be dynamic based on sensor)
                dngCreator.setOrientation(android.media.ExifInterface.ORIENTATION_NORMAL)

                // Write DNG
                dngCreator.writeImage(outputStream, image)
                dngCreator.close()

                Timber.d("RAW image saved: ${file.absolutePath}")
                return file
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to save RAW image")
            return null
        }
    }

    /**
     * Capture JPEG image
     */
    private suspend fun captureJpeg(onComplete: (File?) -> Unit) = withContext(Dispatchers.IO) {
        val reader = jpegImageReader
        val camera = camera2Manager.getCurrentCamera()
        val session = camera2Manager.getCurrentSession()

        if (reader == null || camera == null || session == null) {
            Timber.e("Cannot capture JPEG: reader=$reader, camera=$camera, session=$session")
            onComplete(null)
            return@withContext
        }

        try {
            val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)

            // High quality JPEG
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, 95.toByte())

            reader.setOnImageAvailableListener({ imageReader ->
                val image = imageReader.acquireLatestImage()
                if (image != null) {
                    val file = saveJpegImage(image)
                    image.close()
                    onComplete(file)
                }
            }, null)

            session.capture(captureBuilder.build(), object : android.hardware.camera2.CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: android.hardware.camera2.CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    Timber.d("JPEG capture completed")
                }
            }, null)

        } catch (e: Exception) {
            Timber.e(e, "Failed to capture JPEG")
            onComplete(null)
        }
    }

    /**
     * Save JPEG image
     */
    private fun saveJpegImage(image: Image): File? {
        try {
            val file = createImageFile("JPG")
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            FileOutputStream(file).use { outputStream ->
                outputStream.write(bytes)
            }

            Timber.d("JPEG image saved: ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Timber.e(e, "Failed to save JPEG image")
            return null
        }
    }

    /**
     * Create image file with timestamp
     */
    private fun createImageFile(extension: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "ProShot_${timeStamp}.$extension"

        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "ProShot"
        )

        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        return File(storageDir, fileName)
    }

    /**
     * Get RAW image reader surface
     */
    fun getRawSurface() = rawImageReader?.surface

    /**
     * Get JPEG image reader surface
     */
    fun getJpegSurface() = jpegImageReader?.surface

    /**
     * Release resources
     */
    fun release() {
        rawImageReader?.close()
        jpegImageReader?.close()
        rawImageReader = null
        jpegImageReader = null
        Timber.d("RAW capture controller released")
    }
}

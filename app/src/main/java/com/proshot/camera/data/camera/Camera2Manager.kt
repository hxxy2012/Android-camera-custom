package com.proshot.camera.data.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Manages Camera2 API operations
 * Provides low-level camera control for professional manual photography
 */
@Singleton
class Camera2Manager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var currentCameraId: String? = null
    private var cameraCharacteristics: CameraCharacteristics? = null

    /**
     * Start background thread for camera operations
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    /**
     * Stop background thread
     */
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Timber.e(e, "Error stopping background thread")
        }
    }

    /**
     * Get available camera IDs
     */
    fun getCameraIds(): List<String> {
        return try {
            cameraManager.cameraIdList.toList()
        } catch (e: CameraAccessException) {
            Timber.e(e, "Failed to get camera IDs")
            emptyList()
        }
    }

    /**
     * Get camera characteristics for a specific camera
     */
    fun getCameraCharacteristics(cameraId: String): CameraCharacteristics? {
        return try {
            cameraManager.getCameraCharacteristics(cameraId)
        } catch (e: CameraAccessException) {
            Timber.e(e, "Failed to get camera characteristics for $cameraId")
            null
        }
    }

    /**
     * Get the main back-facing camera ID
     */
    fun getBackCameraId(): String? {
        return getCameraIds().firstOrNull { id ->
            val characteristics = getCameraCharacteristics(id)
            characteristics?.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_BACK
        }
    }

    /**
     * Check if camera has full manual control (FULL or LEVEL_3)
     */
    fun hasManualControl(cameraId: String): Boolean {
        val characteristics = getCameraCharacteristics(cameraId) ?: return false
        val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        return hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
               hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
    }

    /**
     * Get available output sizes for a given format
     */
    fun getOutputSizes(cameraId: String, format: Int): List<Size> {
        val characteristics = getCameraCharacteristics(cameraId) ?: return emptyList()
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return map?.getOutputSizes(format)?.toList() ?: emptyList()
    }

    /**
     * Get ISO range supported by camera
     */
    fun getIsoRange(cameraId: String): IntRange? {
        val characteristics = getCameraCharacteristics(cameraId) ?: return null
        val range = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        return range?.let { IntRange(it.lower, it.upper) }
    }

    /**
     * Get exposure time (shutter speed) range in nanoseconds
     */
    fun getExposureTimeRange(cameraId: String): LongRange? {
        val characteristics = getCameraCharacteristics(cameraId) ?: return null
        val range = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        return range?.let { LongRange(it.lower, it.upper) }
    }

    /**
     * Get focus distance range
     */
    fun getFocusDistanceRange(cameraId: String): ClosedFloatingPointRange<Float>? {
        val characteristics = getCameraCharacteristics(cameraId) ?: return null
        val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        return minFocusDistance?.let { 0f..it }
    }

    /**
     * Check if camera supports RAW capture
     */
    fun supportsRaw(cameraId: String): Boolean {
        val characteristics = getCameraCharacteristics(cameraId) ?: return false
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        return capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true
    }

    /**
     * Open camera device
     */
    suspend fun openCamera(cameraId: String): Result<CameraDevice> = suspendCoroutine { continuation ->
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            continuation.resumeWithException(SecurityException("Camera permission not granted"))
            return@suspendCoroutine
        }

        startBackgroundThread()

        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Timber.d("Camera $cameraId opened successfully")
                    cameraDevice = camera
                    currentCameraId = cameraId
                    cameraCharacteristics = getCameraCharacteristics(cameraId)
                    continuation.resume(Result.success(camera))
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Timber.w("Camera $cameraId disconnected")
                    camera.close()
                    cameraDevice = null
                    continuation.resumeWithException(Exception("Camera disconnected"))
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Timber.e("Camera $cameraId error: $error")
                    camera.close()
                    cameraDevice = null
                    continuation.resumeWithException(Exception("Camera error: $error"))
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Timber.e(e, "Failed to open camera $cameraId")
            continuation.resumeWithException(e)
        }
    }

    /**
     * Create capture session
     */
    suspend fun createCaptureSession(
        surfaces: List<Surface>
    ): Result<CameraCaptureSession> = suspendCoroutine { continuation ->
        val camera = cameraDevice
        if (camera == null) {
            continuation.resumeWithException(IllegalStateException("Camera not opened"))
            return@suspendCoroutine
        }

        try {
            camera.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Timber.d("Capture session configured")
                        captureSession = session
                        continuation.resume(Result.success(session))
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Timber.e("Failed to configure capture session")
                        continuation.resumeWithException(Exception("Failed to configure capture session"))
                    }
                },
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Timber.e(e, "Failed to create capture session")
            continuation.resumeWithException(e)
        }
    }

    /**
     * Create capture request builder
     */
    fun createCaptureRequestBuilder(template: Int): CaptureRequest.Builder? {
        return try {
            cameraDevice?.createCaptureRequest(template)
        } catch (e: CameraAccessException) {
            Timber.e(e, "Failed to create capture request")
            null
        }
    }

    /**
     * Get current camera device
     */
    fun getCurrentCamera(): CameraDevice? = cameraDevice

    /**
     * Get current capture session
     */
    fun getCurrentSession(): CameraCaptureSession? = captureSession

    /**
     * Get current camera characteristics
     */
    fun getCurrentCharacteristics(): CameraCharacteristics? = cameraCharacteristics

    /**
     * Close camera and release resources
     */
    fun closeCamera() {
        try {
            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            stopBackgroundThread()

            currentCameraId = null
            cameraCharacteristics = null

            Timber.d("Camera closed")
        } catch (e: Exception) {
            Timber.e(e, "Error closing camera")
        }
    }
}

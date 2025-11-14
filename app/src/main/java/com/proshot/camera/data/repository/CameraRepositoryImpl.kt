package com.proshot.camera.data.repository

import android.view.Surface
import com.proshot.camera.data.camera.Camera2Manager
import com.proshot.camera.data.camera.ManualCameraController
import com.proshot.camera.data.camera.RawCaptureController
import com.proshot.camera.domain.model.CameraParameter
import com.proshot.camera.domain.model.CameraState
import com.proshot.camera.domain.model.ImageFormat
import com.proshot.camera.domain.repository.CameraRepository
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraRepositoryImpl @Inject constructor(
    private val camera2Manager: Camera2Manager,
    private val manualController: ManualCameraController,
    private val rawCaptureController: RawCaptureController
) : CameraRepository {

    override val cameraState: StateFlow<CameraState> = manualController.cameraState

    private var currentCameraId: String? = null
    private var previewSurface: Surface? = null

    override suspend fun openCamera(cameraId: String): Result<Unit> {
        return try {
            // Open camera
            val result = camera2Manager.openCamera(cameraId)
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Failed to open camera"))
            }

            currentCameraId = cameraId

            // Initialize controllers
            rawCaptureController.initializeImageReaders(cameraId)

            Timber.d("Camera repository: Camera $cameraId opened successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open camera")
            Result.failure(e)
        }
    }

    override suspend fun startPreview(surface: Surface): Result<Unit> {
        return try {
            val cameraId = currentCameraId
            if (cameraId == null) {
                return Result.failure(IllegalStateException("Camera not opened"))
            }

            previewSurface = surface

            // Create capture session with preview surface
            val surfaces = listOfNotNull(
                surface,
                rawCaptureController.getRawSurface(),
                rawCaptureController.getJpegSurface()
            )

            val sessionResult = camera2Manager.createCaptureSession(surfaces)
            if (sessionResult.isFailure) {
                return Result.failure(sessionResult.exceptionOrNull() ?: Exception("Failed to create session"))
            }

            // Initialize manual controller and start preview
            manualController.initialize(cameraId, surface)
            manualController.startPreview()

            Timber.d("Camera repository: Preview started")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start preview")
            Result.failure(e)
        }
    }

    override fun setISO(value: Int) {
        manualController.setISO(value)
    }

    override fun setShutterSpeed(nanoseconds: Long) {
        manualController.setShutterSpeed(nanoseconds)
    }

    override fun setWhiteBalance(kelvin: Int, mode: CameraParameter.WhiteBalance.WBMode) {
        manualController.setWhiteBalance(kelvin, mode)
    }

    override fun setExposureCompensation(evSteps: Int) {
        manualController.setExposureCompensation(evSteps)
    }

    override fun setFocusDistance(distance: Float) {
        manualController.setFocusDistance(distance)
    }

    override fun setFocusMode(mode: CameraParameter.Focus.FocusMode) {
        manualController.setFocusMode(mode)
    }

    override suspend fun captureImage(format: ImageFormat): Result<File?> {
        return try {
            var capturedFile: File? = null

            rawCaptureController.captureImage(format) { file ->
                capturedFile = file
            }

            // Wait a bit for capture to complete (this should be improved with proper callbacks)
            kotlinx.coroutines.delay(1000)

            if (capturedFile != null) {
                Result.success(capturedFile)
            } else {
                Result.failure(Exception("Failed to capture image"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to capture image")
            Result.failure(e)
        }
    }

    override fun closeCamera() {
        manualController.release()
        rawCaptureController.release()
        camera2Manager.closeCamera()
        currentCameraId = null
        previewSurface = null
        Timber.d("Camera repository: Camera closed")
    }
}

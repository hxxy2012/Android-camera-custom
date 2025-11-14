package com.proshot.camera.domain.repository

import android.view.Surface
import com.proshot.camera.domain.model.CameraParameter
import com.proshot.camera.domain.model.CameraState
import com.proshot.camera.domain.model.ImageFormat
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Repository interface for camera operations
 */
interface CameraRepository {

    val cameraState: StateFlow<CameraState>

    suspend fun openCamera(cameraId: String): Result<Unit>

    suspend fun startPreview(surface: Surface): Result<Unit>

    fun setISO(value: Int)

    fun setShutterSpeed(nanoseconds: Long)

    fun setWhiteBalance(kelvin: Int, mode: CameraParameter.WhiteBalance.WBMode)

    fun setExposureCompensation(evSteps: Int)

    fun setFocusDistance(distance: Float)

    fun setFocusMode(mode: CameraParameter.Focus.FocusMode)

    suspend fun captureImage(format: ImageFormat): Result<File?>

    fun closeCamera()
}

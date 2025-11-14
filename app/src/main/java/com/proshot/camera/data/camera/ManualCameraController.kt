package com.proshot.camera.data.camera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.view.Surface
import com.proshot.camera.domain.model.CameraParameter
import com.proshot.camera.domain.model.CameraState
import com.proshot.camera.domain.model.MeteringMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides manual control over camera parameters
 * This is the heart of professional photography - complete manual control
 */
@Singleton
class ManualCameraController @Inject constructor(
    private val camera2Manager: Camera2Manager
) {
    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var previewSurface: Surface? = null

    /**
     * Initialize manual controls for the current camera
     */
    fun initialize(cameraId: String, surface: Surface) {
        previewSurface = surface

        val characteristics = camera2Manager.getCameraCharacteristics(cameraId)
        if (characteristics == null) {
            Timber.e("Failed to get camera characteristics")
            return
        }

        // Get camera capability ranges
        val isoRange = camera2Manager.getIsoRange(cameraId)
        val exposureTimeRange = camera2Manager.getExposureTimeRange(cameraId)
        val focusDistanceRange = camera2Manager.getFocusDistanceRange(cameraId)

        Timber.d("Camera capabilities:")
        Timber.d("  ISO range: $isoRange")
        Timber.d("  Exposure time range: $exposureTimeRange")
        Timber.d("  Focus distance range: $focusDistanceRange")
        Timber.d("  Supports RAW: ${camera2Manager.supportsRaw(cameraId)}")
        Timber.d("  Has manual control: ${camera2Manager.hasManualControl(cameraId)}")

        // Initialize state with default values
        _cameraState.value = CameraState(
            iso = CameraParameter.ISO(100, isoRange),
            shutterSpeed = CameraParameter.ShutterSpeed(
                33_333_333L, // 1/30s default
                exposureTimeRange
            ),
            focus = CameraParameter.Focus(0f, focusDistanceRange)
        )
    }

    /**
     * Start preview with manual control
     */
    fun startPreview() {
        val camera = camera2Manager.getCurrentCamera()
        val session = camera2Manager.getCurrentSession()
        val surface = previewSurface

        if (camera == null || session == null || surface == null) {
            Timber.e("Cannot start preview: camera=$camera, session=$session, surface=$surface")
            return
        }

        try {
            captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                configureManualControls(this)
            }

            session.setRepeatingRequest(
                captureRequestBuilder!!.build(),
                captureCallback,
                null
            )

            Timber.d("Preview started with manual controls")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start preview")
        }
    }

    /**
     * Configure manual controls on a capture request
     */
    private fun configureManualControls(requestBuilder: CaptureRequest.Builder) {
        val state = _cameraState.value

        // Manual control mode
        requestBuilder.set(
            CaptureRequest.CONTROL_MODE,
            CaptureRequest.CONTROL_MODE_OFF
        )

        // Manual ISO (if not auto)
        if (state.iso.value > 0) {
            requestBuilder.set(
                CaptureRequest.SENSOR_SENSITIVITY,
                state.iso.value
            )
        }

        // Manual shutter speed (if not auto)
        if (state.shutterSpeed.value > 0) {
            requestBuilder.set(
                CaptureRequest.SENSOR_EXPOSURE_TIME,
                state.shutterSpeed.value
            )
        }

        // Manual white balance
        if (state.whiteBalance.mode != CameraParameter.WhiteBalance.WBMode.AUTO) {
            requestBuilder.set(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_OFF
            )
            // Note: Camera2 doesn't directly support color temperature
            // This would need RggbChannelVector calculation
        } else {
            requestBuilder.set(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_AUTO
            )
        }

        // Exposure compensation
        requestBuilder.set(
            CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
            state.exposureCompensation.value
        )

        // Focus mode
        when (state.focus.mode) {
            CameraParameter.Focus.FocusMode.AUTO_SINGLE -> {
                requestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_AUTO
                )
            }
            CameraParameter.Focus.FocusMode.AUTO_CONTINUOUS -> {
                requestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            }
            CameraParameter.Focus.FocusMode.MANUAL -> {
                requestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF
                )
                requestBuilder.set(
                    CaptureRequest.LENS_FOCUS_DISTANCE,
                    state.focus.value
                )
            }
        }

        // Disable automatic features for full manual control
        requestBuilder.set(
            CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_OFF
        )

        // Image stabilization (can be enabled/disabled)
        requestBuilder.set(
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
        )
    }

    /**
     * Update ISO value
     */
    fun setISO(iso: Int) {
        _cameraState.value = _cameraState.value.copy(
            iso = CameraParameter.ISO(iso, _cameraState.value.iso.range)
        )
        updatePreview()
        Timber.d("ISO set to: $iso")
    }

    /**
     * Update shutter speed
     */
    fun setShutterSpeed(nanoseconds: Long) {
        _cameraState.value = _cameraState.value.copy(
            shutterSpeed = CameraParameter.ShutterSpeed(
                nanoseconds,
                _cameraState.value.shutterSpeed.range
            )
        )
        updatePreview()
        Timber.d("Shutter speed set to: ${nanoseconds}ns")
    }

    /**
     * Update white balance
     */
    fun setWhiteBalance(kelvin: Int, mode: CameraParameter.WhiteBalance.WBMode) {
        _cameraState.value = _cameraState.value.copy(
            whiteBalance = CameraParameter.WhiteBalance(kelvin, mode)
        )
        updatePreview()
        Timber.d("White balance set to: $kelvin K ($mode)")
    }

    /**
     * Update exposure compensation
     */
    fun setExposureCompensation(evSteps: Int) {
        _cameraState.value = _cameraState.value.copy(
            exposureCompensation = CameraParameter.ExposureCompensation(
                evSteps,
                _cameraState.value.exposureCompensation.range
            )
        )
        updatePreview()
        Timber.d("Exposure compensation set to: ${evSteps / 3.0} EV")
    }

    /**
     * Update focus distance
     */
    fun setFocusDistance(distance: Float) {
        _cameraState.value = _cameraState.value.copy(
            focus = CameraParameter.Focus(
                distance,
                _cameraState.value.focus.range,
                _cameraState.value.focus.mode
            )
        )
        updatePreview()
        Timber.d("Focus distance set to: $distance")
    }

    /**
     * Set focus mode
     */
    fun setFocusMode(mode: CameraParameter.Focus.FocusMode) {
        _cameraState.value = _cameraState.value.copy(
            focus = _cameraState.value.focus.copy(mode = mode)
        )
        updatePreview()
        Timber.d("Focus mode set to: $mode")
    }

    /**
     * Update preview with new settings
     */
    private fun updatePreview() {
        val builder = captureRequestBuilder ?: return
        val session = camera2Manager.getCurrentSession() ?: return

        try {
            configureManualControls(builder)
            session.setRepeatingRequest(builder.build(), captureCallback, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update preview")
        }
    }

    /**
     * Capture callback to receive metadata and update histogram
     */
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            // Extract histogram data if available
            // Note: Histogram statistics require STATISTICS_FACE_DETECT_MODE
            // This is a simplified version
            updateStateFromCaptureResult(result)
        }
    }

    /**
     * Update state from capture result metadata
     */
    private fun updateStateFromCaptureResult(result: CaptureResult) {
        // You can extract real-time camera metadata here
        // For example: actual ISO used, actual exposure time, focus state, etc.

        val actualIso = result.get(CaptureResult.SENSOR_SENSITIVITY)
        val actualExposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)

        // This could be used to show "actual" vs "requested" values
        // Useful for debugging and education
    }

    /**
     * Clean up
     */
    fun release() {
        captureRequestBuilder = null
        previewSurface = null
        Timber.d("Manual camera controller released")
    }
}

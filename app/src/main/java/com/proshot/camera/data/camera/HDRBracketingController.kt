package com.proshot.camera.data.camera

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HDR bracketing controller
 * Captures multiple exposures at different EV values for HDR merge
 */
@Singleton
class HDRBracketingController @Inject constructor(
    private val camera2Manager: Camera2Manager,
    private val rawCaptureController: RawCaptureController
) {

    private val _bracketingState = MutableStateFlow(HDRBracketingState())
    val bracketingState: StateFlow<HDRBracketingState> = _bracketingState.asStateFlow()

    /**
     * Start HDR bracketing sequence
     */
    suspend fun startBracketing(
        numShots: Int = 3,
        evStep: Float = 2.0f,
        baseEV: Int = 0
    ): Result<List<File>> {
        if (_bracketingState.value.isActive) {
            return Result.failure(IllegalStateException("Bracketing already active"))
        }

        _bracketingState.value = HDRBracketingState(
            isActive = true,
            numShots = numShots,
            evStep = evStep,
            currentShot = 0
        )

        val capturedFiles = mutableListOf<File>()

        try {
            // Calculate EV values for bracketing
            val evValues = calculateEVSequence(numShots, evStep, baseEV)

            Timber.d("Starting HDR bracketing: $numShots shots, EV step: $evStep")
            Timber.d("EV sequence: ${evValues.joinToString()}")

            // Capture each exposure
            for ((index, ev) in evValues.withIndex()) {
                _bracketingState.value = _bracketingState.value.copy(
                    currentShot = index + 1,
                    currentEV = ev
                )

                Timber.d("Capturing shot ${index + 1}/$numShots at EV: $ev")

                // Set exposure compensation
                setExposureCompensation(ev)

                // Wait for camera to settle
                delay(200)

                // Capture image
                var capturedFile: File? = null
                rawCaptureController.captureImage(
                    format = com.proshot.camera.domain.model.ImageFormat.JPEG_ONLY
                ) { file ->
                    capturedFile = file
                }

                // Wait for capture to complete
                delay(500)

                if (capturedFile != null) {
                    capturedFiles.add(capturedFile!!)
                    Timber.d("Shot ${index + 1} captured: ${capturedFile!!.absolutePath}")
                } else {
                    Timber.e("Failed to capture shot ${index + 1}")
                    return Result.failure(Exception("Failed to capture shot ${index + 1}"))
                }

                // Small delay between shots
                if (index < evValues.size - 1) {
                    delay(300)
                }
            }

            _bracketingState.value = HDRBracketingState(
                isActive = false,
                numShots = numShots,
                evStep = evStep,
                currentShot = numShots,
                capturedFiles = capturedFiles.map { it.absolutePath }
            )

            Timber.d("HDR bracketing completed: ${capturedFiles.size} images captured")

            return Result.success(capturedFiles)

        } catch (e: Exception) {
            Timber.e(e, "HDR bracketing failed")
            _bracketingState.value = HDRBracketingState(isActive = false)
            return Result.failure(e)
        }
    }

    /**
     * Calculate EV sequence for bracketing
     * For 3 shots: [-2, 0, +2]
     * For 5 shots: [-4, -2, 0, +2, +4]
     * For 7 shots: [-6, -4, -2, 0, +2, +4, +6]
     */
    private fun calculateEVSequence(numShots: Int, evStep: Float, baseEV: Int): List<Int> {
        val evValues = mutableListOf<Int>()

        when (numShots) {
            3 -> {
                evValues.add(baseEV - (evStep * 3).toInt()) // Underexposed
                evValues.add(baseEV)                        // Normal
                evValues.add(baseEV + (evStep * 3).toInt()) // Overexposed
            }
            5 -> {
                evValues.add(baseEV - (evStep * 6).toInt())
                evValues.add(baseEV - (evStep * 3).toInt())
                evValues.add(baseEV)
                evValues.add(baseEV + (evStep * 3).toInt())
                evValues.add(baseEV + (evStep * 6).toInt())
            }
            7 -> {
                evValues.add(baseEV - (evStep * 9).toInt())
                evValues.add(baseEV - (evStep * 6).toInt())
                evValues.add(baseEV - (evStep * 3).toInt())
                evValues.add(baseEV)
                evValues.add(baseEV + (evStep * 3).toInt())
                evValues.add(baseEV + (evStep * 6).toInt())
                evValues.add(baseEV + (evStep * 9).toInt())
            }
            else -> {
                // Default to 3-shot
                evValues.add(baseEV - (evStep * 3).toInt())
                evValues.add(baseEV)
                evValues.add(baseEV + (evStep * 3).toInt())
            }
        }

        return evValues
    }

    /**
     * Set exposure compensation
     */
    private fun setExposureCompensation(ev: Int) {
        // This should be integrated with ManualCameraController
        // For now, this is a placeholder
        Timber.d("Setting EV compensation: $ev")
    }

    /**
     * Cancel active bracketing
     */
    fun cancel() {
        _bracketingState.value = HDRBracketingState(isActive = false)
        Timber.d("HDR bracketing canceled")
    }
}

/**
 * HDR bracketing state
 */
data class HDRBracketingState(
    val isActive: Boolean = false,
    val numShots: Int = 3,
    val evStep: Float = 2.0f,
    val currentShot: Int = 0,
    val currentEV: Int = 0,
    val capturedFiles: List<String> = emptyList()
) {
    val progress: Float
        get() = if (numShots > 0) {
            currentShot.toFloat() / numShots
        } else {
            0f
        }

    val isComplete: Boolean
        get() = currentShot >= numShots && !isActive
}

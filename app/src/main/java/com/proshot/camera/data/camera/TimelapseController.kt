package com.proshot.camera.data.camera

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Timelapse controller for interval photography
 * Captures images at regular intervals for timelapse video creation
 */
@Singleton
class TimelapseController @Inject constructor(
    private val rawCaptureController: RawCaptureController
) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var captureJob: Job? = null

    private val _timelapseState = MutableStateFlow(TimelapseState())
    val timelapseState: StateFlow<TimelapseState> = _timelapseState.asStateFlow()

    /**
     * Start timelapse capture sequence
     */
    fun startTimelapse(
        intervalSeconds: Int = 5,
        totalShots: Int = 100,
        outputFps: Int = 30
    ) {
        if (_timelapseState.value.isActive) {
            Timber.w("Timelapse already active")
            return
        }

        val startTime = SystemClock.elapsedRealtime()

        _timelapseState.value = TimelapseState(
            isActive = true,
            intervalSeconds = intervalSeconds,
            totalShots = totalShots,
            outputFps = outputFps,
            startTime = startTime
        )

        Timber.d("Starting timelapse: interval=${intervalSeconds}s, total=$totalShots shots, fps=$outputFps")

        captureJob = scope.launch {
            val capturedFiles = mutableListOf<String>()
            var shotNumber = 0

            while (isActive && shotNumber < totalShots) {
                // Calculate next capture time
                val nextCaptureTime = startTime + (shotNumber * intervalSeconds * 1000L)
                val currentTime = SystemClock.elapsedRealtime()
                val delayMs = nextCaptureTime - currentTime

                if (delayMs > 0) {
                    delay(delayMs)
                }

                // Capture image
                Timber.d("Capturing timelapse shot ${shotNumber + 1}/$totalShots")

                var capturedFile: File? = null
                rawCaptureController.captureImage(
                    format = com.proshot.camera.domain.model.ImageFormat.JPEG_ONLY
                ) { file ->
                    capturedFile = file
                }

                // Wait for capture
                delay(1000)

                if (capturedFile != null) {
                    capturedFiles.add(capturedFile!!.absolutePath)
                    shotNumber++

                    _timelapseState.value = _timelapseState.value.copy(
                        currentShot = shotNumber,
                        capturedFiles = capturedFiles.toList(),
                        elapsedTime = SystemClock.elapsedRealtime() - startTime
                    )

                    Timber.d("Timelapse shot $shotNumber captured")
                } else {
                    Timber.e("Failed to capture timelapse shot $shotNumber")
                }
            }

            // Timelapse complete
            _timelapseState.value = _timelapseState.value.copy(
                isActive = false
            )

            Timber.d("Timelapse completed: $shotNumber shots captured")
        }
    }

    /**
     * Stop timelapse
     */
    fun stopTimelapse() {
        captureJob?.cancel()
        captureJob = null

        val currentState = _timelapseState.value
        _timelapseState.value = currentState.copy(
            isActive = false
        )

        Timber.d("Timelapse stopped: ${currentState.currentShot} shots captured")
    }

    /**
     * Calculate estimated video duration
     */
    fun getEstimatedVideoDuration(): Float {
        val state = _timelapseState.value
        if (state.totalShots == 0 || state.outputFps == 0) return 0f

        return state.totalShots.toFloat() / state.outputFps
    }

    /**
     * Calculate estimated capture time
     */
    fun getEstimatedCaptureTime(): Long {
        val state = _timelapseState.value
        return state.intervalSeconds * state.totalShots * 1000L
    }

    /**
     * Release resources
     */
    fun release() {
        stopTimelapse()
    }
}

/**
 * Timelapse state
 */
data class TimelapseState(
    val isActive: Boolean = false,
    val intervalSeconds: Int = 5,
    val totalShots: Int = 100,
    val currentShot: Int = 0,
    val outputFps: Int = 30,
    val capturedFiles: List<String> = emptyList(),
    val startTime: Long = 0L,
    val elapsedTime: Long = 0L
) {
    /**
     * Progress (0.0 - 1.0)
     */
    val progress: Float
        get() = if (totalShots > 0) {
            currentShot.toFloat() / totalShots
        } else {
            0f
        }

    /**
     * Estimated remaining time in milliseconds
     */
    val remainingTime: Long
        get() {
            val remainingShots = totalShots - currentShot
            return remainingShots * intervalSeconds * 1000L
        }

    /**
     * Formatted remaining time
     */
    val formattedRemainingTime: String
        get() {
            val totalSeconds = remainingTime / 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600

            return if (hours > 0) {
                String.format("%dh %dm", hours, minutes)
            } else if (minutes > 0) {
                String.format("%dm %ds", minutes, seconds)
            } else {
                String.format("%ds", seconds)
            }
        }

    /**
     * Estimated video duration in seconds
     */
    val videoDuration: Float
        get() = if (outputFps > 0) {
            totalShots.toFloat() / outputFps
        } else {
            0f
        }
}

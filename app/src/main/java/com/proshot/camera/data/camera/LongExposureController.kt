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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for long exposure (Bulb mode) photography
 * Supports exposures from 1 second to 30 minutes
 */
@Singleton
class LongExposureController @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null

    private val _exposureState = MutableStateFlow(LongExposureState())
    val exposureState: StateFlow<LongExposureState> = _exposureState.asStateFlow()

    /**
     * Start long exposure
     */
    fun startExposure(maxDuration: Long = 30 * 60 * 1000L) {
        if (_exposureState.value.isActive) {
            Timber.w("Long exposure already active")
            return
        }

        val startTime = SystemClock.elapsedRealtime()

        _exposureState.value = LongExposureState(
            isActive = true,
            startTime = startTime,
            maxDuration = maxDuration
        )

        // Start timer
        timerJob = scope.launch {
            while (isActive && _exposureState.value.isActive) {
                val elapsed = SystemClock.elapsedRealtime() - startTime

                _exposureState.value = _exposureState.value.copy(
                    elapsedTime = elapsed
                )

                // Auto-stop if max duration reached
                if (elapsed >= maxDuration) {
                    stopExposure()
                    break
                }

                delay(100) // Update every 100ms
            }
        }

        Timber.d("Long exposure started, max duration: ${maxDuration}ms")
    }

    /**
     * Stop long exposure
     */
    fun stopExposure(): Long {
        val elapsedTime = _exposureState.value.elapsedTime

        timerJob?.cancel()
        timerJob = null

        _exposureState.value = LongExposureState(
            isActive = false,
            startTime = 0L,
            elapsedTime = 0L
        )

        Timber.d("Long exposure stopped, duration: ${elapsedTime}ms")

        return elapsedTime
    }

    /**
     * Get current exposure time in milliseconds
     */
    fun getCurrentExposureTime(): Long {
        return if (_exposureState.value.isActive) {
            _exposureState.value.elapsedTime
        } else {
            0L
        }
    }

    /**
     * Check if exposure is active
     */
    fun isActive(): Boolean = _exposureState.value.isActive

    /**
     * Clean up
     */
    fun release() {
        timerJob?.cancel()
        timerJob = null
        _exposureState.value = LongExposureState()
    }
}

/**
 * Long exposure state
 */
data class LongExposureState(
    val isActive: Boolean = false,
    val startTime: Long = 0L,
    val elapsedTime: Long = 0L,
    val maxDuration: Long = 30 * 60 * 1000L  // 30 minutes default
) {
    /**
     * Formatted time string (MM:SS or HH:MM:SS)
     */
    val formattedTime: String
        get() {
            val totalSeconds = elapsedTime / 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600

            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    /**
     * Progress (0.0 - 1.0)
     */
    val progress: Float
        get() = if (maxDuration > 0) {
            (elapsedTime.toFloat() / maxDuration).coerceIn(0f, 1f)
        } else {
            0f
        }
}

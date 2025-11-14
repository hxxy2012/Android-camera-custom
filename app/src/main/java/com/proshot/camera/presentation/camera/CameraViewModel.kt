package com.proshot.camera.presentation.camera

import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proshot.camera.domain.model.CameraParameter
import com.proshot.camera.domain.model.CameraState
import com.proshot.camera.domain.model.ImageFormat
import com.proshot.camera.domain.repository.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraRepository: CameraRepository
) : ViewModel() {

    val cameraState: StateFlow<CameraState> = cameraRepository.cameraState

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _capturedImage = MutableStateFlow<File?>(null)
    val capturedImage: StateFlow<File?> = _capturedImage.asStateFlow()

    /**
     * Open camera and start preview
     */
    fun openCamera(cameraId: String, surface: Surface) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val openResult = cameraRepository.openCamera(cameraId)
            if (openResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = openResult.exceptionOrNull()?.message ?: "Failed to open camera"
                )
                return@launch
            }

            val previewResult = cameraRepository.startPreview(surface)
            if (previewResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = previewResult.exceptionOrNull()?.message ?: "Failed to start preview"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isCameraReady = true
            )

            Timber.d("Camera opened and preview started")
        }
    }

    /**
     * Adjust ISO
     */
    fun adjustISO(iso: Int) {
        cameraRepository.setISO(iso)
    }

    /**
     * Adjust shutter speed
     */
    fun adjustShutterSpeed(nanoseconds: Long) {
        cameraRepository.setShutterSpeed(nanoseconds)
    }

    /**
     * Adjust white balance
     */
    fun adjustWhiteBalance(kelvin: Int, mode: CameraParameter.WhiteBalance.WBMode = CameraParameter.WhiteBalance.WBMode.CUSTOM) {
        cameraRepository.setWhiteBalance(kelvin, mode)
    }

    /**
     * Set white balance mode
     */
    fun setWhiteBalanceMode(mode: CameraParameter.WhiteBalance.WBMode) {
        val currentWB = cameraState.value.whiteBalance
        cameraRepository.setWhiteBalance(currentWB.value, mode)
    }

    /**
     * Adjust exposure compensation
     */
    fun adjustExposureCompensation(evSteps: Int) {
        cameraRepository.setExposureCompensation(evSteps)
    }

    /**
     * Set focus mode
     */
    fun setFocusMode(mode: CameraParameter.Focus.FocusMode) {
        cameraRepository.setFocusMode(mode)
    }

    /**
     * Adjust focus distance (manual focus)
     */
    fun adjustFocusDistance(distance: Float) {
        cameraRepository.setFocusDistance(distance)
    }

    /**
     * Capture image
     */
    fun captureImage(format: ImageFormat = ImageFormat.JPEG_ONLY) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCapturing = true)

            val result = cameraRepository.captureImage(format)

            if (result.isSuccess) {
                _capturedImage.value = result.getOrNull()
                _uiState.value = _uiState.value.copy(
                    isCapturing = false,
                    lastCapturedFile = result.getOrNull()?.absolutePath
                )
                Timber.d("Image captured: ${result.getOrNull()?.absolutePath}")
            } else {
                _uiState.value = _uiState.value.copy(
                    isCapturing = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to capture image"
                )
                Timber.e("Failed to capture image: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Toggle grid type
     */
    fun toggleGrid() {
        val currentGrid = _uiState.value.currentGrid
        val nextGrid = when (currentGrid) {
            GridType.NONE -> GridType.RULE_OF_THIRDS
            GridType.RULE_OF_THIRDS -> GridType.GOLDEN_RATIO
            GridType.GOLDEN_RATIO -> GridType.DIAGONAL
            GridType.DIAGONAL -> GridType.NONE
        }
        _uiState.value = _uiState.value.copy(currentGrid = nextGrid)
    }

    /**
     * Toggle histogram visibility
     */
    fun toggleHistogram() {
        _uiState.value = _uiState.value.copy(
            showHistogram = !_uiState.value.showHistogram
        )
    }

    /**
     * Toggle level (inclinometer)
     */
    fun toggleLevel() {
        _uiState.value = _uiState.value.copy(
            showLevel = !_uiState.value.showLevel
        )
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        cameraRepository.closeCamera()
    }
}

/**
 * UI state for camera screen
 */
data class CameraUiState(
    val isLoading: Boolean = false,
    val isCameraReady: Boolean = false,
    val isCapturing: Boolean = false,
    val error: String? = null,
    val currentGrid: GridType = GridType.NONE,
    val showHistogram: Boolean = false,
    val showLevel: Boolean = false,
    val lastCapturedFile: String? = null
)

enum class GridType {
    NONE,
    RULE_OF_THIRDS,
    GOLDEN_RATIO,
    DIAGONAL
}

package com.proshot.camera.domain.model

/**
 * Represents the current state of the camera
 */
data class CameraState(
    val iso: CameraParameter.ISO = CameraParameter.ISO(0),
    val shutterSpeed: CameraParameter.ShutterSpeed = CameraParameter.ShutterSpeed(0),
    val whiteBalance: CameraParameter.WhiteBalance = CameraParameter.WhiteBalance(5500),
    val exposureCompensation: CameraParameter.ExposureCompensation = CameraParameter.ExposureCompensation(0),
    val focus: CameraParameter.Focus = CameraParameter.Focus(0f),
    val isCapturing: Boolean = false,
    val error: String? = null,
    val histogram: IntArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CameraState

        if (iso != other.iso) return false
        if (shutterSpeed != other.shutterSpeed) return false
        if (whiteBalance != other.whiteBalance) return false
        if (exposureCompensation != other.exposureCompensation) return false
        if (focus != other.focus) return false
        if (isCapturing != other.isCapturing) return false
        if (error != other.error) return false
        if (histogram != null) {
            if (other.histogram == null) return false
            if (!histogram.contentEquals(other.histogram)) return false
        } else if (other.histogram != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = iso.hashCode()
        result = 31 * result + shutterSpeed.hashCode()
        result = 31 * result + whiteBalance.hashCode()
        result = 31 * result + exposureCompensation.hashCode()
        result = 31 * result + focus.hashCode()
        result = 31 * result + isCapturing.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + (histogram?.contentHashCode() ?: 0)
        return result
    }
}

package com.proshot.camera.domain.model

/**
 * Advanced shooting modes for professional photography
 */
sealed class AdvancedShootingMode {

    /**
     * Long exposure mode (Bulb mode)
     * For exposures longer than 30 seconds
     */
    data class LongExposure(
        val exposureTime: Long = 0L,  // Milliseconds (0 = manual control)
        val isActive: Boolean = false,
        val elapsedTime: Long = 0L
    ) : AdvancedShootingMode()

    /**
     * HDR bracketing mode
     * Captures multiple exposures at different EV values
     */
    data class HDRBracketing(
        val numShots: Int = 3,              // 3, 5, or 7 shots
        val evStep: Float = 2.0f,           // EV difference (1.0, 1.5, 2.0, 2.5, 3.0)
        val currentShot: Int = 0,           // Current shot in sequence
        val capturedImages: List<String> = emptyList()
    ) : AdvancedShootingMode()

    /**
     * Timelapse mode
     * Captures images at regular intervals
     */
    data class Timelapse(
        val intervalSeconds: Int = 5,       // Time between shots
        val totalShots: Int = 100,          // Total number of shots (0 = unlimited)
        val currentShot: Int = 0,           // Current shot number
        val isActive: Boolean = false,
        val outputFps: Int = 30,            // Output video framerate
        val smoothExposure: Boolean = true  // Smooth exposure transitions (holy grail)
    ) : AdvancedShootingMode()

    /**
     * Focus stacking mode
     * Captures multiple shots at different focus distances
     */
    data class FocusStacking(
        val numShots: Int = 10,
        val startDistance: Float = 0f,
        val endDistance: Float = 1f,
        val currentShot: Int = 0,
        val capturedImages: List<String> = emptyList()
    ) : AdvancedShootingMode()

    /**
     * Exposure bracketing (non-HDR)
     * Simple bracketing without merge
     */
    data class ExposureBracketing(
        val shots: List<Float> = listOf(-2f, 0f, +2f),  // EV values
        val currentShot: Int = 0
    ) : AdvancedShootingMode()

    /**
     * Interval shooting (different from timelapse)
     * Continuous shooting with intervals
     */
    data class IntervalShooting(
        val intervalMs: Long = 1000L,
        val isActive: Boolean = false
    ) : AdvancedShootingMode()
}

/**
 * Bulb mode controller state
 */
data class BulbModeState(
    val isActive: Boolean = false,
    val startTime: Long = 0L,
    val elapsedTime: Long = 0L,
    val maxDuration: Long = 30 * 60 * 1000L  // 30 minutes max
) {
    val formattedTime: String
        get() {
            val seconds = (elapsedTime / 1000) % 60
            val minutes = (elapsedTime / 1000 / 60) % 60
            val hours = elapsedTime / 1000 / 60 / 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
}

/**
 * HDR capture settings
 */
data class HDRSettings(
    val mode: HDRMode = HDRMode.AUTO,
    val numExposures: Int = 3,
    val evStep: Float = 2.0f,
    val toneMappingStyle: ToneMappingStyle = ToneMappingStyle.NATURAL,
    val ghostingReduction: Boolean = true
)

enum class HDRMode {
    AUTO,      // Auto 3-shot HDR
    MANUAL_3,  // Manual 3-shot
    MANUAL_5,  // Manual 5-shot
    MANUAL_7   // Manual 7-shot
}

enum class ToneMappingStyle {
    NATURAL,   // Natural looking
    ENHANCED,  // More dramatic
    ARTISTIC   // Very stylized
}

/**
 * Timelapse settings
 */
data class TimelapseSettings(
    val intervalSeconds: Int = 5,
    val totalDuration: Int = 0,  // 0 = unlimited
    val outputFps: Int = 30,
    val resolution: TimelapseResolution = TimelapseResolution.UHD_4K,
    val smoothExposure: Boolean = false,  // Holy grail mode
    val motionBlur: Boolean = false
)

enum class TimelapseResolution {
    HD_1080p,
    UHD_4K,
    FULL_RESOLUTION
}

/**
 * Long exposure settings
 */
data class LongExposureSettings(
    val mode: LongExposureMode = LongExposureMode.BULB,
    val noiseReduction: Boolean = true,
    val showPreview: Boolean = true,
    val lockExposure: Boolean = true
)

enum class LongExposureMode {
    BULB,           // Manual control (press and hold)
    TIMED,          // Set specific duration
    LIGHT_PAINTING, // Optimized for light trails
    STAR_TRAILS,    // Optimized for star photography
    FLOWING_WATER   // Optimized for water effects
}

package com.proshot.camera.data.ai

import com.proshot.camera.domain.model.CameraParameter
import com.proshot.camera.domain.model.SceneType
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * AI Parameter Recommender
 * Recommends optimal camera settings based on scene and conditions
 */
@Singleton
class ParameterRecommender @Inject constructor() {

    /**
     * Recommend camera parameters for detected scene
     */
    fun recommendParameters(
        scene: SceneType,
        ambientLight: Float,  // 0.0 (dark) - 1.0 (bright)
        isHandheld: Boolean = true,
        focalLength: Float = 4.0f  // in mm (typical phone camera)
    ): ParameterRecommendation {

        val settings = scene.recommendedSettings

        // Calculate recommended ISO
        val iso = calculateOptimalISO(ambientLight, scene)

        // Calculate recommended shutter speed
        val shutterSpeed = calculateOptimalShutterSpeed(
            scene,
            iso,
            ambientLight,
            isHandheld,
            focalLength
        )

        // Exposure compensation based on scene
        val exposureComp = calculateExposureCompensation(scene, ambientLight)

        // Focus mode recommendation
        val focusMode = recommendFocusMode(scene)

        // White balance recommendation
        val whiteBalance = recommendWhiteBalance(scene, ambientLight)

        // Additional tips
        val tips = generateTips(scene, iso, shutterSpeed, isHandheld)

        return ParameterRecommendation(
            scene = scene,
            iso = iso,
            shutterSpeed = shutterSpeed,
            exposureCompensation = exposureComp,
            focusMode = focusMode,
            whiteBalance = whiteBalance,
            useFlash = shouldUseFlash(ambientLight, scene),
            useTripod = shouldUseTripod(shutterSpeed, isHandheld),
            tips = tips
        )
    }

    /**
     * Calculate optimal ISO based on light conditions
     */
    private fun calculateOptimalISO(ambientLight: Float, scene: SceneType): Int {
        val baseISO = when {
            ambientLight > 0.8f -> 100      // Bright daylight
            ambientLight > 0.6f -> 200      // Overcast
            ambientLight > 0.4f -> 400      // Shade
            ambientLight > 0.2f -> 800      // Indoor/evening
            ambientLight > 0.1f -> 1600     // Dim
            else -> 3200                     // Very dark
        }

        // Scene-specific adjustments
        val sceneMultiplier = when (scene) {
            SceneType.SPORTS, SceneType.ANIMAL -> 2.0  // Need faster shutter
            SceneType.LANDSCAPE, SceneType.ARCHITECTURE -> 0.5  // Can use slower shutter
            SceneType.NIGHT_SKY -> 4.0  // Very high ISO for stars
            else -> 1.0
        }

        return (baseISO * sceneMultiplier).toInt().coerceIn(100, 6400)
    }

    /**
     * Calculate optimal shutter speed
     */
    private fun calculateOptimalShutterSpeed(
        scene: SceneType,
        iso: Int,
        ambientLight: Float,
        isHandheld: Boolean,
        focalLength: Float
    ): Long {
        // Base shutter speed from scene type
        val baseShutterSpeed = when (scene) {
            SceneType.SPORTS, SceneType.ANIMAL -> 1_000_000L       // 1/1000s
            SceneType.PORTRAIT, SceneType.GROUP -> 4_000_000L      // 1/250s
            SceneType.LANDSCAPE, SceneType.ARCHITECTURE -> 30_000_000L  // 1/30s
            SceneType.NIGHT_SKY -> 20_000_000_000L                 // 20s
            SceneType.MOTION -> 60_000_000L                        // 1/15s (blur)
            SceneType.FIREWORKS -> 4_000_000_000L                  // 4s
            else -> 8_000_000L  // 1/125s default
        }

        // Hand-held rule: shutter speed >= 1/focal_length
        // For phones, typically 1/60s minimum for hand-held
        val minHandheldShutter = if (isHandheld) {
            val rule = (1000.0 / (focalLength * 1.5)).toInt()  // Crop factor 1.5
            (1_000_000_000L / rule.coerceAtLeast(60))
        } else {
            0L  // No limit with tripod
        }

        // Adjust for ambient light
        val lightMultiplier = when {
            ambientLight > 0.8f -> 0.5   // Bright, use faster shutter
            ambientLight > 0.4f -> 1.0
            ambientLight > 0.2f -> 2.0
            else -> 4.0                  // Dark, need slower shutter
        }

        val recommendedShutter = (baseShutterSpeed * lightMultiplier).roundToLong()

        return if (isHandheld && recommendedShutter > minHandheldShutter) {
            recommendedShutter
        } else if (isHandheld) {
            minHandheldShutter  // Clamp to hand-held limit
        } else {
            recommendedShutter
        }
    }

    /**
     * Calculate exposure compensation
     */
    private fun calculateExposureCompensation(scene: SceneType, ambientLight: Float): Int {
        return when (scene) {
            SceneType.BEACH, SceneType.BACKLIGHT -> 6  // +2 EV
            SceneType.SUNSET -> -3  // -1 EV (slight underexposure for color)
            SceneType.NIGHT_SCENE -> -6  // -2 EV (preserve lights)
            SceneType.DOCUMENT -> 3  // +1 EV (bright and clear)
            else -> 0  // No compensation
        }
    }

    /**
     * Recommend focus mode
     */
    private fun recommendFocusMode(scene: SceneType): CameraParameter.Focus.FocusMode {
        return when (scene) {
            SceneType.SPORTS,
            SceneType.ANIMAL,
            SceneType.MOTION -> CameraParameter.Focus.FocusMode.AUTO_CONTINUOUS

            SceneType.NIGHT_SKY,
            SceneType.MACRO,
            SceneType.LANDSCAPE -> CameraParameter.Focus.FocusMode.MANUAL

            else -> CameraParameter.Focus.FocusMode.AUTO_SINGLE
        }
    }

    /**
     * Recommend white balance
     */
    private fun recommendWhiteBalance(scene: SceneType, ambientLight: Float): WhiteBalanceRecommendation {
        val (mode, kelvin) = when (scene) {
            SceneType.SUNSET -> CameraParameter.WhiteBalance.WBMode.DAYLIGHT to 5500
            SceneType.BEACH -> CameraParameter.WhiteBalance.WBMode.DAYLIGHT to 5800
            SceneType.NIGHT_SKY -> CameraParameter.WhiteBalance.WBMode.CUSTOM to 3800
            SceneType.NIGHT_SCENE -> CameraParameter.WhiteBalance.WBMode.TUNGSTEN to 3200
            SceneType.FOOD -> CameraParameter.WhiteBalance.WBMode.CUSTOM to 4500
            else -> CameraParameter.WhiteBalance.WBMode.AUTO to 5500
        }

        return WhiteBalanceRecommendation(mode, kelvin)
    }

    /**
     * Determine if flash should be used
     */
    private fun shouldUseFlash(ambientLight: Float, scene: SceneType): Boolean {
        return when (scene) {
            SceneType.PORTRAIT,
            SceneType.GROUP,
            SceneType.FOOD -> ambientLight < 0.3f

            // Never use flash for these scenes
            SceneType.NIGHT_SKY,
            SceneType.SUNSET,
            SceneType.LANDSCAPE,
            SceneType.NIGHT_SCENE -> false

            else -> ambientLight < 0.15f
        }
    }

    /**
     * Determine if tripod is recommended
     */
    private fun shouldUseTripod(shutterSpeed: Long, isHandheld: Boolean): Boolean {
        // Recommend tripod if shutter speed > 1/30s
        return shutterSpeed > 30_000_000L
    }

    /**
     * Generate shooting tips
     */
    private fun generateTips(
        scene: SceneType,
        iso: Int,
        shutterSpeed: Long,
        isHandheld: Boolean
    ): List<String> {
        val tips = mutableListOf<String>()

        // Scene-specific tips
        tips.add(scene.recommendedSettings.tips)

        // ISO tips
        when {
            iso > 1600 -> tips.add("High ISO may introduce noise - consider using tripod")
            iso < 200 -> tips.add("Low ISO ensures maximum image quality")
        }

        // Shutter speed tips
        val shutterSeconds = shutterSpeed / 1_000_000_000.0
        when {
            shutterSeconds > 1.0 && isHandheld ->
                tips.add("Slow shutter speed - use tripod or brace camera")
            shutterSeconds > 0.5 && isHandheld ->
                tips.add("Shutter speed is slow - hold very steady")
            shutterSeconds < 0.001 ->
                tips.add("Fast shutter freezes motion perfectly")
        }

        // General tips
        if (isHandheld && shutterSpeed > 30_000_000L) {
            tips.add("Enable image stabilization if available")
        }

        return tips.take(3)  // Limit to 3 tips
    }

    /**
     * Calculate safe hand-held shutter speed
     * Rule: 1 / (focal_length × crop_factor)
     */
    fun calculateSafeHandheldShutter(focalLength: Float, cropFactor: Float = 1.5f): Long {
        val denominator = (focalLength * cropFactor).toInt().coerceAtLeast(30)
        return 1_000_000_000L / denominator
    }

    /**
     * Calculate depth of field
     */
    fun calculateDepthOfField(
        aperture: Float,
        focalLength: Float,
        distance: Float
    ): DepthOfFieldInfo {
        // Simplified DOF calculation for phone cameras
        val hyperfocalDistance = (focalLength * focalLength) / (aperture * 0.030f)  // CoC = 0.030mm

        val nearLimit = (distance * hyperfocalDistance) / (hyperfocalDistance + distance)
        val farLimit = (distance * hyperfocalDistance) / (hyperfocalDistance - distance)

        return DepthOfFieldInfo(
            nearLimit = nearLimit,
            farLimit = if (farLimit > 0 && farLimit < 1000f) farLimit else Float.POSITIVE_INFINITY,
            hyperfocalDistance = hyperfocalDistance,
            totalDOF = if (farLimit > 0 && farLimit < 1000f) farLimit - nearLimit else Float.POSITIVE_INFINITY
        )
    }

    /**
     * Calculate exposure value (EV)
     */
    fun calculateEV(iso: Int, shutterSpeed: Long, aperture: Float): Float {
        val shutterSeconds = shutterSpeed / 1_000_000_000.0
        return (log2(aperture * aperture / shutterSeconds) - log2(iso / 100.0)).toFloat()
    }
}

/**
 * Parameter recommendation result
 */
data class ParameterRecommendation(
    val scene: SceneType,
    val iso: Int,
    val shutterSpeed: Long,  // nanoseconds
    val exposureCompensation: Int,  // in 1/3 EV steps
    val focusMode: CameraParameter.Focus.FocusMode,
    val whiteBalance: WhiteBalanceRecommendation,
    val useFlash: Boolean,
    val useTripod: Boolean,
    val tips: List<String>
) {
    val shutterSpeedDisplay: String
        get() {
            val seconds = shutterSpeed / 1_000_000_000.0
            return when {
                seconds >= 1.0 -> String.format("%.1fs", seconds)
                else -> "1/${(1.0 / seconds).toInt()}"
            }
        }

    val evDisplay: String
        get() {
            val ev = exposureCompensation / 3.0
            return when {
                ev > 0 -> "+%.1f EV".format(ev)
                ev < 0 -> "%.1f EV".format(ev)
                else -> "0.0 EV"
            }
        }
}

/**
 * White balance recommendation
 */
data class WhiteBalanceRecommendation(
    val mode: CameraParameter.WhiteBalance.WBMode,
    val kelvin: Int
)

/**
 * Depth of field information
 */
data class DepthOfFieldInfo(
    val nearLimit: Float,      // meters
    val farLimit: Float,       // meters
    val hyperfocalDistance: Float,  // meters
    val totalDOF: Float        // meters
)

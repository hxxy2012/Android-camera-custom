package com.proshot.camera.domain.model

/**
 * Camera settings and shooting modes
 */
data class CameraSettings(
    val shootingMode: ShootingMode = ShootingMode.MANUAL,
    val imageFormat: ImageFormat = ImageFormat.JPEG_ONLY,
    val jpegQuality: Int = 95,
    val gridType: GridType = GridType.NONE,
    val showHistogram: Boolean = false,
    val showLevel: Boolean = false,
    val meteringMode: MeteringMode = MeteringMode.MATRIX,
    val saveLocation: String = "",
    val gpsTagging: Boolean = false
)

enum class ShootingMode {
    MANUAL,
    PRO,
    NIGHT,
    HDR,
    PANORAMA,
    LONG_EXPOSURE,
    TIMELAPSE,
    VIDEO
}

enum class ImageFormat {
    JPEG_ONLY,
    RAW_ONLY,
    RAW_JPEG
}

enum class GridType {
    NONE,
    RULE_OF_THIRDS,
    GOLDEN_RATIO,
    DIAGONAL,
    SQUARE
}

enum class MeteringMode {
    MATRIX,    // Evaluative metering
    CENTER,    // Center-weighted
    SPOT       // Spot metering
}

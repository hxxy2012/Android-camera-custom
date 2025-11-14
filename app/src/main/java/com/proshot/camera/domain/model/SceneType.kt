package com.proshot.camera.domain.model

/**
 * AI-detected scene types
 * Using machine learning to automatically detect shooting scenarios
 */
enum class SceneType(
    val displayName: String,
    val description: String,
    val recommendedSettings: SceneSettings
) {
    // People & Portraits
    PORTRAIT(
        displayName = "Portrait",
        description = "Single person or people",
        recommendedSettings = SceneSettings(
            iso = 100..400,
            shutterSpeed = 1000000L..8000000L,  // 1/1000 - 1/125
            aperture = "Wide open (f/1.8 - f/2.8)",
            focusMode = "AF-S (Face/Eye detection)",
            tips = "Use wide aperture for background blur"
        )
    ),

    GROUP(
        displayName = "Group Photo",
        description = "Multiple people",
        recommendedSettings = SceneSettings(
            iso = 100..800,
            shutterSpeed = 2000000L..8000000L,
            aperture = "f/4 - f/8 (deeper DoF)",
            focusMode = "AF-S (Center group)",
            tips = "Use smaller aperture to keep everyone sharp"
        )
    ),

    SELFIE(
        displayName = "Selfie",
        description = "Self-portrait",
        recommendedSettings = SceneSettings(
            iso = 100..800,
            shutterSpeed = 2000000L..16000000L,
            aperture = "f/2.0 - f/2.8",
            focusMode = "AF-S (Face detection)",
            tips = "Enable beauty mode, use front camera"
        )
    ),

    // Landscapes & Nature
    LANDSCAPE(
        displayName = "Landscape",
        description = "Scenic views, mountains, fields",
        recommendedSettings = SceneSettings(
            iso = 100..400,
            shutterSpeed = 4000000L..125000000L,  // 1/250 - 1/8
            aperture = "f/8 - f/16 (deep DoF)",
            focusMode = "AF-S or MF (hyperfocal)",
            tips = "Use tripod for sharpness, enable level indicator"
        )
    ),

    SUNSET(
        displayName = "Sunset/Sunrise",
        description = "Golden hour scenes",
        recommendedSettings = SceneSettings(
            iso = 100..400,
            shutterSpeed = 8000000L..125000000L,
            aperture = "f/8 - f/11",
            focusMode = "AF-S",
            tips = "Use HDR or exposure bracketing, underexpose slightly"
        )
    ),

    NIGHT_SKY(
        displayName = "Night Sky",
        description = "Stars, Milky Way",
        recommendedSettings = SceneSettings(
            iso = 1600..6400,
            shutterSpeed = 15_000_000_000L..30_000_000_000L,  // 15-30s
            aperture = "f/1.8 - f/2.8 (wide open)",
            focusMode = "MF (infinity)",
            tips = "Use tripod, enable long exposure mode"
        )
    ),

    BEACH(
        displayName = "Beach/Snow",
        description = "High-key bright scenes",
        recommendedSettings = SceneSettings(
            iso = 100..400,
            shutterSpeed = 1000000L..8000000L,
            aperture = "f/8 - f/11",
            focusMode = "AF-S",
            tips = "Use +1 to +2 EV compensation to avoid underexposure"
        )
    ),

    // Architecture & Urban
    ARCHITECTURE(
        displayName = "Architecture",
        description = "Buildings, structures",
        recommendedSettings = SceneSettings(
            iso = 100..400,
            shutterSpeed = 4000000L..125000000L,
            aperture = "f/8 - f/11",
            focusMode = "AF-S",
            tips = "Enable level indicator, use grid lines, avoid distortion"
        )
    ),

    CITYSCAPE(
        displayName = "Cityscape",
        description = "Urban scenes, skylines",
        recommendedSettings = SceneSettings(
            iso = 100..800,
            shutterSpeed = 8000000L..500000000L,
            aperture = "f/8 - f/16",
            focusMode = "AF-S",
            tips = "Shoot during blue hour, use tripod for long exposures"
        )
    ),

    NIGHT_SCENE(
        displayName = "Night Scene",
        description = "City lights at night",
        recommendedSettings = SceneSettings(
            iso = 800..3200,
            shutterSpeed = 15000000L..2000000000L,  // 1/60 - 2s
            aperture = "f/2.8 - f/8",
            focusMode = "AF-S or MF",
            tips = "Use tripod, enable night mode, try light trails"
        )
    ),

    // Nature & Wildlife
    FLOWER(
        displayName = "Flowers",
        description = "Close-up flowers",
        recommendedSettings = SceneSettings(
            iso = 100..400,
            shutterSpeed = 2000000L..8000000L,
            aperture = "f/2.8 - f/5.6",
            focusMode = "AF-S (macro)",
            tips = "Use shallow DoF, avoid wind, enable focus peaking"
        )
    ),

    MACRO(
        displayName = "Macro",
        description = "Extreme close-ups",
        recommendedSettings = SceneSettings(
            iso = 100..800,
            shutterSpeed = 1000000L..8000000L,
            aperture = "f/5.6 - f/11",
            focusMode = "MF (focus stacking)",
            tips = "Use focus peaking, steady hands or tripod"
        )
    ),

    ANIMAL(
        displayName = "Animals/Pets",
        description = "Wildlife, pets",
        recommendedSettings = SceneSettings(
            iso = 400..3200,
            shutterSpeed = 500000L..2000000L,  // 1/2000 - 1/500
            aperture = "f/2.8 - f/5.6",
            focusMode = "AF-C (tracking)",
            tips = "Use fast shutter speed, continuous autofocus"
        )
    ),

    // Food & Objects
    FOOD(
        displayName = "Food",
        description = "Culinary photography",
        recommendedSettings = SceneSettings(
            iso = 100..800,
            shutterSpeed = 4000000L..125000000L,
            aperture = "f/2.8 - f/5.6",
            focusMode = "AF-S",
            tips = "Use natural light, shoot from 45° angle"
        )
    ),

    PRODUCT(
        displayName = "Product",
        description = "Commercial products",
        recommendedSettings = SceneSettings(
            iso = 100..400,
            shutterSpeed = 8000000L..125000000L,
            aperture = "f/8 - f/11",
            focusMode = "AF-S",
            tips = "Use even lighting, white background, enable level"
        )
    ),

    // Action & Sports
    SPORTS(
        displayName = "Sports/Action",
        description = "Fast-moving subjects",
        recommendedSettings = SceneSettings(
            iso = 400..3200,
            shutterSpeed = 125000L..1000000L,  // 1/8000 - 1/1000
            aperture = "f/2.8 - f/5.6",
            focusMode = "AF-C (tracking)",
            tips = "Use fast shutter speed, burst mode, track subject"
        )
    ),

    MOTION(
        displayName = "Motion Blur",
        description = "Intentional motion blur",
        recommendedSettings = SceneSettings(
            iso = 100..400,
            shutterSpeed = 16000000L..500000000L,  // 1/60 - 1/2s
            aperture = "f/8 - f/16",
            focusMode = "AF-S",
            tips = "Pan with subject for motion blur effect"
        )
    ),

    // Documents & Text
    DOCUMENT(
        displayName = "Document/Text",
        description = "Papers, text, QR codes",
        recommendedSettings = SceneSettings(
            iso = 100..400,
            shutterSpeed = 4000000L..16000000L,
            aperture = "f/5.6 - f/8",
            focusMode = "AF-S",
            tips = "Use even lighting, shoot straight-on, enable grid"
        )
    ),

    QR_CODE(
        displayName = "QR Code",
        description = "QR codes, barcodes",
        recommendedSettings = SceneSettings(
            iso = 100..800,
            shutterSpeed = 4000000L..16000000L,
            aperture = "Auto",
            focusMode = "AF-S (center)",
            tips = "Ensure good lighting, hold steady"
        )
    ),

    // Weather & Special
    FIREWORKS(
        displayName = "Fireworks",
        description = "Fireworks display",
        recommendedSettings = SceneSettings(
            iso = 100..400,
            shutterSpeed = 2_000_000_000L..8_000_000_000L,  // 2-8s
            aperture = "f/8 - f/16",
            focusMode = "MF (infinity)",
            tips = "Use tripod, bulb mode, multiple exposures"
        )
    ),

    BACKLIGHT(
        displayName = "Backlit",
        description = "Strong backlight",
        recommendedSettings = SceneSettings(
            iso = 100..800,
            shutterSpeed = 2000000L..16000000L,
            aperture = "f/2.8 - f/5.6",
            focusMode = "AF-S",
            tips = "Use +1 to +2 EV compensation or HDR"
        )
    ),

    LOW_LIGHT(
        displayName = "Low Light",
        description = "Indoor, dim lighting",
        recommendedSettings = SceneSettings(
            iso = 800..6400,
            shutterSpeed = 4000000L..125000000L,
            aperture = "f/1.8 - f/2.8 (wide open)",
            focusMode = "AF-S",
            tips = "Use high ISO, wide aperture, stabilize camera"
        )
    ),

    // Default
    AUTO(
        displayName = "Auto",
        description = "General purpose",
        recommendedSettings = SceneSettings(
            iso = 100..1600,
            shutterSpeed = 1000000L..125000000L,
            aperture = "Auto",
            focusMode = "AF-S",
            tips = "Let the camera decide"
        )
    );

    companion object {
        /**
         * Get scene by confidence score
         */
        fun fromConfidence(scores: Map<SceneType, Float>): SceneType? {
            return scores.maxByOrNull { it.value }?.key
        }
    }
}

/**
 * Recommended settings for a scene
 */
data class SceneSettings(
    val iso: IntRange,
    val shutterSpeed: LongRange,
    val aperture: String,
    val focusMode: String,
    val tips: String
)

/**
 * Scene detection result
 */
data class SceneDetectionResult(
    val primaryScene: SceneType,
    val confidence: Float,  // 0.0 - 1.0
    val allScores: Map<SceneType, Float> = emptyMap()
)

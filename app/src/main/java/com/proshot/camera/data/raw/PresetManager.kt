package com.proshot.camera.data.raw

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preset manager for RAW editing presets
 * Manages built-in and user-created presets
 */
@Singleton
class PresetManager @Inject constructor(
    private val context: Context
) {
    private val gson = Gson()
    private val presetsDir = File(context.filesDir, "presets")

    private val _presets = MutableStateFlow<List<EditPreset>>(emptyList())
    val presets: StateFlow<List<EditPreset>> = _presets.asStateFlow()

    init {
        // Create presets directory
        if (!presetsDir.exists()) {
            presetsDir.mkdirs()
        }

        // Initialize with built-in presets
        initializeBuiltInPresets()
    }

    /**
     * Load all presets (built-in + user-created)
     */
    suspend fun loadPresets() = withContext(Dispatchers.IO) {
        try {
            val allPresets = mutableListOf<EditPreset>()

            // Add built-in presets
            allPresets.addAll(getBuiltInPresets())

            // Load user presets from disk
            val userPresets = loadUserPresets()
            allPresets.addAll(userPresets)

            _presets.value = allPresets
            Timber.d("Loaded ${allPresets.size} presets")

        } catch (e: Exception) {
            Timber.e(e, "Failed to load presets")
        }
    }

    /**
     * Save preset
     */
    suspend fun savePreset(preset: EditPreset): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(presetsDir, "${preset.id}.json")
            val json = gson.toJson(preset)
            file.writeText(json)

            // Reload presets
            loadPresets()

            Timber.d("Preset saved: ${preset.name}")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to save preset")
            Result.failure(e)
        }
    }

    /**
     * Delete preset (user presets only)
     */
    suspend fun deletePreset(presetId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(presetsDir, "$presetId.json")
            if (file.exists()) {
                file.delete()
                loadPresets()
                Timber.d("Preset deleted: $presetId")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Preset not found"))
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to delete preset")
            Result.failure(e)
        }
    }

    /**
     * Get preset by ID
     */
    fun getPresetById(id: String): EditPreset? {
        return _presets.value.find { it.id == id }
    }

    /**
     * Load user presets from disk
     */
    private fun loadUserPresets(): List<EditPreset> {
        val presets = mutableListOf<EditPreset>()

        presetsDir.listFiles()?.forEach { file ->
            try {
                val json = file.readText()
                val preset = gson.fromJson(json, EditPreset::class.java)
                if (!preset.isBuiltIn) {
                    presets.add(preset)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load preset: ${file.name}")
            }
        }

        return presets
    }

    /**
     * Initialize built-in presets
     */
    private fun initializeBuiltInPresets() {
        // Built-in presets are defined in code
        // No need to save to disk
    }

    /**
     * Get built-in presets
     */
    private fun getBuiltInPresets(): List<EditPreset> {
        return listOf(
            // Portrait presets
            EditPreset(
                id = "portrait_soft",
                name = "Portrait - Soft",
                category = PresetCategory.PORTRAIT,
                isBuiltIn = true,
                thumbnail = null,
                parameters = RawEditParameters(
                    exposure = 0.3f,
                    contrast = -10f,
                    highlights = -15f,
                    shadows = 20f,
                    temperature = 5f,
                    tint = -5f,
                    saturation = -10f,
                    vibrance = 10f,
                    sharpness = 15f,
                    denoise = 10f,
                    vignette = -10f
                ),
                description = "Soft, flattering portrait with warm tones"
            ),

            EditPreset(
                id = "portrait_dramatic",
                name = "Portrait - Dramatic",
                category = PresetCategory.PORTRAIT,
                isBuiltIn = true,
                thumbnail = null,
                parameters = RawEditParameters(
                    exposure = 0f,
                    contrast = 30f,
                    highlights = -30f,
                    shadows = 30f,
                    saturation = -20f,
                    sharpness = 30f,
                    vignette = 30f
                ),
                description = "High contrast dramatic portrait"
            ),

            // Landscape presets
            EditPreset(
                id = "landscape_vivid",
                name = "Landscape - Vivid",
                category = PresetCategory.LANDSCAPE,
                isBuiltIn = true,
                thumbnail = null,
                parameters = RawEditParameters(
                    exposure = 0.2f,
                    contrast = 20f,
                    highlights = -10f,
                    shadows = 15f,
                    temperature = -5f,
                    saturation = 25f,
                    vibrance = 30f,
                    sharpness = 25f
                ),
                description = "Vibrant landscape with punchy colors"
            ),

            EditPreset(
                id = "landscape_muted",
                name = "Landscape - Muted",
                category = PresetCategory.LANDSCAPE,
                isBuiltIn = true,
                thumbnail = null,
                parameters = RawEditParameters(
                    exposure = 0f,
                    contrast = -10f,
                    saturation = -30f,
                    vibrance = -20f,
                    vignette = 15f
                ),
                description = "Muted, moody landscape"
            ),

            // Black & White presets
            EditPreset(
                id = "bw_classic",
                name = "Black & White - Classic",
                category = PresetCategory.BLACK_AND_WHITE,
                isBuiltIn = true,
                thumbnail = null,
                parameters = RawEditParameters(
                    contrast = 15f,
                    highlights = -10f,
                    shadows = 20f,
                    saturation = -100f,
                    sharpness = 20f
                ),
                description = "Classic black and white"
            ),

            EditPreset(
                id = "bw_high_contrast",
                name = "Black & White - High Contrast",
                category = PresetCategory.BLACK_AND_WHITE,
                isBuiltIn = true,
                thumbnail = null,
                parameters = RawEditParameters(
                    contrast = 50f,
                    highlights = -40f,
                    shadows = 40f,
                    saturation = -100f,
                    sharpness = 30f,
                    vignette = 25f
                ),
                description = "High contrast black and white"
            ),

            // Street photography presets
            EditPreset(
                id = "street_gritty",
                name = "Street - Gritty",
                category = PresetCategory.STREET,
                isBuiltIn = true,
                thumbnail = null,
                parameters = RawEditParameters(
                    exposure = -0.3f,
                    contrast = 35f,
                    highlights = -25f,
                    shadows = 25f,
                    saturation = -40f,
                    sharpness = 35f,
                    vignette = 30f
                ),
                description = "Gritty street photography look"
            ),

            // Vintage presets
            EditPreset(
                id = "vintage_warm",
                name = "Vintage - Warm",
                category = PresetCategory.VINTAGE,
                isBuiltIn = true,
                thumbnail = null,
                parameters = RawEditParameters(
                    exposure = 0.2f,
                    contrast = -15f,
                    highlights = -20f,
                    shadows = 10f,
                    temperature = 25f,
                    tint = -10f,
                    saturation = -25f,
                    vibrance = -10f,
                    vignette = 35f
                ),
                description = "Warm vintage film look"
            ),

            EditPreset(
                id = "vintage_faded",
                name = "Vintage - Faded",
                category = PresetCategory.VINTAGE,
                isBuiltIn = true,
                thumbnail = null,
                parameters = RawEditParameters(
                    exposure = 0.3f,
                    contrast = -25f,
                    highlights = 20f,
                    shadows = -15f,
                    saturation = -40f,
                    vibrance = -30f,
                    vignette = 25f
                ),
                description = "Faded vintage film"
            ),

            // HDR presets
            EditPreset(
                id = "hdr_natural",
                name = "HDR - Natural",
                category = PresetCategory.HDR,
                isBuiltIn = true,
                thumbnail = null,
                parameters = RawEditParameters(
                    exposure = 0.2f,
                    contrast = -15f,
                    highlights = -50f,
                    shadows = 50f,
                    saturation = 10f,
                    vibrance = 15f,
                    sharpness = 20f
                ),
                description = "Natural HDR look"
            ),

            EditPreset(
                id = "hdr_dramatic",
                name = "HDR - Dramatic",
                category = PresetCategory.HDR,
                isBuiltIn = true,
                thumbnail = null,
                parameters = RawEditParameters(
                    exposure = 0f,
                    contrast = 25f,
                    highlights = -70f,
                    shadows = 70f,
                    saturation = 30f,
                    vibrance = 40f,
                    sharpness = 40f
                ),
                description = "Dramatic HDR effect"
            ),

            // Cinematic presets
            EditPreset(
                id = "cinematic_teal_orange",
                name = "Cinematic - Teal & Orange",
                category = PresetCategory.CINEMATIC,
                isBuiltIn = true,
                thumbnail = null,
                parameters = RawEditParameters(
                    exposure = 0.1f,
                    contrast = 20f,
                    highlights = -15f,
                    shadows = 20f,
                    temperature = 15f,
                    tint = -15f,
                    saturation = 15f,
                    vibrance = 20f,
                    vignette = 20f
                ),
                description = "Popular teal and orange cinema look"
            ),

            // Food photography
            EditPreset(
                id = "food_vibrant",
                name = "Food - Vibrant",
                category = PresetCategory.FOOD,
                isBuiltIn = true,
                thumbnail = null,
                parameters = RawEditParameters(
                    exposure = 0.4f,
                    contrast = 15f,
                    highlights = -10f,
                    shadows = 20f,
                    temperature = 10f,
                    saturation = 35f,
                    vibrance = 40f,
                    sharpness = 30f
                ),
                description = "Vibrant, appetizing food photography"
            )
        )
    }
}

/**
 * Edit preset
 */
data class EditPreset(
    val id: String,
    val name: String,
    val category: PresetCategory,
    val isBuiltIn: Boolean = false,
    val thumbnail: String? = null,  // Path to thumbnail image
    val parameters: RawEditParameters,
    val description: String = ""
)

/**
 * Preset categories
 */
enum class PresetCategory(val displayName: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    BLACK_AND_WHITE("Black & White"),
    STREET("Street"),
    VINTAGE("Vintage"),
    HDR("HDR"),
    CINEMATIC("Cinematic"),
    FOOD("Food"),
    CUSTOM("Custom")
}

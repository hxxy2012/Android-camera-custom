package com.proshot.camera.domain.model

/**
 * Represents a camera parameter that can be manually controlled
 */
sealed class CameraParameter {
    abstract val value: Any
    abstract val displayValue: String
    abstract val range: ClosedRange<*>?

    data class ISO(
        override val value: Int,
        override val range: IntRange? = null
    ) : CameraParameter() {
        override val displayValue: String
            get() = if (value == 0) "AUTO" else value.toString()
    }

    data class ShutterSpeed(
        override val value: Long, // in nanoseconds
        override val range: LongRange? = null
    ) : CameraParameter() {
        override val displayValue: String
            get() = formatShutterSpeed(value)

        private fun formatShutterSpeed(nanos: Long): String {
            if (nanos == 0L) return "AUTO"

            val seconds = nanos / 1_000_000_000.0
            return when {
                seconds >= 1.0 -> String.format("%.1fs", seconds)
                else -> {
                    val denominator = (1.0 / seconds).toInt()
                    "1/$denominator"
                }
            }
        }
    }

    data class WhiteBalance(
        override val value: Int, // Color temperature in Kelvin
        val mode: WBMode = WBMode.AUTO
    ) : CameraParameter() {
        override val range: IntRange = 2000..10000
        override val displayValue: String
            get() = when (mode) {
                WBMode.AUTO -> "AUTO"
                WBMode.DAYLIGHT -> "☀ ${value}K"
                WBMode.CLOUDY -> "☁ ${value}K"
                WBMode.SHADE -> "🌤 ${value}K"
                WBMode.TUNGSTEN -> "💡 ${value}K"
                WBMode.FLUORESCENT -> "🔦 ${value}K"
                WBMode.CUSTOM -> "${value}K"
            }

        enum class WBMode {
            AUTO, DAYLIGHT, CLOUDY, SHADE, TUNGSTEN, FLUORESCENT, CUSTOM
        }
    }

    data class ExposureCompensation(
        override val value: Int, // in 1/3 EV steps
        override val range: IntRange? = null
    ) : CameraParameter() {
        override val displayValue: String
            get() {
                val ev = value / 3.0
                return when {
                    ev > 0 -> "+%.1f".format(ev)
                    ev < 0 -> "%.1f".format(ev)
                    else -> "0.0"
                }
            }
    }

    data class Focus(
        override val value: Float, // Focus distance (0 = infinity, max = minimum focus)
        override val range: ClosedFloatingPointRange<Float>? = null,
        val mode: FocusMode = FocusMode.AUTO_SINGLE
    ) : CameraParameter() {
        override val displayValue: String
            get() = when (mode) {
                FocusMode.AUTO_SINGLE -> "AF-S"
                FocusMode.AUTO_CONTINUOUS -> "AF-C"
                FocusMode.MANUAL -> if (value == 0f) "∞" else "%.2fm".format(1 / value)
            }

        enum class FocusMode {
            AUTO_SINGLE,      // AF-S
            AUTO_CONTINUOUS,  // AF-C
            MANUAL           // MF
        }
    }
}

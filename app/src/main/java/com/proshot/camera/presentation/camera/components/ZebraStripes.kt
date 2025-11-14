package com.proshot.camera.presentation.camera.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import com.proshot.camera.presentation.theme.AccentRed
import com.proshot.camera.presentation.theme.AccentYellow
import kotlin.math.abs

/**
 * Zebra stripes overlay for overexposure/underexposure warning
 * Animated diagonal stripes on blown highlights or clipped shadows
 */
@Composable
fun ZebraStripesOverlay(
    modifier: Modifier = Modifier,
    overexposedRegions: List<Rect> = emptyList(),
    underexposedRegions: List<Rect> = emptyList(),
    stripeWidth: Float = 10f,
    showOverexposed: Boolean = true,
    showUnderexposed: Boolean = true
) {
    // Animate the stripe pattern
    val infiniteTransition = rememberInfiniteTransition(label = "zebra")
    val animationOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = stripeWidth * 2,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stripeOffset"
    )

    Canvas(modifier = modifier) {
        if (showOverexposed) {
            overexposedRegions.forEach { region ->
                drawZebraStripes(
                    region = region,
                    color = AccentRed.copy(alpha = 0.6f),
                    stripeWidth = stripeWidth,
                    offset = animationOffset
                )
            }
        }

        if (showUnderexposed) {
            underexposedRegions.forEach { region ->
                drawZebraStripes(
                    region = region,
                    color = Color.Blue.copy(alpha = 0.6f),
                    stripeWidth = stripeWidth,
                    offset = animationOffset
                )
            }
        }
    }
}

/**
 * Draw diagonal zebra stripes in a region
 */
private fun DrawScope.drawZebraStripes(
    region: Rect,
    color: Color,
    stripeWidth: Float,
    offset: Float
) {
    val path = Path()

    // Draw diagonal stripes at 45-degree angle
    val spacing = stripeWidth * 2

    // Calculate how many stripes we need to cover the region
    val diagonalLength = region.width + region.height
    val numStripes = (diagonalLength / spacing).toInt() + 2

    for (i in 0 until numStripes) {
        val startOffset = i * spacing + offset % spacing - diagonalLength / 2

        // Calculate stripe endpoints
        val x1 = region.left + startOffset
        val y1 = region.top
        val x2 = region.left
        val y2 = region.top + startOffset

        val x3 = region.left + startOffset + stripeWidth
        val y3 = region.top
        val x4 = region.left
        val y4 = region.top + startOffset + stripeWidth

        // Create stripe polygon
        path.reset()
        path.moveTo(x1.coerceIn(region.left, region.right), region.top)

        // Top-right corner of stripe
        if (x1 < region.right && x1 >= region.left) {
            path.lineTo(x1.coerceIn(region.left, region.right), region.top)
        } else {
            path.lineTo(region.right, (y1 + (region.right - x1)).coerceIn(region.top, region.bottom))
        }

        if (x3 < region.right && x3 >= region.left) {
            path.lineTo(x3.coerceIn(region.left, region.right), region.top)
        } else {
            path.lineTo(region.right, (y3 + (region.right - x3)).coerceIn(region.top, region.bottom))
        }

        // Bottom-left corner of stripe
        if (y4 < region.bottom && y4 >= region.top) {
            path.lineTo(region.left, y4.coerceIn(region.top, region.bottom))
        } else {
            path.lineTo((x4 + (region.bottom - y4)).coerceIn(region.left, region.right), region.bottom)
        }

        if (y2 < region.bottom && y2 >= region.top) {
            path.lineTo(region.left, y2.coerceIn(region.top, region.bottom))
        } else {
            path.lineTo((x2 + (region.bottom - y2)).coerceIn(region.left, region.right), region.bottom)
        }

        path.close()

        drawPath(path, color, style = Fill)
    }
}

/**
 * Detect overexposed/underexposed regions from pixel data
 */
fun detectExposureRegions(
    pixels: IntArray,
    width: Int,
    height: Int,
    overexposureThreshold: Float = 0.95f,  // 95% of max brightness
    underexposureThreshold: Float = 0.05f   // 5% of max brightness
): ExposureRegions {
    val overexposedMask = BooleanArray(pixels.size)
    val underexposedMask = BooleanArray(pixels.size)

    val maxBrightness = 255
    val overThreshold = (maxBrightness * overexposureThreshold).toInt()
    val underThreshold = (maxBrightness * underexposureThreshold).toInt()

    for (i in pixels.indices) {
        val pixel = pixels[i]
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF

        // Check if all channels are blown (overexposed)
        if (r > overThreshold && g > overThreshold && b > overThreshold) {
            overexposedMask[i] = true
        }

        // Check if all channels are clipped (underexposed)
        if (r < underThreshold && g < underThreshold && b < underThreshold) {
            underexposedMask[i] = true
        }
    }

    // Convert masks to regions (simplified - just create one region per connected area)
    val overexposedRegions = maskToRegions(overexposedMask, width, height)
    val underexposedRegions = maskToRegions(underexposedMask, width, height)

    return ExposureRegions(
        overexposed = overexposedRegions,
        underexposed = underexposedRegions
    )
}

/**
 * Convert boolean mask to list of rectangular regions
 * Simplified version - creates bounding boxes for connected components
 */
private fun maskToRegions(mask: BooleanArray, width: Int, height: Int): List<Rect> {
    val regions = mutableListOf<Rect>()

    // Simple approach: find bounding box of all true pixels
    // More sophisticated: use connected component labeling

    var minX = width
    var maxX = 0
    var minY = height
    var maxY = 0
    var hasPixels = false

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (mask[y * width + x]) {
                minX = minOf(minX, x)
                maxX = maxOf(maxX, x)
                minY = minOf(minY, y)
                maxY = maxOf(maxY, y)
                hasPixels = true
            }
        }
    }

    if (hasPixels) {
        regions.add(
            Rect(
                left = minX.toFloat(),
                top = minY.toFloat(),
                right = maxX.toFloat(),
                bottom = maxY.toFloat()
            )
        )
    }

    return regions
}

/**
 * Exposure regions data class
 */
data class ExposureRegions(
    val overexposed: List<Rect>,
    val underexposed: List<Rect>
)

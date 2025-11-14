package com.proshot.camera.presentation.camera.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proshot.camera.data.camera.ExposureStats
import com.proshot.camera.data.camera.HistogramData
import com.proshot.camera.presentation.theme.AccentGreen
import com.proshot.camera.presentation.theme.AccentOrange
import com.proshot.camera.presentation.theme.AccentRed
import com.proshot.camera.presentation.theme.AccentYellow
import com.proshot.camera.presentation.theme.OverlayBackground
import com.proshot.camera.presentation.theme.PureWhite
import kotlin.math.max

/**
 * Professional histogram display component
 * Shows RGB and luminance histograms for exposure analysis
 */
@Composable
fun HistogramView(
    modifier: Modifier = Modifier,
    histogramData: HistogramData?,
    exposureStats: ExposureStats?,
    showRGB: Boolean = true,
    showLuminance: Boolean = true
) {
    if (histogramData == null) return

    Box(
        modifier = modifier
            .background(OverlayBackground, RoundedCornerShape(8.dp))
            .border(1.dp, AccentOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            // Title
            Text(
                text = "HISTOGRAM",
                style = MaterialTheme.typography.labelSmall,
                color = AccentOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Histogram graph
            HistogramGraph(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                histogramData = histogramData,
                showRGB = showRGB,
                showLuminance = showLuminance
            )

            // Exposure statistics
            if (exposureStats != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ExposureStatsView(stats = exposureStats)
            }
        }
    }
}

/**
 * Histogram graph visualization
 */
@Composable
private fun HistogramGraph(
    modifier: Modifier = Modifier,
    histogramData: HistogramData,
    showRGB: Boolean,
    showLuminance: Boolean
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Find max value for normalization
        val maxRed = histogramData.red.maxOrNull() ?: 1
        val maxGreen = histogramData.green.maxOrNull() ?: 1
        val maxBlue = histogramData.blue.maxOrNull() ?: 1
        val maxLum = histogramData.luminance.maxOrNull() ?: 1
        val maxValue = max(max(maxRed, maxGreen), max(maxBlue, maxLum)).toFloat()

        if (maxValue <= 0) return@Canvas

        // Draw grid
        drawHistogramGrid(width, height)

        // Draw RGB channels
        if (showRGB) {
            drawHistogramChannel(
                data = histogramData.red,
                maxValue = maxValue,
                color = Color.Red.copy(alpha = 0.6f),
                width = width,
                height = height
            )
            drawHistogramChannel(
                data = histogramData.green,
                maxValue = maxValue,
                color = Color.Green.copy(alpha = 0.6f),
                width = width,
                height = height
            )
            drawHistogramChannel(
                data = histogramData.blue,
                maxValue = maxValue,
                color = Color.Blue.copy(alpha = 0.6f),
                width = width,
                height = height
            )
        }

        // Draw luminance channel
        if (showLuminance) {
            drawHistogramChannel(
                data = histogramData.luminance,
                maxValue = maxValue,
                color = PureWhite.copy(alpha = 0.8f),
                width = width,
                height = height,
                stroke = true
            )
        }

        // Draw highlight/shadow clipping indicators
        drawClippingIndicators(width, height)
    }
}

/**
 * Draw histogram grid
 */
private fun DrawScope.drawHistogramGrid(width: Float, height: Float) {
    val gridColor = Color.White.copy(alpha = 0.1f)

    // Horizontal lines (thirds)
    for (i in 1..2) {
        val y = height * i / 3f
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
    }

    // Vertical lines (quarters)
    for (i in 1..3) {
        val x = width * i / 4f
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
    }
}

/**
 * Draw single histogram channel
 */
private fun DrawScope.drawHistogramChannel(
    data: IntArray,
    maxValue: Float,
    color: Color,
    width: Float,
    height: Float,
    stroke: Boolean = false
) {
    val path = Path()
    val binWidth = width / 256f

    // Start from bottom left
    path.moveTo(0f, height)

    // Draw histogram curve
    for (i in data.indices) {
        val x = i * binWidth
        val normalized = data[i] / maxValue
        val y = height - (normalized * height)

        if (i == 0) {
            path.lineTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    // Close path
    path.lineTo(width, height)
    path.close()

    // Draw
    if (stroke) {
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2f)
        )
    } else {
        drawPath(
            path = path,
            color = color,
            style = Fill
        )
    }
}

/**
 * Draw clipping indicators
 */
private fun DrawScope.drawClippingIndicators(width: Float, height: Float) {
    // Shadow clipping area (left 5%)
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                AccentRed.copy(alpha = 0.3f),
                Color.Transparent
            ),
            startX = 0f,
            endX = width * 0.05f
        ),
        topLeft = Offset(0f, 0f),
        size = androidx.compose.ui.geometry.Size(width * 0.05f, height)
    )

    // Highlight clipping area (right 5%)
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                AccentRed.copy(alpha = 0.3f)
            ),
            startX = width * 0.95f,
            endX = width
        ),
        topLeft = Offset(width * 0.95f, 0f),
        size = androidx.compose.ui.geometry.Size(width * 0.05f, height)
    )
}

/**
 * Exposure statistics display
 */
@Composable
private fun ExposureStatsView(
    stats: ExposureStats
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mean luminance
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "MEAN",
                style = MaterialTheme.typography.labelSmall,
                color = AccentOrange.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
            Text(
                text = "${(stats.meanLuminance * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = PureWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Shadows clipping
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SHADOWS",
                style = MaterialTheme.typography.labelSmall,
                color = if (stats.clippedShadows > 0.05f) AccentRed else AccentOrange.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
            Text(
                text = "${(stats.clippedShadows * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = if (stats.clippedShadows > 0.05f) AccentRed else PureWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Highlights clipping
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "HIGHLIGHTS",
                style = MaterialTheme.typography.labelSmall,
                color = if (stats.clippedHighlights > 0.05f) AccentRed else AccentOrange.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
            Text(
                text = "${(stats.clippedHighlights * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = if (stats.clippedHighlights > 0.05f) AccentRed else PureWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Exposure warning indicator
        if (stats.isOverexposed || stats.isUnderexposed) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (stats.isOverexposed) AccentYellow else AccentRed,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = AccentGreen,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

/**
 * Compact histogram view (for small display)
 */
@Composable
fun CompactHistogramView(
    modifier: Modifier = Modifier,
    histogramData: HistogramData?
) {
    if (histogramData == null) return

    Box(
        modifier = modifier
            .background(OverlayBackground.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
            .border(1.dp, AccentOrange.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        HistogramGraph(
            modifier = Modifier
                .width(120.dp)
                .height(40.dp),
            histogramData = histogramData,
            showRGB = false,
            showLuminance = true
        )
    }
}

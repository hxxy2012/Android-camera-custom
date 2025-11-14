package com.proshot.camera.presentation.camera.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proshot.camera.presentation.theme.AccentGreen
import com.proshot.camera.presentation.theme.AccentOrange
import com.proshot.camera.presentation.theme.AccentRed
import com.proshot.camera.presentation.theme.OverlayBackground
import com.proshot.camera.presentation.theme.PureWhite
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Electronic level indicator for camera leveling
 * Shows pitch (up/down tilt) and roll (left/right tilt)
 */
@Composable
fun LevelIndicator(
    modifier: Modifier = Modifier,
    pitch: Float = 0f,  // Degrees (-90 to 90)
    roll: Float = 0f    // Degrees (-180 to 180)
) {
    Box(
        modifier = modifier
            .background(OverlayBackground, RoundedCornerShape(8.dp))
            .border(1.dp, AccentOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "LEVEL",
                style = MaterialTheme.typography.labelSmall,
                color = AccentOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )

            // Circular level indicator
            CircularLevel(
                modifier = Modifier
                    .size(100.dp)
                    .padding(8.dp),
                pitch = pitch,
                roll = roll
            )

            // Angle display
            AngleDisplay(pitch = pitch, roll = roll)
        }
    }
}

/**
 * Circular bubble level indicator
 */
@Composable
private fun CircularLevel(
    modifier: Modifier = Modifier,
    pitch: Float,
    roll: Float
) {
    val pitchAnimated = remember { Animatable(0f) }
    val rollAnimated = remember { Animatable(0f) }

    LaunchedEffect(pitch, roll) {
        pitchAnimated.animateTo(
            targetValue = pitch,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        rollAnimated.animateTo(
            targetValue = roll,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2

        // Draw outer circle
        drawCircle(
            color = PureWhite.copy(alpha = 0.3f),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )

        // Draw crosshair
        val crosshairLength = radius * 0.15f

        // Horizontal line
        drawLine(
            color = PureWhite.copy(alpha = 0.5f),
            start = Offset(centerX - crosshairLength, centerY),
            end = Offset(centerX + crosshairLength, centerY),
            strokeWidth = 1f
        )

        // Vertical line
        drawLine(
            color = PureWhite.copy(alpha = 0.5f),
            start = Offset(centerX, centerY - crosshairLength),
            end = Offset(centerX, centerY + crosshairLength),
            strokeWidth = 1f
        )

        // Calculate bubble position
        // Roll affects X, Pitch affects Y
        val maxOffset = radius * 0.6f
        val rollRadians = Math.toRadians(rollAnimated.value.toDouble())
        val pitchRadians = Math.toRadians(pitchAnimated.value.toDouble())

        // Clamp to max offset
        val offsetX = (sin(rollRadians) * maxOffset).toFloat().coerceIn(-maxOffset, maxOffset)
        val offsetY = (sin(pitchRadians) * maxOffset).toFloat().coerceIn(-maxOffset, maxOffset)

        val bubbleX = centerX + offsetX
        val bubbleY = centerY - offsetY // Invert Y for natural feel

        // Determine bubble color based on level accuracy
        val isLevel = abs(pitch) < 1f && abs(roll) < 1f
        val bubbleColor = when {
            isLevel -> AccentGreen
            abs(pitch) > 10f || abs(roll) > 10f -> AccentRed
            else -> AccentOrange
        }

        // Draw bubble (filled circle)
        drawCircle(
            color = bubbleColor.copy(alpha = 0.8f),
            radius = radius * 0.25f,
            center = Offset(bubbleX, bubbleY)
        )

        // Draw bubble outline
        drawCircle(
            color = PureWhite,
            radius = radius * 0.25f,
            center = Offset(bubbleX, bubbleY),
            style = Stroke(width = 2f)
        )
    }
}

/**
 * Simple horizontal level bar
 */
@Composable
fun HorizontalLevelBar(
    modifier: Modifier = Modifier,
    roll: Float = 0f
) {
    val rollAnimated = remember { Animatable(0f) }

    LaunchedEffect(roll) {
        rollAnimated.animateTo(
            targetValue = roll,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val barWidth = size.width * 0.8f

        // Draw level line
        drawLine(
            color = PureWhite.copy(alpha = 0.3f),
            start = Offset(centerX - barWidth / 2, centerY),
            end = Offset(centerX + barWidth / 2, centerY),
            strokeWidth = 2f
        )

        // Draw tick marks
        for (i in -3..3) {
            val x = centerX + (barWidth / 6) * i
            val tickHeight = if (i == 0) 20f else 10f

            drawLine(
                color = PureWhite.copy(alpha = if (i == 0) 0.8f else 0.4f),
                start = Offset(x, centerY - tickHeight / 2),
                end = Offset(x, centerY + tickHeight / 2),
                strokeWidth = if (i == 0) 3f else 2f,
                cap = StrokeCap.Round
            )
        }

        // Draw bubble indicator
        val isLevel = abs(roll) < 1f
        val bubbleColor = when {
            isLevel -> AccentGreen
            abs(roll) > 10f -> AccentRed
            else -> AccentOrange
        }

        // Calculate bubble position (-30° to +30° maps to bar width)
        val rollClamped = rollAnimated.value.coerceIn(-30f, 30f)
        val bubbleX = centerX + (rollClamped / 30f) * (barWidth / 2)

        // Draw bubble
        drawCircle(
            color = bubbleColor.copy(alpha = 0.8f),
            radius = 15f,
            center = Offset(bubbleX, centerY)
        )

        drawCircle(
            color = PureWhite,
            radius = 15f,
            center = Offset(bubbleX, centerY),
            style = Stroke(width = 2f)
        )
    }
}

/**
 * Display pitch and roll angles
 */
@Composable
private fun AngleDisplay(
    pitch: Float,
    roll: Float
) {
    val isLevel = abs(pitch) < 1f && abs(roll) < 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Level status
        Text(
            text = if (isLevel) "LEVEL" else "TILTED",
            style = MaterialTheme.typography.labelSmall,
            color = if (isLevel) AccentGreen else AccentOrange,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        // Angle values
        Text(
            text = "↕ ${pitch.toInt()}° ↔ ${roll.toInt()}°",
            style = MaterialTheme.typography.labelMedium,
            color = PureWhite,
            fontSize = 11.sp
        )
    }
}

/**
 * Compact level indicator (simple line)
 */
@Composable
fun CompactLevelIndicator(
    modifier: Modifier = Modifier,
    roll: Float = 0f
) {
    Box(
        modifier = modifier
            .background(OverlayBackground.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
            .border(1.dp, AccentOrange.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        HorizontalLevelBar(
            modifier = Modifier.fillMaxWidth(),
            roll = roll
        )
    }
}

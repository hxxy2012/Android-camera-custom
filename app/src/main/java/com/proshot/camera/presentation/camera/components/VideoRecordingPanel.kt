package com.proshot.camera.presentation.camera.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proshot.camera.data.camera.VideoRecordingState
import com.proshot.camera.data.camera.VideoResolution
import com.proshot.camera.presentation.theme.AccentGreen
import com.proshot.camera.presentation.theme.AccentOrange
import com.proshot.camera.presentation.theme.AccentRed
import com.proshot.camera.presentation.theme.AccentYellow
import com.proshot.camera.presentation.theme.OverlayBackground
import com.proshot.camera.presentation.theme.PureWhite

/**
 * Video recording control panel
 */
@Composable
fun VideoRecordingPanel(
    modifier: Modifier = Modifier,
    state: VideoRecordingState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    AnimatedVisibility(visible = state.isRecording) {
        Box(
            modifier = modifier
                .background(OverlayBackground, RoundedCornerShape(12.dp))
                .border(2.dp, AccentRed, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Recording header with blinking indicator
                RecordingHeader(isRecording = state.isRecording)

                Spacer(modifier = Modifier.height(16.dp))

                // Recording time
                Text(
                    text = state.formattedDuration,
                    style = MaterialTheme.typography.displayLarge,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recording stats
                RecordingStats(state)

                Spacer(modifier = Modifier.height(16.dp))

                // Audio level meter
                if (state.audioLevel > 0f) {
                    AudioLevelMeter(
                        level = state.audioLevel,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Control buttons
                RecordingControls(
                    isPaused = state.isPaused,
                    onPause = onPause,
                    onResume = onResume,
                    onStop = onStop
                )
            }
        }
    }
}

/**
 * Recording header with blinking red dot
 */
@Composable
private fun RecordingHeader(isRecording: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording_blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recording_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Blinking red dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(AccentRed)
                .alpha(if (isRecording) alpha else 1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "RECORDING",
            style = MaterialTheme.typography.titleMedium,
            color = AccentRed,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Recording statistics display
 */
@Composable
private fun RecordingStats(state: VideoRecordingState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Resolution
        StatItem(
            label = "RESOLUTION",
            value = when (state.resolution) {
                VideoResolution.UHD_4K -> "4K"
                VideoResolution.QHD_2K -> "2K"
                VideoResolution.FHD_1080P -> "1080p"
                VideoResolution.HD_720P -> "720p"
                VideoResolution.SD_480P -> "480p"
            },
            color = AccentYellow
        )

        // Framerate
        StatItem(
            label = "FPS",
            value = "${state.frameRate}",
            color = AccentGreen
        )

        // File size
        StatItem(
            label = "SIZE",
            value = state.formattedFileSize,
            color = AccentOrange
        )
    }
}

/**
 * Individual stat item
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f),
            fontSize = 9.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

/**
 * Audio level meter
 */
@Composable
private fun AudioLevelMeter(
    level: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Audio",
                    tint = AccentGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AUDIO",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGreen.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
            }

            Text(
                text = "${(level * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { level },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = when {
                level > 0.9f -> AccentRed
                level > 0.7f -> AccentYellow
                else -> AccentGreen
            },
        )
    }
}

/**
 * Recording control buttons
 */
@Composable
private fun RecordingControls(
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pause/Resume button
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isPaused) AccentGreen else AccentYellow)
                .clickable(onClick = if (isPaused) onResume else onPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) "Resume" else "Pause",
                tint = PureWhite,
                modifier = Modifier.size(28.dp)
            )
        }

        // Stop button
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(AccentRed)
                .clickable(onClick = onStop),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop",
                tint = PureWhite,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Compact recording indicator (minimal corner badge)
 */
@Composable
fun CompactRecordingBadge(
    modifier: Modifier = Modifier,
    duration: String,
    fileSize: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "compact_blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "compact_alpha"
    )

    Row(
        modifier = modifier
            .background(OverlayBackground.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
            .border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Blinking dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(AccentRed)
                .alpha(alpha)
        )

        Column {
            Text(
                text = duration,
                style = MaterialTheme.typography.labelMedium,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = fileSize,
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
        }
    }
}

/**
 * Video settings panel (before recording)
 */
@Composable
fun VideoSettingsPanel(
    modifier: Modifier = Modifier,
    selectedResolution: VideoResolution,
    selectedFrameRate: Int,
    onResolutionChange: (VideoResolution) -> Unit,
    onFrameRateChange: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .background(OverlayBackground, RoundedCornerShape(12.dp))
            .border(1.dp, AccentOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "VIDEO SETTINGS",
            style = MaterialTheme.typography.titleSmall,
            color = AccentOrange,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Resolution selector
        Text(
            text = "RESOLUTION",
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite.copy(alpha = 0.7f),
            fontSize = 9.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResolutionButton("4K", VideoResolution.UHD_4K, selectedResolution, onResolutionChange)
            ResolutionButton("2K", VideoResolution.QHD_2K, selectedResolution, onResolutionChange)
            ResolutionButton("1080p", VideoResolution.FHD_1080P, selectedResolution, onResolutionChange)
            ResolutionButton("720p", VideoResolution.HD_720P, selectedResolution, onResolutionChange)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Frame rate selector
        Text(
            text = "FRAME RATE",
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite.copy(alpha = 0.7f),
            fontSize = 9.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FrameRateButton(24, selectedFrameRate, onFrameRateChange)
            FrameRateButton(30, selectedFrameRate, onFrameRateChange)
            FrameRateButton(60, selectedFrameRate, onFrameRateChange)
            FrameRateButton(120, selectedFrameRate, onFrameRateChange)
        }
    }
}

/**
 * Resolution selection button
 */
@Composable
private fun ResolutionButton(
    label: String,
    resolution: VideoResolution,
    selectedResolution: VideoResolution,
    onSelect: (VideoResolution) -> Unit
) {
    val isSelected = resolution == selectedResolution

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) AccentOrange else OverlayBackground)
            .border(
                1.dp,
                if (isSelected) AccentOrange else AccentOrange.copy(alpha = 0.3f),
                RoundedCornerShape(6.dp)
            )
            .clickable { onSelect(resolution) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Frame rate selection button
 */
@Composable
private fun FrameRateButton(
    frameRate: Int,
    selectedFrameRate: Int,
    onSelect: (Int) -> Unit
) {
    val isSelected = frameRate == selectedFrameRate

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) AccentGreen else OverlayBackground)
            .border(
                1.dp,
                if (isSelected) AccentGreen else AccentGreen.copy(alpha = 0.3f),
                RoundedCornerShape(6.dp)
            )
            .clickable { onSelect(frameRate) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "${frameRate}fps",
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

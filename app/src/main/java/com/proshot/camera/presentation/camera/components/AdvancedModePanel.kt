package com.proshot.camera.presentation.camera.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proshot.camera.data.camera.HDRBracketingState
import com.proshot.camera.data.camera.LongExposureState
import com.proshot.camera.data.camera.TimelapseState
import com.proshot.camera.presentation.theme.AccentGreen
import com.proshot.camera.presentation.theme.AccentOrange
import com.proshot.camera.presentation.theme.AccentRed
import com.proshot.camera.presentation.theme.AccentYellow
import com.proshot.camera.presentation.theme.OverlayBackground
import com.proshot.camera.presentation.theme.PureWhite

/**
 * Panel for advanced shooting modes
 */
@Composable
fun AdvancedModePanel(
    modifier: Modifier = Modifier,
    selectedMode: AdvancedMode = AdvancedMode.NONE,
    onModeSelect: (AdvancedMode) -> Unit
) {
    Row(
        modifier = modifier
            .background(OverlayBackground, RoundedCornerShape(8.dp))
            .border(1.dp, AccentOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AdvancedModeButton(
            mode = AdvancedMode.LONG_EXPOSURE,
            label = "BULB",
            icon = { Icon(Icons.Default.Timer, contentDescription = null) },
            isSelected = selectedMode == AdvancedMode.LONG_EXPOSURE,
            onClick = { onModeSelect(AdvancedMode.LONG_EXPOSURE) }
        )

        AdvancedModeButton(
            mode = AdvancedMode.HDR,
            label = "HDR",
            icon = { Icon(Icons.Default.FlashOn, contentDescription = null) },
            isSelected = selectedMode == AdvancedMode.HDR,
            onClick = { onModeSelect(AdvancedMode.HDR) }
        )

        AdvancedModeButton(
            mode = AdvancedMode.TIMELAPSE,
            label = "T-LAPSE",
            icon = { Icon(Icons.Default.Timelapse, contentDescription = null) },
            isSelected = selectedMode == AdvancedMode.TIMELAPSE,
            onClick = { onModeSelect(AdvancedMode.TIMELAPSE) }
        )
    }
}

/**
 * Individual mode button
 */
@Composable
private fun AdvancedModeButton(
    mode: AdvancedMode,
    label: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) AccentOrange else OverlayBackground)
            .border(
                1.dp,
                if (isSelected) AccentOrange else AccentOrange.copy(alpha = 0.3f),
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Long exposure (Bulb mode) display
 */
@Composable
fun LongExposureDisplay(
    modifier: Modifier = Modifier,
    state: LongExposureState,
    onStop: () -> Unit
) {
    AnimatedVisibility(visible = state.isActive) {
        Box(
            modifier = modifier
                .background(OverlayBackground, RoundedCornerShape(12.dp))
                .border(2.dp, AccentRed, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "BULB MODE",
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentRed,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Elapsed time
                Text(
                    text = state.formattedTime,
                    style = MaterialTheme.typography.displayLarge,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Progress bar
                if (state.maxDuration > 0) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = AccentRed,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stop button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AccentRed)
                        .clickable(onClick = onStop),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "STOP",
                        style = MaterialTheme.typography.labelLarge,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * HDR bracketing display
 */
@Composable
fun HDRBracketingDisplay(
    modifier: Modifier = Modifier,
    state: HDRBracketingState
) {
    AnimatedVisibility(visible = state.isActive) {
        Box(
            modifier = modifier
                .background(OverlayBackground, RoundedCornerShape(12.dp))
                .border(2.dp, AccentYellow, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "HDR BRACKETING",
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentYellow,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Shot progress
                Text(
                    text = "${state.currentShot} / ${state.numShots}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold
                )

                // Current EV
                Text(
                    text = "EV: ${if (state.currentEV > 0) "+" else ""}${state.currentEV / 3.0}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AccentYellow
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Progress indicator
                CircularProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.size(48.dp),
                    color = AccentYellow,
                )
            }
        }
    }
}

/**
 * Timelapse display
 */
@Composable
fun TimelapseDisplay(
    modifier: Modifier = Modifier,
    state: TimelapseState,
    onStop: () -> Unit
) {
    AnimatedVisibility(visible = state.isActive) {
        Box(
            modifier = modifier
                .background(OverlayBackground, RoundedCornerShape(12.dp))
                .border(2.dp, AccentGreen, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "TIMELAPSE",
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Shot counter
                Text(
                    text = "${state.currentShot} / ${state.totalShots}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold
                )

                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "INTERVAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGreen.copy(alpha = 0.7f),
                            fontSize = 9.sp
                        )
                        Text(
                            text = "${state.intervalSeconds}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "REMAINING",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGreen.copy(alpha = 0.7f),
                            fontSize = 9.sp
                        )
                        Text(
                            text = state.formattedRemainingTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "VIDEO",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGreen.copy(alpha = 0.7f),
                            fontSize = 9.sp
                        )
                        Text(
                            text = "${state.videoDuration.toInt()}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = AccentGreen,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Stop button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentRed)
                        .clickable(onClick = onStop)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "STOP",
                        style = MaterialTheme.typography.labelLarge,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Advanced shooting modes
 */
enum class AdvancedMode {
    NONE,
    LONG_EXPOSURE,
    HDR,
    TIMELAPSE,
    FOCUS_STACKING
}

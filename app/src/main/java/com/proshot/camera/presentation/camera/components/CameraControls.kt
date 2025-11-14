package com.proshot.camera.presentation.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proshot.camera.domain.model.CameraParameter
import com.proshot.camera.domain.model.CameraState
import com.proshot.camera.presentation.camera.CameraUiState
import com.proshot.camera.presentation.theme.AccentGreen
import com.proshot.camera.presentation.theme.AccentOrange
import com.proshot.camera.presentation.theme.OverlayBackground
import com.proshot.camera.presentation.theme.PureWhite
import com.proshot.camera.presentation.theme.SemiTransparentBlack
import kotlin.math.roundToInt

/**
 * Professional camera controls overlay
 * Provides manual control over all camera parameters
 */
@Composable
fun CameraControls(
    modifier: Modifier = Modifier,
    cameraState: CameraState,
    uiState: CameraUiState,
    onISOChange: (Int) -> Unit,
    onShutterSpeedChange: (Long) -> Unit,
    onWhiteBalanceChange: (Int) -> Unit,
    onExposureCompensationChange: (Int) -> Unit,
    onFocusModeChange: (CameraParameter.Focus.FocusMode) -> Unit,
    onCapture: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleHistogram: () -> Unit,
    onToggleLevel: () -> Unit
) {
    Box(modifier = modifier) {
        // Top bar - shooting info
        TopInfoBar(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            cameraState = cameraState
        )

        // Left side - manual parameter controls
        LeftParameterControls(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            cameraState = cameraState,
            onISOChange = onISOChange,
            onShutterSpeedChange = onShutterSpeedChange,
            onWhiteBalanceChange = onWhiteBalanceChange,
            onExposureCompensationChange = onExposureCompensationChange
        )

        // Right side - tool buttons
        RightToolButtons(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            onToggleGrid = onToggleGrid,
            onToggleHistogram = onToggleHistogram,
            onToggleLevel = onToggleLevel
        )

        // Bottom bar - capture button and modes
        BottomControlBar(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            isCapturing = uiState.isCapturing,
            focusMode = cameraState.focus.mode,
            onCapture = onCapture,
            onFocusModeChange = onFocusModeChange
        )
    }
}

/**
 * Top information bar showing current settings
 */
@Composable
fun TopInfoBar(
    modifier: Modifier = Modifier,
    cameraState: CameraState
) {
    Row(
        modifier = modifier
            .background(OverlayBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ISO indicator
        ParameterIndicator(
            label = "ISO",
            value = cameraState.iso.displayValue,
            color = AccentOrange
        )

        // Shutter speed indicator
        ParameterIndicator(
            label = "SHUTTER",
            value = cameraState.shutterSpeed.displayValue,
            color = AccentOrange
        )

        // White balance indicator
        ParameterIndicator(
            label = "WB",
            value = cameraState.whiteBalance.displayValue,
            color = AccentOrange
        )

        // EV indicator
        ParameterIndicator(
            label = "EV",
            value = cameraState.exposureCompensation.displayValue,
            color = AccentOrange
        )
    }
}

/**
 * Left side parameter controls with vertical drag
 */
@Composable
fun LeftParameterControls(
    modifier: Modifier = Modifier,
    cameraState: CameraState,
    onISOChange: (Int) -> Unit,
    onShutterSpeedChange: (Long) -> Unit,
    onWhiteBalanceChange: (Int) -> Unit,
    onExposureCompensationChange: (Int) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ISO control
        VerticalParameterControl(
            label = "ISO",
            value = cameraState.iso.displayValue,
            onDrag = { delta ->
                val current = cameraState.iso.value
                val newValue = (current - delta * 10).coerceIn(100, 6400)
                onISOChange(newValue.roundToInt())
            }
        )

        // Shutter speed control
        VerticalParameterControl(
            label = "S",
            value = cameraState.shutterSpeed.displayValue,
            onDrag = { delta ->
                // Logarithmic adjustment for shutter speed
                val current = cameraState.shutterSpeed.value
                val factor = if (delta > 0) 1.1 else 0.9
                val newValue = (current * factor).toLong()
                    .coerceIn(1_000_000L, 1_000_000_000L) // 1ms to 1s
                onShutterSpeedChange(newValue)
            }
        )

        // White balance control
        VerticalParameterControl(
            label = "WB",
            value = "${cameraState.whiteBalance.value}K",
            onDrag = { delta ->
                val current = cameraState.whiteBalance.value
                val newValue = (current - delta * 50).coerceIn(2000, 10000)
                onWhiteBalanceChange(newValue.roundToInt())
            }
        )

        // EV control
        VerticalParameterControl(
            label = "EV",
            value = cameraState.exposureCompensation.displayValue,
            onDrag = { delta ->
                val current = cameraState.exposureCompensation.value
                val newValue = (current - delta.roundToInt()).coerceIn(-9, 9) // -3EV to +3EV (in 1/3 steps)
                onExposureCompensationChange(newValue)
            }
        )
    }
}

/**
 * Vertical drag control for a single parameter
 */
@Composable
fun VerticalParameterControl(
    label: String,
    value: String,
    onDrag: (Float) -> Unit
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SemiTransparentBlack)
            .border(1.dp, AccentOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    dragOffset += dragAmount
                    onDrag(dragAmount / 10f) // Scale drag for sensitivity
                }
            }
            .padding(12.dp)
            .width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AccentOrange,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = PureWhite,
            fontSize = 13.sp
        )
    }
}

/**
 * Right side tool buttons
 */
@Composable
fun RightToolButtons(
    modifier: Modifier = Modifier,
    onToggleGrid: () -> Unit,
    onToggleHistogram: () -> Unit,
    onToggleLevel: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ToolButton(
            icon = { Icon(Icons.Default.GridOn, "Grid", tint = PureWhite) },
            onClick = onToggleGrid
        )

        ToolButton(
            icon = { Icon(Icons.Outlined.BarChart, "Histogram", tint = PureWhite) },
            onClick = onToggleHistogram
        )

        ToolButton(
            icon = { Icon(Icons.Default.Straighten, "Level", tint = PureWhite) },
            onClick = onToggleLevel
        )
    }
}

/**
 * Tool button component
 */
@Composable
fun ToolButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(SemiTransparentBlack)
            .border(1.dp, AccentOrange.copy(alpha = 0.3f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

/**
 * Bottom control bar with capture button
 */
@Composable
fun BottomControlBar(
    modifier: Modifier = Modifier,
    isCapturing: Boolean,
    focusMode: CameraParameter.Focus.FocusMode,
    onCapture: () -> Unit,
    onFocusModeChange: (CameraParameter.Focus.FocusMode) -> Unit
) {
    Row(
        modifier = modifier
            .background(OverlayBackground)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Focus mode selector
        FocusModeSelector(
            currentMode = focusMode,
            onModeChange = onFocusModeChange
        )

        // Capture button (large circular button)
        CaptureButton(
            isCapturing = isCapturing,
            onClick = onCapture
        )

        // Placeholder for balance
        Spacer(modifier = Modifier.width(80.dp))
    }
}

/**
 * Focus mode selector
 */
@Composable
fun FocusModeSelector(
    currentMode: CameraParameter.Focus.FocusMode,
    onModeChange: (CameraParameter.Focus.FocusMode) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FocusModeButton("AF-S", currentMode == CameraParameter.Focus.FocusMode.AUTO_SINGLE) {
            onModeChange(CameraParameter.Focus.FocusMode.AUTO_SINGLE)
        }
        FocusModeButton("AF-C", currentMode == CameraParameter.Focus.FocusMode.AUTO_CONTINUOUS) {
            onModeChange(CameraParameter.Focus.FocusMode.AUTO_CONTINUOUS)
        }
        FocusModeButton("MF", currentMode == CameraParameter.Focus.FocusMode.MANUAL) {
            onModeChange(CameraParameter.Focus.FocusMode.MANUAL)
        }
    }
}

/**
 * Focus mode button
 */
@Composable
fun FocusModeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) AccentOrange else SemiTransparentBlack)
            .border(
                1.dp,
                if (isSelected) AccentOrange else AccentOrange.copy(alpha = 0.3f),
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = PureWhite,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
    )
}

/**
 * Large circular capture button
 */
@Composable
fun CaptureButton(
    isCapturing: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (isCapturing) AccentOrange.copy(alpha = 0.5f) else Color.Transparent)
            .border(4.dp, if (isCapturing) AccentOrange else PureWhite, CircleShape)
            .clickable(enabled = !isCapturing, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isCapturing) {
            // Show smaller filled circle when capturing
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AccentOrange)
            )
        } else {
            // Show camera icon when ready
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "Capture",
                tint = PureWhite,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Parameter indicator display
 */
@Composable
fun ParameterIndicator(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

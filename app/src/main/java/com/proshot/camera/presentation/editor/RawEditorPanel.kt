package com.proshot.camera.presentation.editor

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proshot.camera.data.raw.EditPreset
import com.proshot.camera.data.raw.PresetCategory
import com.proshot.camera.data.raw.RawEditParameters
import com.proshot.camera.presentation.theme.AccentBlue
import com.proshot.camera.presentation.theme.AccentGreen
import com.proshot.camera.presentation.theme.AccentOrange
import com.proshot.camera.presentation.theme.AccentYellow
import com.proshot.camera.presentation.theme.OverlayBackground
import com.proshot.camera.presentation.theme.PureWhite

/**
 * RAW editor panel with all editing controls
 */
@Composable
fun RawEditorPanel(
    modifier: Modifier = Modifier,
    parameters: RawEditParameters,
    onParametersChange: (RawEditParameters) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = modifier
            .background(OverlayBackground, RoundedCornerShape(12.dp))
            .border(1.dp, AccentBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Header with actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RAW EDITOR",
                style = MaterialTheme.typography.titleMedium,
                color = AccentBlue,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Reset button
                ActionButton(
                    icon = { Icon(Icons.Default.Refresh, contentDescription = "Reset") },
                    label = "Reset",
                    color = AccentOrange,
                    onClick = onReset
                )

                // Apply button
                ActionButton(
                    icon = { Icon(Icons.Default.Check, contentDescription = "Apply") },
                    label = "Apply",
                    color = AccentGreen,
                    onClick = onApply
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Editing controls
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Exposure section
            item {
                EditorSection(title = "EXPOSURE") {
                    EditSlider(
                        label = "Exposure",
                        value = parameters.exposure,
                        valueRange = -2f..2f,
                        displayValue = String.format("%.1f", parameters.exposure),
                        onValueChange = { onParametersChange(parameters.copy(exposure = it)) }
                    )

                    EditSlider(
                        label = "Contrast",
                        value = parameters.contrast,
                        valueRange = -100f..100f,
                        displayValue = String.format("%.0f", parameters.contrast),
                        onValueChange = { onParametersChange(parameters.copy(contrast = it)) }
                    )

                    EditSlider(
                        label = "Highlights",
                        value = parameters.highlights,
                        valueRange = -100f..100f,
                        displayValue = String.format("%.0f", parameters.highlights),
                        onValueChange = { onParametersChange(parameters.copy(highlights = it)) }
                    )

                    EditSlider(
                        label = "Shadows",
                        value = parameters.shadows,
                        valueRange = -100f..100f,
                        displayValue = String.format("%.0f", parameters.shadows),
                        onValueChange = { onParametersChange(parameters.copy(shadows = it)) }
                    )
                }
            }

            // Color section
            item {
                EditorSection(title = "COLOR") {
                    EditSlider(
                        label = "Temperature",
                        value = parameters.temperature,
                        valueRange = -100f..100f,
                        displayValue = String.format("%.0f", parameters.temperature),
                        onValueChange = { onParametersChange(parameters.copy(temperature = it)) },
                        color = AccentOrange
                    )

                    EditSlider(
                        label = "Tint",
                        value = parameters.tint,
                        valueRange = -100f..100f,
                        displayValue = String.format("%.0f", parameters.tint),
                        onValueChange = { onParametersChange(parameters.copy(tint = it)) },
                        color = AccentOrange
                    )

                    EditSlider(
                        label = "Saturation",
                        value = parameters.saturation,
                        valueRange = -100f..100f,
                        displayValue = String.format("%.0f", parameters.saturation),
                        onValueChange = { onParametersChange(parameters.copy(saturation = it)) },
                        color = AccentOrange
                    )

                    EditSlider(
                        label = "Vibrance",
                        value = parameters.vibrance,
                        valueRange = -100f..100f,
                        displayValue = String.format("%.0f", parameters.vibrance),
                        onValueChange = { onParametersChange(parameters.copy(vibrance = it)) },
                        color = AccentOrange
                    )
                }
            }

            // Detail section
            item {
                EditorSection(title = "DETAIL") {
                    EditSlider(
                        label = "Sharpness",
                        value = parameters.sharpness,
                        valueRange = 0f..100f,
                        displayValue = String.format("%.0f", parameters.sharpness),
                        onValueChange = { onParametersChange(parameters.copy(sharpness = it)) },
                        color = AccentYellow
                    )

                    EditSlider(
                        label = "Denoise",
                        value = parameters.denoise,
                        valueRange = 0f..100f,
                        displayValue = String.format("%.0f", parameters.denoise),
                        onValueChange = { onParametersChange(parameters.copy(denoise = it)) },
                        color = AccentYellow
                    )
                }
            }

            // Effects section
            item {
                EditorSection(title = "EFFECTS") {
                    EditSlider(
                        label = "Vignette",
                        value = parameters.vignette,
                        valueRange = -100f..100f,
                        displayValue = String.format("%.0f", parameters.vignette),
                        onValueChange = { onParametersChange(parameters.copy(vignette = it)) },
                        color = AccentGreen
                    )
                }
            }
        }
    }
}

/**
 * Editor section with title
 */
@Composable
private fun EditorSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = AccentBlue.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        content()
    }
}

/**
 * Edit slider with label and value display
 */
@Composable
private fun EditSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
    color: androidx.compose.ui.graphics.Color = AccentBlue
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = PureWhite,
                fontSize = 13.sp
            )

            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.3f)
            )
        )
    }
}

/**
 * Action button
 */
@Composable
private fun ActionButton(
    icon: @Composable () -> Unit,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
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
            fontSize = 10.sp
        )
    }
}

/**
 * Preset selector panel
 */
@Composable
fun PresetSelectorPanel(
    modifier: Modifier = Modifier,
    presets: List<EditPreset>,
    selectedPreset: EditPreset? = null,
    onPresetSelect: (EditPreset) -> Unit,
    onSavePreset: () -> Unit
) {
    Column(
        modifier = modifier
            .background(OverlayBackground, RoundedCornerShape(12.dp))
            .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PRESETS",
                style = MaterialTheme.typography.titleMedium,
                color = AccentGreen,
                fontWeight = FontWeight.Bold
            )

            // Save preset button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentGreen.copy(alpha = 0.2f))
                    .border(1.dp, AccentGreen, RoundedCornerShape(6.dp))
                    .clickable(onClick = onSavePreset)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save",
                        tint = AccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Group presets by category
        val groupedPresets = presets.groupBy { it.category }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groupedPresets.forEach { (category, categoryPresets) ->
                item {
                    PresetCategorySection(
                        category = category,
                        presets = categoryPresets,
                        selectedPreset = selectedPreset,
                        onPresetSelect = onPresetSelect
                    )
                }
            }
        }
    }
}

/**
 * Preset category section
 */
@Composable
private fun PresetCategorySection(
    category: PresetCategory,
    presets: List<EditPreset>,
    selectedPreset: EditPreset?,
    onPresetSelect: (EditPreset) -> Unit
) {
    Column {
        Text(
            text = category.displayName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = AccentGreen.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presets) { preset ->
                PresetCard(
                    preset = preset,
                    isSelected = preset.id == selectedPreset?.id,
                    onClick = { onPresetSelect(preset) }
                )
            }
        }
    }
}

/**
 * Preset card
 */
@Composable
private fun PresetCard(
    preset: EditPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AccentGreen.copy(alpha = 0.3f) else OverlayBackground)
            .border(
                2.dp,
                if (isSelected) AccentGreen else AccentGreen.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.labelMedium,
                color = PureWhite,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            if (preset.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = PureWhite.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    maxLines = 2
                )
            }

            if (preset.isBuiltIn) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "BUILT-IN",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentBlue,
                    fontSize = 8.sp
                )
            }
        }
    }
}

/**
 * Compact editor toolbar (minimal controls)
 */
@Composable
fun CompactEditorToolbar(
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit,
    onPresetsClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Row(
        modifier = modifier
            .background(OverlayBackground.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
            .border(1.dp, AccentBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToolbarButton("Edit", AccentBlue, onEditClick)
        ToolbarButton("Presets", AccentGreen, onPresetsClick)
        ToolbarButton("Export", AccentOrange, onExportClick)
    }
}

/**
 * Toolbar button
 */
@Composable
private fun ToolbarButton(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = PureWhite,
            fontSize = 12.sp
        )
    }
}

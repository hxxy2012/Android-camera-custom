package com.proshot.camera.presentation.camera.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proshot.camera.data.ai.CompositionAnalysis
import com.proshot.camera.data.ai.ParameterRecommendation
import com.proshot.camera.domain.model.SceneDetectionResult
import com.proshot.camera.presentation.theme.AccentBlue
import com.proshot.camera.presentation.theme.AccentGreen
import com.proshot.camera.presentation.theme.AccentOrange
import com.proshot.camera.presentation.theme.AccentYellow
import com.proshot.camera.presentation.theme.OverlayBackground
import com.proshot.camera.presentation.theme.PureWhite

/**
 * AI Assistant Panel
 * Shows scene detection, composition score, and parameter recommendations
 */
@Composable
fun AIAssistantPanel(
    modifier: Modifier = Modifier,
    sceneResult: SceneDetectionResult?,
    composition: CompositionAnalysis?,
    recommendation: ParameterRecommendation?,
    isVisible: Boolean = true
) {
    AnimatedVisibility(visible = isVisible) {
        Column(
            modifier = modifier
                .background(OverlayBackground, RoundedCornerShape(12.dp))
                .border(2.dp, AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI",
                    tint = AccentBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI ASSISTANT",
                    style = MaterialTheme.typography.titleSmall,
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scene Detection
            if (sceneResult != null) {
                SceneDetectionDisplay(sceneResult)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Composition Score
            if (composition != null) {
                CompositionScoreDisplay(composition)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Parameter Recommendations
            if (recommendation != null) {
                ParameterRecommendationDisplay(recommendation)
            }
        }
    }
}

/**
 * Scene detection display
 */
@Composable
private fun SceneDetectionDisplay(result: SceneDetectionResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SCENE",
                style = MaterialTheme.typography.labelSmall,
                color = AccentOrange.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
            Text(
                text = result.primaryScene.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = PureWhite,
                fontWeight = FontWeight.Bold
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "CONFIDENCE",
                style = MaterialTheme.typography.labelSmall,
                color = AccentOrange.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
            Text(
                text = "${(result.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    result.confidence > 0.8f -> AccentGreen
                    result.confidence > 0.5f -> AccentYellow
                    else -> AccentOrange
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Composition score display
 */
@Composable
private fun CompositionScoreDisplay(composition: CompositionAnalysis) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "COMPOSITION",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGreen.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
                Text(
                    text = composition.rating,
                    style = MaterialTheme.typography.labelLarge,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "${composition.overallScore}/100",
                style = MaterialTheme.typography.headlineSmall,
                color = when {
                    composition.overallScore >= 80 -> AccentGreen
                    composition.overallScore >= 60 -> AccentYellow
                    else -> AccentOrange
                },
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { composition.overallScore / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = when {
                composition.overallScore >= 80 -> AccentGreen
                composition.overallScore >= 60 -> AccentYellow
                else -> AccentOrange
            },
        )

        // Suggestions
        if (composition.suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.EmojiObjects,
                    contentDescription = "Tip",
                    tint = AccentYellow,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = composition.suggestions.first(),
                    style = MaterialTheme.typography.labelSmall,
                    color = PureWhite,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Parameter recommendation display
 */
@Composable
private fun ParameterRecommendationDisplay(recommendation: ParameterRecommendation) {
    Column {
        Text(
            text = "RECOMMENDED SETTINGS",
            style = MaterialTheme.typography.labelSmall,
            color = AccentBlue.copy(alpha = 0.7f),
            fontSize = 9.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ISO
        ParameterRow("ISO", recommendation.iso.toString())

        // Shutter
        ParameterRow("Shutter", recommendation.shutterSpeedDisplay)

        // EV
        if (recommendation.exposureCompensation != 0) {
            ParameterRow("EV", recommendation.evDisplay)
        }

        // Tips
        if (recommendation.tips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "💡 ${recommendation.tips.first()}",
                style = MaterialTheme.typography.labelSmall,
                color = AccentYellow,
                fontSize = 9.sp
            )
        }
    }
}

/**
 * Parameter row display
 */
@Composable
private fun ParameterRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

/**
 * Compact AI badge (small corner indicator)
 */
@Composable
fun CompactAIBadge(
    modifier: Modifier = Modifier,
    sceneName: String,
    compositionScore: Int
) {
    Column(
        modifier = modifier
            .background(OverlayBackground.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
            .border(1.dp, AccentBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI",
                tint = AccentBlue,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = sceneName,
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
                fontSize = 10.sp
            )
        }

        Text(
            text = "$compositionScore/100",
            style = MaterialTheme.typography.labelMedium,
            color = AccentGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

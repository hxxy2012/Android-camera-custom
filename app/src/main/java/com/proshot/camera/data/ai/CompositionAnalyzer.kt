package com.proshot.camera.data.ai

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * AI Composition Analyzer
 * Analyzes photo composition and provides score + suggestions
 *
 * Based on photography composition rules:
 * - Rule of Thirds
 * - Golden Ratio
 * - Leading Lines
 * - Symmetry
 * - Negative Space
 * - Framing
 */
@Singleton
class CompositionAnalyzer @Inject constructor() {

    /**
     * Analyze image composition
     */
    suspend fun analyzeComposition(bitmap: Bitmap): CompositionAnalysis = withContext(Dispatchers.Default) {
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // Detect visual elements
            val subjects = detectSubjects(pixels, width, height)
            val lines = detectLines(pixels, width, height)
            val balance = analyzeBalance(pixels, width, height)
            val symmetry = analyzeSymmetry(pixels, width, height)
            val negativeSpace = analyzeNegativeSpace(pixels, width, height)

            // Calculate composition scores
            val ruleOfThirdsScore = scoreRuleOfThirds(subjects, width, height)
            val goldenRatioScore = scoreGoldenRatio(subjects, width, height)
            val balanceScore = balance
            val symmetryScore = symmetry
            val leadingLinesScore = scoreLeadingLines(lines, subjects)
            val negativeSpaceScore = negativeSpace

            // Overall composition score (weighted average)
            val overallScore = (
                ruleOfThirdsScore * 0.25f +
                goldenRatioScore * 0.20f +
                balanceScore * 0.20f +
                symmetryScore * 0.15f +
                leadingLinesScore * 0.10f +
                negativeSpaceScore * 0.10f
            ).coerceIn(0f, 100f)

            // Generate suggestions
            val suggestions = generateSuggestions(
                overallScore,
                ruleOfThirdsScore,
                balanceScore,
                subjects,
                width,
                height
            )

            CompositionAnalysis(
                overallScore = overallScore.toInt(),
                ruleOfThirdsScore = ruleOfThirdsScore.toInt(),
                goldenRatioScore = goldenRatioScore.toInt(),
                balanceScore = balanceScore.toInt(),
                symmetryScore = symmetryScore.toInt(),
                leadingLinesScore = leadingLinesScore.toInt(),
                negativeSpaceScore = negativeSpaceScore.toInt(),
                subjects = subjects,
                suggestions = suggestions
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to analyze composition")
            CompositionAnalysis.empty()
        }
    }

    /**
     * Detect main subjects in image
     */
    private fun detectSubjects(pixels: IntArray, width: Int, height: Int): List<SubjectRegion> {
        val subjects = mutableListOf<SubjectRegion>()

        // Simple subject detection: find regions with high contrast and detail
        val gridSize = 16  // Divide image into 16x16 grid
        val cellWidth = width / gridSize
        val cellHeight = height / gridSize

        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val x = col * cellWidth
                val y = row * cellHeight

                val contrast = calculateRegionContrast(pixels, x, y, cellWidth, cellHeight, width)
                val detail = calculateRegionDetail(pixels, x, y, cellWidth, cellHeight, width)

                // High contrast + high detail = likely subject
                if (contrast > 0.5f && detail > 0.5f) {
                    subjects.add(
                        SubjectRegion(
                            center = PointF(
                                x + cellWidth / 2f,
                                y + cellHeight / 2f
                            ),
                            bounds = RectF(
                                x.toFloat(),
                                y.toFloat(),
                                (x + cellWidth).toFloat(),
                                (y + cellHeight).toFloat()
                            ),
                            importance = (contrast + detail) / 2f
                        )
                    )
                }
            }
        }

        // Merge nearby subjects
        return mergeNearbySubjects(subjects)
    }

    /**
     * Calculate contrast in a region
     */
    private fun calculateRegionContrast(
        pixels: IntArray,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        imageWidth: Int
    ): Float {
        var minLum = 255
        var maxLum = 0

        for (dy in 0 until h step 2) {
            for (dx in 0 until w step 2) {
                val px = x + dx
                val py = y + dy
                if (px < imageWidth && py * imageWidth + px < pixels.size) {
                    val lum = getLuminance(pixels[py * imageWidth + px])
                    minLum = minOf(minLum, lum)
                    maxLum = maxOf(maxLum, lum)
                }
            }
        }

        return ((maxLum - minLum).toFloat() / 255f)
    }

    /**
     * Calculate detail/texture in a region
     */
    private fun calculateRegionDetail(
        pixels: IntArray,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        imageWidth: Int
    ): Float {
        var edgeCount = 0
        var totalPixels = 0

        for (dy in 1 until h - 1 step 2) {
            for (dx in 1 until w - 1 step 2) {
                val px = x + dx
                val py = y + dy

                if (px < imageWidth - 1 && py < pixels.size / imageWidth - 1) {
                    val center = getLuminance(pixels[py * imageWidth + px])
                    val right = getLuminance(pixels[py * imageWidth + px + 1])
                    val down = getLuminance(pixels[(py + 1) * imageWidth + px])

                    if (abs(center - right) > 20 || abs(center - down) > 20) {
                        edgeCount++
                    }
                    totalPixels++
                }
            }
        }

        return if (totalPixels > 0) (edgeCount.toFloat() / totalPixels) else 0f
    }

    /**
     * Merge nearby subject regions
     */
    private fun mergeNearbySubjects(subjects: List<SubjectRegion>): List<SubjectRegion> {
        if (subjects.size <= 1) return subjects

        val merged = mutableListOf<SubjectRegion>()
        val used = BooleanArray(subjects.size)

        for (i in subjects.indices) {
            if (used[i]) continue

            var currentBounds = subjects[i].bounds
            var totalImportance = subjects[i].importance
            var count = 1
            used[i] = true

            // Find nearby subjects
            for (j in i + 1 until subjects.size) {
                if (used[j]) continue

                if (RectF.intersects(currentBounds, subjects[j].bounds) ||
                    distance(subjects[i].center, subjects[j].center) < 100f
                ) {
                    currentBounds.union(subjects[j].bounds)
                    totalImportance += subjects[j].importance
                    count++
                    used[j] = true
                }
            }

            merged.add(
                SubjectRegion(
                    center = PointF(
                        currentBounds.centerX(),
                        currentBounds.centerY()
                    ),
                    bounds = currentBounds,
                    importance = totalImportance / count
                )
            )
        }

        return merged.sortedByDescending { it.importance }
    }

    /**
     * Score based on Rule of Thirds
     */
    private fun scoreRuleOfThirds(subjects: List<SubjectRegion>, width: Int, height: Int): Float {
        if (subjects.isEmpty()) return 50f

        val thirdX1 = width / 3f
        val thirdX2 = 2 * width / 3f
        val thirdY1 = height / 3f
        val thirdY2 = 2 * height / 3f

        // Power points (intersections of thirds)
        val powerPoints = listOf(
            PointF(thirdX1, thirdY1),
            PointF(thirdX2, thirdY1),
            PointF(thirdX1, thirdY2),
            PointF(thirdX2, thirdY2)
        )

        // Find closest distance from main subject to any power point
        val mainSubject = subjects.first()
        val minDistance = powerPoints.minOf { distance(mainSubject.center, it) }

        // Maximum possible distance (corner to corner)
        val maxDistance = sqrt((width * width + height * height).toFloat())

        // Closer to power point = higher score
        val proximityScore = (1f - (minDistance / maxDistance)) * 100f

        return proximityScore
    }

    /**
     * Score based on Golden Ratio
     */
    private fun scoreGoldenRatio(subjects: List<SubjectRegion>, width: Int, height: Int): Float {
        if (subjects.isEmpty()) return 50f

        val phi = 1.618f

        // Golden ratio points
        val goldenX1 = width / phi
        val goldenX2 = width - goldenX1
        val goldenY1 = height / phi
        val goldenY2 = height - goldenY1

        val goldenPoints = listOf(
            PointF(goldenX1, goldenY1),
            PointF(goldenX2, goldenY1),
            PointF(goldenX1, goldenY2),
            PointF(goldenX2, goldenY2)
        )

        val mainSubject = subjects.first()
        val minDistance = goldenPoints.minOf { distance(mainSubject.center, it) }
        val maxDistance = sqrt((width * width + height * height).toFloat())

        return (1f - (minDistance / maxDistance)) * 100f
    }

    /**
     * Analyze visual balance
     */
    private fun analyzeBalance(pixels: IntArray, width: Int, height: Int): Float {
        // Calculate "visual weight" on left vs right
        var leftWeight = 0.0
        var rightWeight = 0.0

        val centerX = width / 2

        for (y in 0 until height step 4) {
            for (x in 0 until width step 4) {
                val weight = getLuminance(pixels[y * width + x]) / 255.0

                if (x < centerX) {
                    leftWeight += weight
                } else {
                    rightWeight += weight
                }
            }
        }

        val totalWeight = leftWeight + rightWeight
        if (totalWeight == 0.0) return 50f

        val balance = minOf(leftWeight, rightWeight) / totalWeight

        // Perfect balance = 0.5, convert to 0-100 score
        return ((balance / 0.5) * 100f).coerceIn(0f, 100f)
    }

    /**
     * Analyze symmetry
     */
    private fun analyzeSymmetry(pixels: IntArray, width: Int, height: Int): Float {
        var symmetryScore = 0.0
        var totalPixels = 0

        val centerX = width / 2

        // Compare left and right halves
        for (y in 0 until height step 4) {
            for (x in 0 until centerX step 4) {
                val leftPixel = pixels[y * width + x]
                val rightPixel = pixels[y * width + (width - 1 - x)]

                val leftLum = getLuminance(leftPixel)
                val rightLum = getLuminance(rightPixel)

                val similarity = 1f - (abs(leftLum - rightLum) / 255f)
                symmetryScore += similarity
                totalPixels++
            }
        }

        return if (totalPixels > 0) {
            ((symmetryScore / totalPixels) * 100f).toFloat()
        } else {
            50f
        }
    }

    /**
     * Analyze negative space
     */
    private fun analyzeNegativeSpace(pixels: IntArray, width: Int, height: Int): Float {
        // Negative space = areas with low detail
        var emptyArea = 0
        var totalArea = 0

        for (y in 0 until height step 8) {
            for (x in 0 until width step 8) {
                val detail = calculateRegionDetail(pixels, x, y, 8, 8, width)

                if (detail < 0.1f) {
                    emptyArea++
                }
                totalArea++
            }
        }

        val negativeSpaceRatio = emptyArea.toFloat() / totalArea

        // Ideal negative space: 20-40%
        return when {
            negativeSpaceRatio < 0.1f -> 30f  // Too cluttered
            negativeSpaceRatio in 0.2f..0.4f -> 100f  // Perfect
            negativeSpaceRatio > 0.6f -> 40f  // Too empty
            else -> 70f  // Acceptable
        }
    }

    /**
     * Detect lines (leading lines)
     */
    private fun detectLines(pixels: IntArray, width: Int, height: Int): List<Line> {
        // Simplified line detection
        // In production, use Hough transform
        return emptyList()
    }

    /**
     * Score leading lines
     */
    private fun scoreLeadingLines(lines: List<Line>, subjects: List<SubjectRegion>): Float {
        // Placeholder - would analyze if lines lead to subjects
        return 50f
    }

    /**
     * Generate composition suggestions
     */
    private fun generateSuggestions(
        overallScore: Float,
        ruleOfThirdsScore: Float,
        balanceScore: Float,
        subjects: List<SubjectRegion>,
        width: Int,
        height: Int
    ): List<String> {
        val suggestions = mutableListOf<String>()

        if (overallScore < 40f) {
            suggestions.add("Consider repositioning your main subject")
        }

        if (ruleOfThirdsScore < 50f && subjects.isNotEmpty()) {
            val mainSubject = subjects.first()
            val thirdX = width / 3f
            val thirdY = height / 3f

            when {
                mainSubject.center.x < width / 2 -> suggestions.add("Move subject slightly right")
                else -> suggestions.add("Move subject slightly left")
            }

            when {
                mainSubject.center.y < height / 2 -> suggestions.add("Lower the camera angle slightly")
                else -> suggestions.add("Raise the camera angle slightly")
            }
        }

        if (balanceScore < 40f) {
            suggestions.add("Try balancing elements on both sides")
        }

        if (overallScore >= 80f) {
            suggestions.add("Excellent composition! Ready to shoot.")
        }

        return suggestions
    }

    /**
     * Helper: Calculate distance between two points
     */
    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Helper: Get luminance from pixel
     */
    private fun getLuminance(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }
}

/**
 * Subject region in image
 */
data class SubjectRegion(
    val center: PointF,
    val bounds: RectF,
    val importance: Float
)

/**
 * Line in image
 */
data class Line(
    val start: PointF,
    val end: PointF,
    val strength: Float
)

/**
 * Composition analysis result
 */
data class CompositionAnalysis(
    val overallScore: Int,              // 0-100
    val ruleOfThirdsScore: Int,         // 0-100
    val goldenRatioScore: Int,          // 0-100
    val balanceScore: Int,              // 0-100
    val symmetryScore: Int,             // 0-100
    val leadingLinesScore: Int,         // 0-100
    val negativeSpaceScore: Int,        // 0-100
    val subjects: List<SubjectRegion>,
    val suggestions: List<String>
) {
    val rating: String
        get() = when (overallScore) {
            in 90..100 -> "Excellent"
            in 75..89 -> "Great"
            in 60..74 -> "Good"
            in 40..59 -> "Fair"
            else -> "Needs Improvement"
        }

    companion object {
        fun empty() = CompositionAnalysis(
            overallScore = 0,
            ruleOfThirdsScore = 0,
            goldenRatioScore = 0,
            balanceScore = 0,
            symmetryScore = 0,
            leadingLinesScore = 0,
            negativeSpaceScore = 0,
            subjects = emptyList(),
            suggestions = emptyList()
        )
    }
}

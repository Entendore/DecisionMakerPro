package com.decisionpro.app.domain.engine.matrix

import kotlin.random.Random

data class Criterion(
    val id: String,
    val name: String,
    val weight: Float = 5f
) {
    companion object {
        fun create(name: String, weight: Float = 5f): Criterion =
            Criterion(id = newId(), name = name, weight = weight)
        internal fun newId(): String =
            java.util.UUID.randomUUID().toString().take(8)
    }
}

data class DecisionOption(
    val id: String,
    val name: String,
    val scores: Map<String, Float> = emptyMap()
) {
    companion object {
        fun create(name: String): DecisionOption =
            DecisionOption(id = Criterion.newId(), name = name)
    }
}

data class MatrixResult(
    val optionId: String,
    val optionName: String,
    val weightedScore: Float,
    val normalizedScore: Float,
    val breakdown: Map<String, Float>
)

data class SensitivityResult(
    val criterionId: String,
    val criterionName: String,
    val sensitivityScore: Float,
    val isCritical: Boolean
)

/** Pure engine: same input → same output. Random injected for tests. */
class WeightedMatrixEngine(
    private val random: Random = Random.Default
) {

    fun evaluate(
        options: List<DecisionOption>,
        criteria: List<Criterion>
    ): List<MatrixResult> {
        if (options.isEmpty() || criteria.isEmpty()) return emptyList()

        val totalWeight = criteria.sumOf { it.weight.toDouble() }.toFloat()
        if (totalWeight <= 0f) return emptyList()

        val maxPossible = totalWeight * MAX_SCORE

        return options.map { option ->
            val breakdown = criteria.associate { criterion ->
                val raw = (option.scores[criterion.id] ?: 0f).coerceIn(0f, MAX_SCORE)
                criterion.name to raw * criterion.weight
            }
            val total = breakdown.values.sum()
            MatrixResult(
                optionId = option.id,
                optionName = option.name,
                weightedScore = total,
                normalizedScore = (total / maxPossible) * 100f,
                breakdown = breakdown
            )
        }.sortedByDescending { it.weightedScore }
    }

    fun sensitivityAnalysis(
        options: List<DecisionOption>,
        criteria: List<Criterion>,
        baseline: List<MatrixResult>
    ): List<SensitivityResult> {
        if (baseline.size < 2 || criteria.isEmpty()) return emptyList()
        val baselineWinner = baseline.first().optionId

        return criteria.map { criterion ->
            var flips = 0
            for (factor in listOf(0.8f, 1.2f)) {
                val perturbed = criteria.map {
                    if (it.id == criterion.id) it.copy(weight = it.weight * factor) else it
                }
                val result = evaluate(options, perturbed)
                if (result.firstOrNull()?.optionId != baselineWinner) flips++
            }
            SensitivityResult(
                criterionId = criterion.id,
                criterionName = criterion.name,
                sensitivityScore = flips / 2f,
                isCritical = flips > 0
            )
        }
    }

    fun confidence(
        results: List<MatrixResult>,
        sensitivity: List<SensitivityResult>
    ): Int {
        if (results.size < 2) return DEFAULT_CONFIDENCE

        val gap = results[0].normalizedScore - results[1].normalizedScore
        val gapScore = (gap / GAP_FULL_CONFIDENCE).coerceIn(0f, 1f)

        val stability = if (sensitivity.isEmpty()) 1f
        else 1f - sensitivity.count { it.isCritical }.toFloat() / sensitivity.size

        return ((gapScore * GAP_WEIGHT + stability * STABILITY_WEIGHT) * 100f)
            .toInt().coerceIn(0, 100)
    }

    companion object {
        const val MAX_SCORE = 10f
        const val GAP_FULL_CONFIDENCE = 25f
        const val GAP_WEIGHT = 0.6f
        const val STABILITY_WEIGHT = 0.4f
        const val DEFAULT_CONFIDENCE = 50
        const val MAX_CRITERIA = 7
        const val MAX_OPTIONS = 8
    }
}

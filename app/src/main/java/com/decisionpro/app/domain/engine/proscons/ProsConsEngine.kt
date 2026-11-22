package com.decisionpro.app.domain.engine.proscons

enum class ImpactLevel(val multiplier: Float, val label: String) {
    LOW(1f, "Minor"),
    MEDIUM(2f, "Notable"),
    HIGH(3f, "Major"),
    CRITICAL(4f, "Deal-breaker level")
}

data class Argument(
    val text: String,
    val isPro: Boolean,
    val impact: ImpactLevel = ImpactLevel.MEDIUM,
    val category: String? = null
)

data class ProsConsResult(
    val proScore: Float,
    val conScore: Float,
    val netScore: Float,
    val netPercentage: Int,
    val recommendation: Recommendation,
    val breakdown: Map<String, Float>,
    val strongestPro: Argument?,
    val strongestCon: Argument?
)

enum class Recommendation(val headline: String, val emoji: String) {
    STRONG_YES("Strong yes — pros clearly dominate", "✅"),
    LEAN_YES("Lean yes — but review the top cons", "👍"),
    TOO_CLOSE("Too close to call — gather more information", "⚖️"),
    LEAN_NO("Lean no — check if top pros address your real needs", "👎"),
    STRONG_NO("Strong no — cons clearly dominate", "❌"),
    NOT_ENOUGH_INPUT("Add arguments to get an analysis", "📝")
}

class ProsConsEngine {

    fun analyze(arguments: List<Argument>): ProsConsResult {
        if (arguments.isEmpty()) {
            return ProsConsResult(
                proScore = 0f, conScore = 0f, netScore = 0f, netPercentage = 0,
                recommendation = Recommendation.NOT_ENOUGH_INPUT,
                breakdown = emptyMap(), strongestPro = null, strongestCon = null
            )
        }

        val pros = arguments.filter { it.isPro }
        val cons = arguments.filterNot { it.isPro }

        val proScore = pros.sumOf { it.impact.multiplier.toDouble() }.toFloat()
        val conScore = cons.sumOf { it.impact.multiplier.toDouble() }.toFloat()
        val netScore = proScore - conScore
        val totalScore = proScore + conScore

        val netPercentage =
            if (totalScore == 0f) 0
            else ((netScore / totalScore) * 100).toInt().coerceIn(-100, 100)

        val breakdown = arguments
            .filter { it.category != null }
            .groupBy { it.category!! }
            .mapValues { (_, args) ->
                args.sumOf {
                    ((if (it.isPro) 1 else -1) * it.impact.multiplier).toDouble()
                }.toFloat()
            }

        val recommendation = when {
            totalScore == 0f -> Recommendation.NOT_ENOUGH_INPUT
            netPercentage >= STRONG_THRESHOLD -> Recommendation.STRONG_YES
            netPercentage > 0 -> Recommendation.LEAN_YES
            netPercentage > -STRONG_THRESHOLD -> Recommendation.LEAN_NO
            else -> Recommendation.STRONG_NO
        }

        return ProsConsResult(
            proScore = proScore, conScore = conScore, netScore = netScore,
            netPercentage = netPercentage, recommendation = recommendation,
            breakdown = breakdown,
            strongestPro = pros.maxByOrNull { it.impact.multiplier },
            strongestCon = cons.maxByOrNull { it.impact.multiplier }
        )
    }

    companion object { const val STRONG_THRESHOLD = 30 }
}

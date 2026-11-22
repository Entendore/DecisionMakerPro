package com.decisionpro.app.domain.engine.random

import kotlin.random.Random

/** Probability-curved 8-ball: ~60% positive, ~25% non-committal, ~15% negative. */
class Magic8Ball(private val random: Random = Random.Default) {

    private val answers: List<Pair<String, Float>> = listOf(
        "It is certain." to 0.15f,
        "It is decidedly so." to 0.12f,
        "Without a doubt." to 0.10f,
        "Yes — definitely." to 0.08f,
        "You may rely on it." to 0.06f,
        "As I see it, yes." to 0.05f,
        "Most likely." to 0.04f,
        "Ask again later." to 0.08f,
        "Cannot predict now." to 0.06f,
        "Concentrate and ask." to 0.06f,
        "Reply hazy, try again." to 0.05f,
        "Don't count on it." to 0.06f,
        "My reply is no." to 0.05f,
        "Very doubtful." to 0.04f
    )

    fun shake(): String {
        val roll = random.nextFloat()
        var cumulative = 0f
        for ((answer, probability) in answers) {
            cumulative += probability
            if (roll < cumulative) return answer
        }
        return answers.last().first
    }

    fun totalProbability(): Float = answers.sumOf { it.second.toDouble() }.toFloat()
}

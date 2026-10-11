package com.decisionpro.app.domain.engine.random

import kotlin.random.Random

class RandomTools(private val random: Random = Random.Default) {

    sealed class CoinResult(val label: String) {
        data object Heads : CoinResult("Heads")
        data object Tails : CoinResult("Tails")
    }

    fun flipCoin(): CoinResult =
        if (random.nextBoolean()) CoinResult.Heads else CoinResult.Tails

    fun rollDice(sides: Int = 6): Int {
        require(sides in MIN_DIE..MAX_DIE) { "sides must be $MIN_DIE..$MAX_DIE" }
        return random.nextInt(1, sides + 1)
    }

    fun <T> pickOne(items: List<T>): T? = items.randomOrNull(random)

    fun <T> pickMany(items: List<T>, count: Int): List<T> =
        if (count <= 0) emptyList()
        else items.shuffled(random).take(count.coerceAtMost(items.size))

    companion object {
        const val MIN_DIE = 2
        const val MAX_DIE = 20
    }
}

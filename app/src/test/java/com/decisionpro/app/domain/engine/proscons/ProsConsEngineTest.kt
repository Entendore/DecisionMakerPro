package com.decisionpro.app.domain.engine.proscons

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProsConsEngineTest {

    private val engine = ProsConsEngine()

    @Test
    fun `empty arguments yield NOT_ENOUGH_INPUT`() {
        assertThat(engine.analyze(emptyList()).recommendation)
            .isEqualTo(Recommendation.NOT_ENOUGH_INPUT)
    }

    @Test
    fun `critical pro outweighs several low cons`() {
        val result = engine.analyze(
            listOf(
                Argument("Doubles salary", true, ImpactLevel.CRITICAL),
                Argument("Longer commute", false, ImpactLevel.LOW),
                Argument("Worse weather", false, ImpactLevel.LOW),
                Argument("New team to learn", false, ImpactLevel.LOW)
            )
        )
        assertThat(result.netScore).isEqualTo(1f)
        assertThat(result.recommendation).isEqualTo(Recommendation.LEAN_YES)
    }

    @Test
    fun `strong threshold produces STRONG_YES`() {
        val result = engine.analyze(
            listOf(
                Argument("P1", true, ImpactLevel.CRITICAL),
                Argument("P2", true, ImpactLevel.HIGH),
                Argument("C1", false, ImpactLevel.LOW)
            )
        )
        assertThat(result.recommendation).isEqualTo(Recommendation.STRONG_YES)
    }

    @Test
    fun `balanced arguments yield TOO_CLOSE`() {
        val result = engine.analyze(
            listOf(
                Argument("P", true, ImpactLevel.HIGH),
                Argument("C", false, ImpactLevel.HIGH)
            )
        )
        assertThat(result.recommendation).isEqualTo(Recommendation.TOO_CLOSE)
    }

    @Test
    fun `category breakdown sums signed contributions`() {
        val result = engine.analyze(
            listOf(
                Argument("Save money", true, ImpactLevel.HIGH, category = "money"),
                Argument("Hidden fees", false, ImpactLevel.MEDIUM, category = "money"),
                Argument("Exercise", true, ImpactLevel.LOW, category = "health")
            )
        )
        assertThat(result.breakdown["money"]).isWithin(0.01f).of(1f)
        assertThat(result.breakdown["health"]).isWithin(0.01f).of(1f)
    }

    @Test
    fun `strongest pro and con identified by multiplier`() {
        val weakPro = Argument("Weak", true, ImpactLevel.LOW)
        val strongPro = Argument("Strong", true, ImpactLevel.CRITICAL)
        val con = Argument("Con", false, ImpactLevel.MEDIUM)
        val result = engine.analyze(listOf(weakPro, strongPro, con))
        assertThat(result.strongestPro).isEqualTo(strongPro)
        assertThat(result.strongestCon).isEqualTo(con)
    }
}

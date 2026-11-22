package com.decisionpro.app.domain.engine.matrix

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class WeightedMatrixEngineTest {

    private lateinit var engine: WeightedMatrixEngine

    @Before fun setUp() { engine = WeightedMatrixEngine() }

    private fun criterion(name: String, weight: Float) = Criterion.create(name, weight)

    private fun option(name: String, vararg scores: Pair<Criterion, Float>): DecisionOption {
        val opt = DecisionOption.create(name)
        return opt.copy(scores = scores.associate { (c, s) -> c.id to s })
    }

    @Test
    fun `ranks options by descending weighted score`() {
        val cost = criterion("Cost", 5f)
        val quality = criterion("Quality", 10f)
        val a = option("A", cost to 3f, quality to 9f)
        val b = option("B", cost to 9f, quality to 4f)

        val results = engine.evaluate(listOf(a, b), listOf(cost, quality))

        assertThat(results.map { it.optionName }).containsExactly("A", "B").inOrder()
        assertThat(results.zipWithNext().all { (x, y) -> x.weightedScore >= y.weightedScore })
            .isTrue()
    }

    @Test
    fun `normalized scores are percentage of maximum`() {
        val only = criterion("Only", 5f)
        val results = engine.evaluate(
            listOf(option("A", only to 6f), option("B", only to 4f)),
            listOf(only)
        )
        assertThat(results[0].normalizedScore).isWithin(0.01f).of(60f)
        assertThat(results[1].normalizedScore).isWithin(0.01f).of(40f)
    }

    @Test
    fun `empty inputs yield empty results - never throws`() {
        assertThat(engine.evaluate(emptyList(), emptyList())).isEmpty()
        val c = criterion("C", 5f)
        assertThat(engine.evaluate(emptyList(), listOf(c))).isEmpty()
        assertThat(engine.evaluate(listOf(option("A")), emptyList())).isEmpty()
    }

    @Test
    fun `all-zero weights yield empty results - division guard`() {
        val c = criterion("C", 0f)
        assertThat(engine.evaluate(listOf(option("A", c to 5f)), listOf(c))).isEmpty()
    }

    @Test
    fun `missing score contributes zero`() {
        val scored = criterion("Scored", 1f)
        val unscored = criterion("Unscored", 1f)
        val results = engine.evaluate(
            listOf(option("A", scored to 10f)), listOf(scored, unscored)
        )
        assertThat(results[0].weightedScore).isWithin(0.01f).of(10f)
    }

    @Test
    fun `out-of-range scores are clamped`() {
        val c = criterion("C", 1f)
        val a = DecisionOption("1", "A", mapOf(c.id to 99f))
        val results = engine.evaluate(listOf(a), listOf(c))
        assertThat(results[0].weightedScore).isWithin(0.01f).of(10f)
    }

    @Test
    fun `sensitivity flags criterion that flips ranking`() {
        val deciding = criterion("Deciding", 5f)
        val neutral = criterion("Neutral", 5f)
        val a = option("A", deciding to 5.1f, neutral to 5.1f)
        val b = option("B", deciding to 4.9f, neutral to 4.9f)

        val baseline = engine.evaluate(listOf(a, b), listOf(deciding, neutral))
        val sensitivity = engine.sensitivityAnalysis(
            listOf(a, b), listOf(deciding, neutral), baseline
        )
        assertThat(sensitivity.first { it.criterionName == "Deciding" }.isCritical).isTrue()
    }

    @Test
    fun `confidence rises with score gap`() {
        val c = criterion("C", 5f)
        val results = engine.evaluate(
            listOf(option("A", c to 9f), option("B", c to 3f)), listOf(c)
        )
        assertThat(engine.confidence(results, emptyList())).isGreaterThan(70)
    }

    @Test
    fun `confidence falls when ranking is sensitive`() {
        val deciding = criterion("Deciding", 5f)
        val a = option("A", deciding to 5.1f)
        val b = option("B", deciding to 4.9f)
        val results = engine.evaluate(listOf(a, b), listOf(deciding))
        val sensitivity = engine.sensitivityAnalysis(listOf(a, b), listOf(deciding), results)
        assertThat(engine.confidence(results, sensitivity)).isLessThan(50)
    }
}

package com.decisionpro.app.domain.engine.random

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class RandomToolsTest {

    @Test
    fun `magic8ball probability curve sums to ~1`() {
        assertThat(Magic8Ball().totalProbability()).isWithin(0.01f).of(1f)
    }

    @Test
    fun `magic8ball is deterministic with seeded random`() {
        val a = Magic8Ball(Random(42))
        val b = Magic8Ball(Random(42))
        assertThat((1..20).map { a.shake() }).isEqualTo((1..20).map { b.shake() })
    }

    @Test
    fun `dice respects bounds across many rolls`() {
        val tools = RandomTools(Random(1))
        repeat(1000) {
            assertThat(tools.rollDice(6)).isIn(1..6)
            assertThat(tools.rollDice(20)).isIn(1..20)
        }
    }

    @Test
    fun `dice rejects invalid sides`() {
        try {
            RandomTools().rollDice(1)
            assertThat(false).isTrue()
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessageThat().contains("sides")
        }
    }

    @Test
    fun `pickMany returns unique items without replacement`() {
        val picked = RandomTools(Random(7)).pickMany(listOf("a", "b", "c", "d", "e"), 3)
        assertThat(picked).hasSize(3)
        assertThat(picked.distinct()).hasSize(3)
    }

    @Test
    fun `pickMany clamps count to list size`() {
        val tools = RandomTools()
        assertThat(tools.pickMany(listOf("x"), 5)).containsExactly("x")
        assertThat(tools.pickMany(emptyList(), 3)).isEmpty()
    }

    @Test
    fun `pickOne handles empty list`() {
        assertThat(RandomTools().pickOne(emptyList<String>())).isNull()
    }
}

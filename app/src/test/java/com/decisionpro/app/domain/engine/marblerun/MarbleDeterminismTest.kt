package com.decisionpro.app.domain.engine.marblerun

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The replay-share contract: same seed → bit-identical race. */
class MarbleDeterminismTest {

    private fun simulate(seed: Long, frames: Int = 600): List<Marble> {
        val race = RaceFactory.create(
            BoardPreset.PLINKO,
            listOf("Alice", "Bob", "Cara", "Dan"),
            400f, 700f, seed
        )
        val marbles = race.marbles.toMutableList()
        val counter = intArrayOf(1)
        repeat(frames) {
            MarblePhysics.step(marbles, race.board, 400f, 1f / 60f) { counter[0]++ }
        }
        return marbles
    }

    @Test
    fun `same seed produces bit-identical full race`() {
        val r1 = simulate(seed = 42)
        val r2 = simulate(seed = 42)
        assertThat(r1.map { it.finishRank }).isEqualTo(r2.map { it.finishRank })
        assertThat(r1.map { it.x }).isEqualTo(r2.map { it.x })
        assertThat(r1.map { it.y }).isEqualTo(r2.map { it.y })
    }

    @Test
    fun `same seed produces identical trajectory mid-race`() {
        fun midRace(seed: Long): List<Pair<Float, Float>> {
            val race = RaceFactory.create(
                BoardPreset.FUNNEL, listOf("A", "B", "C"), 400f, 700f, seed
            )
            val marbles = race.marbles.toMutableList()
            repeat(180) {
                MarblePhysics.step(marbles, race.board, 400f, 1f / 60f) { 1 }
            }
            return marbles.map { it.x to it.y }
        }
        assertThat(midRace(7L)).isEqualTo(midRace(7L))
    }

    @Test
    fun `different seeds diverge`() {
        val r1 = simulate(seed = 42)
        val r2 = simulate(seed = 1337)
        val same = r1.zip(r2).all { (a, b) -> a.x == b.x && a.y == b.y }
        assertThat(same).isFalse()
    }

    @Test
    fun `every preset is deterministic`() {
        BoardPreset.values().forEach { preset ->
            fun run(): List<Pair<Float, Float>> {
                val race = RaceFactory.create(
                    preset, listOf("X", "Y"), 400f, 700f, 99L
                )
                val marbles = race.marbles.toMutableList()
                repeat(300) {
                    MarblePhysics.step(marbles, race.board, 400f, 1f / 60f) { 1 }
                }
                return marbles.map { it.x to it.y }
            }
            assertThat(run()).isEqualTo(run())
        }
    }

    @Test
    fun `race terminates - all marbles finish with distinct ranks`() {
        val marbles = simulate(seed = 5L, frames = 60 * 20)
        assertThat(marbles.all { it.finished }).isTrue()
        assertThat(marbles.map { it.finishRank }.distinct()).hasSize(marbles.size)
    }
}

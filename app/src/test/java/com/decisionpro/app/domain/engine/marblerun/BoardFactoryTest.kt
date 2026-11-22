package com.decisionpro.app.domain.engine.marblerun

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BoardFactoryTest {

    private val width = 400f
    private val height = 700f

    @Test
    fun `same seed produces bit-identical board`() {
        val b1 = BoardFactory.build(BoardPreset.PLINKO, width, height, 4, 42L)
        val b2 = BoardFactory.build(BoardPreset.PLINKO, width, height, 4, 42L)
        assertThat(b1.pegs).isEqualTo(b2.pegs)
        assertThat(b1.walls).isEqualTo(b2.walls)
    }

    @Test
    fun `different seeds produce different boards`() {
        val b1 = BoardFactory.build(BoardPreset.CHAOS, width, height, 4, 1L)
        val b2 = BoardFactory.build(BoardPreset.CHAOS, width, height, 4, 2L)
        assertThat(b1.pegs).isNotEqualTo(b2.pegs)
    }

    @Test
    fun `plinko golden geometry - 44 pegs`() {
        val board = BoardFactory.build(BoardPreset.PLINKO, width, height, 4, 42L)
        assertThat(board.pegs).hasSize(44)
    }

    @Test
    fun `all pegs stay within bounds including jitter`() {
        BoardPreset.values().forEach { preset ->
            val board = BoardFactory.build(preset, width, height, 4, 77L)
            board.pegs.forEach { peg ->
                assertThat(peg.x).isAtLeast(-3f)
                assertThat(peg.x).isAtMost(width + 3f)
            }
        }
    }

    @Test
    fun `race marbles drop from evenly spaced slots`() {
        val race = RaceFactory.create(
            BoardPreset.PLINKO, listOf("A", "B", "C", "D"), width, height, 42L
        )
        val spacing = width / 5f
        assertThat(race.marbles.map { it.x })
            .containsExactly(spacing, spacing * 2, spacing * 3, spacing * 4).inOrder()
    }

    @Test
    fun `race refuses fewer than two options`() {
        assertThat(
            runCatching {
                RaceFactory.create(BoardPreset.PLINKO, listOf("Only"), width, height, 1L)
            }.isFailure
        ).isTrue()
    }
}

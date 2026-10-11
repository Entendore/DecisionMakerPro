package com.decisionpro.app.domain.engine.marblerun

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class MarblePhysicsTest {

    private fun marble(
        x: Float, y: Float, vx: Float = 0f, vy: Float = 0f, radius: Float = 18f
    ) = Marble(1, "T", 0xFFFF0000, x, y, vx, vy, radius)

    private fun emptyBoard(finishY: Float = 5000f) =
        Board(emptyList(), emptyList(), finishY)

    @Test
    fun `marble falls under gravity`() {
        val marbles = mutableListOf(marble(200f, 100f))
        repeat(60) { MarblePhysics.step(marbles, emptyBoard(), 400f, 1f / 60f) { 1 } }
        assertThat(marbles[0].y).isGreaterThan(100f)
        assertThat(marbles[0].vy).isGreaterThan(0f)
    }

    @Test
    fun `free-fall distance matches kinematics within tolerance`() {
        val marbles = mutableListOf(marble(200f, 0f))
        repeat(60) { MarblePhysics.step(marbles, emptyBoard(), 400f, 1f / 60f) { 1 } }
        assertThat(marbles[0].y).isWithin(40f).of(700f)
    }

    @Test
    fun `marble never overlaps a peg at any frame boundary`() {
        val peg = Peg(200f, 500f)
        val marbles = mutableListOf(marble(200f, 100f, vy = 2000f))
        val board = Board(listOf(peg), emptyList(), 5000f)
        val minDist = marbles[0].radius + peg.radius

        repeat(300) {
            MarblePhysics.step(marbles, board, 400f, 1f / 60f) { 1 }
            val d = sqrt(
                (marbles[0].x - peg.x).let { it * it } +
                    (marbles[0].y - peg.y).let { it * it }
            )
            assertThat(d).isAtLeast(minDist - 0.5f)
        }
    }

    @Test
    fun `marble respects side walls under extreme velocity`() {
        val marbles = mutableListOf(marble(200f, 100f, vx = -4000f, vy = 0f))
        repeat(120) { MarblePhysics.step(marbles, emptyBoard(), 400f, 1f / 60f) { 1 } }
        assertThat(marbles[0].x).isAtLeast(marbles[0].radius - 1f)
        assertThat(marbles[0].x).isAtMost(400f - marbles[0].radius + 1f)
    }

    @Test
    fun `marble collision conserves momentum along normal`() {
        val a = marble(100f, 200f, vx = 200f)
        val b = marble(130f, 200f, vx = 0f)
        val marbles = mutableListOf(a, b)
        val board = Board(emptyList(), emptyList(), 99999f)

        val pBefore = a.vx + b.vx
        MarblePhysics.step(marbles, board, 4000f, 0.005f) { 1 }

        assertThat(marbles[0].vx + marbles[1].vx).isWithin(1f).of(pBefore)
    }

    @Test
    fun `collision separates overlapping marbles`() {
        val a = marble(100f, 200f)
        val b = marble(112f, 200f)
        val marbles = mutableListOf(a, b)
        MarblePhysics.step(
            marbles, Board(emptyList(), emptyList(), 99999f), 4000f, 0.005f
        ) { 1 }
        assertThat(abs(marbles[0].x - marbles[1].x))
            .isAtLeast(a.radius + b.radius - 0.5f)
    }

    @Test
    fun `finish ranks assigned in crossing order`() {
        val fast = marble(100f, 900f, vy = 300f)
        val slow = marble(300f, 800f, vy = 50f)
        val marbles = mutableListOf(fast, slow)
        val board = emptyBoard(1000f)
        val counter = intArrayOf(1)

        repeat(120) {
            MarblePhysics.step(marbles, board, 400f, 1f / 60f) { counter[0]++ }
        }
        assertThat(fast.finishRank).isLessThan(slow.finishRank)
    }

    @Test
    fun `finished marbles are frozen`() {
        val m = marble(100f, 990f, vy = 500f)
        val marbles = mutableListOf(m)
        MarblePhysics.step(marbles, emptyBoard(1000f), 400f, 1f / 60f) { 1 }
        val frozenY = m.y
        repeat(60) { MarblePhysics.step(marbles, emptyBoard(1000f), 400f, 1f / 60f) { 2 } }
        assertThat(m.y).isEqualTo(frozenY)
    }

    @Test
    fun `NaN injection recovers deterministically`() {
        val m = marble(Float.NaN, 200f, vx = Float.NaN, vy = 0f)
        val marbles = mutableListOf(m)
        repeat(30) { MarblePhysics.step(marbles, emptyBoard(), 400f, 1f / 60f) { 1 } }
        assertThat(m.x.isNaN()).isFalse()
        assertThat(m.vx.isNaN()).isFalse()
    }

    @Test
    fun `marble bounces off wall segment endpoint`() {
        val wall = WallSegment(0f, 200f, 200f, 200f)
        val marbles = mutableListOf(marble(210f, 200f, vx = -300f))
        repeat(60) {
            MarblePhysics.step(
                marbles, Board(emptyList(), listOf(wall), 5000f), 400f, 1f / 60f
            ) { 1 }
        }
        assertThat(marbles[0].x).isAtLeast(0f)
    }
}

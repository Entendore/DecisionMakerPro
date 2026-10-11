package com.decisionpro.app.domain.engine.marblerun

import kotlin.random.Random

/**
 * One Random(seed) drives EVERYTHING (peg jitter included).
 * Same seed → bit-identical board → honest seed-shareable replays.
 */
enum class BoardPreset(val displayName: String, val emoji: String) {
    PLINKO("Classic Plinko", "🔻"),
    FUNNEL("Funnel Chaos", "🌀"),
    ZIGZAG("Zigzag Ramps", "⚡"),
    CHAOS("Random Chaos", "🎲")
}

object BoardFactory {

    fun build(
        preset: BoardPreset,
        width: Float,
        height: Float,
        optionCount: Int,
        seed: Long = System.currentTimeMillis()
    ): Board {
        val rng = Random(seed)
        val finishY = height * 0.92f

        val pegs = when (preset) {
            BoardPreset.PLINKO -> plinkoPegs(width, height, rng)
            BoardPreset.FUNNEL -> funnelPegs(width, height, rng)
            BoardPreset.ZIGZAG -> zigzagPegs(width, height, rng)
            BoardPreset.CHAOS -> chaosPegs(width, height, rng)
        }
        val walls = when (preset) {
            BoardPreset.FUNNEL -> funnelWalls(width, height)
            BoardPreset.ZIGZAG -> zigzagWalls(width, height)
            else -> emptyList()
        }
        return Board(pegs, walls, finishY)
    }

    /** Golden geometry: 8 rows × alternating (5,6) = 44 pegs. */
    private fun plinkoPegs(width: Float, height: Float, rng: Random): List<Peg> {
        val pegs = mutableListOf<Peg>()
        val rows = 8
        val topMargin = height * 0.12f
        val bottomMargin = height * 0.10f
        val rowSpacing = (height - topMargin - bottomMargin) / rows

        for (row in 0 until rows) {
            val cols = 5 + (row % 2)
            val spacing = width / (cols + 1)
            for (col in 1..cols) {
                pegs += Peg(
                    x = col * spacing + (if (row % 2 == 0) spacing / 2 else 0f) + jitter(rng, 2f),
                    y = topMargin + row * rowSpacing + jitter(rng, 2f)
                )
            }
        }
        return pegs
    }

    private fun funnelPegs(width: Float, height: Float, rng: Random): List<Peg> {
        val cx = width / 2f
        return List(45) { i ->
            val t = i / 45f
            val y = height * 0.12f + t * height * 0.68f
            val spread = width * (0.15f + t * 0.35f)
            Peg(cx + (rng.nextFloat() * 2f - 1f) * spread, y)
        }
    }

    private fun zigzagPegs(width: Float, height: Float, rng: Random): List<Peg> =
        List(30) {
            Peg(
                x = rng.nextFloat() * width,
                y = height * 0.12f + rng.nextFloat() * height * 0.68f
            )
        }

    private fun chaosPegs(width: Float, height: Float, rng: Random): List<Peg> =
        List(50) {
            Peg(
                x = rng.nextFloat() * width,
                y = height * 0.12f + rng.nextFloat() * height * 0.68f
            )
        }

    private fun funnelWalls(width: Float, height: Float) = listOf(
        WallSegment(0f, height * 0.05f, width * 0.28f, height * 0.55f),
        WallSegment(width, height * 0.05f, width * 0.72f, height * 0.55f)
    )

    private fun zigzagWalls(width: Float, height: Float): List<WallSegment> {
        val walls = mutableListOf<WallSegment>()
        for (i in 0 until 4) {
            val y = height * (0.18f + i * 0.17f)
            walls += if (i % 2 == 0)
                WallSegment(0f, y, width * 0.65f, y)
            else
                WallSegment(width * 0.35f, y, width, y)
        }
        return walls
    }

    private fun jitter(rng: Random, magnitude: Float): Float =
        (rng.nextFloat() * 2f - 1f) * magnitude
}

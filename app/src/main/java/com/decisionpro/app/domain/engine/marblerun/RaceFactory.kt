package com.decisionpro.app.domain.engine.marblerun

import kotlin.random.Random

/** ONE seed → board + marbles + nudges. Replay is bit-exact. */
object RaceFactory {

    data class Race(val board: Board, val marbles: List<Marble>)

    const val DROP_Y = 40f
    const val NUDGE_STRENGTH = 30f

    fun create(
        preset: BoardPreset,
        options: List<String>,
        width: Float,
        height: Float,
        seed: Long
    ): Race {
        require(options.size >= 2) { "A race needs at least 2 options" }

        val board = BoardFactory.build(preset, width, height, options.size, seed)
        val rng = Random(seed)

        val spacing = width / (options.size + 1)
        val marbles = options.mapIndexed { index, label ->
            Marble(
                id = index,
                label = label,
                color = MarblePalette.forIndex(index),
                x = spacing * (index + 1),
                y = DROP_Y,
                vx = (rng.nextFloat() - 0.5f) * NUDGE_STRENGTH,
                vy = 0f
            )
        }
        return Race(board, marbles)
    }
}

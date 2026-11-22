package com.decisionpro.app.domain.engine.marblerun

/** Chosen to differ in LIGHTNESS too, not just hue (color-blind friendly). */
object MarblePalette {
    val colors: List<Long> = listOf(
        0xFFEF5350, 0xFF42A5F5, 0xFF66BB6A, 0xFFFFCA28,
        0xFFAB47BC, 0xFFFF7043, 0xFF26C6DA, 0xFFEC407A
    )

    fun forIndex(index: Int): Long = colors[index.mod(colors.size)]
}

package com.decisionpro.app.domain.engine.marblerun

import kotlin.math.sqrt

data class Marble(
    val id: Int,
    val label: String,
    val color: Long,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val radius: Float = 18f,
    var finished: Boolean = false,
    var finishRank: Int = 0
) {
    val trail = MarbleTrail()
    fun speed(): Float = sqrt(vx * vx + vy * vy)
}

/** Ring buffer — zero allocation at 30Hz × 8 marbles. */
class MarbleTrail(private val capacity: Int = DEFAULT_CAPACITY) {
    private val xs = FloatArray(capacity)
    private val ys = FloatArray(capacity)
    private var head = 0
    var size = 0
        private set

    fun add(x: Float, y: Float) {
        xs[head] = x
        ys[head] = y
        head = (head + 1) % capacity
        if (size < capacity) size++
    }

    inline fun forEachIndexed(action: (index: Int, x: Float, y: Float) -> Unit) {
        for (i in 0 until size) {
            val idx = (head - size + i + capacity) % capacity
            action(i, xs[idx], ys[idx])
        }
    }

    fun clear() { head = 0; size = 0 }

    companion object { const val DEFAULT_CAPACITY = 25 }
}

data class Peg(val x: Float, val y: Float, val radius: Float = 7f)
data class WallSegment(val x1: Float, val y1: Float, val x2: Float, val y2: Float)
data class Board(
    val pegs: List<Peg>,
    val walls: List<WallSegment>,
    val finishLineY: Float
)

/**
 * Impulse-based engine.
 * 240Hz fixed sub-steps: max travel 10px/step < 25px min contact distance
 * → tunneling GEOMETRICALLY impossible. Semi-implicit Euler. Deterministic:
 * same state + same dt sequence → bit-identical outcome (tested).
 */
object MarblePhysics {

    const val GRAVITY = 1400f
    const val RESTITUTION = 0.55f
    const val WALL_RESTITUTION = 0.6f
    const val AIR_DRAG = 0.9985f
    const val PEG_FRICTION = 0.92f
    const val MAX_SPEED = 2400f
    const val FIXED_DT = 1f / 240f

    private const val MAX_FRAME_DT = 0.05f
    private const val EPSILON = 1e-4f

    fun step(
        marbles: MutableList<Marble>,
        board: Board,
        worldWidth: Float,
        frameDt: Float,
        nextRank: () -> Int
    ) {
        var remaining = frameDt.coerceIn(0f, MAX_FRAME_DT)
        while (remaining > 0f) {
            val dt = minOf(FIXED_DT, remaining)
            remaining -= dt
            subStep(marbles, board, worldWidth, dt, nextRank)
        }
    }

    private fun subStep(
        marbles: MutableList<Marble>,
        board: Board,
        worldWidth: Float,
        dt: Float,
        nextRank: () -> Int
    ) {
        for (m in marbles) {
            if (m.finished) continue

            recoverIfNan(m, worldWidth)

            m.vy += GRAVITY * dt
            m.vx *= AIR_DRAG
            m.vy *= AIR_DRAG
            clampSpeed(m)

            m.x += m.vx * dt
            m.y += m.vy * dt

            for (peg in board.pegs) {
                val dx = m.x - peg.x
                val dy = m.y - peg.y
                val minDist = m.radius + peg.radius
                val distSq = dx * dx + dy * dy
                if (distSq < minDist * minDist && distSq > EPSILON) {
                    val dist = sqrt(distSq)
                    val nx = dx / dist
                    val ny = dy / dist
                    m.x = peg.x + nx * minDist
                    m.y = peg.y + ny * minDist
                    val dot = m.vx * nx + m.vy * ny
                    if (dot < 0f) {
                        m.vx -= (1f + RESTITUTION) * dot * nx
                        m.vy -= (1f + RESTITUTION) * dot * ny
                        m.vx *= PEG_FRICTION
                        m.vy *= PEG_FRICTION
                    }
                }
            }

            for (wall in board.walls) resolveWall(m, wall)

            if (m.x < m.radius) {
                m.x = m.radius
                m.vx = -m.vx * WALL_RESTITUTION
            } else if (m.x > worldWidth - m.radius) {
                m.x = worldWidth - m.radius
                m.vx = -m.vx * WALL_RESTITUTION
            }

            if (m.y + m.radius >= board.finishLineY) {
                m.finished = true
                m.finishRank = nextRank()
            }
        }

        for (i in marbles.indices) {
            for (j in i + 1 until marbles.size) {
                collideMarbles(marbles[i], marbles[j])
            }
        }
    }

    private fun collideMarbles(a: Marble, b: Marble) {
        if (a.finished || b.finished) return

        val dx = b.x - a.x
        val dy = b.y - a.y
        val minDist = a.radius + b.radius
        val distSq = dx * dx + dy * dy
        if (distSq >= minDist * minDist || distSq < EPSILON) return

        val dist = sqrt(distSq)
        val nx = dx / dist
        val ny = dy / dist
        val overlap = (minDist - dist) / 2f

        a.x -= nx * overlap; a.y -= ny * overlap
        b.x += nx * overlap; b.y += ny * overlap

        val relVel = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
        if (relVel < 0f) {
            val impulse = -(1f + RESTITUTION) * relVel / 2f
            a.vx -= impulse * nx; a.vy -= impulse * ny
            b.vx += impulse * nx; b.vy += impulse * ny
        }
    }

    private fun resolveWall(m: Marble, w: WallSegment) {
        val ex = w.x2 - w.x1
        val ey = w.y2 - w.y1
        val lenSq = ex * ex + ey * ey
        if (lenSq < EPSILON) return

        var t = ((m.x - w.x1) * ex + (m.y - w.y1) * ey) / lenSq
        t = t.coerceIn(0f, 1f)
        val cx = w.x1 + t * ex
        val cy = w.y1 + t * ey

        val dx = m.x - cx
        val dy = m.y - cy
        val distSq = dx * dx + dy * dy
        if (distSq >= m.radius * m.radius || distSq < EPSILON) return

        val dist = sqrt(distSq)
        val nx = dx / dist
        val ny = dy / dist

        m.x = cx + nx * m.radius
        m.y = cy + ny * m.radius

        val dot = m.vx * nx + m.vy * ny
        if (dot < 0f) {
            m.vx -= (1f + WALL_RESTITUTION) * dot * nx
            m.vy -= (1f + WALL_RESTITUTION) * dot * ny
        }
    }

    private fun clampSpeed(m: Marble) {
        val speed = m.speed()
        if (speed > MAX_SPEED) {
            val scale = MAX_SPEED / speed
            m.vx *= scale
            m.vy *= scale
        }
    }

    private fun recoverIfNan(m: Marble, worldWidth: Float) {
        if (m.x.isNaN()) m.x = worldWidth / 2f
        if (m.y.isNaN()) m.y = 0f
        if (m.vx.isNaN() || m.vy.isNaN()) { m.vx = 0f; m.vy = 0f }
    }
}

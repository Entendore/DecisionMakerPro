package com.decisionpro.app.domain.engine.marblerun

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Physics on a dedicated thread → publishes immutable RenderFrame
 * snapshots. The UI NEVER touches mutable physics state — torn reads
 * are impossible by construction.
 */
class PhysicsOrchestrator(
    private val physicsDispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    data class TrailPoint(val x: Float, val y: Float)

    data class MarbleSnapshot(
        val id: Int,
        val x: Float,
        val y: Float,
        val finished: Boolean,
        val rank: Int,
        val trail: List<TrailPoint>
    )

    data class RenderFrame(
        val marbles: List<MarbleSnapshot>,
        val finishedCount: Int,
        val total: Int
    )

    private val _frame = MutableStateFlow<RenderFrame?>(null)
    val frame: StateFlow<RenderFrame?> = _frame.asStateFlow()

    private val _raceComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val raceComplete: SharedFlow<Unit> = _raceComplete.asSharedFlow()

    private var job: Job? = null
    val isRunning: Boolean get() = job?.isActive == true

    fun start(
        marbles: MutableList<Marble>,
        board: Board,
        worldWidth: Float,
        scope: CoroutineScope
    ) {
        stop()   // never two loops (recomposition / rapid restarts)
        var rank = 1

        job = scope.launch(physicsDispatcher) {
            var lastNanos = System.nanoTime()

            while (isActive) {
                val now = System.nanoTime()
                val dt = ((now - lastNanos) / 1_000_000_000f)
                    .coerceIn(MarblePhysics.FIXED_DT, 0.05f)
                lastNanos = now

                MarblePhysics.step(marbles, board, worldWidth, dt) { rank++ }
                marbles.forEach { if (!it.finished) it.trail.add(it.x, it.y) }

                _frame.value = snapshot(marbles)

                if (marbles.isNotEmpty() && marbles.all { it.finished }) {
                    _raceComplete.tryEmit(Unit)
                    break
                }

                delay(TARGET_FRAME_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun snapshot(marbles: List<Marble>): RenderFrame = RenderFrame(
        marbles = marbles.map { m ->
            val pts = ArrayList<TrailPoint>(m.trail.size)
            m.trail.forEachIndexed { _, x, y -> pts += TrailPoint(x, y) }
            MarbleSnapshot(m.id, m.x, m.y, m.finished, m.finishRank, pts)
        },
        finishedCount = marbles.count { it.finished },
        total = marbles.size
    )

    companion object { private const val TARGET_FRAME_MS = 16L }
}

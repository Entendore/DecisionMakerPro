package com.decisionpro.app.ui.screens.marblerun

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionpro.app.data.repository.DecisionRepository
import com.decisionpro.app.domain.engine.marblerun.BoardPreset
import com.decisionpro.app.domain.engine.marblerun.PhysicsOrchestrator
import com.decisionpro.app.domain.engine.marblerun.RaceFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Race machine: SETUP → COUNTDOWN → RACING → FINISHED.
 * Persistence rule: inputs yes (options/preset/rounds), animation frames no.
 */
@HiltViewModel
class MarbleRunViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: DecisionRepository
) : ViewModel() {

    private val orchestrator = PhysicsOrchestrator()

    companion object {
        private const val KEY_OPTIONS = "options"
        private const val KEY_PRESET = "preset"
        private const val KEY_BEST_OF = "best_of_three"
        private const val KEY_ROUND = "round"
        private const val KEY_WINS = "round_wins"
    }

    enum class Phase { SETUP, COUNTDOWN, RACING, FINISHED }

    data class UiState(
        val phase: Phase = Phase.SETUP,
        val options: List<String> = listOf("Option A", "Option B", "Option C"),
        val preset: BoardPreset = BoardPreset.PLINKO,
        val bestOfThree: Boolean = false,
        val round: Int = 1,
        val roundWins: Map<String, Int> = emptyMap(),
        val countdown: Int = 3,
        val winner: String? = null,
        val rankings: List<Pair<String, Int>> = emptyList(),
        val seed: Long = 0L,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(restore() ?: UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val renderFrame: StateFlow<PhysicsOrchestrator.RenderFrame?>
        get() = orchestrator.frame

    private var race: RaceFactory.Race? = null
    private var raceWidth = 0f
    private var raceHeight = 0f
    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            orchestrator.raceComplete.collect { onRaceComplete() }
        }
    }

    fun updateOptions(options: List<String>) {
        _uiState.update {
            it.copy(options = options.map { o -> o.trim() }.filter { o -> o.isNotEmpty() })
        }
        persist()
    }

    fun setPreset(preset: BoardPreset) {
        _uiState.update { it.copy(preset = preset) }
        persist()
    }

    fun toggleBestOfThree() {
        _uiState.update {
            it.copy(
                bestOfThree = !it.bestOfThree,
                round = 1, roundWins = emptyMap(),
                winner = null, rankings = emptyList()
            )
        }
        persist()
    }

    fun startRace(width: Float, height: Float) {
        val s = _uiState.value
        if (s.options.size < 2) {
            _uiState.update { it.copy(error = "Add at least 2 options") }
            return
        }

        stopEverything()

        val seed = System.currentTimeMillis()
        raceWidth = width
        raceHeight = height
        race = RaceFactory.create(s.preset, s.options, width, height, seed)

        _uiState.update {
            it.copy(
                phase = Phase.COUNTDOWN, countdown = 3, seed = seed,
                winner = null, rankings = emptyList(), error = null
            )
        }

        countdownJob = viewModelScope.launch {
            for (count in 3 downTo 1) {
                _uiState.update { it.copy(countdown = count) }
                delay(700)
            }
            beginRacing()
        }
    }

    private fun beginRacing() {
        val r = race ?: return
        _uiState.update { it.copy(phase = Phase.RACING) }
        orchestrator.start(r.marbles.toMutableList(), r.board, raceWidth, viewModelScope)
    }

    fun pause() {
        if (_uiState.value.phase == Phase.RACING) orchestrator.stop()
    }

    fun resume() {
        val r = race ?: return
        if (_uiState.value.phase == Phase.RACING && !orchestrator.isRunning) {
            orchestrator.start(r.marbles, r.board, raceWidth, viewModelScope)
        }
    }

    fun reset() {
        stopEverything()
        _uiState.update {
            it.copy(phase = Phase.SETUP, winner = null, rankings = emptyList())
        }
    }

    private fun stopEverything() {
        countdownJob?.cancel()
        countdownJob = null
        orchestrator.stop()
        race = null
    }

    private fun onRaceComplete() {
        val r = race ?: return
        val s = _uiState.value

        val ranked = r.marbles.sortedBy { it.finishRank }
        val raceWinner = ranked.first().label
        val rankings = ranked.map { it.label to it.finishRank }

        if (s.bestOfThree && s.round < 3) {
            val wins = s.roundWins.toMutableMap()
            wins[raceWinner] = (wins[raceWinner] ?: 0) + 1
            _uiState.update {
                it.copy(
                    roundWins = wins, round = s.round + 1,
                    phase = Phase.FINISHED,
                    winner = raceWinner, rankings = rankings
                )
            }
            viewModelScope.launch {
                delay(1800)
                if (_uiState.value.phase == Phase.FINISHED && race != null) {
                    startRace(raceWidth, raceHeight)
                }
            }
            return
        }

        val finalWinner = if (!s.bestOfThree) raceWinner else {
            val topWins = s.roundWins.values.maxOrNull() ?: 0
            val leaders = s.roundWins.filterValues { it == topWins }.keys
            if (leaders.size == 1) leaders.first() else raceWinner
        }

        _uiState.update {
            it.copy(phase = Phase.FINISHED, winner = finalWinner, rankings = rankings)
        }

        viewModelScope.launch {
            repository.saveDecision(
                question = "Marble run (${s.preset.displayName}" +
                        if (s.bestOfThree) ", best of 3)" else ")",
                choice = finalWinner,
                type = "marble_run",
                confidence = null,
                decisionTimeMs = null
            )
        }
    }

    private fun restore(): UiState? {
        val options = savedStateHandle[KEY_OPTIONS] ?: return null
        if (options.size < 2) return null
        return UiState(
            options = options,
            preset = savedStateHandle[KEY_PRESET] ?: BoardPreset.PLINKO,
            bestOfThree = savedStateHandle[KEY_BEST_OF] ?: false,
            round = savedStateHandle[KEY_ROUND] ?: 1,
            roundWins = savedStateHandle[KEY_WINS] ?: emptyMap()
        )
    }

    private fun persist() {
        val s = _uiState.value
        savedStateHandle[KEY_OPTIONS] = ArrayList(s.options)
        savedStateHandle[KEY_PRESET] = s.preset
        savedStateHandle[KEY_BEST_OF] = s.bestOfThree
        savedStateHandle[KEY_ROUND] = s.round
        savedStateHandle[KEY_WINS] = HashMap(s.roundWins)
    }

    override fun onCleared() {
        stopEverything()
        super.onCleared()
    }
}

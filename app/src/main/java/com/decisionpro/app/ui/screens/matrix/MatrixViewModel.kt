package com.decisionpro.app.ui.screens.matrix

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionpro.app.data.repository.DecisionRepository
import com.decisionpro.app.di.DefaultDispatcher
import com.decisionpro.app.domain.engine.Validators
import com.decisionpro.app.domain.engine.matrix.Criterion
import com.decisionpro.app.domain.engine.matrix.DecisionOption
import com.decisionpro.app.domain.engine.matrix.MatrixResult
import com.decisionpro.app.domain.engine.matrix.SensitivityResult
import com.decisionpro.app.domain.engine.matrix.WeightedMatrixEngine
import com.decisionpro.app.util.persistence.StateCodecs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MatrixViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: DecisionRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val engine = WeightedMatrixEngine()

    companion object {
        private const val KEY_STATE = "matrix_state_json"
        private const val KEY_FIRST_EDIT = "matrix_first_edit_ts"
    }

    data class UiState(
        val title: String = "Which option wins?",
        val criteria: List<Criterion> = defaultCriteria(),
        val options: List<DecisionOption> = defaultOptions(),
        val results: List<MatrixResult> = emptyList(),
        val sensitivity: List<SensitivityResult> = emptyList(),
        val confidence: Int = 0,
        val isAnalyzing: Boolean = false,
        val validationError: String? = null
    ) {
        val isReady: Boolean get() = criteria.isNotEmpty() && options.size >= 2
    }

    private val _uiState = MutableStateFlow(restore() ?: UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var firstEditTs: Long = savedStateHandle[KEY_FIRST_EDIT] ?: 0L

    private fun restore(): UiState? {
        val saved = StateCodecs.decodeMatrix(savedStateHandle[KEY_STATE]) ?: return null
        if (saved.criteria.isEmpty() || saved.options.isEmpty()) return null
        return UiState(
            title = saved.title.ifBlank { "Which option wins?" },
            criteria = saved.criteria.map {
                Criterion(id = it.id, name = it.name, weight = it.weight)
            },
            options = saved.options.map {
                DecisionOption(id = it.id, name = it.name, scores = it.scores)
            }
        )
    }

    private fun persist() {
        val s = _uiState.value
        savedStateHandle[KEY_STATE] = StateCodecs.encodeMatrix(s.title, s.criteria, s.options)
    }

    private fun markDeliberationStart() {
        if (firstEditTs == 0L) {
            firstEditTs = System.currentTimeMillis()
            savedStateHandle[KEY_FIRST_EDIT] = firstEditTs
        }
    }

    fun setTitle(title: String) {
        _uiState.update { it.copy(title = title.take(200)) }
        markDeliberationStart(); persist()
    }

    fun addCriterion(name: String) {
        if (name.isBlank()) return
        _uiState.update { s ->
            if (s.criteria.size >= Validators.MAX_CRITERION_COUNT) return@update s
            s.copy(criteria = s.criteria + Criterion.create(name.trim()))
        }
        persist()
    }

    fun addOption(name: String) {
        if (name.isBlank()) return
        _uiState.update { s ->
            if (s.options.size >= Validators.MAX_OPTION_COUNT) return@update s
            s.copy(options = s.options + DecisionOption.create(name.trim()))
        }
        persist()
    }

    fun removeCriterion(id: String) =
        _uiState.update { s -> s.copy(criteria = s.criteria.filterNot { it.id == id }) }
            .also { persist() }

    fun removeOption(id: String) =
        _uiState.update { s -> s.copy(options = s.options.filterNot { it.id == id }) }
            .also { persist() }

    fun setWeight(criterionId: String, weight: Float) {
        _uiState.update { s ->
            s.copy(criteria = s.criteria.map {
                if (it.id == criterionId)
                    it.copy(weight = weight.coerceIn(0f, Validators.MAX_WEIGHT))
                else it
            })
        }
        markDeliberationStart(); persist()
    }

    fun setScore(optionId: String, criterionId: String, score: Float) {
        _uiState.update { s ->
            s.copy(options = s.options.map {
                if (it.id == optionId)
                    it.copy(scores = it.scores + (criterionId to score.coerceIn(0f, Validators.MAX_SCORE)))
                else it
            })
        }
        markDeliberationStart(); persist()
    }

    fun analyze() {
        val s = _uiState.value
        if (!s.isReady || s.isAnalyzing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, validationError = null) }
            delay(350)

            val optionCheck = Validators.validateOptions(s.options.map { it.name })
            if (optionCheck is Validators.ValidationResult.Invalid) {
                _uiState.update {
                    it.copy(isAnalyzing = false, validationError = optionCheck.firstError)
                }
                return@launch
            }

            val results = withContext(defaultDispatcher) {
                engine.evaluate(s.options, s.criteria)
            }
            val sensitivity = withContext(defaultDispatcher) {
                engine.sensitivityAnalysis(s.options, s.criteria, results)
            }
            val confidence = engine.confidence(results, sensitivity)

            _uiState.update {
                it.copy(
                    results = results, sensitivity = sensitivity,
                    confidence = confidence, isAnalyzing = false
                )
            }

            val deliberated = firstEditTs.takeIf { it > 0 }
                ?.let { System.currentTimeMillis() - it }
            firstEditTs = 0L
            savedStateHandle[KEY_FIRST_EDIT] = 0L

            results.firstOrNull()?.let { top ->
                repository.saveDecision(
                    question = s.title,
                    choice = top.optionName,
                    type = "matrix",
                    confidence = confidence,
                    decisionTimeMs = deliberated
                )
            }
        }
    }

    companion object {
        private fun defaultCriteria() = listOf(
            Criterion.create("Cost", 7f),
            Criterion.create("Quality", 8f),
            Criterion.create("Convenience", 5f)
        )

        private fun defaultOptions() = listOf(
            DecisionOption.create("Option A"),
            DecisionOption.create("Option B")
        )
    }
}

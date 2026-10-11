package com.decisionpro.app.ui.screens.tree

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionpro.app.data.repository.DecisionRepository
import com.decisionpro.app.domain.engine.Validators
import com.decisionpro.app.domain.engine.tree.Chance
import com.decisionpro.app.domain.engine.tree.Decision
import com.decisionpro.app.domain.engine.tree.Outcome
import com.decisionpro.app.domain.engine.tree.TreeAnalyzer
import com.decisionpro.app.domain.engine.tree.TreeOps
import com.decisionpro.app.domain.engine.tree.TreeNode
import com.decisionpro.app.domain.engine.tree.TreeTemplates
import com.decisionpro.app.util.persistence.StateCodecs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Undo/redo is ~15 lines because trees are immutable. */
@HiltViewModel
class TreeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: DecisionRepository
) : ViewModel() {

    companion object {
        private const val KEY_TREE = "tree_json"
        private const val KEY_QUESTION = "tree_question"
        private const val MAX_UNDO = 50
    }

    private val _question = MutableStateFlow(
        savedStateHandle[KEY_QUESTION] ?: "Career decision"
    )
    val question: StateFlow<String> = _question.asStateFlow()

    fun setQuestion(q: String) {
        _question.value = q.trim().take(200)
        savedStateHandle[KEY_QUESTION] = _question.value
    }

    private val _tree = MutableStateFlow(restoreTree() ?: TreeTemplates.jobDecision())
    val tree: StateFlow<TreeNode> = _tree.asStateFlow()

    val evaluation: StateFlow<TreeAnalyzer.TreeEvaluation> = _tree
        .map { TreeAnalyzer.evaluate(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = TreeAnalyzer.evaluate(_tree.value)
        )

    var selectedNodeId = MutableStateFlow<String?>(null)
        private set

    fun selectNode(id: String?) { selectedNodeId.value = id }

    private val undoStack = ArrayDeque<TreeNode>()
    private val redoStack = ArrayDeque<TreeNode>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private fun edit(transform: (TreeNode) -> TreeNode) {
        undoStack.addLast(_tree.value)
        if (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        redoStack.clear()
        _tree.value = transform(_tree.value)
        persist()
        refreshFlags()
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_tree.value)
        _tree.value = previous
        persist()
        refreshFlags()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_tree.value)
        _tree.value = next
        persist()
        refreshFlags()
    }

    private fun refreshFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun rename(id: String, label: String) =
        edit { TreeOps.setLabel(it, id, label.take(100)) }

    fun setValue(id: String, value: Double) {
        if (Validators.validatePayoff(value) is Validators.ValidationResult.Invalid) return
        edit { TreeOps.setValue(it, id, value) }
    }

    fun setProbability(parentId: String, childId: String, p: Double) =
        edit { TreeOps.setProbability(it, parentId, childId, p) }

    fun addBranch(parentId: String) =
        edit { TreeOps.addBranch(it, parentId, "New branch") }

    fun removeBranch(parentId: String, childId: String) =
        edit { TreeOps.removeBranch(it, parentId, childId) }

    fun convertToChance(id: String) =
        edit { TreeOps.convertToChance(it, id) }

    fun resetToTemplate() = edit { TreeTemplates.jobDecision() }

    /** "Take the new job beats staying as long as you'd put odds above 33%." */
    fun breakevenInsight(): String? {
        val root = _tree.value as? Decision ?: return null

        var certain: Outcome? = null
        var gambleBranchLabel: String? = null
        var gambleChance: Chance? = null
        for (branch in root.children) {
            when (val child = branch.child) {
                is Outcome -> certain = child
                is Chance ->
                    if (child.children.size == 2 &&
                        child.children.all { it.child is Outcome }
                    ) {
                        gambleChance = child
                        gambleBranchLabel = branch.label
                    }
            }
        }
        val chance = gambleChance ?: return null
        val certainty = certain ?: return null

        val values = chance.children.mapNotNull { (it.child as? Outcome)?.value }
        if (values.size != 2) return null
        val good = maxOf(values[0], values[1])
        val bad = minOf(values[0], values[1])

        val p = TreeAnalyzer.breakeven(certainty.value, good, bad) ?: return null
        return "\"$gambleBranchLabel\" beats \"${certainty.label}\" as long as you'd put " +
                "the odds of the good outcome above ${(p * 100).toInt()}%"
    }

    private fun restoreTree(): TreeNode? =
        StateCodecs.decodeTree(savedStateHandle[KEY_TREE])

    private fun persist() {
        savedStateHandle[KEY_TREE] = StateCodecs.encodeTree(_tree.value)
    }

    fun saveResult() {
        val root = _tree.value as? Decision ?: return
        val bestChildId = evaluation.value.nodeEvaluations[root.id]?.bestChildId ?: return
        val bestBranch = root.children.firstOrNull { it.child.id == bestChildId } ?: return

        viewModelScope.launch {
            repository.saveDecision(
                question = _question.value,
                choice = bestBranch.label,
                type = "tree",
                confidence = null
            )
        }
    }
}

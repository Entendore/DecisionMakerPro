package com.decisionpro.app.domain.engine.tree

import kotlin.math.abs

/** Rollback (backward induction). Validation is NON-FATAL — broken trees
 *  still evaluate; the error surfaces in the UI as a warning banner. */
object TreeAnalyzer {

    data class NodeEvaluation(
        val nodeId: String,
        val value: Double,
        val isDecision: Boolean = false,
        val bestChildId: String? = null,
        val prunedChildIds: Set<String> = emptySet()
    )

    data class TreeEvaluation(
        val rootValue: Double,
        val nodeEvaluations: Map<String, NodeEvaluation>,
        val bestPathIds: Set<String>,
        val validationError: String?
    )

    private const val SUM_TOLERANCE = 0.001

    fun evaluate(root: TreeNode): TreeEvaluation {
        val evals = mutableMapOf<String, NodeEvaluation>()
        var validationError: String? = null

        fun flagError(message: String) {
            if (validationError == null) validationError = message
        }

        fun walk(node: TreeNode): Double = when (node) {
            is Outcome -> {
                evals[node.id] = NodeEvaluation(node.id, node.value)
                node.value
            }
            is Chance -> {
                if (node.children.isEmpty())
                    flagError("Chance \"${node.label}\" has no outcomes yet")
                val sum = node.children.sumOf { it.probability ?: 0.0 }
                if (node.children.isNotEmpty() && abs(sum - 1.0) > SUM_TOLERANCE) {
                    flagError(
                        "Probabilities under \"${node.label}\" sum to " +
                            "%.0f%% — must equal 100%%".format(sum * 100)
                    )
                }
                val emv = node.children.sumOf { branch ->
                    (branch.probability ?: 0.0) * walk(branch.child)
                }
                evals[node.id] = NodeEvaluation(node.id, emv)
                emv
            }
            is Decision -> {
                if (node.children.isEmpty()) {
                    flagError("Decision \"${node.label}\" has no options yet")
                    evals[node.id] = NodeEvaluation(node.id, 0.0, isDecision = true)
                    return 0.0
                }
                val childValues = node.children.associate { b -> b.child.id to walk(b.child) }
                val best = childValues.maxByOrNull { it.value }!!
                val pruned = childValues.filterValues { abs(it - best.value) > 1e-9 }.keys
                evals[node.id] = NodeEvaluation(
                    nodeId = node.id, value = best.value,
                    isDecision = true, bestChildId = best.key, prunedChildIds = pruned
                )
                best.value
            }
        }

        val rootValue = walk(root)

        return TreeEvaluation(
            rootValue = rootValue,
            nodeEvaluations = evals,
            bestPathIds = traceMostLikelyOptimalPath(root, evals),
            validationError = validationError
        )
    }

    private fun traceMostLikelyOptimalPath(
        root: TreeNode,
        evals: Map<String, NodeEvaluation>
    ): Set<String> {
        val path = mutableSetOf<String>()
        var current: TreeNode = root
        while (true) {
            path += current.id
            val next: TreeNode? = when (current) {
                is Decision -> evals[current.id]?.bestChildId
                    ?.let { bestId ->
                        current.children.firstOrNull { it.child.id == bestId }?.child
                    }
                is Chance -> current.children
                    .maxByOrNull { it.probability ?: 0.0 }?.child
                is Outcome -> null
            }
            current = next ?: break
        }
        return path
    }

    /**
     * p* where p×good + (1−p)×bad = certain. Below p* → take the certain
     * option; above → take the gamble. Null when no crossing in [0,1].
     */
    fun breakeven(certainValue: Double, goodValue: Double, badValue: Double): Double? {
        val range = goodValue - badValue
        if (abs(range) < 1e-9) return null
        val p = (certainValue - badValue) / range
        return if (p in 0.0..1.0) p else null
    }

    fun outcomeRange(root: TreeNode): Pair<Double, Double>? {
        var min = Double.MAX_VALUE
        var max = -Double.MAX_VALUE
        var found = false
        fun walk(node: TreeNode) {
            when (node) {
                is Outcome -> { found = true; min = minOf(min, node.value); max = maxOf(max, node.value) }
                is Decision -> node.children.forEach { walk(it.child) }
                is Chance -> node.children.forEach { walk(it.child) }
            }
        }
        walk(root)
        return if (found) min to max else null
    }
}

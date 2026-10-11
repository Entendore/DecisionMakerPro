package com.decisionpro.app.util.persistence

import com.decisionpro.app.domain.engine.matrix.Criterion
import com.decisionpro.app.domain.engine.matrix.DecisionOption
import com.decisionpro.app.domain.engine.tree.Branch
import com.decisionpro.app.domain.engine.tree.Chance
import com.decisionpro.app.domain.engine.tree.Decision
import com.decisionpro.app.domain.engine.tree.Outcome
import com.decisionpro.app.domain.engine.tree.TreeNode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** JSON round-trips for SavedStateHandle. Corrupt input → null →
 *  defaults. Never crashes on a bad process-death snapshot. */
object StateCodecs {

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Serializable
    data class SavedCriterion(val id: String, val name: String, val weight: Float)

    @Serializable
    data class SavedOption(
        val id: String,
        val name: String,
        val scores: Map<String, Float> = emptyMap()
    )

    @Serializable
    data class SavedMatrixState(
        val title: String = "",
        val criteria: List<SavedCriterion> = emptyList(),
        val options: List<SavedOption> = emptyList()
    )

    fun encodeMatrix(title: String, criteria: List<Criterion>, options: List<DecisionOption>): String =
        json.encodeToString(
            SavedMatrixState(
                title = title,
                criteria = criteria.map { SavedCriterion(it.id, it.name, it.weight) },
                options = options.map { SavedOption(it.id, it.name, it.scores) }
            )
        )

    fun decodeMatrix(raw: String?): SavedMatrixState? = raw?.let {
        runCatching { json.decodeFromString<SavedMatrixState>(it) }.getOrNull()
    }

    @Serializable
    data class SavedBranch(
        val label: String,
        val probability: Double? = null,
        val child: SavedNode
    )

    @Serializable
    data class SavedNode(
        val t: String,
        val id: String,
        val label: String,
        val value: Double? = null,
        val children: List<SavedBranch> = emptyList()
    )

    fun encodeTree(root: TreeNode): String = json.encodeToString(toSaved(root))

    fun decodeTree(raw: String?): TreeNode? = raw?.let {
        runCatching { fromSaved(json.decodeFromString<SavedNode>(it)) }.getOrNull()
    }

    private fun toSaved(node: TreeNode): SavedNode = when (node) {
        is Decision -> SavedNode(
            "D", node.id, node.label,
            children = node.children.map { SavedBranch(it.label, it.probability, toSaved(it.child)) }
        )
        is Chance -> SavedNode(
            "C", node.id, node.label,
            children = node.children.map { SavedBranch(it.label, it.probability, toSaved(it.child)) }
        )
        is Outcome -> SavedNode("O", node.id, node.label, value = node.value)
    }

    private fun fromSaved(s: SavedNode): TreeNode = when (s.t) {
        "D" -> Decision(s.id, s.label, s.children.map { Branch(it.label, it.probability, fromSaved(it.child)) })
        "C" -> Chance(s.id, s.label, s.children.map { Branch(it.label, it.probability, fromSaved(it.child)) })
        else -> Outcome(s.id, s.label, s.value ?: 0.0)
    }
}

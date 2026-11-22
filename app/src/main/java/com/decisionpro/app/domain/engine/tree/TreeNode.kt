package com.decisionpro.app.domain.engine.tree

sealed class TreeNode {
    abstract val id: String
    abstract val label: String
}

data class Decision(
    override val id: String = newId(),
    override val label: String,
    val children: List<Branch> = emptyList()
) : TreeNode()

data class Chance(
    override val id: String = newId(),
    override val label: String,
    val children: List<Branch> = emptyList()
) : TreeNode()

data class Outcome(
    override val id: String = newId(),
    override val label: String,
    val value: Double
) : TreeNode()

data class Branch(
    val label: String,
    val probability: Double? = null,
    val child: TreeNode
)

private fun newId(): String =
    java.util.UUID.randomUUID().toString().take(8)

fun TreeNode.branches(): List<Branch> = when (this) {
    is Decision -> children
    is Chance -> children
    is Outcome -> emptyList()
}

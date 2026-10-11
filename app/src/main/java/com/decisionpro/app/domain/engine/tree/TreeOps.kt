package com.decisionpro.app.domain.engine.tree

/**
 * Immutable tree operations. Every edit returns a NEW tree; undo/redo is
 * just a stack of previous roots. One recursion kernel, tested once.
 */
object TreeOps {

    private fun transform(root: TreeNode, id: String, fn: (TreeNode) -> TreeNode): TreeNode {
        if (root.id == id) return fn(root)
        return when (root) {
            is Decision -> root.copy(
                children = root.children.map { it.copy(child = transform(it.child, id, fn)) }
            )
            is Chance -> root.copy(
                children = root.children.map { it.copy(child = transform(it.child, id, fn)) }
            )
            is Outcome -> root
        }
    }

    fun setLabel(root: TreeNode, id: String, label: String): TreeNode =
        transform(root, id) { node ->
            when (node) {
                is Decision -> node.copy(label = label)
                is Chance -> node.copy(label = label)
                is Outcome -> node.copy(label = label)
            }
        }

    fun setValue(root: TreeNode, id: String, value: Double): TreeNode =
        transform(root, id) { node ->
            if (node is Outcome) node.copy(value = value) else node
        }

    fun setProbability(root: TreeNode, parentId: String, childId: String, p: Double): TreeNode =
        transform(root, parentId) { node ->
            if (node is Chance) {
                node.copy(children = node.children.map {
                    if (it.child.id == childId) it.copy(probability = p.coerceIn(0.0, 1.0)) else it
                })
            } else node
        }

    fun normalizeChance(root: TreeNode, chanceId: String): TreeNode =
        transform(root, chanceId) { node ->
            if (node is Chance) normalize(node) else node
        }

    fun addBranch(root: TreeNode, parentId: String, label: String): TreeNode =
        transform(root, parentId) { node ->
            when (node) {
                is Decision -> node.copy(
                    children = node.children + Branch(
                        label, null, Outcome(label = "Outcome", value = 0.0)
                    )
                )
                is Chance -> normalize(
                    node.copy(
                        children = node.children + Branch(
                            label, 0.0, Outcome(label = "Outcome", value = 0.0)
                        )
                    )
                )
                is Outcome -> node
            }
        }

    /** Refuses to empty a decision/chance — empty internal nodes are invalid. */
    fun removeBranch(root: TreeNode, parentId: String, childId: String): TreeNode =
        transform(root, parentId) { node ->
            when (node) {
                is Decision ->
                    if (node.children.size > 1)
                        node.copy(children = node.children.filterNot { it.child.id == childId })
                    else node
                is Chance ->
                    if (node.children.size > 1)
                        normalize(node.copy(children = node.children.filterNot { it.child.id == childId }))
                    else node
                is Outcome -> node
            }
        }

    /** Keeps the node id — UI selection survives the structural edit. */
    fun convertToChance(root: TreeNode, id: String): TreeNode =
        transform(root, id) { node ->
            if (node is Outcome) Chance(
                id = node.id,
                label = node.label,
                children = listOf(
                    Branch("Goes well", 0.5, Outcome("Goes well", node.value * 1.5)),
                    Branch("Goes badly", 0.5, Outcome("Goes badly", node.value * 0.25))
                )
            ) else node
        }

    fun normalize(chance: Chance): Chance {
        val sum = chance.children.sumOf { it.probability ?: 0.0 }
        return if (sum <= 1e-9) {
            val even = 1.0 / chance.children.size
            chance.copy(children = chance.children.map { it.copy(probability = even) })
        } else {
            chance.copy(
                children = chance.children.map {
                    it.copy(probability = (it.probability ?: 0.0) / sum)
                }
            )
        }
    }

    fun maxDepth(root: TreeNode): Int =
        1 + (branches(root).maxOfOrNull { maxDepth(it.child) } ?: 0)

    fun countNodes(root: TreeNode): Int =
        1 + branches(root).sumOf { countNodes(it.child) }

    private fun branches(node: TreeNode): List<Branch> = when (node) {
        is Decision -> node.children
        is Chance -> node.children
        is Outcome -> emptyList()
    }
}

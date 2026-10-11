package com.decisionpro.app.domain.engine.tree

/**
 * Tidy-tree layout: leaves stack vertically, depth flows L→R (mirrored
 * under RTL). Pure math — screenshot tests pin the visuals, these tests
 * pin the numbers.
 */
object TreeLayout {

    data class PlacedNode(
        val node: TreeNode,
        val branchLabel: String?,
        val probability: Double?,
        val x: Float,
        val y: Float,
        val evaluation: TreeAnalyzer.NodeEvaluation?
    )

    data class Edge(
        val from: PlacedNode,
        val to: PlacedNode,
        val probability: Double?,
        val pruned: Boolean,
        val onBestPath: Boolean
    )

    data class Layout(
        val nodes: List<PlacedNode>,
        val edges: List<Edge>,
        val totalDepth: Int
    )

    private const val MARGIN_X = 110f
    private const val MARGIN_Y = 30f

    fun layout(
        root: TreeNode,
        width: Float,
        height: Float,
        evaluation: TreeAnalyzer.TreeEvaluation?,
        isRtl: Boolean = false
    ): Layout {
        val slots = mutableMapOf<String, Float>()
        val depths = mutableMapOf<String, Int>()
        var nextSlot = 0f

        fun assign(node: TreeNode, depth: Int) {
            depths[node.id] = depth
            val kids = node.branches()
            if (kids.isEmpty()) {
                slots[node.id] = nextSlot++
            } else {
                kids.forEach { assign(it.child, depth + 1) }
                slots[node.id] = kids.map { slots[it.child.id]!! }.average().toFloat()
            }
        }
        assign(root, 0)
        val totalDepth = depths.values.max().coerceAtLeast(1)
        val slotCount = nextSlot.coerceAtLeast(2f)

        val usableW = (width - 2 * MARGIN_X).coerceAtLeast(1f)
        val usableH = (height - 2 * MARGIN_Y).coerceAtLeast(1f)

        fun xFor(depth: Int): Float {
            val raw = MARGIN_X + usableW * (depth.toFloat() / totalDepth)
            return if (isRtl) width - raw else raw
        }
        fun yFor(slot: Float): Float =
            MARGIN_Y + usableH * (slot / (slotCount - 1f))

        val placed = mutableListOf<PlacedNode>()
        val edges = mutableListOf<Edge>()
        val bestPath = evaluation?.bestPathIds ?: emptySet()

        fun build(node: TreeNode, parent: PlacedNode?, branch: Branch?): PlacedNode {
            val p = PlacedNode(
                node = node,
                branchLabel = branch?.label,
                probability = branch?.probability,
                x = xFor(depths[node.id]!!),
                y = yFor(slots[node.id]!!),
                evaluation = evaluation?.nodeEvaluations?.get(node.id)
            )
            placed += p

            if (parent != null && branch != null) {
                edges += Edge(
                    from = parent, to = p,
                    probability = branch.probability,
                    pruned = parent.evaluation?.prunedChildIds?.contains(node.id) == true,
                    onBestPath = parent.node.id in bestPath && node.id in bestPath
                )
            }
            node.branches().forEach { build(it.child, p, it) }
            return p
        }
        build(root, parent = null, branch = null)

        return Layout(placed, edges, totalDepth)
    }
}

package com.decisionpro.app.domain.engine.tree

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TreeLayoutTest {

    private val width = 800f
    private val height = 600f

    @Test
    fun `every node placed exactly once`() {
        val tree = TreeTemplates.jobDecision()
        val layout = TreeLayout.layout(tree, width, height, null)
        assertThat(layout.nodes).hasSize(TreeOps.countNodes(tree))
    }

    @Test
    fun `edge count equals node count minus one`() {
        val tree = TreeTemplates.productLaunch()
        val layout = TreeLayout.layout(tree, width, height, null)
        assertThat(layout.edges).isEqualTo(layout.nodes.size - 1)
    }

    @Test
    fun `leaves stack vertically without overlap`() {
        val layout = TreeLayout.layout(TreeTemplates.productLaunch(), width, height, null)
        val leafYs = layout.nodes
            .filter { it.node is TreeNode.Outcome }
            .map { it.y }.sorted()
        leafYs.zipWithNext().forEach { (a, b) -> assertThat(b - a).isGreaterThan(1f) }
    }

    @Test
    fun `pruned and best-path edges are mutually exclusive`() {
        val tree = TreeTemplates.jobDecision()
        val eval = TreeAnalyzer.evaluate(tree)
        val layout = TreeLayout.layout(tree, width, height, eval)
        assertThat(layout.edges.any { it.pruned }).isTrue()
        assertThat(layout.edges.any { it.onBestPath }).isTrue()
        assertThat(layout.edges.any { it.pruned && it.onBestPath }).isFalse()
    }

    @Test
    fun `rtl mirrors the x-axis for every node`() {
        val tree = TreeTemplates.jobDecision()
        val ltr = TreeLayout.layout(tree, width, height, null, isRtl = false)
        val rtl = TreeLayout.layout(tree, width, height, null, isRtl = true)
        rtl.nodes.forEach { r ->
            val l = ltr.nodes.first { it.node.id == r.node.id }
            assertThat(r.x).isWithin(0.01f).of(width - l.x)
            assertThat(r.y).isWithin(0.01f).of(l.y)
        }
    }

    @Test
    fun `all coordinates stay inside canvas bounds`() {
        val layout = TreeLayout.layout(TreeTemplates.productLaunch(), width, height, null)
        layout.nodes.forEach { p ->
            assertThat(p.x).isAtLeast(0f); assertThat(p.x).isAtMost(width)
            assertThat(p.y).isAtLeast(0f); assertThat(p.y).isAtMost(height)
        }
    }
}

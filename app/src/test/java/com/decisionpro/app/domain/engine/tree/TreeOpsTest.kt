package com.decisionpro.app.domain.engine.tree

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TreeOpsTest {

    private fun sampleTree() = TreeTemplates.jobDecision()

    private fun stayOutcome(tree: Decision): Outcome =
        tree.children[1].child as Outcome

    @Test
    fun `setLabel returns new tree and leaves original untouched`() {
        val original = sampleTree()
        val targetId = stayOutcome(original).id

        val edited = TreeOps.setLabel(original, targetId, "Renamed") as Decision

        assertThat((original.children[1].child as Outcome).label).isEqualTo("Stay put")
        assertThat((edited.children[1].child as Outcome).label).isEqualTo("Renamed")
    }

    @Test
    fun `setValue on unknown id is a safe no-op`() {
        val original = sampleTree()
        assertThat(TreeOps.setValue(original, "nope", 999.0)).isEqualTo(original)
    }

    @Test
    fun `setValue updates the leaf and rollback reflects it`() {
        val tree = sampleTree()
        val edited = TreeOps.setValue(tree, stayOutcome(tree).id, 200.0)
        assertThat(TreeAnalyzer.evaluate(edited).rootValue).isWithin(0.01).of(200.0)
    }

    @Test
    fun `setProbability clamps out-of-range values`() {
        val tree = sampleTree()
        val chance = tree.children[0].child as TreeNode.Chance
        val firstChildId = chance.children[0].child.id

        val p999 = TreeOps.setProbability(tree, chance.id, firstChildId, 9.99)
        assertThat(
            ((p999 as Decision).children[0].child as TreeNode.Chance)
                .children[0].probability
        ).isEqualTo(1.0)

        val pNeg = TreeOps.setProbability(tree, chance.id, firstChildId, -5.0)
        assertThat(
            ((pNeg as Decision).children[0].child as TreeNode.Chance)
                .children[0].probability
        ).isEqualTo(0.0)
    }

    @Test
    fun `normalizeChance rescales branches to sum exactly 1`() {
        val tree = sampleTree()
        val chance = tree.children[0].child as TreeNode.Chance
        val broken = TreeOps.setProbability(
            tree, chance.id, chance.children[0].child.id, 0.9
        )
        val fixed = TreeOps.normalizeChance(broken, chance.id)
        val sum = ((fixed as Decision).children[0].child as TreeNode.Chance)
            .children.sumOf { it.probability!! }
        assertThat(sum).isWithin(0.0001).of(1.0)
    }

    @Test
    fun `addBranch under chance auto-normalizes`() {
        val tree = sampleTree()
        val chance = tree.children[0].child as TreeNode.Chance
        val edited = TreeOps.addBranch(tree, chance.id, "Breaks even")
        val editedChance = ((edited as Decision).children[0].child as TreeNode.Chance)
        assertThat(editedChance.children).hasSize(3)
        assertThat(editedChance.children.sumOf { it.probability!! })
            .isWithin(0.0001).of(1.0)
    }

    @Test
    fun `removeBranch refuses to empty a decision`() {
        val single = TreeNode.Decision(
            "Lonely",
            listOf(Branch("Only", null, TreeNode.Outcome("O", 1.0)))
        )
        val edited = TreeOps.removeBranch(single, single.id, single.children[0].child.id)
        assertThat((edited as Decision).children).hasSize(1)
    }

    @Test
    fun `convertToChance keeps node id and seeds plausible scenarios`() {
        val tree = sampleTree()
        val stayLeaf = stayOutcome(tree)
        val converted =
            (TreeOps.convertToChance(tree, stayLeaf.id) as Decision).children[1].child

        assertThat(converted).isInstanceOf(TreeNode.Chance::class.java)
        assertThat(converted.id).isEqualTo(stayLeaf.id)
        val chance = converted as TreeNode.Chance
        assertThat((chance.children[0].child as Outcome).value).isWithin(0.01).of(105.0)
        assertThat((chance.children[1].child as Outcome).value).isWithin(0.01).of(17.5)
    }

    @Test
    fun `maxDepth and countNodes measure the template`() {
        val tree = TreeTemplates.jobDecision()
        assertThat(TreeOps.maxDepth(tree)).isEqualTo(3)
        assertThat(TreeOps.countNodes(tree)).isEqualTo(5)
    }
}

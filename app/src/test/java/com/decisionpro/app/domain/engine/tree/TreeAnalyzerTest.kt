package com.decisionpro.app.domain.engine.tree

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TreeAnalyzerTest {

    @Test
    fun `rollback computes correct EMV for the job template`() {
        assertThat(TreeAnalyzer.evaluate(TreeTemplates.jobDecision()).rootValue)
            .isWithin(0.01).of(94.0)
    }

    @Test
    fun `decision picks maximum EMV and prunes the rest`() {
        val tree = TreeNode.Decision(
            "Root",
            listOf(
                Branch("Bad", null, TreeNode.Outcome("Bad", 50.0)),
                Branch("Good", null, TreeNode.Outcome("Good", 100.0))
            )
        )
        val eval = TreeAnalyzer.evaluate(tree)

        assertThat(eval.rootValue).isWithin(0.01).of(100.0)
        assertThat(eval.nodeEvaluations[tree.id]!!.prunedChildIds).hasSize(1)
        val bestBranch = tree.children.first {
            it.child.id == eval.nodeEvaluations[tree.id]!!.bestChildId
        }
        assertThat(bestBranch.label).isEqualTo("Good")
    }

    @Test
    fun `nested tree evaluates deepest-first`() {
        val inner = TreeNode.Chance(
            "Inner",
            listOf(
                Branch("A", 0.5, TreeNode.Outcome("L1", 100.0)),
                Branch("B", 0.5, TreeNode.Outcome("L2", 20.0))
            )
        )
        val root = TreeNode.Decision(
            "Root",
            listOf(
                Branch("Via chance", null, inner),
                Branch("Direct", null, TreeNode.Outcome("L3", 70.0))
            )
        )
        val eval = TreeAnalyzer.evaluate(root)

        assertThat(eval.rootValue).isWithin(0.01).of(70.0)
        assertThat(eval.nodeEvaluations[inner.id]!!.value).isWithin(0.01).of(60.0)
    }

    @Test
    fun `evaluation is deterministic`() {
        val tree = TreeTemplates.productLaunch()
        val e1 = TreeAnalyzer.evaluate(tree)
        val e2 = TreeAnalyzer.evaluate(tree)
        assertThat(e1.rootValue).isEqualTo(e2.rootValue)
        assertThat(e1.bestPathIds).isEqualTo(e2.bestPathIds)
    }

    @Test
    fun `probabilities summing over 1 are flagged`() {
        val tree = TreeNode.Decision(
            "Root",
            listOf(
                Branch("C", null, TreeNode.Chance(
                    "Chance",
                    listOf(
                        Branch("A", 0.6, TreeNode.Outcome("A", 10.0)),
                        Branch("B", 0.7, TreeNode.Outcome("B", 20.0))
                    )
                ))
            )
        )
        val error = TreeAnalyzer.evaluate(tree).validationError
        assertThat(error).isNotNull()
        assertThat(error!!).contains("130%")
    }

    @Test
    fun `tiny float drift in probability sum is tolerated`() {
        val tree = TreeNode.Chance(
            "C",
            listOf(
                Branch("A", 1.0 / 3.0, TreeNode.Outcome("A", 10.0)),
                Branch("B", 1.0 / 3.0, TreeNode.Outcome("B", 10.0)),
                Branch("C", 1.0 / 3.0, TreeNode.Outcome("C", 10.0))
            )
        )
        assertThat(TreeAnalyzer.evaluate(tree).validationError).isNull()
    }

    @Test
    fun `empty decision is flagged not crashed`() {
        val eval = TreeAnalyzer.evaluate(TreeNode.Decision("Empty"))
        assertThat(eval.validationError).contains("no options")
        assertThat(eval.rootValue).isEqualTo(0.0)
    }

    @Test
    fun `breakeven threshold for the job template is one third`() {
        val p = TreeAnalyzer.breakeven(70.0, 130.0, 40.0)
        assertThat(p).isWithin(0.001).of(1.0 / 3.0)
    }

    @Test
    fun `breakeven returns null when certain value dominates`() {
        assertThat(TreeAnalyzer.breakeven(100.0, 60.0, 40.0)).isNull()
    }

    @Test
    fun `breakeven returns null for flat gamble`() {
        assertThat(TreeAnalyzer.breakeven(50.0, 60.0, 60.0)).isNull()
    }

    @Test
    fun `outcome range spans min to max leaf`() {
        val range = TreeAnalyzer.outcomeRange(TreeTemplates.productLaunch())
        assertThat(range!!.first).isWithin(0.01).of(-200.0)
        assertThat(range.second).isWithin(0.01).of(500.0)
    }

    @Test
    fun `productLaunch template evaluates to 167_5`() {
        val eval = TreeAnalyzer.evaluate(TreeTemplates.productLaunch())
        assertThat(eval.rootValue).isWithin(0.01).of(167.5)
        assertThat(eval.validationError).isNull()
    }
}

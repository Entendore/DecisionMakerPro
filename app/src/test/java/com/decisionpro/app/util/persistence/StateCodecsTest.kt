package com.decisionpro.app.util.persistence

import com.decisionpro.app.domain.engine.matrix.Criterion
import com.decisionpro.app.domain.engine.matrix.DecisionOption
import com.decisionpro.app.domain.engine.tree.Decision
import com.decisionpro.app.domain.engine.tree.TreeAnalyzer
import com.decisionpro.app.domain.engine.tree.TreeOps
import com.decisionpro.app.domain.engine.tree.TreeTemplates
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StateCodecsTest {

    @Test
    fun `matrix state round-trips losslessly`() {
        val criteria = listOf(Criterion.create("Cost", 7f), Criterion.create("Quality", 8f))
        val options = listOf(
            DecisionOption.create("A")
                .copy(scores = mapOf(criteria[0].id to 6f, criteria[1].id to 9f)),
            DecisionOption.create("B")
        )

        val decoded = StateCodecs.decodeMatrix(
            StateCodecs.encodeMatrix("Which?", criteria, options)
        )!!

        assertThat(decoded.title).isEqualTo("Which?")
        assertThat(decoded.criteria[0].weight).isWithin(0.01f).of(7f)
        assertThat(decoded.options[0].scores[criteria[0].id]).isWithin(0.01f).of(6f)
    }

    @Test
    fun `tree round-trips with identical evaluation`() {
        val tree = TreeTemplates.jobDecision()
        val decoded = StateCodecs.decodeTree(StateCodecs.encodeTree(tree)) as Decision

        assertThat(TreeAnalyzer.evaluate(decoded).rootValue)
            .isWithin(0.01).of(TreeAnalyzer.evaluate(tree).rootValue)
        assertThat(TreeOps.countNodes(decoded)).isEqualTo(TreeOps.countNodes(tree))
    }

    @Test
    fun `corrupt input decodes to null - never crashes`() {
        assertThat(StateCodecs.decodeMatrix("not json{")).isNull()
        assertThat(StateCodecs.decodeTree("[[[broken")).isNull()
        assertThat(StateCodecs.decodeMatrix(null)).isNull()
        assertThat(StateCodecs.decodeTree(null)).isNull()
    }

    @Test
    fun `unknown fields ignored - forward compatible`() {
        val future = """{"title":"T","criteria":[],"options":[],"futureField":42}"""
        assertThat(StateCodecs.decodeMatrix(future)!!.title).isEqualTo("T")
    }
}

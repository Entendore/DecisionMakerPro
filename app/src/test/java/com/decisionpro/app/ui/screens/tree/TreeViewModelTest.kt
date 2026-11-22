package com.decisionpro.app.ui.screens.tree

import androidx.lifecycle.SavedStateHandle
import com.decisionpro.app.data.repository.DecisionRepository
import com.decisionpro.app.domain.engine.tree.Decision
import com.decisionpro.app.domain.engine.tree.Outcome
import com.decisionpro.app.ui.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TreeViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private lateinit var handle: SavedStateHandle
    private lateinit var vm: TreeViewModel

    @Before
    fun setUp() {
        handle = SavedStateHandle()
        vm = TreeViewModel(handle, mockk(relaxed = true))
    }

    private fun stayOutcome(tree: Decision): Outcome =
        tree.children[1].child as Outcome

    @Test
    fun `undo restores previous tree and redo reapplies`() = runTest {
        val stayId = stayOutcome(vm.tree.value as Decision).id

        vm.setValue(stayId, 200.0)
        assertThat(vm.canUndo.value).isTrue()

        vm.undo()
        assertThat(stayOutcome(vm.tree.value as Decision).value).isEqualTo(70.0)
        assertThat(vm.canRedo.value).isTrue()

        vm.redo()
        assertThat(stayOutcome(vm.tree.value as Decision).value).isEqualTo(200.0)
    }

    @Test
    fun `new edit clears the redo stack`() = runTest {
        val rootId = (vm.tree.value as Decision).id
        vm.addBranch(rootId)
        vm.undo()
        assertThat(vm.canRedo.value).isTrue()

        vm.addBranch(rootId)
        assertThat(vm.canRedo.value).isFalse()
    }

    @Test
    fun `edits persist and restore into a fresh VM - process death`() = runTest {
        val stayId = stayOutcome(vm.tree.value as Decision).id
        vm.setValue(stayId, 250.0)

        val restored = TreeViewModel(handle, mockk(relaxed = true))
        assertThat(stayOutcome(restored.tree.value as Decision).value).isEqualTo(250.0)
    }

    @Test
    fun `invalid payoff is rejected - edit is a no-op`() = runTest {
        val before = vm.tree.value
        vm.setValue(stayOutcome(before as Decision).id, Double.NaN)
        assertThat(vm.tree.value).isEqualTo(before)
    }

    @Test
    fun `breakevenInsight surfaces the one-third threshold`() = runTest {
        val insight = vm.breakevenInsight()
        assertThat(insight).isNotNull()
        assertThat(insight!!).contains("33%")
    }
}

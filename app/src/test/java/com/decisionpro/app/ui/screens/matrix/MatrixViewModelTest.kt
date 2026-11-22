package com.decisionpro.app.ui.screens.matrix

import androidx.lifecycle.SavedStateHandle
import com.decisionpro.app.data.repository.AppResult
import com.decisionpro.app.data.repository.DecisionRepository
import com.decisionpro.app.ui.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MatrixViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private lateinit var handle: SavedStateHandle
    private lateinit var repo: DecisionRepository

    @Before
    fun setUp() {
        handle = SavedStateHandle()
        repo = mockk(relaxed = true)
        coEvery { repo.saveDecision(any(), any(), any(), any(), any()) }
            .returns(AppResult.Success(1L))
    }

    @Test
    fun `score edits persist across process death`() = runTest {
        val vm = MatrixViewModel(handle, repo, Dispatchers.Unconfined)
        val optionId = vm.uiState.value.options[0].id
        val criterionId = vm.uiState.value.criteria[0].id

        vm.setScore(optionId, criterionId, 9f)

        val restored = MatrixViewModel(handle, repo, Dispatchers.Unconfined)
        assertThat(restored.uiState.value.options[0].scores[criterionId])
            .isWithin(0.01f).of(9f)
    }

    @Test
    fun `analyze computes ranked results and saves the winner`() = runTest {
        val vm = MatrixViewModel(handle, repo, Dispatchers.Unconfined)
        vm.analyze()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertThat(s.results).isNotEmpty()
        assertThat(s.confidence).isIn(0..100)
        coVerify { repo.saveDecision(any(), any(), "matrix", any(), any()) }
    }

    @Test
    fun `duplicate option names surface validation error`() = runTest {
        val vm = MatrixViewModel(handle, repo, Dispatchers.Unconfined)
        vm.addOption("Option A")

        vm.analyze()
        advanceUntilIdle()

        assertThat(vm.uiState.value.validationError).isNotNull()
        assertThat(vm.uiState.value.results).isEmpty()
    }

    @Test
    fun `score clamping keeps engine input valid`() = runTest {
        val vm = MatrixViewModel(handle, repo, Dispatchers.Unconfined)
        val optionId = vm.uiState.value.options[0].id
        val criterionId = vm.uiState.value.criteria[0].id

        vm.setScore(optionId, criterionId, 99f)

        assertThat(vm.uiState.value.options[0].scores[criterionId]).isEqualTo(10f)
    }
}

package com.decisionpro.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionpro.app.data.model.AnalyticsSnapshot
import com.decisionpro.app.data.model.DecisionRecord
import com.decisionpro.app.data.repository.DecisionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DecisionRepository
) : ViewModel() {

    data class HomeState(
        val history: List<DecisionRecord> = emptyList(),
        val analytics: AnalyticsSnapshot = AnalyticsSnapshot.EMPTY,
        val isLoading: Boolean = true
    )

    val state: StateFlow<HomeState> = repository.observeDecisions()
        .map { history ->
            HomeState(
                history = history,
                analytics = repository.getAnalytics(),
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeState()
        )

    private var lastDeleted: DecisionRecord? = null

    fun delete(id: Long) {
        viewModelScope.launch {
            lastDeleted = state.value.history.firstOrNull { it.id == id }
            repository.softDelete(id)
        }
    }

    fun undoDelete() {
        val record = lastDeleted ?: return
        viewModelScope.launch {
            repository.restore(record.id)
            lastDeleted = null
        }
    }

    fun deleteAll() = viewModelScope.launch { repository.deleteAll() }
}

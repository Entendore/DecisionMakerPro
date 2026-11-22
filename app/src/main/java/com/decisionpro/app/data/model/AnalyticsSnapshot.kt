package com.decisionpro.app.data.model

data class AnalyticsSnapshot(
    val totalCount: Int = 0,
    val byType: Map<String, Int> = emptyMap(),
    val mostFrequentChoice: String? = null,
    val confidenceHistory: List<Int> = emptyList(),
    val recent: List<DecisionRecord> = emptyList()
) {
    companion object { val EMPTY = AnalyticsSnapshot() }
}

package com.decisionpro.app.data.repository

import com.decisionpro.app.data.local.DecisionDao
import com.decisionpro.app.data.model.AnalyticsSnapshot
import com.decisionpro.app.data.model.DecisionRecord
import com.decisionpro.app.di.IoDispatcher
import com.decisionpro.app.util.crash.CrashReporter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val exception: AppException) : AppResult<Nothing>()
}

sealed class AppException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    class DatabaseError(origin: Throwable) :
        AppException("Local storage error: ${origin.message}", origin)

    class ValidationError(field: String, reason: String) :
        AppException("$field: $reason")
}

/**
 * Contracts: all I/O on @IoDispatcher · reads never throw (degrade to
 * empty) · writes return AppResult · user content length-capped.
 */
@Singleton
class DecisionRepository @Inject constructor(
    private val dao: DecisionDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val crashReporter: CrashReporter
) {

    fun observeDecisions(): Flow<List<DecisionRecord>> =
        dao.observeAll()
            .catch { e ->
                crashReporter.recordException(e, tag = "repo.observe")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)

    suspend fun saveDecision(
        question: String,
        choice: String,
        type: String,
        confidence: Int? = null,
        decisionTimeMs: Long? = null
    ): AppResult<Long> {
        validate(question, choice, confidence)?.let { return AppResult.Error(it) }

        return withContext(ioDispatcher) {
            runCatching {
                dao.insert(
                    DecisionRecord(
                        question = question.trim().take(MAX_QUESTION_LENGTH),
                        choice = choice.trim().take(MAX_CHOICE_LENGTH),
                        type = type,
                        confidence = confidence,
                        decisionTimeMs = decisionTimeMs
                    )
                )
            }.fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = { e ->
                    crashReporter.recordException(e, tag = "repo.save")
                    AppResult.Error(AppException.DatabaseError(e))
                }
            )
        }
    }

    suspend fun getAllOnce(): List<DecisionRecord> = withContext(ioDispatcher) {
        runCatching { dao.getAllOnce() }
            .onFailure { e -> crashReporter.recordException(e, tag = "repo.getAll") }
            .getOrElse { emptyList() }
    }

    suspend fun getAnalytics(): AnalyticsSnapshot = withContext(ioDispatcher) {
        runCatching {
            val all = dao.getAllOnce()
            AnalyticsSnapshot(
                totalCount = all.size,
                byType = all.groupingBy { it.type }.eachCount(),
                mostFrequentChoice = all.groupingBy { it.choice }.eachCount()
                    .maxByOrNull { it.value }?.key,
                confidenceHistory = all.mapNotNull { it.confidence }.takeLast(30),
                recent = all.take(10)
            )
        }.getOrElse { e ->
            crashReporter.recordException(e, tag = "repo.analytics")
            AnalyticsSnapshot.EMPTY
        }
    }

    suspend fun softDelete(id: Long): Boolean = withContext(ioDispatcher) {
        runCatching { dao.softDelete(id); true }
            .onFailure { e -> crashReporter.recordException(e, tag = "repo.delete") }
            .getOrDefault(false)
    }

    suspend fun restore(id: Long): Boolean = withContext(ioDispatcher) {
        runCatching { dao.restore(id); true }
            .onFailure { e -> crashReporter.recordException(e, tag = "repo.restore") }
            .getOrDefault(false)
    }

    suspend fun deleteAll(): Boolean = withContext(ioDispatcher) {
        runCatching { dao.deleteAll(); true }
            .onFailure { e -> crashReporter.recordException(e, tag = "repo.deleteAll") }
            .getOrDefault(false)
    }

    suspend fun count(): Int = withContext(ioDispatcher) {
        runCatching { dao.count() }
            .onFailure { e -> crashReporter.recordException(e, tag = "repo.count") }
            .getOrDefault(0)
    }

    private fun validate(
        question: String, choice: String, confidence: Int?
    ): AppException.ValidationError? = when {
        question.isBlank() -> AppException.ValidationError("question", "cannot be blank")
        choice.isBlank() -> AppException.ValidationError("choice", "cannot be blank")
        confidence != null && confidence !in 0..100 ->
            AppException.ValidationError("confidence", "must be 0–100, was $confidence")
        else -> null
    }

    companion object {
        const val MAX_QUESTION_LENGTH = 500
        const val MAX_CHOICE_LENGTH = 200
    }
}

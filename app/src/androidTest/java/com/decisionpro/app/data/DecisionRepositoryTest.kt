package com.decisionpro.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.decisionpro.app.data.local.DecisionDatabase
import com.decisionpro.app.data.repository.AppResult
import com.decisionpro.app.data.repository.DecisionRepository
import com.decisionpro.app.util.crash.DebugCrashReporter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DecisionRepositoryTest {

    private lateinit var db: DecisionDatabase
    private lateinit var repo: DecisionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, DecisionDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = DecisionRepository(db.decisionDao(), Dispatchers.IO, DebugCrashReporter())
    }

    @After fun tearDown() { db.close() }

    @Test
    fun saveAndObserve_roundTrips() = runTest {
        val result = repo.saveDecision("Job offer?", "Accept", "matrix", 80)
        assertThat(result).isInstanceOf(AppResult.Success::class.java)

        val records = repo.observeDecisions().first()
        assertThat(records).hasSize(1)
        assertThat(records[0].confidence).isEqualTo(80)
    }

    @Test
    fun softDeleteHidesRow_andRestoreBringsItBack() = runTest {
        val id = (repo.saveDecision("Q", "A", "tree") as AppResult.Success).data

        repo.softDelete(id)
        assertThat(repo.observeDecisions().first()).isEmpty()

        repo.restore(id)
        assertThat(repo.observeDecisions().first()).hasSize(1)
    }

    @Test
    fun blankQuestionFailsValidation_withoutTouchingDatabase() = runTest {
        assertThat(repo.saveDecision("   ", "A", "matrix"))
            .isInstanceOf(AppResult.Error::class.java)
        assertThat(repo.count()).isEqualTo(0)
    }

    @Test
    fun analyticsAggregatesTypesAndConfidence() = runTest {
        repo.saveDecision("Q1", "A", "matrix", 70)
        repo.saveDecision("Q2", "B", "matrix", 50)
        repo.saveDecision("Q3", "C", "tree", null)

        val snapshot = repo.getAnalytics()
        assertThat(snapshot.totalCount).isEqualTo(3)
        assertThat(snapshot.byType["matrix"]).isEqualTo(2)
        assertThat(snapshot.confidenceHistory).containsExactly(70, 50).inOrder()
    }

    @Test
    fun deleteAllIsTotal_forGdprErasure() = runTest {
        repo.saveDecision("Q", "A", "matrix")
        repo.saveDecision("Q", "B", "tree")
        repo.deleteAll()
        assertThat(repo.count()).isEqualTo(0)
    }
}

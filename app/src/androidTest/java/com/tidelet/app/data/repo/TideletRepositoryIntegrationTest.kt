package com.tidelet.app.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tidelet.app.data.db.TideletDatabase
import com.tidelet.app.data.prefs.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Exercises [RoomTideletRepository] against a real Room in-memory database +
 * real DataStore. Round-trips the most important invariants end-to-end.
 */
@RunWith(AndroidJUnit4::class)
class TideletRepositoryIntegrationTest {

    private lateinit var db: TideletDatabase
    private lateinit var prefs: UserPreferences
    private lateinit var repo: RoomTideletRepository

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TideletDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefs = UserPreferences(context)
        prefs.clearAll()
        repo = RoomTideletRepository(db, prefs)
    }

    @After
    fun tearDown() = runTest {
        db.close()
        prefs.clearAll()
    }

    @Test
    fun addReason_isNoOpForWhitespace() = runTest {
        repo.addReason("   ")
        repo.addReason("")

        val reasons = repo.reasons.first()
        assertThat(reasons).isEmpty()
    }

    @Test
    fun addReason_trimsAndPersists() = runTest {
        repo.addReason("   hello   ")

        val reasons = repo.reasons.first()
        assertThat(reasons.map { it.text }).containsExactly("hello")
    }

    @Test
    fun addFunctionalAnalysis_returnsMinusOneWhenAllBlank() = runTest {
        val id = repo.addFunctionalAnalysis(
            date = LocalDate.of(2026, 4, 18),
            antecedent = "  ",
            thoughtAtMoment = "   ",
            followingAction = "",
        )

        assertThat(id).isEqualTo(-1L)
    }

    @Test
    fun wipeAll_clearsRoomAndDataStore() = runTest {
        repo.addReason("keep me")
        repo.completeOnboarding(LocalDate.of(2026, 4, 1))

        repo.wipeAll()

        assertThat(repo.reasons.first()).isEmpty()
        assertThat(repo.profile.first().onboardingComplete).isFalse()
        assertThat(repo.profile.first().startDateEpochDay).isNull()
    }
}

package com.tidelet.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CheckInDaoTest {

    private lateinit var db: TideletDatabase
    private lateinit var dao: CheckInDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TideletDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.checkInDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsert_thenFindByDate_returnsRow() = runTest {
        val row = CheckIn(
            date = "2026-04-18",
            didDrink = false,
            drinkCount = null,
            mood = 4,
            trigger = null,
            note = "clear head",
        )

        dao.upsert(row)
        val found = dao.findByDate("2026-04-18")

        assertThat(found).isNotNull()
        assertThat(found!!.didDrink).isFalse()
        assertThat(found.mood).isEqualTo(4)
        assertThat(found.note).isEqualTo("clear head")
    }

    @Test
    fun upsert_sameDate_replacesPriorRow() = runTest {
        dao.upsert(CheckIn(date = "2026-04-18", didDrink = true, drinkCount = 2))
        dao.upsert(CheckIn(date = "2026-04-18", didDrink = true, drinkCount = 5))

        val found = dao.findByDate("2026-04-18")

        assertThat(found?.drinkCount).isEqualTo(5)
    }

    @Test
    fun observeAll_emitsNewList_afterInsert() = runTest {
        dao.observeAll().test {
            assertThat(awaitItem()).isEmpty()

            dao.upsert(CheckIn(date = "2026-04-18", didDrink = false))

            val next = awaitItem()
            assertThat(next).hasSize(1)
            assertThat(next.first().date).isEqualTo("2026-04-18")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun countDrinkFreeDays_countsOnlyDidDrinkFalseRows() = runTest {
        dao.upsert(CheckIn(date = "2026-04-16", didDrink = false))
        dao.upsert(CheckIn(date = "2026-04-17", didDrink = true, drinkCount = 2))
        dao.upsert(CheckIn(date = "2026-04-18", didDrink = false))

        assertThat(dao.countDrinkFreeDays()).isEqualTo(2)
    }

    @Test
    fun clear_wipesTable() = runTest {
        dao.upsert(CheckIn(date = "2026-04-18", didDrink = false))

        dao.clear()

        assertThat(dao.findByDate("2026-04-18")).isNull()
    }
}

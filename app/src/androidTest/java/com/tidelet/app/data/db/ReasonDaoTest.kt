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
class ReasonDaoTest {

    private lateinit var db: TideletDatabase
    private lateinit var dao: ReasonDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TideletDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.reasonDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun insert_thenObserveAll_ordersByCreatedAsc() = runTest {
        dao.insert(Reason(text = "second", createdEpochMillis = 2_000L))
        dao.insert(Reason(text = "first", createdEpochMillis = 1_000L))
        dao.insert(Reason(text = "third", createdEpochMillis = 3_000L))

        dao.observeAll().test {
            val rows = awaitItem()
            assertThat(rows.map { it.text }).containsExactly("first", "second", "third").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteById_removesSpecificRow() = runTest {
        val id = dao.insert(Reason(text = "keep"))
        val doomed = dao.insert(Reason(text = "delete me"))

        dao.deleteById(doomed)

        dao.observeAll().test {
            val rows = awaitItem()
            assertThat(rows).hasSize(1)
            assertThat(rows.first().id).isEqualTo(id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

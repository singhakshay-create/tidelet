package com.tidelet.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAOs ("Data Access Objects") are Room's way of declaring queries.
 * Room generates the implementation at compile time (via KSP).
 */

@Dao
interface CheckInDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkIn: CheckIn)

    @Query("SELECT * FROM check_in WHERE date = :date LIMIT 1")
    suspend fun findByDate(date: String): CheckIn?

    @Query("SELECT * FROM check_in ORDER BY date DESC")
    fun observeAll(): Flow<List<CheckIn>>

    @Query("SELECT COUNT(*) FROM check_in WHERE didDrink = 0")
    suspend fun countDrinkFreeDays(): Int

    @Query("DELETE FROM check_in")
    suspend fun clear()
}

@Dao
interface CravingEventDao {
    @Insert
    suspend fun insert(event: CravingEvent): Long

    @Query("SELECT * FROM craving_event ORDER BY timestampEpochMillis DESC")
    fun observeAll(): Flow<List<CravingEvent>>

    @Query("SELECT COUNT(*) FROM craving_event WHERE outcome = :outcome")
    suspend fun countByOutcome(outcome: String): Int

    @Query("DELETE FROM craving_event")
    suspend fun clear()
}

@Dao
interface ReasonDao {
    @Insert
    suspend fun insert(reason: Reason): Long

    @Query("SELECT * FROM reason ORDER BY createdEpochMillis ASC")
    fun observeAll(): Flow<List<Reason>>

    @Query("DELETE FROM reason WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM reason")
    suspend fun clear()
}

@Dao
interface JournalEntryDao {
    @Insert
    suspend fun insert(entry: JournalEntry): Long

    @Query("SELECT * FROM journal_entry ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entry WHERE dateIso = :dateIso ORDER BY createdAtEpochMillis DESC")
    fun observeByDate(dateIso: String): Flow<List<JournalEntry>>

    @Query("DELETE FROM journal_entry")
    suspend fun clear()
}

@Dao
interface WeeklyReflectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reflection: WeeklyReflection)

    @Query("SELECT * FROM weekly_reflection WHERE weekStartIso = :weekStartIso LIMIT 1")
    suspend fun findByWeekStart(weekStartIso: String): WeeklyReflection?

    @Query("SELECT * FROM weekly_reflection ORDER BY weekStartIso DESC")
    fun observeAll(): Flow<List<WeeklyReflection>>

    @Query("DELETE FROM weekly_reflection")
    suspend fun clear()
}

@Dao
interface ThoughtRecordDao {
    @Insert
    suspend fun insert(record: ThoughtRecord): Long

    @Query("SELECT * FROM thought_record ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<ThoughtRecord>>

    @Query(
        """
        SELECT distortionTag AS tag, COUNT(*) AS count
        FROM thought_record
        WHERE distortionTag IS NOT NULL
        GROUP BY distortionTag
        ORDER BY count DESC
        """
    )
    fun observeTagCounts(): Flow<List<TagCount>>

    @Query("DELETE FROM thought_record")
    suspend fun clear()
}

/** Tiny projection type for [ThoughtRecordDao.observeTagCounts]. */
data class TagCount(val tag: String, val count: Int)

@Dao
interface FunctionalAnalysisDao {
    @Insert
    suspend fun insert(analysis: FunctionalAnalysis): Long

    @Query("SELECT * FROM functional_analysis ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<FunctionalAnalysis>>

    @Query("DELETE FROM functional_analysis")
    suspend fun clear()
}

@Dao
interface RelapsePlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: RelapsePlan)

    // There is only ever one row, but we expose it as a Flow so the UI can
    // observe edits made from any screen.
    @Query("SELECT * FROM relapse_plan WHERE id = 1 LIMIT 1")
    fun observeSingleton(): Flow<RelapsePlan?>

    @Query("SELECT * FROM relapse_plan WHERE id = 1 LIMIT 1")
    suspend fun findSingleton(): RelapsePlan?

    @Query("DELETE FROM relapse_plan")
    suspend fun clear()
}

@Dao
interface RefusalPhraseDao {
    @Insert
    suspend fun insert(phrase: RefusalPhrase): Long

    @Query("SELECT * FROM refusal_phrase ORDER BY createdEpochMillis ASC")
    fun observeAll(): Flow<List<RefusalPhrase>>

    @Query("DELETE FROM refusal_phrase WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM refusal_phrase")
    suspend fun clear()
}

@Dao
interface MilestoneLetterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(letter: MilestoneLetter)

    @Query("SELECT * FROM milestone_letter WHERE milestoneDays = :days LIMIT 1")
    suspend fun findByMilestone(days: Int): MilestoneLetter?

    @Query("SELECT * FROM milestone_letter ORDER BY milestoneDays ASC")
    fun observeAll(): Flow<List<MilestoneLetter>>

    /**
     * The most recent letter whose milestone is < current streak and which has
     * not yet been revealed. Feeds the Home reveal card.
     */
    @Query(
        """
        SELECT * FROM milestone_letter
        WHERE milestoneDays < :currentDays AND revealedAtEpochMillis IS NULL
        ORDER BY milestoneDays DESC
        LIMIT 1
        """
    )
    suspend fun findOldestUnrevealedBefore(currentDays: Int): MilestoneLetter?

    @Query("UPDATE milestone_letter SET revealedAtEpochMillis = :atMillis WHERE milestoneDays = :days")
    suspend fun markRevealed(days: Int, atMillis: Long)

    @Query("DELETE FROM milestone_letter")
    suspend fun clear()
}

// PHASE_2 §9A5 — evening mini-review. One row per date at most; upsert-by-PK
// means a same-day re-submit overwrites rather than stacking entries.
@Dao
interface EveningReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: EveningReview)

    @Query("SELECT * FROM evening_review WHERE dateIso = :dateIso LIMIT 1")
    suspend fun findByDate(dateIso: String): EveningReview?

    @Query("SELECT * FROM evening_review ORDER BY dateIso DESC")
    fun observeAll(): Flow<List<EveningReview>>

    @Query("DELETE FROM evening_review")
    suspend fun clear()
}

// PHASE_2 §9B2 — insight card shown markers. Used by the selector to enforce
// recency (no repeat within 60 days) and spacing (no card more than once
// every 2 days).
@Dao
interface InsightCardShownDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: InsightCardShown)

    @Query("SELECT * FROM insight_card_shown")
    suspend fun all(): List<InsightCardShown>

    @Query("SELECT MAX(shownAtEpochMillis) FROM insight_card_shown")
    suspend fun lastShownAtMillis(): Long?

    @Query("DELETE FROM insight_card_shown")
    suspend fun clear()
}

package com.tidelet.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The single Room database for the app.
 *
 * When you add a new entity:
 *  1. Add it to the `entities` array below.
 *  2. Bump the `version` number.
 *  3. Either supply a Migration or allow destructive migration during dev
 *     (we do the latter for now — safe because all data is local and early-beta).
 */
@Database(
    entities = [
        CheckIn::class,
        CravingEvent::class,
        Reason::class,
        JournalEntry::class,
        WeeklyReflection::class,
        // CBT tier
        ThoughtRecord::class,
        FunctionalAnalysis::class,
        RelapsePlan::class,
        RefusalPhrase::class,
        // Phase 2
        MilestoneLetter::class,
        // Phase 2 §9A5
        EveningReview::class,
        // Phase 2 §9B2
        InsightCardShown::class,
    ],
    version = 10,
    exportSchema = true,
)
abstract class TideletDatabase : RoomDatabase() {
    abstract fun checkInDao(): CheckInDao
    abstract fun cravingEventDao(): CravingEventDao
    abstract fun reasonDao(): ReasonDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun weeklyReflectionDao(): WeeklyReflectionDao

    // CBT tier
    abstract fun thoughtRecordDao(): ThoughtRecordDao
    abstract fun functionalAnalysisDao(): FunctionalAnalysisDao
    abstract fun relapsePlanDao(): RelapsePlanDao
    abstract fun refusalPhraseDao(): RefusalPhraseDao

    // Phase 2
    abstract fun milestoneLetterDao(): MilestoneLetterDao
    abstract fun eveningReviewDao(): EveningReviewDao
    abstract fun insightCardShownDao(): InsightCardShownDao

    companion object {
        @Volatile private var INSTANCE: TideletDatabase? = null

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE craving_event ADD COLUMN intensity INTEGER DEFAULT NULL")
            }
        }

        fun get(context: Context): TideletDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                TideletDatabase::class.java,
                "tidelet.db",
            )
                .addMigrations(MIGRATION_9_10)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}

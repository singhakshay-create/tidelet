package com.tidelet.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The user-authored relapse-prevention plan.
 *
 * Three long-form text fields that together make Marlatt's classic
 * relapse-prevention model portable into the app: high-risk situations, early
 * warning signs, coping plan. The user writes this in a calm moment and can
 * pull it up from Settings or from the Reasons screen during a craving.
 *
 * Singleton by design — there's only ever one plan. We pin [id] to 1 so
 * `upsert` behaves like "save the one plan" without additional DAO logic.
 * (Rationale: a list of plans is structurally more complex without clinical
 * benefit — a single evolving document mirrors how real CBT therapy handles
 * this worksheet.)
 */
@Entity(tableName = "relapse_plan")
data class RelapsePlan(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val highRiskSituations: String = "",
    val earlyWarningSigns: String = "",
    val copingPlan: String = "",
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
) {
    companion object {
        const val SINGLETON_ID: Int = 1
    }
}

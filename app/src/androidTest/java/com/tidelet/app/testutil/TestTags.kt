package com.tidelet.app.testutil

/**
 * Stable Compose `testTag` constants referenced by instrumented UI tests.
 *
 * Keeping these in one file means tests don't match on localisable strings
 * (titles, button labels) and screens can be edited without breaking tests
 * so long as the tagged nodes remain.
 */
object TestTags {
    // Onboarding
    const val ONBOARDING_WELCOME_CONTINUE = "onboarding_welcome_continue"
    const val ONBOARDING_START_DATE_TODAY = "onboarding_start_date_today"
    const val ONBOARDING_START_DATE_YESTERDAY = "onboarding_start_date_yesterday"
    const val ONBOARDING_START_DATE_EARLIER = "onboarding_start_date_earlier"
    const val ONBOARDING_FINISH = "onboarding_finish"

    // Home
    const val HOME_STREAK_DAYS = "home_streak_days"
    const val HOME_NEXT_MILESTONE = "home_next_milestone"
    const val HOME_WEEKLY_REFLECTION_CARD = "home_weekly_reflection_card"

    // SOS
    const val SOS_GRID_RIDE_WAVE = "sos_grid_ride_wave"
    const val SOS_GRID_BREATHE = "sos_grid_breathe"
    const val SOS_GRID_REASONS = "sos_grid_reasons"
    const val SOS_GRID_DISTRACTIONS = "sos_grid_distractions"

    // Ride the Wave
    const val WAVE_TIMER_TEXT = "wave_timer_text"
    const val WAVE_END_EARLY = "wave_end_early"
    const val WAVE_GOT_THROUGH = "wave_got_through"
    const val WAVE_DRANK = "wave_drank"
    const val WAVE_ANALYSIS_PANEL = "wave_analysis_panel"

    // Breathe
    const val BREATHE_PHASE_LABEL = "breathe_phase_label"
    const val BREATHE_CYCLE_COUNT = "breathe_cycle_count"

    // Reasons
    const val REASONS_EMPTY_CTA = "reasons_empty_cta"
    const val REASONS_INPUT = "reasons_input"
    const val REASONS_SUBMIT = "reasons_submit"
    const val REASONS_CHIP_PREFIX = "reasons_chip_"
    const val REASONS_DELETE_PREFIX = "reasons_delete_"

    // Distractions
    const val DISTRACTIONS_SUGGESTION = "distractions_suggestion"
    const val DISTRACTIONS_SHUFFLE = "distractions_shuffle"
    const val DISTRACTIONS_DID_IT = "distractions_did_it"
    const val DISTRACTIONS_NOTHING_WORKED = "distractions_nothing_worked"

    // Check-in
    const val CHECKIN_DID_DRINK_YES = "checkin_did_drink_yes"
    const val CHECKIN_DID_DRINK_NO = "checkin_did_drink_no"
    const val CHECKIN_DRINK_COUNT = "checkin_drink_count"
    const val CHECKIN_DRINK_INCREMENT = "checkin_drink_increment"
    const val CHECKIN_DRINK_DECREMENT = "checkin_drink_decrement"
    const val CHECKIN_TRIGGER_CHIPS = "checkin_trigger_chips"
    const val CHECKIN_SAVE = "checkin_save"
}

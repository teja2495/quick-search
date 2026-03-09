package com.tk.quicksearch.app.startup

/**
 * Startup service-level targets used by startup orchestration and benchmark definitions.
 *
 * These are engineering targets, not hard runtime enforcement.
 */
object StartupSla {
    const val SEARCH_BAR_FIRST_FRAME_BUDGET_MS: Long = 16L
    const val PHASE_1_TARGET_MS: Long = 250L
    const val PHASE_2_TARGET_MS: Long = 2_000L
}

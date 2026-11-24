package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Discreetly triggers Google Play's official In-App Review flow after genuine
 * sustained usage (at least 12 logged entries and 5 days since first launch),
 * with a 60-day cooldown so users are never nagged.
 */
object InAppReviewManager {
    private const val TAG = "InAppReviewManager"
    private const val PREFS_NAME = "finsay_in_app_review_prefs"
    private const val KEY_FIRST_OPEN_TS = "first_open_ts"
    private const val KEY_ENTRIES_LOGGED = "entries_logged_count"
    private const val KEY_LAST_PROMPT_TS = "last_review_prompt_ts"

    private const val MIN_ENTRIES_THRESHOLD = 12
    private const val MIN_DAYS_INSTALLED_MS = 5L * 24L * 60L * 60L * 1000L
    private const val COOLDOWN_MS = 60L * 24L * 60L * 60L * 1000L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordAppOpen(context: Context) {
        val p = prefs(context)
        if (p.getLong(KEY_FIRST_OPEN_TS, 0L) == 0L) {
            p.edit().putLong(KEY_FIRST_OPEN_TS, System.currentTimeMillis()).apply()
        }
    }

    /**
     * Increments the logged-entry counter and returns true if the user is now eligible
     * for an In-App Review prompt.
     */
    fun recordEntryLoggedAndCheckEligible(context: Context): Boolean {
        val p = prefs(context)
        val now = System.currentTimeMillis()
        val firstOpen = p.getLong(KEY_FIRST_OPEN_TS, 0L).let {
            if (it == 0L) {
                p.edit().putLong(KEY_FIRST_OPEN_TS, now).apply()
                now
            } else it
        }
        val newCount = p.getInt(KEY_ENTRIES_LOGGED, 0) + 1
        p.edit().putInt(KEY_ENTRIES_LOGGED, newCount).apply()

        if (newCount < MIN_ENTRIES_THRESHOLD) return false
        if (now - firstOpen < MIN_DAYS_INSTALLED_MS) return false

        val lastPrompt = p.getLong(KEY_LAST_PROMPT_TS, 0L)
        if (lastPrompt > 0L && (now - lastPrompt) < COOLDOWN_MS) return false

        return true
    }

    fun launchReviewIfEligible(activity: Activity) {
        try {
            val p = prefs(activity)
            val now = System.currentTimeMillis()
            val lastPrompt = p.getLong(KEY_LAST_PROMPT_TS, 0L)
            if (lastPrompt > 0L && (now - lastPrompt) < COOLDOWN_MS) return

            // Record attempt timestamp immediately to respect Google Play quota guidelines
            p.edit().putLong(KEY_LAST_PROMPT_TS, now).apply()

            val reviewManager = ReviewManagerFactory.create(activity)
            val request = reviewManager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        reviewManager.launchReviewFlow(activity, reviewInfo)
                            .addOnCompleteListener {
                                Log.d(TAG, "Play In-App Review flow finished")
                            }
                    }
                } else {
                    Log.d(TAG, "Play In-App Review flow unavailable: ${task.exception?.message}")
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Safe fallback skipping In-App Review: ${t.message}")
        }
    }
}

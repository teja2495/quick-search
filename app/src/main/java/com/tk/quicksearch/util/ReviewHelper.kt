package com.tk.quicksearch.util

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import com.tk.quicksearch.data.UserAppPreferences

/**
 * Utility class to handle in-app review flow using Google Play's ReviewManager API.
 */
object ReviewHelper {

    private const val TAG = "ReviewHelper"

    /**
     * Requests an in-app review if the user is eligible based on days since first app open.
     * Shows review prompt after 2 days (first prompt) and 4 days after first prompt (second prompt).
     *
     * @param activity The activity context to show the review flow
     * @param userPreferences User preferences to check eligibility and track review prompts
     */
    fun requestReviewIfEligible(activity: Activity, userPreferences: UserAppPreferences) {
        if (!userPreferences.shouldShowReviewPrompt()) {
            return
        }

        try {
            val reviewManager = ReviewManagerFactory.create(activity)
            val requestReviewFlow = reviewManager.requestReviewFlow()

            requestReviewFlow.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                    
                    flow.addOnCompleteListener { _ ->
                        // The flow has finished. Whether the user actually reviewed or not,
                        // we increment the prompted count and record the time so we don't show it again at this milestone.
                        userPreferences.recordReviewPromptTime()
                        userPreferences.incrementReviewPromptedCount()
                        Log.d(TAG, "Review flow completed. Prompted count: ${userPreferences.getReviewPromptedCount()}")
                    }
                } else {
                    // If we couldn't get review info, still record to avoid repeated attempts
                    Log.w(TAG, "Failed to request review flow", task.exception)
                    userPreferences.recordReviewPromptTime()
                    userPreferences.incrementReviewPromptedCount()
                }
            }
        } catch (e: Exception) {
            // Gracefully handle any errors - review API should never crash the app
            Log.e(TAG, "Error requesting review", e)
            userPreferences.recordReviewPromptTime()
            userPreferences.incrementReviewPromptedCount()
        }
    }
}

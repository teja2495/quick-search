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
     * Requests an in-app review if the user is eligible based on days and app opens.
     * First prompt: at least 5 opens AND at least 2 days since first open.
     * Second prompt: at least 4 days since first prompt AND at least 5 more opens.
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
                        // we record the time, app open count, and increment prompted count.
                        userPreferences.recordReviewPromptTime()
                        userPreferences.recordAppOpenCountAtPrompt()
                        userPreferences.incrementReviewPromptedCount()
                        Log.d(TAG, "Review flow completed. Prompted count: ${userPreferences.getReviewPromptedCount()}")
                    }
                } else {
                    // If we couldn't get review info, still record to avoid repeated attempts
                    Log.w(TAG, "Failed to request review flow", task.exception)
                    userPreferences.recordReviewPromptTime()
                    userPreferences.recordAppOpenCountAtPrompt()
                    userPreferences.incrementReviewPromptedCount()
                }
            }
        } catch (e: Exception) {
            // Gracefully handle any errors - review API should never crash the app
            Log.e(TAG, "Error requesting review", e)
            userPreferences.recordReviewPromptTime()
            userPreferences.recordAppOpenCountAtPrompt()
            userPreferences.incrementReviewPromptedCount()
        }
    }
}

package com.tk.quicksearch.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object FeedbackUtils {

    private const val FEEDBACK_EMAIL = "tejakarlapudi.apps@gmail.com"

    fun launchFeedbackEmail(context: Context, feedbackText: String?) {
        val versionName = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }

        val androidVersion = Build.VERSION.RELEASE
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val emailBody = buildFeedbackBody(feedbackText, androidVersion, deviceModel)
        val subject = "Quick Search Feedback - v$versionName"

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(
                "mailto:$FEEDBACK_EMAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(emailBody)}"
            )
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle case where no email app is installed
        }
    }

    private fun buildFeedbackBody(
        feedbackText: String?,
        androidVersion: String,
        deviceModel: String
    ): String {
        val trimmedFeedback = feedbackText?.trim().orEmpty()
        return buildString {
            if (trimmedFeedback.isNotEmpty()) {
                append(trimmedFeedback)
                append("\n\n")
            } else {
                append("\n\n")
            }
            append("---\n")
            append("Android Version: ")
            append(androidVersion)
            append("\nDevice: ")
            append(deviceModel)
        }
    }
}

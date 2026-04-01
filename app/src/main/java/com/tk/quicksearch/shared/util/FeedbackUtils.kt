package com.tk.quicksearch.shared.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider

object FeedbackUtils {
    private const val FEEDBACK_EMAIL = "tejakarlapudi.apps@gmail.com"

    fun launchFeedbackEmail(
        context: Context,
        feedbackText: String?,
    ) {
        val versionName = getVersionName(context)
        val androidVersion = Build.VERSION.RELEASE
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val emailBody = buildFeedbackBody(feedbackText, androidVersion, deviceModel)
        val subject = "Quick Search Feedback - v$versionName"

        val intent =
            Intent(Intent.ACTION_SENDTO).apply {
                data =
                    Uri.parse(
                        "mailto:$FEEDBACK_EMAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(emailBody)}",
                    )
            }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle case where no email app is installed
        }
    }

    fun launchFeedbackEmailWithCrashLog(context: Context) {
        val versionName = getVersionName(context)
        val androidVersion = Build.VERSION.RELEASE
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val emailBody = buildFeedbackBody(null, androidVersion, deviceModel)
        val subject = "Quick Search Logs - v$versionName"

        val crashLogFile = CrashLogManager.getOrCreateLogFile(context)
        val crashLogUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            crashLogFile,
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            // "text/plain" matches the actual crash log file type — "message/rfc822" is
            // for .eml files and causes email clients to ignore EXTRA_STREAM as an attachment
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, emailBody)
            putExtra(Intent.EXTRA_STREAM, crashLogUri)
            // ClipData is required for FLAG_GRANT_READ_URI_PERMISSION to propagate
            // through createChooser to the resolved email app on Android 7+
            clipData = ClipData.newRawUri("", crashLogUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(sendIntent, null))
        } catch (e: Exception) {
            // Handle case where no email app is installed
        }
    }

    private fun getVersionName(context: Context): String =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }

    private fun buildFeedbackBody(
        feedbackText: String?,
        androidVersion: String,
        deviceModel: String,
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

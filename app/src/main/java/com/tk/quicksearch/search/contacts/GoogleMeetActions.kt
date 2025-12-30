package com.tk.quicksearch.search.contacts

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import com.tk.quicksearch.R

/**
 * Google Meet-specific contact intent helpers for video calling.
 */
object GoogleMeetActions {

    /**
     * Opens a Google Meet video call using the working method identified through testing.
     * Uses the custom action "com.google.android.apps.tachyon.action.CALL" which was confirmed to work.
     */
    fun openGoogleMeet(context: Application, dataId: Long): Boolean {
        return try {
            val pm = context.packageManager

            val meetPackage = "com.google.android.apps.tachyon"

            if (pm.getLaunchIntentForPackage(meetPackage) == null) {
                Toast.makeText(
                    context,
                    "Google Meet is not installed",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }

            // Extract phone number from contact data
            var phoneNumber: String? = null
            try {
                val phoneUri = Uri.withAppendedPath(ContactsContract.Data.CONTENT_URI, dataId.toString())
                val phoneCursor = context.contentResolver.query(
                    phoneUri,
                    arrayOf(ContactsContract.Data.DATA1),
                    null, null, null
                )

                phoneCursor?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        phoneNumber = cursor.getString(0)
                    }
                }
            } catch (e: Exception) {
                // Failed to extract phone number, continue without it
            }

            if (phoneNumber.isNullOrBlank()) {
                Log.w("ContactIntentHelpers", "No phone number found, cannot make call")
                Toast.makeText(
                    context,
                    "Cannot make Google Meet call - no phone number found",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }

            // Use the confirmed working method: custom action with Google Meet package
            val callIntent = Intent("com.google.android.apps.tachyon.action.CALL").apply {
                data = Uri.parse("tel:$phoneNumber")
                setPackage(meetPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (callIntent.resolveActivity(pm) != null) {
                context.startActivity(callIntent)
                return true
            } else {
                Log.w("ContactIntentHelpers", "Google Meet custom action not resolved - app may not be installed or updated")
                Toast.makeText(
                    context,
                    "Google Meet call failed - app may not support this method",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }

        } catch (e: Exception) {
            Log.e("ContactIntentHelpers", "Failed to open Google Meet video call", e)
            Toast.makeText(
                context,
                "Failed to open Google Meet video call",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }
}

package com.tk.quicksearch.search.contacts.actions

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.ContactInfo

/**
 * Call and SMS related contact actions.
 * Consolidates all calling and messaging functionality in one place.
 */
object CallSmsActions {
    /**
     * Opens the dialer without requiring call permission.
     */
    fun performDial(
        context: Application,
        number: String,
    ) {
        val intent =
            Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(number)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    }

    /**
     * Initiates a direct phone call. Requires CALL_PHONE permission.
     */
    fun performDirectCall(
        context: Application,
        number: String,
    ) {
        val intent =
            Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${Uri.encode(number)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    }

    /**
     * Initiates a phone call.
     * Uses ACTION_CALL if CALL_PHONE permission is granted, otherwise falls back to ACTION_DIAL.
     */
    fun performCall(
        context: Application,
        number: String,
    ) {
        val hasCallPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE,
            ) == PackageManager.PERMISSION_GRANTED

        if (hasCallPermission) {
            performDirectCall(context, number)
        } else {
            performDial(context, number)
        }
    }

    /**
     * Opens SMS app for a phone number.
     */
    fun performSms(
        context: Application,
        number: String,
    ) {
        val intent =
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(number)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    }

    /**
     * Opens the default email app with a prefilled recipient.
     */
    fun composeEmail(
        context: Application,
        email: String,
        onShowToast: ((Int) -> Unit)? = null,
    ) {
        if (email.isBlank()) return

        val intent =
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${Uri.encode(email)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        runCatching {
            context.startActivity(intent)
        }.onFailure {
            onShowToast?.invoke(R.string.error_open_email)
        }
    }

    /**
     * Opens a contact's details.
     */
    fun openContact(
        context: Application,
        contactInfo: ContactInfo,
        onShowToast: ((Int) -> Unit)? = null,
    ) {
        val lookupUri = ContactsContract.Contacts.getLookupUri(contactInfo.contactId, contactInfo.lookupKey)
        if (lookupUri == null) {
            onShowToast?.invoke(R.string.error_open_contact)
            return
        }
        val intent =
            Intent(Intent.ACTION_VIEW, lookupUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    }
}

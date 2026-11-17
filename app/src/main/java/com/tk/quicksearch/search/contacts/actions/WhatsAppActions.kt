package com.tk.quicksearch.search.contacts.actions

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.ContactMethodMimeTypes
import com.tk.quicksearch.search.utils.PhoneNumberUtils

/**
 * WhatsApp-specific contact actions.
 * All WhatsApp functionality consolidated in one place.
 */
object WhatsAppActions {
    private const val WHATSAPP_PACKAGE = "com.whatsapp"
    private const val WHATSAPP_MESSAGE_MIME = ContactMethodMimeTypes.WHATSAPP_MESSAGE
    private const val WHATSAPP_VOICE_CALL_MIME = ContactMethodMimeTypes.WHATSAPP_VOICE_CALL
    private const val WHATSAPP_VIDEO_CALL_MIME = ContactMethodMimeTypes.WHATSAPP_VIDEO_CALL

    /**
     * Opens WhatsApp chat using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openWhatsAppChat(
        context: Application,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (dataId == null) {
            Log.w("WhatsAppActions", "No dataId provided for WhatsApp chat")
            return false
        }

        return try {
            if (
                !launchContactDataIntent(
                    context = context,
                    dataId = dataId,
                    packageName = WHATSAPP_PACKAGE,
                    mimeType = WHATSAPP_MESSAGE_MIME,
                )
            ) {
                Log.w("WhatsAppActions", "WhatsApp chat intent cannot be resolved")
                onShowToast?.invoke(R.string.error_whatsapp_not_installed)
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e("WhatsAppActions", "Failed to open WhatsApp chat", e)
            onShowToast?.invoke(R.string.error_whatsapp_chat_failed)
            false
        }
    }

    /** Opens the chat directly, avoiding WhatsApp's contact-profile trampoline. */
    fun openWhatsAppChat(
        context: Application,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
        packageName: String = WHATSAPP_PACKAGE,
    ): Boolean {
        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanNumber == null) {
            onShowToast?.invoke(R.string.error_missing_phone_number)
            return false
        }

        return try {
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${cleanNumber.removePrefix("+")}")).apply {
                    setPackage(packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("WhatsAppActions", "Failed to open WhatsApp chat by phone number", e)
            onShowToast?.invoke(R.string.error_whatsapp_chat_failed)
            false
        }
    }

    /**
     * Initiates a WhatsApp voice call through the contact data row. [whatsAppId] (the row's JID,
     * e.g. `15551234567@s.whatsapp.net`) lets a stale row fall back to opening the chat.
     */
    fun openWhatsAppCall(
        context: Application,
        dataId: Long?,
        whatsAppId: String? = null,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean =
        launchWhatsAppCall(
            context = context,
            dataId = dataId,
            whatsAppId = whatsAppId,
            mimeType = WHATSAPP_VOICE_CALL_MIME,
            unresolvedErrorRes = R.string.error_whatsapp_not_installed,
            failedErrorRes = R.string.error_whatsapp_call_failed,
            onShowToast = onShowToast,
        )

    /** Opens a WhatsApp video call; see [openWhatsAppCall]. */
    fun openWhatsAppVideoCall(
        context: Application,
        dataId: Long?,
        whatsAppId: String? = null,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean =
        launchWhatsAppCall(
            context = context,
            dataId = dataId,
            whatsAppId = whatsAppId,
            mimeType = WHATSAPP_VIDEO_CALL_MIME,
            unresolvedErrorRes = R.string.error_whatsapp_video_call_unavailable,
            failedErrorRes = R.string.error_whatsapp_video_call_failed,
            onShowToast = onShowToast,
        )

    private fun launchWhatsAppCall(
        context: Application,
        dataId: Long?,
        whatsAppId: String?,
        mimeType: String,
        unresolvedErrorRes: Int,
        failedErrorRes: Int,
        onShowToast: ((Int) -> Unit)?,
    ): Boolean {
        if (dataId == null) {
            Log.w("WhatsAppActions", "No dataId provided for WhatsApp call ($mimeType)")
            return false
        }

        return try {
            // WhatsApp's call landing screen closes silently for a row that no longer exists.
            if (!contactDataRowExists(context, dataId, mimeType)) {
                Log.w("WhatsAppActions", "WhatsApp call row $dataId no longer exists")
                return openChatForUnavailableCall(context, whatsAppId, failedErrorRes, onShowToast)
            }
            if (
                launchContactDataIntent(
                    context = context,
                    dataId = dataId,
                    packageName = WHATSAPP_PACKAGE,
                    mimeType = mimeType,
                )
            ) {
                true
            } else {
                Log.w("WhatsAppActions", "WhatsApp call intent cannot be resolved ($mimeType)")
                onShowToast?.invoke(unresolvedErrorRes)
                false
            }
        } catch (e: Exception) {
            Log.e("WhatsAppActions", "Failed to start WhatsApp call ($mimeType)", e)
            onShowToast?.invoke(failedErrorRes)
            false
        }
    }

    private fun openChatForUnavailableCall(
        context: Application,
        whatsAppId: String?,
        failedErrorRes: Int,
        onShowToast: ((Int) -> Unit)?,
    ): Boolean {
        val number = whatsAppId?.substringBefore('@')?.takeIf { it.isNotBlank() }
        if (number == null || !openWhatsAppChat(context, number, onShowToast)) {
            if (number == null) onShowToast?.invoke(failedErrorRes)
            return false
        }
        onShowToast?.invoke(R.string.error_whatsapp_call_unavailable_opened_chat)
        return true
    }
}

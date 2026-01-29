package com.tk.quicksearch.search.overlay

import android.content.Context
import android.content.Intent
import com.tk.quicksearch.app.MainActivity

object OverlayModeController {
    const val EXTRA_FORCE_NORMAL_LAUNCH = "overlay_force_normal_launch"
    const val EXTRA_OPEN_SETTINGS = "overlay_open_settings"
    const val EXTRA_CLOSE_OVERLAY = "overlay_close"
    const val EXTRA_CONTACT_ACTION_PICKER = "overlay_contact_action_picker"
    const val EXTRA_CONTACT_ACTION_PICKER_ID = "overlay_contact_action_picker_id"
    const val EXTRA_CONTACT_ACTION_PICKER_IS_PRIMARY = "overlay_contact_action_picker_primary"
    const val EXTRA_CONTACT_ACTION_PICKER_SERIALIZED_ACTION =
        "overlay_contact_action_picker_serialized_action"

    data class ContactActionRequest(
        val contactId: Long,
        val isPrimary: Boolean,
        val serializedAction: String?,
    )

    fun startOverlay(context: Context) {
        val intent =
            Intent(context, OverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        context.startActivity(intent)
    }

    fun stopOverlay(context: Context) {
        val intent =
            Intent(context, OverlayActivity::class.java).apply {
                putExtra(EXTRA_CLOSE_OVERLAY, true)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            }
        context.startActivity(intent)
    }

    fun openMainActivity(
        context: Context,
        openSettings: Boolean = false,
        contactActionRequest: ContactActionRequest? = null,
    ) {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_FORCE_NORMAL_LAUNCH, true)
                putExtra(EXTRA_OPEN_SETTINGS, openSettings)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (contactActionRequest != null) {
                    putExtra(EXTRA_CONTACT_ACTION_PICKER, true)
                    putExtra(EXTRA_CONTACT_ACTION_PICKER_ID, contactActionRequest.contactId)
                    putExtra(
                        EXTRA_CONTACT_ACTION_PICKER_IS_PRIMARY,
                        contactActionRequest.isPrimary,
                    )
                    putExtra(
                        EXTRA_CONTACT_ACTION_PICKER_SERIALIZED_ACTION,
                        contactActionRequest.serializedAction,
                    )
                }
            }
        context.startActivity(intent)
    }
}

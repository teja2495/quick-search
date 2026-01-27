package com.tk.quicksearch.widget.customButtons

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.utils.ContactIntentHelpers
import com.tk.quicksearch.search.contacts.dialogs.ContactMethodsDialog
import com.tk.quicksearch.search.core.IntentHelpers
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.launchStaticShortcut
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.ui.theme.QuickSearchTheme
import kotlinx.coroutines.launch

class QuickSearchWidgetActionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = CustomWidgetButtonAction.fromJson(
            intent.getStringExtra(EXTRA_CUSTOM_BUTTON_ACTION)
        )
        if (action != null) {
            if (action is CustomWidgetButtonAction.Contact) {
                loadFullContactInfo(action)
            } else {
                handleAction(action)
                finish()
            }
        } else {
            finish()
        }
    }

    private fun loadFullContactInfo(contactAction: CustomWidgetButtonAction.Contact) {
        lifecycleScope.launch {
            val contactRepository = ContactRepository(application.applicationContext)
            val contacts = contactRepository.getContactsByIds(setOf(contactAction.contactId))
            val fullContactInfo = contacts.firstOrNull()
            if (fullContactInfo != null) {
                showContactMethodsDialog(fullContactInfo)
            } else {
                // Fallback to basic contact info if loading fails
                showContactMethodsDialog(contactAction.toContactInfo())
            }
        }
    }

    private fun showContactMethodsDialog(contactInfo: ContactInfo) {
        setContent {
            QuickSearchTheme {
                var showDialog by remember { mutableStateOf(true) }
                if (showDialog) {
                    ContactMethodsDialog(
                        contactInfo = contactInfo,
                        onContactMethodClick = { _, contactMethod ->
                            handleContactMethod(contactMethod)
                            showDialog = false
                            finish()
                        },
                        onDismiss = {
                            showDialog = false
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun handleContactMethod(method: ContactMethod) {
        when (method) {
            is ContactMethod.Phone -> {
                ContactIntentHelpers.performCall(application, method.data)
            }
            is ContactMethod.Sms -> {
                ContactIntentHelpers.performSms(application, method.data)
            }
            is ContactMethod.Email -> {
                ContactIntentHelpers.composeEmail(application, method.data) { resId ->
                    showToast(resId)
                }
            }
            is ContactMethod.WhatsAppCall -> {
                ContactIntentHelpers.openWhatsAppCall(application, method.dataId) { resId ->
                    showToast(resId)
                }
            }
            is ContactMethod.WhatsAppMessage -> {
                ContactIntentHelpers.openWhatsAppChat(application, method.dataId) { resId ->
                    showToast(resId)
                }
            }
            is ContactMethod.WhatsAppVideoCall -> {
                ContactIntentHelpers.openWhatsAppVideoCall(application, method.dataId) { resId ->
                    showToast(resId)
                }
            }
            is ContactMethod.TelegramMessage -> {
                ContactIntentHelpers.openTelegramChat(application, method.dataId) { resId ->
                    showToast(resId)
                }
            }
            is ContactMethod.TelegramCall -> {
                ContactIntentHelpers.openTelegramCall(application, method.dataId) { resId ->
                    showToast(resId)
                }
            }
            is ContactMethod.TelegramVideoCall -> {
                ContactIntentHelpers.openTelegramVideoCall(application, method.dataId, method.data)
            }
            is ContactMethod.VideoCall -> {
                ContactIntentHelpers.openVideoCall(application, method.data, method.packageName) { resId ->
                    showToast(resId)
                }
            }
            is ContactMethod.GoogleMeet -> {
                ContactIntentHelpers.openGoogleMeet(application, method.dataId ?: return) { resId ->
                    showToast(resId)
                }
            }
            is ContactMethod.CustomApp -> {
                if (method.dataId != null) {
                    ContactIntentHelpers.openCustomAppWithDataId(application, method.dataId, method.mimeType, method.packageName) { resId ->
                        showToast(resId)
                    }
                } else {
                    ContactIntentHelpers.openCustomApp(application, method.data, method.mimeType, method.packageName) { resId ->
                        showToast(resId)
                    }
                }
            }
            is ContactMethod.ViewInContactsApp -> {
                // This would open the contact in the contacts app, but since we're already showing the dialog,
                // we don't need to do anything special here
                finish()
            }
        }
    }

    private fun handleAction(action: CustomWidgetButtonAction) {
        val app = application as Application
        when (action) {
            is CustomWidgetButtonAction.App -> {
                IntentHelpers.launchApp(app, action.toAppInfo()) { resId, arg ->
                    showToast(resId, arg)
                }
            }
            is CustomWidgetButtonAction.Contact -> {
                ContactIntentHelpers.openContact(app, action.toContactInfo()) { resId ->
                    showToast(resId)
                }
            }
            is CustomWidgetButtonAction.File -> {
                IntentHelpers.openFile(app, action.toDeviceFile()) { resId, arg ->
                    showToast(resId, arg)
                }
            }
            is CustomWidgetButtonAction.Setting -> {
                val setting = action.toDeviceSetting()
                if (!setting.isSupported()) {
                    showToast(R.string.error_open_setting, setting.title)
                    return
                }
                runCatching { startActivity(setting.toIntent(this)) }
                    .onFailure { showToast(R.string.error_open_setting, setting.title) }
            }
            is CustomWidgetButtonAction.AppShortcut -> {
                val error = launchStaticShortcut(this, action.toStaticShortcut())
                if (error != null) {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showToast(resId: Int, formatArg: String? = null) {
        val message = if (formatArg.isNullOrBlank()) {
            getString(resId)
        } else {
            getString(resId, formatArg)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_CUSTOM_BUTTON_ACTION =
            "com.tk.quicksearch.extra.CUSTOM_WIDGET_BUTTON_ACTION"

        fun createIntent(context: Context, action: CustomWidgetButtonAction): Intent {
            return Intent(context, QuickSearchWidgetActionActivity::class.java).apply {
                putExtra(EXTRA_CUSTOM_BUTTON_ACTION, action.toJson())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}

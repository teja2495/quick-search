package com.tk.quicksearch.widgets.customButtonsWidget

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
import com.tk.quicksearch.search.contacts.dialogs.ContactActionsPopup
import com.tk.quicksearch.search.contacts.dialogs.ContactActionsPopupState
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.contacts.utils.ContactIntentHelpers
import com.tk.quicksearch.search.contacts.utils.TelegramContactUtils
import com.tk.quicksearch.search.core.IntentHelpers
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.AppShortcutRepository.launchStaticShortcut
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import kotlinx.coroutines.launch

class WidgetActionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action =
            CustomWidgetButtonAction.fromJson(
                intent.getStringExtra(EXTRA_CUSTOM_BUTTON_ACTION),
            )
        if (action != null) {
            if (action is CustomWidgetButtonAction.Contact) {
                loadFullContactInfo(
                    contactAction = action,
                    specificAction = action.toContactCardAction(),
                )
            } else {
                handleAction(action)
                finish()
            }
        } else {
            finish()
        }
    }

    private fun loadFullContactInfo(
        contactAction: CustomWidgetButtonAction.Contact,
        specificAction: ContactCardAction? = null,
    ) {
        lifecycleScope.launch {
            val contactRepository = ContactRepository(application.applicationContext)
            val contacts = contactRepository.getContactsByIds(setOf(contactAction.contactId))
            val fullContactInfo = contacts.firstOrNull()
            val resolvedContact = fullContactInfo ?: contactAction.toContactInfo()
            if (specificAction != null) {
                handleSpecificContactAction(resolvedContact, specificAction)
                finish()
            } else {
                showContactMethodsDialog(resolvedContact)
            }
        }
    }

    private fun handleSpecificContactAction(
        contactInfo: ContactInfo,
        action: ContactCardAction,
    ) {
        val methods = contactInfo.contactMethods

        fun matchesPhoneNumber(method: ContactMethod): Boolean {
            if (method.data.isBlank()) return false
            return PhoneNumberUtils.isSameNumber(method.data, action.phoneNumber)
        }

        fun matchesTelegramNumber(method: ContactMethod): Boolean =
            TelegramContactUtils.isTelegramMethodForPhoneNumber(
                context = application.applicationContext,
                phoneNumber = action.phoneNumber,
                telegramMethod = method,
            ) || matchesPhoneNumber(method)

        fun matchesSignalNumber(method: ContactMethod): Boolean =
            method.data.isBlank() || matchesPhoneNumber(method)

        val matchedMethod: ContactMethod? =
            methods.find { method ->
                when (action) {
                    is ContactCardAction.Phone -> {
                        method is ContactMethod.Phone && matchesPhoneNumber(method)
                    }
                    is ContactCardAction.Sms -> {
                        method is ContactMethod.Sms && matchesPhoneNumber(method)
                    }
                    is ContactCardAction.WhatsAppCall -> {
                        method is ContactMethod.WhatsAppCall && matchesPhoneNumber(method)
                    }
                    is ContactCardAction.WhatsAppMessage -> {
                        method is ContactMethod.WhatsAppMessage && matchesPhoneNumber(method)
                    }
                    is ContactCardAction.WhatsAppVideoCall -> {
                        method is ContactMethod.WhatsAppVideoCall && matchesPhoneNumber(method)
                    }
                    is ContactCardAction.TelegramMessage -> {
                        method is ContactMethod.TelegramMessage && matchesTelegramNumber(method)
                    }
                    is ContactCardAction.TelegramCall -> {
                        method is ContactMethod.TelegramCall && matchesTelegramNumber(method)
                    }
                    is ContactCardAction.TelegramVideoCall -> {
                        method is ContactMethod.TelegramVideoCall && matchesTelegramNumber(method)
                    }
                    is ContactCardAction.SignalMessage -> {
                        method is ContactMethod.SignalMessage && matchesSignalNumber(method)
                    }
                    is ContactCardAction.SignalCall -> {
                        method is ContactMethod.SignalCall && matchesSignalNumber(method)
                    }
                    is ContactCardAction.SignalVideoCall -> {
                        method is ContactMethod.SignalVideoCall && matchesSignalNumber(method)
                    }
                    is ContactCardAction.GoogleMeet -> {
                        method is ContactMethod.GoogleMeet && matchesPhoneNumber(method)
                    }
                    is ContactCardAction.Email -> {
                        method is ContactMethod.Email &&
                            (method.data == action.phoneNumber || matchesPhoneNumber(method))
                    }
                    is ContactCardAction.VideoCall -> {
                        method is ContactMethod.VideoCall &&
                            method.packageName == action.packageName &&
                            (method.data == action.phoneNumber || matchesPhoneNumber(method))
                    }
                    is ContactCardAction.CustomApp -> {
                        method is ContactMethod.CustomApp &&
                            (
                                (action.dataId != null && method.dataId == action.dataId) ||
                                    (
                                        method.mimeType == action.mimeType &&
                                            method.packageName == action.packageName &&
                                            (
                                                method.data == action.phoneNumber ||
                                                    matchesPhoneNumber(method)
                                            )
                                    )
                            )
                    }
                    is ContactCardAction.ViewInContactsApp -> {
                        method is ContactMethod.ViewInContactsApp
                    }
                }
            }

        if (matchedMethod != null) {
            handleContactMethod(matchedMethod)
            return
        }

        when (action) {
            is ContactCardAction.Phone -> {
                handleContactMethod(ContactMethod.Phone(getString(R.string.contact_method_call_label), action.phoneNumber))
            }
            is ContactCardAction.Sms -> {
                handleContactMethod(ContactMethod.Sms(getString(R.string.contact_method_message_label), action.phoneNumber))
            }
            is ContactCardAction.Email -> {
                handleContactMethod(ContactMethod.Email(getString(R.string.contact_method_email_label), action.phoneNumber))
            }
            is ContactCardAction.ViewInContactsApp -> {
                handleContactMethod(ContactMethod.ViewInContactsApp(getString(R.string.contacts_action_button_contacts)))
            }
            is ContactCardAction.VideoCall -> {
                handleContactMethod(
                    ContactMethod.VideoCall(
                        displayLabel = getString(R.string.contacts_action_button_video_call),
                        data = action.phoneNumber,
                        packageName = action.packageName,
                    ),
                )
            }
            is ContactCardAction.CustomApp -> {
                handleContactMethod(
                    ContactMethod.CustomApp(
                        displayLabel = action.displayLabel,
                        data = action.phoneNumber,
                        mimeType = action.mimeType,
                        packageName = action.packageName,
                        dataId = action.dataId,
                    ),
                )
            }
            else -> showToast(R.string.error_action_not_available)
        }
    }

    private fun showContactMethodsDialog(contactInfo: ContactInfo) {
        val userPreferences = UserAppPreferences(applicationContext)
        val appTheme = userPreferences.getAppTheme()
        val appThemeMode = userPreferences.getAppThemeMode()
        setContent {
            QuickSearchTheme(appTheme = appTheme, appThemeMode = appThemeMode) {
                var showDialog by remember { mutableStateOf(true) }
                if (showDialog) {
                    ContactActionsPopup(
                        state =
                            ContactActionsPopupState.ContactActions(
                                contactInfo = contactInfo,
                                onContactMethodClick = { _, contactMethod ->
                                    handleContactMethod(contactMethod)
                                    showDialog = false
                                    finish()
                                },
                                onAvatarClick = {
                                    showDialog = false
                                    finish()
                                },
                            ),
                        onDismiss = {
                            showDialog = false
                            finish()
                        },
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

            is ContactMethod.SignalMessage -> {
                ContactIntentHelpers.openSignalChat(application, method.dataId) { resId ->
                    showToast(resId)
                }
            }

            is ContactMethod.SignalCall -> {
                ContactIntentHelpers.openSignalCall(application, method.dataId) { resId ->
                    showToast(resId)
                }
            }

            is ContactMethod.SignalVideoCall -> {
                ContactIntentHelpers.openSignalVideoCall(application, method.dataId) { resId ->
                    showToast(resId)
                }
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
                    showToast(R.string.common_error_unable_to_open, setting.title)
                    return
                }
                runCatching { startActivity(setting.toIntent(this)) }
                    .onFailure { showToast(R.string.common_error_unable_to_open, setting.title) }
            }

            is CustomWidgetButtonAction.AppShortcut -> {
                val error = launchStaticShortcut(this, action.toStaticShortcut())
                if (error != null) {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }

            is CustomWidgetButtonAction.Note -> {
                OverlayModeController.openMainActivity(
                    context = this,
                    openSettings = true,
                    settingsDetailType = SettingsDetailType.NOTE_EDITOR,
                    noteId = action.noteId,
                )
            }
        }
    }

    private fun showToast(
        resId: Int,
        formatArg: String? = null,
    ) {
        val message =
            if (formatArg.isNullOrBlank()) {
                getString(resId)
            } else {
                getString(resId, formatArg)
            }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_CUSTOM_BUTTON_ACTION =
            "com.tk.quicksearch.extra.CUSTOM_WIDGET_BUTTON_ACTION"

        fun createIntent(
            context: Context,
            action: CustomWidgetButtonAction,
        ): Intent =
            Intent(context, WidgetActionActivity::class.java).apply {
                putExtra(EXTRA_CUSTOM_BUTTON_ACTION, action.toJson())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }
}

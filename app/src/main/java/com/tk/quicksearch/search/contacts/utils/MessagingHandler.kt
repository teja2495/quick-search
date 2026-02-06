package com.tk.quicksearch.search.contacts.utils

import android.app.Application
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.util.PackageConstants
import kotlinx.coroutines.flow.update

class MessagingHandler(
    private val application: Application,
    private val userPreferences: UserAppPreferences,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit,
) {
    var messagingApp: MessagingApp = userPreferences.getMessagingApp()
        private set

    var isWhatsAppInstalled: Boolean = false
        private set

    var isTelegramInstalled: Boolean = false
        private set
    var isSignalInstalled: Boolean = false
        private set

    fun resolveMessagingApp(
        whatsappInstalled: Boolean,
        telegramInstalled: Boolean,
        signalInstalled: Boolean,
    ): MessagingApp =
        when (messagingApp) {
            MessagingApp.WHATSAPP -> if (whatsappInstalled) MessagingApp.WHATSAPP else MessagingApp.MESSAGES
            MessagingApp.TELEGRAM -> if (telegramInstalled) MessagingApp.TELEGRAM else MessagingApp.MESSAGES
            MessagingApp.SIGNAL -> if (signalInstalled) MessagingApp.SIGNAL else MessagingApp.MESSAGES
            MessagingApp.MESSAGES -> MessagingApp.MESSAGES
        }

    fun updateMessagingAvailability(
        whatsappInstalled: Boolean,
        telegramInstalled: Boolean,
        signalInstalled: Boolean,
        updateState: Boolean = true,
    ): MessagingApp {
        isWhatsAppInstalled = whatsappInstalled
        isTelegramInstalled = telegramInstalled
        isSignalInstalled = signalInstalled

        val resolvedMessagingApp = resolveMessagingApp(whatsappInstalled, telegramInstalled, signalInstalled)
        if (resolvedMessagingApp != messagingApp) {
            messagingApp = resolvedMessagingApp
            userPreferences.setMessagingApp(resolvedMessagingApp)
        }

        if (updateState) {
            uiStateUpdater { state ->
                state.copy(
                    messagingApp = messagingApp,
                    isWhatsAppInstalled = whatsappInstalled,
                    isTelegramInstalled = telegramInstalled,
                    isSignalInstalled = signalInstalled,
                )
            }
        }

        return resolvedMessagingApp
    }

    fun setMessagingApp(app: MessagingApp) {
        messagingApp = app
        // Persist the user's explicit choice before resolving availability
        userPreferences.setMessagingApp(app)
        updateMessagingAvailability(
            whatsappInstalled = isWhatsAppInstalled,
            telegramInstalled = isTelegramInstalled,
            signalInstalled = isSignalInstalled,
        )
    }

    fun isPackageInstalled(packageName: String): Boolean {
        val packageManager = application.packageManager
        return packageManager.getLaunchIntentForPackage(packageName) != null
    }
}

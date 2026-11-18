package com.tk.quicksearch.search.contacts

import com.tk.quicksearch.search.contacts.models.ContactActionProvider
import android.app.Application
import com.tk.quicksearch.search.contacts.models.ContactButtonAction
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences

class MessagingHandler(
    private val application: Application,
    private val userPreferences: UserAppPreferences,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit,
) {
    var primaryContactButton: ContactButtonAction = userPreferences.getPrimaryContactButton()
        private set
    var secondaryContactButton: ContactButtonAction = userPreferences.getSecondaryContactButton()
        private set

    private var installedProviders = emptySet<ContactActionProvider>()

    private fun resolveInstalled(action: ContactButtonAction, isPrimary: Boolean): ContactButtonAction =
        if (action.isAppInstalled(installedProviders)) action else ContactButtonAction.fallback(isPrimary)

    /** Records installed contact apps and falls back to Call/SMS when an app is no longer available. */
    fun updateMessagingAvailability(installed: Set<ContactActionProvider>, updateState: Boolean = true) {
        installedProviders = installed

        val resolvedPrimary = resolveInstalled(primaryContactButton, isPrimary = true)
        if (resolvedPrimary != primaryContactButton) {
            primaryContactButton = resolvedPrimary
            userPreferences.setPrimaryContactButton(resolvedPrimary)
        }
        val resolvedSecondary = resolveInstalled(secondaryContactButton, isPrimary = false)
        if (resolvedSecondary != secondaryContactButton) {
            secondaryContactButton = resolvedSecondary
            userPreferences.setSecondaryContactButton(resolvedSecondary)
        }

        if (updateState) {
            uiStateUpdater { state ->
                state.copy(
                    primaryContactButton = primaryContactButton,
                    secondaryContactButton = secondaryContactButton,
                    installedContactProviders = installedProviders,
                )
            }
        }
    }

    fun setPrimaryContactButton(action: ContactButtonAction) {
        primaryContactButton = action
        // Persist the user's explicit choice before resolving availability
        userPreferences.setPrimaryContactButton(action)
        refreshAvailability()
    }

    fun setSecondaryContactButton(action: ContactButtonAction) {
        secondaryContactButton = action
        userPreferences.setSecondaryContactButton(action)
        refreshAvailability()
    }

    /** Onboarding picks a messaging app, which sets the second contact button. */
    fun setMessagingApp(app: MessagingApp) {
        setSecondaryContactButton(ContactButtonAction.fromMessagingApp(app))
    }

    private fun refreshAvailability() {
        updateMessagingAvailability(installedProviders)
    }

    fun isPackageInstalled(packageName: String): Boolean {
        val packageManager = application.packageManager
        return packageManager.getLaunchIntentForPackage(packageName) != null
    }
}

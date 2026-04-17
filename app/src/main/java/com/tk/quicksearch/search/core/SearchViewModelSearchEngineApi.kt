package com.tk.quicksearch.search.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal interface SearchViewModelSearchEngineApi {
    val searchEngineApiDelegate: SearchViewModelSearchEngineApiDelegate

    fun setAliasesEnabled(enabled: Boolean) = searchEngineApiDelegate.setAliasesEnabled(enabled)

    fun setAlias(target: SearchTarget, code: String) = searchEngineApiDelegate.setAlias(target, code)

    fun setAlias(targetId: String, code: String) = searchEngineApiDelegate.setAlias(targetId, code)

    fun setAliasEnabled(target: SearchTarget, enabled: Boolean) =
        searchEngineApiDelegate.setAliasEnabled(target, enabled)

    fun getAlias(target: SearchTarget): String = searchEngineApiDelegate.getAlias(target)

    fun getAlias(targetId: String, defaultCode: String = ""): String =
        searchEngineApiDelegate.getAlias(targetId, defaultCode)

    fun isAliasEnabled(target: SearchTarget): Boolean = searchEngineApiDelegate.isAliasEnabled(target)

    fun setSectionEnabled(section: SearchSection, enabled: Boolean) =
        searchEngineApiDelegate.setSectionEnabled(section, enabled)

    fun canEnableSection(section: SearchSection): Boolean = searchEngineApiDelegate.canEnableSection(section)

    fun setMessagingApp(app: MessagingApp) = searchEngineApiDelegate.setMessagingApp(app)

    fun setCallingApp(app: CallingApp) = searchEngineApiDelegate.setCallingApp(app)

    fun acknowledgeReleaseNotes() = searchEngineApiDelegate.acknowledgeReleaseNotes()

    fun requestAiSearch(query: String) = searchEngineApiDelegate.requestAiSearch(query)

    fun setShowStartSearchingOnOnboarding(show: Boolean) =
        searchEngineApiDelegate.setShowStartSearchingOnOnboarding(show)

    fun onSearchEngineOnboardingDismissed() = searchEngineApiDelegate.onSearchEngineOnboardingDismissed()

    fun getEnabledSearchTargets(): List<SearchTarget> = searchEngineApiDelegate.getEnabledSearchTargets()

    fun setSearchTargetEnabled(target: SearchTarget, enabled: Boolean) =
        searchEngineApiDelegate.setSearchTargetEnabled(target, enabled)

    fun reorderSearchTargets(newOrder: List<SearchTarget>) =
        searchEngineApiDelegate.reorderSearchTargets(newOrder)

    fun addCustomSearchEngine(
        name: String,
        normalizedTemplate: String,
        faviconBase64: String,
        browserPackage: String? = null,
    ) = searchEngineApiDelegate.addCustomSearchEngine(name, normalizedTemplate, faviconBase64, browserPackage)

    fun updateCustomSearchEngine(
        customId: String,
        name: String,
        urlTemplateInput: String,
        faviconBase64: String?,
        browserPackage: String? = null,
    ) =
        searchEngineApiDelegate.updateCustomSearchEngine(
            customId,
            name,
            urlTemplateInput,
            faviconBase64,
            browserPackage,
        )

    fun deleteCustomSearchEngine(customId: String) =
        searchEngineApiDelegate.deleteCustomSearchEngine(customId)

    fun setSearchEngineCompactMode(enabled: Boolean) =
        searchEngineApiDelegate.setSearchEngineCompactMode(enabled)

    fun setSearchEngineCompactRowCount(rowCount: Int) =
        searchEngineApiDelegate.setSearchEngineCompactRowCount(rowCount)
}

class SearchViewModelSearchEngineApiDelegate internal constructor(
    private val scope: CoroutineScope,
    private val userPreferences: com.tk.quicksearch.search.data.UserAppPreferences,
    private val aliasHandler: () -> com.tk.quicksearch.searchEngines.AliasHandler,
    private val sectionManager: () -> SectionManager,
    private val messagingHandler: () -> com.tk.quicksearch.search.contacts.utils.MessagingHandler,
    private val searchEngineManager: () -> com.tk.quicksearch.searchEngines.SearchEngineManager,
    private val aiSearchHandler: () -> com.tk.quicksearch.tools.aiSearch.AiSearchHandler,
    private val releaseNotesHandler: () -> com.tk.quicksearch.app.ReleaseNotesHandler,
    private val permissionStateProvider: () -> SearchPermissionState,
    private val configStateProvider: () -> SearchUiConfigState,
    private val updatePermissionState: ((SearchPermissionState) -> SearchPermissionState) -> Unit,
    private val updateConfigState: ((SearchUiConfigState) -> SearchUiConfigState) -> Unit,
) {
    fun setAliasesEnabled(enabled: Boolean) = aliasHandler().setAliasesEnabled(enabled)

    fun setAlias(target: SearchTarget, code: String) = aliasHandler().setAlias(target, code)

    fun setAlias(targetId: String, code: String) = aliasHandler().setAlias(targetId, code)

    fun setAliasEnabled(target: SearchTarget, enabled: Boolean) =
        aliasHandler().setAliasEnabled(target, enabled)

    fun getAlias(target: SearchTarget): String = aliasHandler().getAlias(target)

    fun getAlias(targetId: String, defaultCode: String): String =
        aliasHandler().getAlias(targetId, defaultCode)

    fun isAliasEnabled(target: SearchTarget): Boolean = aliasHandler().isAliasEnabled(target)

    fun setSectionEnabled(section: SearchSection, enabled: Boolean) =
        sectionManager().setSectionEnabled(section, enabled)

    fun canEnableSection(section: SearchSection): Boolean = sectionManager().canEnableSection(section)

    fun setMessagingApp(app: MessagingApp) = messagingHandler().setMessagingApp(app)

    fun setCallingApp(app: CallingApp) {
        userPreferences.setCallingApp(app)
        val permState = permissionStateProvider()
        val resolvedCallingApp =
            resolveCallingApp(
                app = app,
                isWhatsAppInstalled = permState.isWhatsAppInstalled,
                isTelegramInstalled = permState.isTelegramInstalled,
                isSignalInstalled = permState.isSignalInstalled,
                isGoogleMeetInstalled = permState.isGoogleMeetInstalled,
            )
        if (resolvedCallingApp != app) {
            userPreferences.setCallingApp(resolvedCallingApp)
        }
        updatePermissionState { it.copy(callingApp = resolvedCallingApp) }
    }

    fun acknowledgeReleaseNotes() {
        releaseNotesHandler().acknowledgeReleaseNotes(configStateProvider().releaseNotesVersionName)
    }

    fun requestAiSearch(query: String) = aiSearchHandler().requestAiSearch(query)

    fun setShowStartSearchingOnOnboarding(show: Boolean) {
        updateConfigState { it.copy(showStartSearchingOnOnboarding = show) }
    }

    fun onSearchEngineOnboardingDismissed() {
        updateConfigState {
            it.copy(
                showSearchEngineOnboarding = false,
                showStartSearchingOnOnboarding = false,
            )
        }
        scope.launch(Dispatchers.IO) {
            userPreferences.setHasSeenSearchEngineOnboarding(true)
        }
    }

    fun getEnabledSearchTargets(): List<SearchTarget> = searchEngineManager().getEnabledSearchTargets()

    fun setSearchTargetEnabled(target: SearchTarget, enabled: Boolean) =
        searchEngineManager().setSearchTargetEnabled(target, enabled)

    fun reorderSearchTargets(newOrder: List<SearchTarget>) =
        searchEngineManager().reorderSearchTargets(newOrder)

    fun addCustomSearchEngine(
        name: String,
        normalizedTemplate: String,
        faviconBase64: String,
        browserPackage: String?,
    ) = searchEngineManager().addCustomSearchEngine(name, normalizedTemplate, faviconBase64, browserPackage)

    fun updateCustomSearchEngine(
        customId: String,
        name: String,
        urlTemplateInput: String,
        faviconBase64: String?,
        browserPackage: String?,
    ) =
        searchEngineManager().updateCustomSearchEngine(
            customId,
            name,
            urlTemplateInput,
            faviconBase64,
            browserPackage,
        )

    fun deleteCustomSearchEngine(customId: String) =
        searchEngineManager().deleteCustomSearchEngine(customId)

    fun setSearchEngineCompactMode(enabled: Boolean) =
        searchEngineManager().setSearchEngineCompactMode(enabled)

    fun setSearchEngineCompactRowCount(rowCount: Int) =
        searchEngineManager().setSearchEngineCompactRowCount(rowCount)
}

internal fun resolveCallingApp(
    app: CallingApp,
    isWhatsAppInstalled: Boolean,
    isTelegramInstalled: Boolean,
    isSignalInstalled: Boolean,
    isGoogleMeetInstalled: Boolean,
): CallingApp =
    when (app) {
        CallingApp.WHATSAPP -> if (isWhatsAppInstalled) CallingApp.WHATSAPP else CallingApp.CALL
        CallingApp.TELEGRAM -> if (isTelegramInstalled) CallingApp.TELEGRAM else CallingApp.CALL
        CallingApp.SIGNAL -> if (isSignalInstalled) CallingApp.SIGNAL else CallingApp.CALL
        CallingApp.GOOGLE_MEET ->
            if (isGoogleMeetInstalled) CallingApp.GOOGLE_MEET else CallingApp.CALL
        CallingApp.CALL -> CallingApp.CALL
    }

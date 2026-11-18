package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.contacts.models.ContactButtonAction
import kotlinx.coroutines.CoroutineScope

internal interface SearchViewModelSearchEngineApi {
    val searchEngineApiDelegate: SearchViewModelSearchEngineApiDelegate

    fun setAlias(target: SearchTarget, code: String) = searchEngineApiDelegate.setAlias(target, code)

    fun setAlias(targetId: String, code: String) = searchEngineApiDelegate.setAlias(targetId, code)

    fun getAlias(target: SearchTarget): String = searchEngineApiDelegate.getAlias(target)

    fun getAlias(targetId: String, defaultCode: String = ""): String =
        searchEngineApiDelegate.getAlias(targetId, defaultCode)

    fun setSectionEnabled(section: SearchSection, enabled: Boolean) =
        searchEngineApiDelegate.setSectionEnabled(section, enabled)

    fun canEnableSection(section: SearchSection): Boolean = searchEngineApiDelegate.canEnableSection(section)

    fun setMessagingApp(app: MessagingApp) = searchEngineApiDelegate.setMessagingApp(app)

    fun setPrimaryContactButton(action: ContactButtonAction) =
        searchEngineApiDelegate.setPrimaryContactButton(action)

    fun setSecondaryContactButton(action: ContactButtonAction) =
        searchEngineApiDelegate.setSecondaryContactButton(action)

    fun acknowledgeReleaseNotes() = searchEngineApiDelegate.acknowledgeReleaseNotes()

    fun dismissAccessibilityPermissionDisclaimer() =
        searchEngineApiDelegate.dismissAccessibilityPermissionDisclaimer()

    fun showReleaseNotes() = searchEngineApiDelegate.showReleaseNotes()

    fun requestAiSearch(query: String) = searchEngineApiDelegate.requestAiSearch(query)

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
    private val userPreferences: com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences,
    private val aliasHandler: () -> com.tk.quicksearch.searchEngines.AliasHandler,
    private val sectionManager: () -> SectionManager,
    private val messagingHandler: () -> com.tk.quicksearch.search.contacts.MessagingHandler,
    private val searchEngineManager: () -> com.tk.quicksearch.searchEngines.SearchEngineManager,
    private val aiSearchHandler: () -> com.tk.quicksearch.tools.aiSearch.AiSearchHandler,
    private val releaseNotesHandler: () -> com.tk.quicksearch.app.ReleaseNotesHandler,
    private val permissionStateProvider: () -> SearchPermissionState,
    private val configStateProvider: () -> SearchUiConfigState,
    private val updatePermissionState: ((SearchPermissionState) -> SearchPermissionState) -> Unit,
    private val updateConfigState: ((SearchUiConfigState) -> SearchUiConfigState) -> Unit,
) {
    fun setAlias(target: SearchTarget, code: String) = aliasHandler().setAlias(target, code)

    fun setAlias(targetId: String, code: String) = aliasHandler().setAlias(targetId, code)

    fun getAlias(target: SearchTarget): String = aliasHandler().getAlias(target)

    fun getAlias(targetId: String, defaultCode: String): String =
        aliasHandler().getAlias(targetId, defaultCode)

    fun setSectionEnabled(section: SearchSection, enabled: Boolean) =
        sectionManager().setSectionEnabled(section, enabled)

    fun canEnableSection(section: SearchSection): Boolean = sectionManager().canEnableSection(section)

    fun setMessagingApp(app: MessagingApp) = messagingHandler().setMessagingApp(app)

    fun setPrimaryContactButton(action: ContactButtonAction) =
        messagingHandler().setPrimaryContactButton(action)

    fun setSecondaryContactButton(action: ContactButtonAction) =
        messagingHandler().setSecondaryContactButton(action)

    fun acknowledgeReleaseNotes() {
        releaseNotesHandler().acknowledgeReleaseNotes(configStateProvider().releaseNotesVersionName)
    }

    fun dismissAccessibilityPermissionDisclaimer() {
        releaseNotesHandler().dismissAccessibilityPermissionDisclaimer()
    }

    fun showReleaseNotes() {
        releaseNotesHandler().showReleaseNotes()
    }

    fun requestAiSearch(query: String) = aiSearchHandler().requestAiSearch(query)

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

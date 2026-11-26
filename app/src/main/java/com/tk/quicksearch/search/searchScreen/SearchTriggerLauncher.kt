package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import kotlinx.coroutines.launch

internal fun openMatchingSearchTrigger(
    query: String,
    state: SearchUiState,
    renderingState: SectionRenderingState,
    appsParams: AppsSectionParams,
    appShortcutsParams: AppShortcutsSectionParams,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    settingsParams: SettingsSectionParams,
    notesParams: NotesSectionParams,
    getAllContactActionTriggers: () -> Map<com.tk.quicksearch.search.data.preferences.ContactActionTriggerKey, com.tk.quicksearch.search.data.preferences.ResultTrigger>,
    triggerWords: Collection<String>,
    onContactActionTrigger: (Long, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit,
    onAppClick: (com.tk.quicksearch.search.models.AppInfo) -> Unit,
): Boolean {
    fun matchesTrigger(
        query: String,
        word: String,
        triggerAfterSpace: Boolean,
    ): Boolean {
        if (word.isBlank()) return false
        val normalizedQuery = query.lowercase()
        // A trigger can hold several comma-separated words; any one of them opens the item.
        return com.tk.quicksearch.searchEngines.AliasValidator.splitAliasCodes(word).any { normalizedWord ->
            if (triggerAfterSpace) {
                normalizedQuery == "$normalizedWord "
            } else {
                normalizedQuery == normalizedWord
            }
        }
    }

    fun openMatchingTrigger(query: String): Boolean {
        // This runs on the main thread for every results update while typing. The preloaded
        // trigger words rule out almost every query before the per-item lookups below, which
        // read preferences and parse JSON once per app, shortcut, contact, and file.
        val hasMatchingTriggerWord =
            triggerWords.any { word ->
                matchesTrigger(query, word, triggerAfterSpace = false) ||
                    matchesTrigger(query, word, triggerAfterSpace = true)
            }
        if (!hasMatchingTriggerWord) return false

        // App catalogs also load asynchronously. Search results can be ready first, so use both
        // sources and retry when either one changes.
        (state.allApps + renderingState.displayApps)
            .distinctBy { it.launchCountKey() }
            .firstOrNull { app ->
                // Archived apps keep their trigger, but it only works again once they're restored.
                !app.isArchived && appsParams.getAppTrigger(app.launchCountKey())?.let { trigger ->
                    matchesTrigger(query, trigger.word, trigger.triggerAfterSpace)
                } == true
            }?.let { app ->
                onAppClick(app)
                return true
            }

        (state.allAppShortcuts + renderingState.appShortcutResults)
            .distinctBy { com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey(it) }
            .firstOrNull { shortcut ->
                appShortcutsParams.getShortcutTrigger(
                    com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey(shortcut),
                )?.let { trigger ->
                    matchesTrigger(query, trigger.word, trigger.triggerAfterSpace)
                } == true
            }?.let { shortcut ->
                appShortcutsParams.onShortcutClick(shortcut)
                return true
            }

        (renderingState.contactResults + state.pinnedContacts)
            .distinctBy { it.contactId }
            .firstOrNull { contact ->
                contactsParams.getContactTrigger(contact.contactId)?.let { trigger ->
                    matchesTrigger(query, trigger.word, trigger.triggerAfterSpace)
                } == true
            }?.let { contact ->
                if (contact.hasContactMethods) {
                    contactsParams.onShowContactMethods(contact)
                } else {
                    contactsParams.onContactClick(contact)
                }
                return true
            }

        getAllContactActionTriggers().firstNotNullOfOrNull { (key, trigger) ->
            if (matchesTrigger(query, trigger.word, trigger.triggerAfterSpace)) {
                key
            } else {
                null
            }
        }?.let { key ->
            onContactActionTrigger(key.contactId, key.action)
            return true
        }

        (renderingState.fileResults + state.pinnedFiles)
            .distinctBy { it.uri }
            .firstOrNull { file ->
                filesParams.getFileTrigger(file.uri.toString())?.let { trigger ->
                    matchesTrigger(query, trigger.word, trigger.triggerAfterSpace)
                } == true
            }?.let { file ->
                filesParams.onFileClick(file)
                return true
            }

        // Settings shortcuts load asynchronously. A trigger can already have surfaced its
        // matching result before the full catalog reaches allDeviceSettings, so include that
        // rendered result as a launch candidate as well.
        (state.allDeviceSettings + renderingState.settingResults)
            .distinctBy { it.id }
            .firstOrNull { setting ->
                settingsParams.getSettingTrigger(setting.id)?.let { trigger ->
                    matchesTrigger(query, trigger.word, trigger.triggerAfterSpace)
                } == true
            }?.let { setting ->
                settingsParams.onSettingClick(setting)
                return true
            }

        (renderingState.noteResults + state.pinnedNotes)
            .distinctBy { it.noteId }
            .firstOrNull { note ->
                notesParams.getNoteTrigger(note.noteId)?.let { trigger ->
                    matchesTrigger(query, trigger.word, trigger.triggerAfterSpace)
                } == true
            }?.let { note ->
                notesParams.onNoteClick(note)
                return true
            }

        if (
            appShortcutsParams.onUnavailableShortcutTrigger { trigger ->
                matchesTrigger(query, trigger.word, trigger.triggerAfterSpace)
            }
        ) {
            return true
        }

        return false
    }

    return openMatchingTrigger(query)
}

/**
 * Opens the result whose trigger word matches the query, once per query: re-checks as results load
 * but does not fire again for the same query after it opened something.
 */
@Composable
internal fun AutoOpenMatchingTrigger(
    state: SearchUiState,
    renderingState: SectionRenderingState,
    triggerWords: Collection<String>,
    openMatchingTrigger: (String) -> Boolean,
) {
    var lastTriggeredQuery by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(
        state.query,
        state.allApps,
        renderingState.displayApps,
        state.allAppShortcuts,
        renderingState.appShortcutResults,
        renderingState.contactResults,
        state.pinnedContacts,
        renderingState.fileResults,
        state.pinnedFiles,
        renderingState.settingResults,
        state.allDeviceSettings,
        renderingState.calendarEvents,
        renderingState.noteResults,
        state.pinnedNotes,
        triggerWords,
    ) {
        if (state.query == lastTriggeredQuery) return@LaunchedEffect
        lastTriggeredQuery = null
        if (state.query.isBlank()) return@LaunchedEffect
        if (openMatchingTrigger(state.query)) {
            lastTriggeredQuery = state.query
        }
    }
}

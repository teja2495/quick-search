package com.tk.quicksearch.search.searchScreen

import androidx.compose.material.icons.rounded.Search
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
    onContactActionTrigger: (Long, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit,
    onAppClick: (com.tk.quicksearch.search.models.AppInfo) -> Unit,
): Boolean {
    fun matchesTrigger(
        query: String,
        word: String,
        triggerAfterSpace: Boolean,
    ): Boolean {
        val normalizedWord = word.trim().lowercase()
        if (normalizedWord.isBlank()) return false
        val normalizedQuery = query.lowercase()
        return if (triggerAfterSpace) {
            normalizedQuery == "$normalizedWord "
        } else {
            normalizedQuery == normalizedWord
        }
    }

    fun openMatchingTrigger(query: String): Boolean {
        // App catalogs also load asynchronously. Search results can be ready first, so use both
        // sources and retry when either one changes.
        (state.allApps + renderingState.displayApps)
            .distinctBy { it.launchCountKey() }
            .firstOrNull { app ->
                appsParams.getAppTrigger(app.packageName)?.let { trigger ->
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

        return false
    }

    return openMatchingTrigger(query)
}

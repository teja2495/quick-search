package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.appSettings.AppSettingsSearchHandler
import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchHandler
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.withQueryCancellation
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsSearchHandler
import com.tk.quicksearch.search.files.FileSearchAlgorithm
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.models.FileTypeUtils
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import java.util.Locale

/**
 * Loads the items that follow the recently opened ones in a blank-query alias view: the rest of
 * the section's items in its natural order, skipping pinned, excluded and already-shown items.
 */
internal class AliasRecentFillLoader(
    private val userPreferences: UserAppPreferences,
    private val contactRepository: ContactRepository,
    private val fileRepository: FileSearchRepository,
    private val settingsSearchHandler: DeviceSettingsSearchHandler,
    private val appShortcutSearchHandler: AppShortcutSearchHandler,
    private val appSettingsSearchHandler: AppSettingsSearchHandler,
    private val notesRepository: NotesRepository,
    private val configStateProvider: () -> SearchUiConfigState,
) {
    suspend fun loadFill(
        section: SearchSection,
        shownKeys: Set<String>,
        limit: Int,
        fileTypeFilter: FileType? = null,
    ): List<RecentSearchItem> {
        if (limit <= 0) return emptyList()
        return when (section) {
            SearchSection.CONTACTS -> contactFill(shownKeys, limit)
            SearchSection.FILES -> fileFill(shownKeys, limit, fileTypeFilter)
            SearchSection.SETTINGS -> settingFill(shownKeys, limit)
            SearchSection.APP_SHORTCUTS -> appShortcutFill(shownKeys, limit)
            SearchSection.APP_SETTINGS -> appSettingFill(shownKeys, limit)
            SearchSection.NOTES -> noteFill(shownKeys, limit)
            else -> emptyList()
        }
    }

    private fun contactFill(
        shownKeys: Set<String>,
        limit: Int,
    ): List<RecentSearchItem> {
        val hiddenIds = userPreferences.getPinnedContactIds() + userPreferences.getExcludedContactIds()
        val contacts =
            contactRepository
                .getRecentContacts(limit + shownKeys.size + hiddenIds.size)
                .filter { it.contactId !in hiddenIds }
                .map { RecentSearchEntry.Contact(it.contactId) to it }
                .filter { (entry, _) -> entry.stableKey !in shownKeys }
                .take(limit)
        val hydratedById =
            contactRepository
                .hydrateContactsForDisplay(contacts.map { it.second })
                .associateBy { it.contactId }
        return contacts.map { (entry, contact) ->
            RecentSearchItem.Contact(entry, hydratedById[contact.contactId] ?: contact)
        }
    }

    private suspend fun fileFill(
        shownKeys: Set<String>,
        limit: Int,
        fileTypeFilter: FileType?,
    ): List<RecentSearchItem> {
        val config = configStateProvider()
        val hiddenUris = userPreferences.getPinnedFileUris() + shownKeys.map { it.removePrefix(FILE_KEY_PREFIX) }
        val isVisible =
            FileSearchAlgorithm.visibilityPredicate(
                enabledFileTypes = config.enabledFileTypes,
                excludedFileUris = userPreferences.getExcludedFileUris(),
                excludedFileExtensions = config.excludedFileExtensions,
                folderWhitelistPatterns = config.folderWhitelistPatterns,
                folderBlacklistPatterns = config.folderBlacklistPatterns,
                showFolders = config.showFolders,
                showSystemFiles = config.showSystemFiles,
            )
        // A filtered scan can walk most of MediaStore; stop it when a newer refresh replaces it.
        return withQueryCancellation { cancellationSignal ->
            fileRepository.getRecentFiles(limit, cancellationSignal) { file ->
                file.uri.toString() !in hiddenUris &&
                    matchesFileType(file, fileTypeFilter) &&
                    isVisible(file)
            }
        }.map { RecentSearchItem.File(RecentSearchEntry.File(it.uri.toString()), it) }
    }

    private suspend fun settingFill(
        shownKeys: Set<String>,
        limit: Int,
    ): List<RecentSearchItem> {
        if (settingsSearchHandler.getAvailableSettings().isEmpty()) {
            settingsSearchHandler.loadShortcuts()
        }
        val hiddenIds = userPreferences.getPinnedSettingIds() + userPreferences.getExcludedSettingIds()
        return settingsSearchHandler
            .getAvailableSettings()
            .asSequence()
            .filter { it.id !in hiddenIds }
            .map { RecentSearchEntry.Setting(it.id) to it }
            .filter { (entry, _) -> entry.stableKey !in shownKeys }
            .take(limit)
            .map { (entry, setting) -> RecentSearchItem.Setting(entry, setting) }
            .toList()
    }

    private fun appShortcutFill(
        shownKeys: Set<String>,
        limit: Int,
    ): List<RecentSearchItem> {
        val hiddenKeys =
            userPreferences.getPinnedAppShortcutIds() +
                userPreferences.getExcludedAppShortcutIds() +
                userPreferences.getDisabledAppShortcutIds()
        return appShortcutSearchHandler
            .getAvailableShortcuts()
            .asSequence()
            .filter { it.enabled && shortcutKey(it) !in hiddenKeys }
            .sortedWith(
                compareBy(
                    { it.appLabel.lowercase(Locale.getDefault()) },
                    { it.shortLabel.orEmpty().lowercase(Locale.getDefault()) },
                ),
            ).map { RecentSearchEntry.AppShortcut(shortcutKey(it)) to it }
            .filter { (entry, _) -> entry.stableKey !in shownKeys }
            .take(limit)
            .map { (entry, shortcut) -> RecentSearchItem.AppShortcut(entry, shortcut) }
            .toList()
    }

    private fun appSettingFill(
        shownKeys: Set<String>,
        limit: Int,
    ): List<RecentSearchItem> =
        appSettingsSearchHandler
            .getAvailableSettings()
            .asSequence()
            .map { RecentSearchEntry.AppSetting(it.id) to it }
            .filter { (entry, _) -> entry.stableKey !in shownKeys }
            .take(limit)
            .map { (entry, setting) -> RecentSearchItem.AppSetting(entry, setting) }
            .toList()

    private fun noteFill(
        shownKeys: Set<String>,
        limit: Int,
    ): List<RecentSearchItem> {
        val pinnedIds = notesRepository.getPinnedNoteIds()
        return notesRepository
            .getAllNotes()
            .asSequence()
            .filter { it.noteId !in pinnedIds && !notesRepository.isQuickNote(it.noteId) }
            .map { RecentSearchEntry.Note(it.noteId) to it }
            .filter { (entry, _) -> entry.stableKey !in shownKeys }
            .take(limit)
            .map { (entry, note) -> RecentSearchItem.Note(entry, note) }
            .toList()
    }

    private companion object {
        const val FILE_KEY_PREFIX = "file:"
    }
}

/** True when [file] belongs to the alias file chip [fileType]; null means All. */
internal fun matchesFileType(
    file: DeviceFile,
    fileType: FileType?,
): Boolean = fileType == null || (!file.isDirectory && FileTypeUtils.getFileType(file) == fileType)

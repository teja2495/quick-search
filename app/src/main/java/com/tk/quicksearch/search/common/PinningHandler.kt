package com.tk.quicksearch.search.common

import com.tk.quicksearch.search.core.PermissionManager
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.models.ReminderInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tk.quicksearch.app.startup.StartupTrace

class PinningHandler(
    private val scope: CoroutineScope,
    private val permissionManager: PermissionManager,
    private val contactRepository: ContactRepository,
    private val fileRepository: FileSearchRepository,
    private val notesRepository: NotesRepository,
    private val reminderRepository: ReminderRepository,
    private val userPreferences: UserAppPreferences,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit,
) {
    suspend fun loadPinnedContactsForStartup() {
        withContext(Dispatchers.IO) {
            val pinnedContacts =
                runCatching {
                    loadPinnedContacts(
                        hasPermission = permissionManager.hasContactPermission(),
                        hydrateDetails = false,
                    )
                }.getOrDefault(emptyList())
            uiStateUpdater { state -> state.copy(pinnedContacts = pinnedContacts) }
            StartupTrace.mark("QS.Home.PinnedContactsMinimalAvailable")
        }
    }

    fun loadPinnedContactsAndFiles() {
        scope.launch(Dispatchers.IO) {
            loadPinnedContactsAndFilesNow()
        }
    }

    suspend fun loadPinnedContactsAndFilesNow() {
        withContext(Dispatchers.IO) {
            val permissions = checkPermissions()

            val pinnedContacts = loadPinnedContacts(permissions.contacts)
            val pinnedFiles = loadPinnedFiles(permissions.files)
            val pinnedNotes = loadPinnedNotes()
            val pinnedReminders = loadPinnedReminders()

            uiStateUpdater { state ->
                state.copy(
                    pinnedContacts = pinnedContacts,
                    pinnedFiles = pinnedFiles,
                    pinnedNotes = pinnedNotes,
                    pinnedReminders = pinnedReminders,
                )
            }
            StartupTrace.mark("QS.Home.PinnedItemsAvailable")
        }
    }

    fun loadExcludedContactsAndFiles() {
        scope.launch(Dispatchers.IO) {
            loadExcludedContactsAndFilesNow()
        }
    }

    suspend fun loadExcludedContactsAndFilesNow() {
        withContext(Dispatchers.IO) {
            val permissions = checkPermissions()

            val excludedContacts = loadExcludedContacts(permissions.contacts)
            val excludedFiles = loadExcludedFiles(permissions.files)

            uiStateUpdater { state ->
                state.copy(
                    excludedContacts = excludedContacts,
                    excludedFiles = excludedFiles,
                    excludedFileExtensions = userPreferences.getExcludedFileExtensions(),
                )
            }
            StartupTrace.mark("QS.Home.ExcludedItemsAvailable")
        }
    }

    fun loadPinnedAndExcludedContacts() {
        scope.launch(Dispatchers.IO) {
            val hasContactPermission = permissionManager.hasContactPermission()
            val pinnedContacts = loadPinnedContacts(hasContactPermission)
            val excludedContacts = loadExcludedContacts(hasContactPermission)

            uiStateUpdater { state ->
                state.copy(
                    pinnedContacts = pinnedContacts,
                    excludedContacts = excludedContacts,
                )
            }
        }
    }

    fun loadPinnedAndExcludedFiles() {
        scope.launch(Dispatchers.IO) {
            val hasFilePermission = permissionManager.hasFilePermission()
            val pinnedFiles = loadPinnedFiles(hasFilePermission)
            val excludedFiles = loadExcludedFiles(hasFilePermission)

            uiStateUpdater { state ->
                state.copy(
                    pinnedFiles = pinnedFiles,
                    excludedFiles = excludedFiles,
                    excludedFileExtensions = userPreferences.getExcludedFileExtensions(),
                )
            }
        }
    }

    private fun checkPermissions() =
        PermissionsState(
            contacts = permissionManager.hasContactPermission(),
            files = permissionManager.hasFilePermission(),
        )

    private fun loadPinnedContacts(
        hasPermission: Boolean,
        hydrateDetails: Boolean = true,
    ): List<com.tk.quicksearch.search.models.ContactInfo> {
        if (!hasPermission) return emptyList()

        val pinnedIds = userPreferences.getPinnedContactIds()
        if (pinnedIds.isEmpty()) return emptyList()

        val excludedIds = userPreferences.getExcludedContactIds()
        val contacts =
            if (hydrateDetails) {
                contactRepository.getContactsByIds(pinnedIds)
            } else {
                contactRepository.getContactsByIdsMinimal(pinnedIds)
            }
        return contacts
            .filterNot { excludedIds.contains(it.contactId) }
            .sortedByPinnedOrder(userPreferences.getPinnedContactOrder()) { it.contactId }
    }

    private fun loadPinnedFiles(hasPermission: Boolean): List<com.tk.quicksearch.search.models.DeviceFile> {
        if (!hasPermission) return emptyList()

        val pinnedUris = userPreferences.getPinnedFileUris()
        if (pinnedUris.isEmpty()) return emptyList()

        val excludedUris = userPreferences.getExcludedFileUris()
        return fileRepository
            .getFilesByUris(pinnedUris)
            .filterNot { excludedUris.contains(it.uri.toString()) }
            .sortedByPinnedOrder(userPreferences.getPinnedFileOrder()) { it.uri.toString() }
    }

    private fun loadExcludedContacts(hasPermission: Boolean): List<com.tk.quicksearch.search.models.ContactInfo> {
        if (!hasPermission) return emptyList()

        val excludedIds = userPreferences.getExcludedContactIds()
        if (excludedIds.isEmpty()) return emptyList()

        return contactRepository.getContactsByIds(excludedIds)
    }

    private fun loadExcludedFiles(hasPermission: Boolean): List<com.tk.quicksearch.search.models.DeviceFile> {
        if (!hasPermission) return emptyList()

        val excludedUris = userPreferences.getExcludedFileUris()
        if (excludedUris.isEmpty()) return emptyList()

        return fileRepository.getFilesByUris(excludedUris)
    }

    private fun loadPinnedNotes(): List<NoteInfo> {
        val pinnedIds = userPreferences.getPinnedNoteIds()
        if (pinnedIds.isEmpty()) return emptyList()
        return notesRepository
            .getAllNotes()
            .filter { pinnedIds.contains(it.noteId) }
            .sortedByPinnedOrder(userPreferences.getPinnedNoteOrder()) { it.noteId }
    }

    /** Refreshes pinned reminders, e.g. after a reminder is edited or marked done. */
    fun refreshPinnedReminders() {
        scope.launch(Dispatchers.IO) {
            val pinnedReminders = loadPinnedReminders()
            uiStateUpdater { state -> state.copy(pinnedReminders = pinnedReminders) }
        }
    }

    private fun loadPinnedReminders(): List<ReminderInfo> {
        val pinnedIds = userPreferences.getPinnedReminderIds()
        if (pinnedIds.isEmpty()) return emptyList()
        return reminderRepository
            .getRemindersByIds(pinnedIds)
            .sortedByPinnedOrder(userPreferences.getPinnedReminderOrder()) { it.reminderId }
    }

    private data class PermissionsState(
        val contacts: Boolean,
        val files: Boolean,
    )
}

private fun <T, K> List<T>.sortedByPinnedOrder(
    order: List<K>,
    keySelector: (T) -> K,
): List<T> {
    if (order.isEmpty()) return this
    val orderIndex = order.withIndex().associate { it.value to it.index }
    return sortedBy { orderIndex[keySelector(it)] ?: Int.MAX_VALUE }
}

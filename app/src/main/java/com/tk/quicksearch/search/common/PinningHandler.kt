package com.tk.quicksearch.search.common

import com.tk.quicksearch.search.core.PermissionManager
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PinningHandler(
    private val scope: CoroutineScope,
    private val permissionManager: PermissionManager,
    private val contactRepository: ContactRepository,
    private val fileRepository: FileSearchRepository,
    private val userPreferences: UserAppPreferences,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit,
) {
    fun loadPinnedContactsAndFiles() {
        scope.launch(Dispatchers.IO) {
            val permissions = checkPermissions()

            val pinnedContacts = loadPinnedContacts(permissions.contacts)
            val pinnedFiles = loadPinnedFiles(permissions.files)

            uiStateUpdater { state ->
                state.copy(
                    pinnedContacts = pinnedContacts,
                    pinnedFiles = pinnedFiles,
                )
            }
        }
    }

    fun loadExcludedContactsAndFiles() {
        scope.launch(Dispatchers.IO) {
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

    private fun loadPinnedContacts(hasPermission: Boolean): List<com.tk.quicksearch.search.models.ContactInfo> {
        if (!hasPermission) return emptyList()

        val pinnedIds = userPreferences.getPinnedContactIds()
        if (pinnedIds.isEmpty()) return emptyList()

        val excludedIds = userPreferences.getExcludedContactIds()
        return contactRepository
            .getContactsByIds(pinnedIds)
            .filterNot { excludedIds.contains(it.contactId) }
    }

    private fun loadPinnedFiles(hasPermission: Boolean): List<com.tk.quicksearch.search.models.DeviceFile> {
        if (!hasPermission) return emptyList()

        val pinnedUris = userPreferences.getPinnedFileUris()
        if (pinnedUris.isEmpty()) return emptyList()

        val excludedUris = userPreferences.getExcludedFileUris()
        return fileRepository
            .getFilesByUris(pinnedUris)
            .filterNot { excludedUris.contains(it.uri.toString()) }
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

    private data class PermissionsState(
        val contacts: Boolean,
        val files: Boolean,
    )
}

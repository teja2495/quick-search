package com.tk.quicksearch.search

import com.tk.quicksearch.data.ContactRepository
import com.tk.quicksearch.data.FileSearchRepository
import com.tk.quicksearch.data.UserAppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

class PinningHandler(
    private val scope: CoroutineScope,
    private val permissionManager: PermissionManager,
    private val contactRepository: ContactRepository,
    private val fileRepository: FileSearchRepository,
    private val userPreferences: UserAppPreferences,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit
) {

    fun loadPinnedContactsAndFiles() {
        scope.launch(Dispatchers.IO) {
            val hasContactsPermission = permissionManager.hasContactPermission()
            val hasFilesPermission = permissionManager.hasFilePermission()

            val pinnedContactIds = userPreferences.getPinnedContactIds()
            val excludedContactIds = userPreferences.getExcludedContactIds()
            val pinnedContacts = if (hasContactsPermission && pinnedContactIds.isNotEmpty()) {
                contactRepository.getContactsByIds(pinnedContactIds)
                    .filterNot { excludedContactIds.contains(it.contactId) }
            } else {
                emptyList()
            }

            val pinnedFileUris = userPreferences.getPinnedFileUris()
            val excludedFileUris = userPreferences.getExcludedFileUris()
            val pinnedFiles = if (hasFilesPermission && pinnedFileUris.isNotEmpty()) {
                fileRepository.getFilesByUris(pinnedFileUris)
                    .filterNot { excludedFileUris.contains(it.uri.toString()) }
            } else {
                emptyList()
            }

            uiStateUpdater { state ->
                state.copy(
                    pinnedContacts = pinnedContacts,
                    pinnedFiles = pinnedFiles
                )
            }
        }
    }

    fun loadExcludedContactsAndFiles() {
        scope.launch(Dispatchers.IO) {
            val hasContactsPermission = permissionManager.hasContactPermission()
            val hasFilesPermission = permissionManager.hasFilePermission()

            val excludedContactIds = userPreferences.getExcludedContactIds()
            val excludedContacts = if (hasContactsPermission && excludedContactIds.isNotEmpty()) {
                contactRepository.getContactsByIds(excludedContactIds)
            } else {
                emptyList()
            }

            val excludedFileUris = userPreferences.getExcludedFileUris()
            val excludedFiles = if (hasFilesPermission && excludedFileUris.isNotEmpty()) {
                fileRepository.getFilesByUris(excludedFileUris)
            } else {
                emptyList()
            }

            uiStateUpdater { state ->
                state.copy(
                    excludedContacts = excludedContacts,
                    excludedFiles = excludedFiles,
                    excludedFileExtensions = userPreferences.getExcludedFileExtensions()
                )
            }
        }
    }
}

package com.tk.quicksearch.search.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.interfaces.PermissionService
import com.tk.quicksearch.interfaces.SearchOperationsService
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.search.contacts.ContactManagementHandler
import com.tk.quicksearch.search.files.FileManagementHandler
import com.tk.quicksearch.search.settings.SettingsManagementHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dedicated ViewModel for managing search UI state and coordinating with services
 * This separates UI state management from business logic
 */
class SearchStateViewModel(
    private val userPreferences: UserAppPreferences,
    private val permissionService: PermissionService,
    private val searchOperationsService: SearchOperationsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Management handlers will be created by the main ViewModel and passed in
    // For now, we'll create them as needed
    // TODO: Add other managers as needed

    private fun updateUiState(updater: (SearchUiState) -> SearchUiState) {
        _uiState.update(updater)
    }

    fun refreshDerivedState(
        lastUpdated: Long? = null,
        isLoading: Boolean? = null
    ) {
        // This method will be implemented when we refactor the main ViewModel
        // For now, it's a placeholder
    }

    fun updateSearchResults(
        query: String,
        appResults: List<AppInfo> = emptyList(),
        contactResults: List<ContactInfo> = emptyList(),
        fileResults: List<DeviceFile> = emptyList(),
        settingResults: List<SettingShortcut> = emptyList()
    ) {
        _uiState.update { state ->
            state.copy(
                query = query,
                searchResults = appResults,
                contactResults = contactResults,
                fileResults = fileResults,
                settingResults = settingResults
            )
        }
    }

    fun clearSearchResults() {
        _uiState.update { state ->
            state.copy(
                searchResults = emptyList(),
                contactResults = emptyList(),
                fileResults = emptyList(),
                settingResults = emptyList()
            )
        }
    }

    fun setLoadingState(isLoading: Boolean, errorMessage: String? = null) {
        _uiState.update { it.copy(isLoading = isLoading, errorMessage = errorMessage) }
    }

    fun updatePermissions() {
        viewModelScope.launch(Dispatchers.IO) {
            val hasContacts = permissionService.hasContactPermission()
            val hasFiles = permissionService.hasFilePermission()
            val hasCall = permissionService.hasCallPermission()

            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    state.copy(
                        hasContactPermission = hasContacts,
                        hasFilePermission = hasFiles,
                        hasCallPermission = hasCall,
                        contactResults = if (hasContacts) state.contactResults else emptyList(),
                        fileResults = if (hasFiles) state.fileResults else emptyList()
                    )
                }
            }
        }
    }
}
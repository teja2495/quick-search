package com.tk.quicksearch.interfaces

import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.search.core.SearchSection

/**
 * Service for handling search operations across different content types
 */
interface SearchOperationsService {
    suspend fun searchContacts(query: String, enabledSections: Set<SearchSection>): List<ContactInfo>
    suspend fun searchFiles(query: String, enabledSections: Set<SearchSection>, enabledFileTypes: Set<String>): List<DeviceFile>
    suspend fun searchSettings(query: String, enabledSections: Set<SearchSection>): List<SettingShortcut>
    suspend fun getPinnedContacts(): List<ContactInfo>
    suspend fun getPinnedFiles(): List<DeviceFile>
    suspend fun getExcludedContacts(): List<ContactInfo>
    suspend fun getExcludedFiles(): List<DeviceFile>
}
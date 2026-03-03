package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.searchScreen.ExpandedSection

/** Data class holding all the state needed for section rendering. */
data class SectionRenderingState(
    val isSearching: Boolean,
    val expandedSection: ExpandedSection,
    val hasAppResults: Boolean,
    val hasAppShortcutResults: Boolean,
    val hasContactResults: Boolean,
    val hasFileResults: Boolean,
    val hasSettingResults: Boolean,
    val hasPinnedAppShortcuts: Boolean,
    val hasPinnedContacts: Boolean,
    val hasPinnedFiles: Boolean,
    val hasPinnedSettings: Boolean,
    val shouldShowApps: Boolean,
    val shouldShowAppShortcuts: Boolean,
    val shouldShowContacts: Boolean,
    val shouldShowFiles: Boolean,
    val shouldShowSettings: Boolean,
    val hasMultipleExpandableSections: Boolean,
    val displayApps: List<AppInfo>,
    val appShortcutResults: List<StaticShortcut>,
    val contactResults: List<ContactInfo>,
    val fileResults: List<DeviceFile>,
    val settingResults: List<DeviceSetting>,
    val pinnedAppShortcuts: List<StaticShortcut>,
    val pinnedContacts: List<ContactInfo>,
    val pinnedFiles: List<DeviceFile>,
    val pinnedSettings: List<DeviceSetting>,
    val orderedSections: List<SearchSection>,
    val shortcutDetected: Boolean = false,
)
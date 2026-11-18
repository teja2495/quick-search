package com.tk.quicksearch.settings.settingsScreen

import com.tk.quicksearch.search.data.preferences.BasePreferences
import com.tk.quicksearch.settings.settingsScreen.SettingsBackupManager.ExportItem
import com.tk.quicksearch.settings.settingsScreen.SettingsBackupManager.ExportOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSectionsTest {
    @Test
    fun exportPickerRequiresAnExplicitChoiceForApiKeys() {
        val selection = ExportSelectionState.forAvailable(setOf(ExportItem.SEARCH_HISTORY, ExportItem.API_KEYS))
        assertEquals(
            setOf(ExportItem.SETTINGS, ExportItem.SEARCH_ENGINES, ExportItem.SHORTCUTS, ExportItem.SEARCH_HISTORY),
            selection.toExportOptions().selectedItems,
        )
        assertTrue(selection.withSelection(ExportItem.API_KEYS, true).toExportOptions().includes(ExportItem.API_KEYS))
        assertFalse(ExportOptions().includes(ExportItem.API_KEYS))
    }

    @Test
    fun selectedHistoryRoundTripsThroughPreferenceFiltering() {
        val file = BasePreferences.SESSION_PREFS_NAME
        val key = BasePreferences.KEY_RECENT_QUERIES
        val selected = setOf(ExportItem.SEARCH_HISTORY)
        assertTrue(SettingsBackupManager.shouldIncludeKeyForExport(file, key, ExportOptions(selected)))
        assertTrue(SettingsBackupManager.shouldRestorePreference(file, key, selected))
        assertFalse(SettingsBackupManager.shouldRestorePreference(file, key, emptySet()))
        assertFalse(SettingsBackupManager.shouldRestorePreference(file, key, null))
    }

    @Test
    fun notePinsStillFollowPinnedItemsAndBookmarksUseTheLegacyField() {
        assertEquals(
            ExportItem.PINNED_ITEMS,
            ExportItem.forPreference(BasePreferences.PREFS_NAME, BasePreferences.KEY_PINNED_NOTE_IDS),
        )
        assertEquals("bookmarks", ExportItem.BOOKMARKS.payload?.field)
    }
}

package com.tk.quicksearch.search.startup

import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartupHomeSurfaceSnapshotTest {
    @Test
    fun bounded_limitsCachedHomeCollections() {
        val snapshot =
            StartupHomeSurfaceSnapshot(
                pinnedApps = (0 until 60).map(::app),
                recentApps = (0 until 40).map(::app),
                recentItems =
                    (0 until 40).map { index ->
                        RecentSearchItem.Query(RecentSearchEntry.Query("query $index"))
                    },
            ).bounded()

        assertEquals(50, snapshot.pinnedApps.size)
        assertEquals(24, snapshot.recentApps.size)
        assertEquals(30, snapshot.recentItems.size)
    }

    @Test
    fun bounded_removesShortcutIconPayloads() {
        val shortcut =
            com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut(
                packageName = "example.package",
                appLabel = "Example",
                id = "shortcut",
                shortLabel = "Shortcut",
                longLabel = null,
                iconResId = null,
                iconBase64 = "large icon payload",
                enabled = true,
                intents = emptyList(),
            )

        val cached = StartupHomeSurfaceSnapshot(pinnedAppShortcuts = listOf(shortcut)).bounded()

        assertNull(cached.pinnedAppShortcuts.single().iconBase64)
    }

    private fun app(index: Int) =
        AppInfo(
            appName = "App $index",
            packageName = "example.package.$index",
            lastUsedTime = index.toLong(),
            totalTimeInForeground = index.toLong(),
            firstInstallTime = index.toLong(),
            isSystemApp = false,
        )
}

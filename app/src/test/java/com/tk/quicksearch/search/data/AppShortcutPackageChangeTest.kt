package com.tk.quicksearch.search.data

import android.content.Intent
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.removeSystemShortcutsForPackage
import org.junit.Assert.assertEquals
import org.junit.Test

class AppShortcutPackageChangeTest {
    @Test
    fun removalPrunesSystemShortcutsForTheUnavailablePackage() {
        val removed = shortcut(packageName = "com.example.removed", id = "system_shortcut")
        val retained = shortcut(packageName = "com.example.retained", id = "system_shortcut")

        assertEquals(
            listOf(retained),
            removeSystemShortcutsForPackage(
                shortcuts = listOf(removed, retained),
                packageName = "com.example.removed",
            ),
        )
    }

    @Test
    fun removalPreservesUserCreatedShortcuts() {
        val custom = shortcut(packageName = "com.example.removed", id = "custom_deeplink_1")

        assertEquals(
            listOf(custom),
            removeSystemShortcutsForPackage(
                shortcuts = listOf(custom),
                packageName = "com.example.removed",
            ),
        )
    }

    private fun shortcut(
        packageName: String,
        id: String,
    ) =
        StaticShortcut(
            packageName = packageName,
            appLabel = packageName,
            id = id,
            shortLabel = id,
            longLabel = null,
            iconResId = null,
            enabled = true,
            intents = listOf(Intent("com.example.SHORTCUT")),
        )
}

package com.tk.quicksearch.search.appShortcuts

import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.allAppShortcutsKey
import com.tk.quicksearch.search.data.AppShortcutRepository.areAllAppShortcutsDisabled
import com.tk.quicksearch.search.data.AppShortcutRepository.isShortcutDisabled
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShortcutDisabledStateTest {

    private fun shortcut(packageName: String, id: String) =
        StaticShortcut(
            packageName = packageName,
            appLabel = packageName,
            id = id,
            shortLabel = id,
            longLabel = null,
            iconResId = null,
            enabled = true,
            intents = emptyList(),
        )

    @Test
    fun individuallyDisabledShortcutIsDisabled() {
        val target = shortcut("com.example.maps", "home")
        val other = shortcut("com.example.maps", "work")
        val disabledIds = setOf(shortcutKey(target))

        assertTrue(isShortcutDisabled(target, disabledIds))
        assertFalse(isShortcutDisabled(other, disabledIds))
        assertFalse(areAllAppShortcutsDisabled("com.example.maps", disabledIds))
    }

    @Test
    fun appWideKeyDisablesCurrentAndFutureShortcutsOfThatAppOnly() {
        val disabledIds = setOf(allAppShortcutsKey("com.example.maps"))

        assertTrue(areAllAppShortcutsDisabled("com.example.maps", disabledIds))
        assertTrue(isShortcutDisabled(shortcut("com.example.maps", "home"), disabledIds))
        assertTrue(isShortcutDisabled(shortcut("com.example.maps", "added_later"), disabledIds))
        assertFalse(isShortcutDisabled(shortcut("com.example.mail", "compose"), disabledIds))
    }
}

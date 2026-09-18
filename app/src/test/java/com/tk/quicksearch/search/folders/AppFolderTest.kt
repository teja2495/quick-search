package com.tk.quicksearch.search.folders

import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFolderTest {

    private fun app(packageName: String, userHandleId: Int? = null) =
        AppInfo(
            appName = packageName,
            packageName = packageName,
            lastUsedTime = 0L,
            totalTimeInForeground = 0L,
            firstInstallTime = 0L,
            isSystemApp = false,
            userHandleId = userHandleId,
        )

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
    fun gridAndMemberKeysRoundTrip() {
        val workApp = app("com.example.mail", userHandleId = 10)
        val appGridKey = workApp.launchCountKey()
        val shortcutGridKey = shortcutGridKey(shortcut("com.example.maps", "home"))

        assertEquals(appFolderMemberKey(workApp), gridKeyToMemberKey(appGridKey))
        assertEquals(appGridKey, memberKeyToGridKey(appFolderMemberKey(workApp)))
        assertEquals(
            shortcutGridKey,
            memberKeyToGridKey(requireNotNull(gridKeyToMemberKey(shortcutGridKey))),
        )
        assertTrue(isShortcutMemberKey(requireNotNull(gridKeyToMemberKey(shortcutGridKey))))
        assertNull(gridKeyToMemberKey(folderGridKey("abc")))
        assertEquals("abc", folderIdFromGridKey(folderGridKey("abc")))
    }

    @Test
    fun resolveSkipsUnavailableMembersAndEmptyFolders() {
        val mail = app("com.example.mail")
        val maps = shortcut("com.example.maps", "home")
        val disabled = shortcut("com.example.maps", "work")
        val folders =
            listOf(
                AppFolder(
                    id = "a",
                    memberKeys =
                        listOf(
                            appFolderMemberKey(app("com.example.uninstalled")),
                            appFolderMemberKey(maps),
                            appFolderMemberKey(mail),
                        ),
                ),
                AppFolder(id = "b", memberKeys = listOf(appFolderMemberKey(disabled))),
            )

        val resolved =
            resolveAppFolders(
                folders = folders,
                apps = listOf(mail),
                shortcuts = listOf(maps, disabled),
                disabledShortcutIds = setOf("com.example.maps:work"),
            )

        assertEquals(listOf("a"), resolved.map { it.id })
        assertEquals(
            listOf(appFolderMemberKey(maps), appFolderMemberKey(mail)),
            resolved.single().members.map { it.memberKey },
        )
    }
}

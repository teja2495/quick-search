package com.tk.quicksearch.search.folders

import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.models.AppInfo

private const val APP_MEMBER_PREFIX = "APP:"
private const val SHORTCUT_MEMBER_PREFIX = "SHORTCUT:"
private const val FOLDER_GRID_KEY_PREFIX = "folder:"
private const val SHORTCUT_GRID_KEY_PREFIX = "shortcut:"

/** A Pinned-tab folder of apps and app shortcuts. [memberKeys] are ordered member keys. */
data class AppFolder(
    val id: String,
    val name: String = "",
    val memberKeys: List<String> = emptyList(),
) {
    val gridKey: String get() = folderGridKey(id)
}

/** A folder member resolved against the installed apps and available shortcuts. */
sealed interface AppFolderMember {
    val memberKey: String

    data class App(val app: AppInfo) : AppFolderMember {
        override val memberKey: String get() = appFolderMemberKey(app)
    }

    data class Shortcut(val shortcut: StaticShortcut) : AppFolderMember {
        override val memberKey: String get() = appFolderMemberKey(shortcut)
    }
}

/** A folder whose members are all currently available, in folder order. */
data class ResolvedAppFolder(
    val folder: AppFolder,
    val members: List<AppFolderMember>,
) {
    val id: String get() = folder.id
    val name: String get() = folder.name
}

fun appFolderMemberKey(app: AppInfo): String = "$APP_MEMBER_PREFIX${app.launchCountKey()}"

fun appFolderMemberKey(shortcut: StaticShortcut): String = shortcutMemberKey(shortcutKey(shortcut))

/** Folder member key of the shortcut whose [shortcutKey] is [shortcutId]. */
fun shortcutMemberKey(shortcutId: String): String = "$SHORTCUT_MEMBER_PREFIX$shortcutId"

fun folderGridKey(folderId: String): String = "$FOLDER_GRID_KEY_PREFIX$folderId"

/** Pinned app grid order key of a pinned shortcut tile. */
fun shortcutGridKey(shortcut: StaticShortcut): String =
    "$SHORTCUT_GRID_KEY_PREFIX${shortcutKey(shortcut)}"

fun isFolderGridKey(gridKey: String): Boolean = gridKey.startsWith(FOLDER_GRID_KEY_PREFIX)

fun folderIdFromGridKey(gridKey: String): String? =
    gridKey.takeIf(::isFolderGridKey)?.removePrefix(FOLDER_GRID_KEY_PREFIX)

/** Maps a pinned app grid order key to a folder member key; folders have none. */
fun gridKeyToMemberKey(gridKey: String): String? =
    when {
        isFolderGridKey(gridKey) -> null
        gridKey.startsWith(SHORTCUT_GRID_KEY_PREFIX) ->
            "$SHORTCUT_MEMBER_PREFIX${gridKey.removePrefix(SHORTCUT_GRID_KEY_PREFIX)}"
        else -> "$APP_MEMBER_PREFIX$gridKey"
    }

fun isShortcutMemberKey(memberKey: String): Boolean = memberKey.startsWith(SHORTCUT_MEMBER_PREFIX)

fun memberKeyToGridKey(memberKey: String): String? =
    when {
        memberKey.startsWith(APP_MEMBER_PREFIX) -> memberKey.removePrefix(APP_MEMBER_PREFIX)
        memberKey.startsWith(SHORTCUT_MEMBER_PREFIX) ->
            "$SHORTCUT_GRID_KEY_PREFIX${memberKey.removePrefix(SHORTCUT_MEMBER_PREFIX)}"
        else -> null
    }

/**
 * Resolves folder members against the available apps and enabled shortcuts, skipping members that
 * are unavailable and folders left with no members.
 */
fun resolveAppFolders(
    folders: List<AppFolder>,
    apps: List<AppInfo>,
    shortcuts: List<StaticShortcut>,
    disabledShortcutIds: Set<String>,
): List<ResolvedAppFolder> {
    if (folders.isEmpty()) return emptyList()
    val appsByKey = apps.associateBy { appFolderMemberKey(it) }
    val shortcutsByKey =
        shortcuts
            .filterNot { disabledShortcutIds.contains(shortcutKey(it)) }
            .associateBy { appFolderMemberKey(it) }
    return folders.mapNotNull { folder ->
        val members =
            folder.memberKeys.mapNotNull { key ->
                appsByKey[key]?.let { AppFolderMember.App(it) }
                    ?: shortcutsByKey[key]?.let { AppFolderMember.Shortcut(it) }
            }
        members.takeIf { it.isNotEmpty() }?.let { ResolvedAppFolder(folder, it) }
    }
}

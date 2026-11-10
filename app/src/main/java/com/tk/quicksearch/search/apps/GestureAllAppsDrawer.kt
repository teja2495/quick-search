package com.tk.quicksearch.search.apps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.notificationDots.rememberNotificationDotKeys
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.isShortcutDisabled
import com.tk.quicksearch.search.folders.appFolderMemberKey
import com.tk.quicksearch.search.folders.resolveAppFolders
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.searchScreen.AppsSectionParams
import java.util.Locale

/** Opens the All Apps drawer from gestures, independently of the apps section's own button. */
@Stable
internal class AllAppsDrawerController {
    var isOpen by mutableStateOf(false)
        private set

    fun open() {
        isOpen = true
    }

    fun dismiss() {
        isOpen = false
    }
}

internal val LocalAllAppsDrawerController = staticCompositionLocalOf { AllAppsDrawerController() }

internal fun List<AppInfo>.sortedForAllApps(): List<AppInfo> =
    sortedWith(
        compareBy<AppInfo> { it.appName.lowercase(Locale.getDefault()) }
            .thenBy { it.packageName.lowercase(Locale.getDefault()) }
            .thenBy { it.userHandleId ?: Int.MIN_VALUE },
    )

internal fun List<StaticShortcut>.enabledByPackage(
    disabledShortcutIds: Set<String>,
): Map<String, List<StaticShortcut>> =
    asSequence()
        .filterNot { shortcut -> isShortcutDisabled(shortcut, disabledShortcutIds) }
        .groupBy { it.packageName }

/** Shows the All Apps drawer while [LocalAllAppsDrawerController] is open. */
@Composable
internal fun GestureAllAppsDrawer(params: AppsSectionParams?) {
    val controller = LocalAllAppsDrawerController.current
    if (!controller.isOpen || params == null || params.allApps.isEmpty()) return
    val notificationDotKeys = rememberNotificationDotKeys(params.notificationDotsEnabled)
    val apps = remember(params.allApps) { params.allApps.sortedForAllApps() }
    val shortcutsByPackage =
        remember(params.appShortcuts, params.disabledAppShortcutIds) {
            params.appShortcuts.enabledByPackage(params.disabledAppShortcutIds)
        }
    val folderIdByMemberKey =
        remember(params.appFolders, params.allApps, params.appShortcuts, params.disabledAppShortcutIds) {
            resolveAppFolders(params.appFolders, params.allApps, params.appShortcuts, params.disabledAppShortcutIds)
                .flatMap { folder -> folder.members.map { it.memberKey to folder.id } }
                .toMap()
        }
    // Apps inside a folder are unpinned from the grid but still count as pinned.
    val pinnedPackageNames =
        remember(params.pinnedPackageNames, params.appFolders, params.allApps) {
            val appsByMemberKey = params.allApps.associateBy { appFolderMemberKey(it) }
            params.pinnedPackageNames +
                folderIdByMemberKey.keys.mapNotNull { appsByMemberKey[it]?.launchCountKey() }
        }
    val unpinFromFolder: (AppInfo) -> Boolean = { app ->
        val memberKey = appFolderMemberKey(app)
        val folderId = folderIdByMemberKey[memberKey]
        val folderActions = params.appFolderActions
        if (folderId != null && folderActions != null) {
            folderActions.onUnpinFromFolder(folderId, memberKey, params.pinnedAppGridOrder)
            true
        } else {
            false
        }
    }
    AllAppsDrawer(
        title = stringResource(R.string.settings_app_shortcuts_filter_all_apps),
        apps = apps,
        onDismiss = controller::dismiss,
        onAppClick = { app ->
            controller.dismiss()
            params.onAppClick(app)
        },
        onAppShortcutClick = params.onAppShortcutClick,
        onAppInfoClick = params.onAppInfoClick,
        onUninstallClick = params.onUninstallClick,
        onHideApp = { app ->
            unpinFromFolder(app)
            params.onHideApp(app)
        },
        onDisableAppShortcut = params.onDisableAppShortcut,
        onPinApp = params.onPinApp,
        onUnpinApp = { app -> if (!unpinFromFolder(app)) params.onUnpinApp(app) },
        onNicknameClick = params.onNicknameClick,
        onTriggerClick = params.onTriggerClick,
        onOpenInSplitScreen = params.onOpenInSplitScreen,
        getAppNickname = params.getAppNickname,
        getAppTrigger = params.getAppTrigger,
        pinnedPackageNames = pinnedPackageNames,
        shortcutsByPackage = shortcutsByPackage,
        phoneColumnOverride = params.phoneColumnOverride,
        appIconSizeStep = params.appIconSizeStep,
        iconPackPackage = params.iconPackPackage,
        appIconShape = params.appIconShape,
        notificationDotKeys = notificationDotKeys,
    )
}

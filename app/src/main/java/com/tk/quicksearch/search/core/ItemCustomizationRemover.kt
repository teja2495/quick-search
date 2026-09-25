package com.tk.quicksearch.search.core

import androidx.compose.runtime.staticCompositionLocalOf
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile

/** Clears a result's nickname or trigger, such as from the long-press Remove option in item menus. */
class ItemCustomizationRemover(
    val removeAppNickname: (AppInfo) -> Unit,
    val removeAppTrigger: (AppInfo) -> Unit,
    val removeAppShortcutNickname: (StaticShortcut) -> Unit,
    val removeAppShortcutTrigger: (StaticShortcut) -> Unit,
    val removeContactNickname: (ContactInfo) -> Unit,
    val removeContactTrigger: (ContactInfo) -> Unit,
    val removeFileNickname: (DeviceFile) -> Unit,
    val removeFileTrigger: (DeviceFile) -> Unit,
)

/** Null where results can't be edited, so menus hide the Remove option. */
val LocalItemCustomizationRemover = staticCompositionLocalOf<ItemCustomizationRemover?> { null }

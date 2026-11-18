package com.tk.quicksearch.search.appSettings

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Opens a settings page from inside the search UI, such as long-press item menus.
 * Null where no settings navigation is available, so callers can hide the option.
 */
val LocalOpenAppSettingDestination = staticCompositionLocalOf<((AppSettingsTarget.Host) -> Unit)?> { null }

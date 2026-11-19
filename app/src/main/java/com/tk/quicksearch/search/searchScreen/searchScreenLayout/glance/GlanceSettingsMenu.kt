package com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.AppSettingsTarget
import com.tk.quicksearch.search.appSettings.LocalOpenAppSettingDestination
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuItem
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

/** Opens the At a Glance settings page, or null where Settings can't be reached from this surface. */
@Composable
internal fun rememberOpenAtAGlanceSettings(): (() -> Unit)? {
    val openDestination = LocalOpenAppSettingDestination.current ?: return null
    return remember(openDestination) {
        { openDestination(AppSettingsTarget.Page(SettingsDetailType.AT_A_GLANCE)) }
    }
}

/**
 * The last item of every At a Glance row's long-press menu. Put an [AppDropdownMenuDivider] before
 * it when the row has its own options above.
 */
@Composable
internal fun AtAGlanceSettingsMenuItem(
    openSettings: () -> Unit,
    closeMenu: () -> Unit,
) {
    AppDropdownMenuItem(
        text = stringResource(R.string.action_at_a_glance_settings),
        icon = MaterialSymbols.Settings,
        onClick = {
            closeMenu()
            openSettings()
        },
    )
}

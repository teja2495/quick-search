package com.tk.quicksearch.settings.settingsDetailScreen

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.AppSettingsToggleKey
import com.tk.quicksearch.search.apps.notificationDots.rememberNotificationDotsCheckedChange
import com.tk.quicksearch.search.apps.speedBump.SpeedBumpAccessibilityDisclosureHost
import com.tk.quicksearch.search.apps.speedBump.SpeedBumpOutsideQuickSearchToggleState
import com.tk.quicksearch.search.apps.speedBump.rememberSpeedBumpOutsideQuickSearchToggleState
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsCommand
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.shared.util.rememberPhysicalKeyboardConnected

@Composable
fun MoreOptionsSettings(
    isToggleEnabled: (AppSettingsToggleKey) -> Boolean,
    onApplySettingsCommand: (SettingsCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDefaultLauncher = context.isDefaultHomeApp()
    val isPhysicalKeyboardConnected = rememberPhysicalKeyboardConnected()

    val onNotificationDotsCheckedChange =
        rememberNotificationDotsCheckedChange { enabled ->
            onApplySettingsCommand(
                SettingsCommand.Toggle(
                    key = AppSettingsToggleKey.NOTIFICATION_DOTS,
                    enabled = enabled,
                ),
            )
        }
    val speedBumpToggle = rememberSpeedBumpOutsideQuickSearchToggleState()
    SpeedBumpAccessibilityDisclosureHost(speedBumpToggle)
    val isMoreOptionEnabled: (AppSettingsToggleKey) -> Boolean = { key ->
        if (key == AppSettingsToggleKey.SPEED_BUMP_OUTSIDE_QUICK_SEARCH) {
            speedBumpToggle.isOn
        } else {
            isToggleEnabled(key)
        }
    }
    val appToggleItems =
        listOfNotNull(
            ToggleItem(
                key = AppSettingsToggleKey.NOTIFICATION_DOTS,
                titleRes = R.string.notification_dots_toggle_title,
                subtitleRes = R.string.notification_dots_toggle_desc,
                leadingIcon = MaterialSymbols.Notifications,
            ),
            ToggleItem(
                key = AppSettingsToggleKey.SPEED_BUMP_OUTSIDE_QUICK_SEARCH,
                titleRes = R.string.speed_bump_outside_quick_search_title,
                subtitleRes = R.string.speed_bump_outside_quick_search_desc,
                leadingIcon = MaterialSymbols.Spa,
            ),
            ToggleItem(
                key = AppSettingsToggleKey.SHOW_ALL_APPS_BUTTON,
                titleRes = R.string.settings_app_shortcuts_filter_all_apps,
                subtitleRes = R.string.show_all_apps_button_toggle_desc,
                leadingIcon = MaterialSymbols.Apps,
            ),
            ToggleItem(
                key = AppSettingsToggleKey.INCLUDE_NON_LAUNCHABLE_APPS_IN_SEARCH,
                titleRes = R.string.include_non_launchable_apps_toggle_title,
                subtitleRes = R.string.include_non_launchable_apps_toggle_desc,
                leadingIcon = MaterialSymbols.Apps,
            ),
            // Android archives apps starting with Android 15.
            ToggleItem(
                key = AppSettingsToggleKey.INCLUDE_ARCHIVED_APPS_IN_SEARCH,
                titleRes = R.string.include_archived_apps_toggle_title,
                subtitleRes = R.string.include_archived_apps_toggle_desc,
                leadingIcon = MaterialSymbols.Archive,
            ).takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM },
        )
    val otherToggleItems =
        listOf(
            ToggleItem(
                key = AppSettingsToggleKey.SHOW_IN_RECENTS,
                titleRes = R.string.show_in_recents_toggle_title,
                subtitleRes = R.string.show_in_recents_toggle_desc,
                leadingIcon = MaterialSymbols.Apps,
            ),
            ToggleItem(
                key = AppSettingsToggleKey.SEARCH_HINTS,
                titleRes = R.string.settings_search_hints_title,
                subtitleRes = R.string.settings_search_hints_desc,
                leadingIcon = MaterialSymbols.Lightbulb,
            ),
            ToggleItem(
                key = AppSettingsToggleKey.OPEN_TOP_RESULT_USING_KEYBOARD,
                titleRes = R.string.open_top_result_using_keyboard_toggle_title,
                subtitleRes = R.string.open_top_result_using_keyboard_toggle_desc,
                leadingIcon = MaterialSymbols.Keyboard,
            ),
            ToggleItem(
                key = AppSettingsToggleKey.TOP_RESULT_INDICATOR,
                titleRes = R.string.top_result_indicator_toggle_title,
                subtitleRes = R.string.top_result_indicator_toggle_desc,
                leadingIcon = MaterialSymbols.CheckCircle,
            ),
            ToggleItem(
                key = AppSettingsToggleKey.OPEN_KEYBOARD,
                titleRes = R.string.open_keyboard_toggle_title,
                subtitleRes = R.string.open_keyboard_toggle_desc,
                leadingIcon = MaterialSymbols.Keyboard,
            ),
            ToggleItem(
                key = AppSettingsToggleKey.CLEAR_QUERY,
                titleRes = R.string.clear_query_toggle_title,
                subtitleRes = R.string.clear_query_toggle_desc,
                leadingIcon = MaterialSymbols.SearchOff,
            ),
            ToggleItem(
                key = AppSettingsToggleKey.AUTO_CLOSE_OVERLAY,
                titleRes = R.string.auto_close_overlay_toggle_title,
                subtitleRes = R.string.auto_close_overlay_toggle_desc,
                leadingIcon = MaterialSymbols.Close,
            ),
        )
            .filterNot {
                isPhysicalKeyboardConnected &&
                    it.key == AppSettingsToggleKey.TOP_RESULT_INDICATOR
            }
            .filterNot {
                it.key == AppSettingsToggleKey.TOP_RESULT_INDICATOR &&
                    !isToggleEnabled(AppSettingsToggleKey.OPEN_TOP_RESULT_USING_KEYBOARD)
            }

    Column(modifier = modifier.fillMaxWidth()) {
        MoreOptionsToggleCard(
            items = appToggleItems,
            isDefaultLauncher = isDefaultLauncher,
            isToggleEnabled = isMoreOptionEnabled,
            onApplySettingsCommand = onApplySettingsCommand,
            onNotificationDotsCheckedChange = onNotificationDotsCheckedChange,
            speedBumpToggle = speedBumpToggle,
        )
        Spacer(modifier = Modifier.height(DesignTokens.SpacingLarge))
        MoreOptionsToggleCard(
            items = otherToggleItems,
            isDefaultLauncher = isDefaultLauncher,
            isToggleEnabled = isMoreOptionEnabled,
            onApplySettingsCommand = onApplySettingsCommand,
            onNotificationDotsCheckedChange = onNotificationDotsCheckedChange,
            speedBumpToggle = speedBumpToggle,
        )
    }
}

@Composable
private fun MoreOptionsToggleCard(
    items: List<ToggleItem>,
    isDefaultLauncher: Boolean,
    isToggleEnabled: (AppSettingsToggleKey) -> Boolean,
    onApplySettingsCommand: (SettingsCommand) -> Unit,
    onNotificationDotsCheckedChange: (Boolean) -> Unit,
    speedBumpToggle: SpeedBumpOutsideQuickSearchToggleState,
) {
    SettingsCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            items.forEachIndexed { index, item ->
                val isItemEnabled = if (item.key == AppSettingsToggleKey.AUTO_CLOSE_OVERLAY) !isDefaultLauncher else true
                val itemSubtitle = when {
                    item.key == AppSettingsToggleKey.AUTO_CLOSE_OVERLAY && isDefaultLauncher ->
                        stringResource(R.string.settings_overlay_mode_desc_launcher_blocked)
                    item.key == AppSettingsToggleKey.SPEED_BUMP_OUTSIDE_QUICK_SEARCH &&
                        !speedBumpToggle.hasAccessibility ->
                        stringResource(R.string.settings_notch_accessibility_needed)
                    else -> stringResource(item.subtitleRes)
                }
                SettingsToggleRow(
                    title = stringResource(item.titleRes),
                    subtitle = itemSubtitle,
                    checked = isToggleEnabled(item.key),
                    enabled = isItemEnabled,
                    onCheckedChange = { enabled ->
                        if (item.key == AppSettingsToggleKey.NOTIFICATION_DOTS) {
                            onNotificationDotsCheckedChange(enabled)
                        } else if (item.key == AppSettingsToggleKey.SPEED_BUMP_OUTSIDE_QUICK_SEARCH) {
                            speedBumpToggle.setEnabled(enabled)
                        } else {
                            onApplySettingsCommand(
                                SettingsCommand.Toggle(
                                    key = item.key,
                                    enabled = enabled,
                                ),
                            )
                        }
                    },
                    leadingIcon = item.leadingIcon,
                    isFirstItem = index == 0,
                    isLastItem = index == items.lastIndex,
                )
            }
        }
    }
}

private data class ToggleItem(
    val key: AppSettingsToggleKey,
    val titleRes: Int,
    val subtitleRes: Int,
    val leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
)

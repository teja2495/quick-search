package com.tk.quicksearch.widgets.atAGlanceWidget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.AppNotificationFilters
import com.tk.quicksearch.settings.settingsDetailScreen.AtAGlanceSettingsSection
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.settings.shared.SettingsSegmentNavigationRow
import com.tk.quicksearch.shared.ui.components.SegmentedToggleTile
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.ProvideSettingsColors
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.shared.ui.components.AppBottomSheet
import com.tk.quicksearch.shared.ui.components.AppDrawerHeader
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.utils.WidgetPreferences

/**
 * The At a Glance widget's At a Glance Items row and its empty-date toggle, as two tiles. The row opens the
 * At a Glance toggles in a drawer, so they read as this widget's own rather than the app's settings page;
 * changes save with the widget.
 */
@Composable
fun AtAGlanceWidgetItemsSection(
    state: WidgetPreferences,
    onStateChange: (WidgetPreferences) -> Unit,
    searchViewModel: SearchViewModel,
) {
    var showDrawer by rememberSaveable { mutableStateOf(false) }
    SegmentedGroup {
        SettingsSegmentNavigationRow(
            item =
                SettingsCardItem(
                    title = stringResource(R.string.widget_at_a_glance_items_title),
                    description = stringResource(R.string.widget_at_a_glance_items_summary),
                    icon = MaterialSymbols.Today,
                    actionOnPress = { showDrawer = true },
                ),
            index = 0,
            count = 2,
        )
        // Whether the widget shows today's date or stays blank when there are no items.
        SegmentedToggleTile(
            title = stringResource(R.string.widget_toggle_show_empty_date),
            icon = MaterialSymbols.Event,
            checked = state.showEmptyDate,
            onCheckedChange = { onStateChange(state.copy(showEmptyDate = it)) },
            index = 1,
            count = 2,
        )
    }
    if (showDrawer) {
        AtAGlanceWidgetItemsDrawer(
            items = state.glanceItems,
            onItemsChange = { onStateChange(state.copy(glanceItems = it)) },
            appNotificationFilters = state.glanceAppNotificationFilters,
            onAppNotificationFiltersChange = { onStateChange(state.copy(glanceAppNotificationFilters = it)) },
            onDismissRequest = { showDrawer = false },
            searchViewModel = searchViewModel,
        )
    }
}

/** Drawn with the settings background and card colors, so it matches the app's At a Glance settings. */
@Composable
private fun AtAGlanceWidgetItemsDrawer(
    items: Map<String, Boolean>,
    onItemsChange: (Map<String, Boolean>) -> Unit,
    appNotificationFilters: AppNotificationFilters?,
    onAppNotificationFiltersChange: (AppNotificationFilters) -> Unit,
    onDismissRequest: () -> Unit,
    searchViewModel: SearchViewModel,
) {
    val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()
    ProvideSettingsColors(
        appTheme = uiState.appTheme,
        overlayThemeIntensity = uiState.overlayThemeIntensity,
        deviceThemeEnabled = uiState.deviceThemeEnabled,
        amoledThemeEnabled = uiState.amoledThemeEnabled,
    ) {
        AppBottomSheet(
            onDismissRequest = onDismissRequest,
            background = {
                SettingsScreenBackground(
                    appTheme = uiState.appTheme,
                    overlayThemeIntensity = uiState.overlayThemeIntensity,
                    deviceThemeEnabled = uiState.deviceThemeEnabled,
                    amoledThemeEnabled = uiState.amoledThemeEnabled,
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                ) {}
            },
        ) { dismiss ->
            AppDrawerHeader(
                onClose = dismiss,
                modifier = Modifier.padding(horizontal = DesignTokens.ContentHorizontalPadding),
            ) {
                Text(text = stringResource(R.string.widget_at_a_glance_items_title))
            }
            AtAGlanceSettingsSection(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = DesignTokens.ContentHorizontalPadding,
                            end = DesignTokens.ContentHorizontalPadding,
                            top = DesignTokens.SpacingSmall,
                            bottom = DesignTokens.SpacingLarge,
                        ),
                widgetItems = items,
                onWidgetItemsChange = onItemsChange,
                widgetAppNotificationFilters = appNotificationFilters,
                onWidgetAppNotificationFiltersChange = onAppNotificationFiltersChange,
            )
        }
    }
}

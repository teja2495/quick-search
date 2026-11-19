package com.tk.quicksearch.search.searchScreen.searchRoute

import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.search.appSettings.AppSettingsTarget
import com.tk.quicksearch.search.apps.appLock.AppLockAccessibilityDisclosureHost
import com.tk.quicksearch.search.apps.speedBump.SpeedBumpAccessibilityDisclosureHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.tk.quicksearch.pinnedNotifications.PinnedNotifications
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.settings.appearanceSettings.LauncherIconDrawer
import com.tk.quicksearch.settings.appearanceSettings.PinnedSectionsOrderDrawer
import com.tk.quicksearch.settings.aiSettings.AiApiKeyRequiredDialog
import com.tk.quicksearch.settings.settingsDetailScreen.AppSuggestionTabsDialog
import com.tk.quicksearch.settings.gestureSettings.GestureSettingsDialogs
import com.tk.quicksearch.settings.settingsDetailScreen.PinnedNotificationItemsDialog
import com.tk.quicksearch.settings.settingsDetailScreen.PriorityReorderDialog
import com.tk.quicksearch.settings.settingsDetailScreen.TOP_MATCHES_DRAWER_HEIGHT_FRACTION
import com.tk.quicksearch.settings.gestureSettings.rememberGestureSettingsState
import com.tk.quicksearch.settings.shared.SettingsCommand
import com.tk.quicksearch.settings.settingsScreen.AppLanguagePickerDialog
import com.tk.quicksearch.settings.settingsScreen.SettingsBackupFlow
import com.tk.quicksearch.settings.settingsScreen.SettingsBackupRequest
import com.tk.quicksearch.settings.shared.applySettingsCommand
import com.tk.quicksearch.shared.util.AppLanguageManager
import com.tk.quicksearch.tools.aiSearch.ModelPickerDialog

/** Settings dialogs opened directly from app-setting search results. */
internal enum class AppSettingRouteDialog {
    TOP_MATCHES_PRIORITY,
    APP_SUGGESTION_TABS,
    LAUNCHER_ICON,
    PINNED_SECTIONS_ORDER,
    PINNED_NOTIFICATION_ITEMS,
    API_KEY_REQUIRED,
    AI_MODEL,
    APP_LANGUAGE,
    EXPORT_SETTINGS,
    IMPORT_SETTINGS,
}

@Composable
internal fun AppSettingRouteDialogs(
    settingActions: RouteSettingActions,
    viewModel: SearchViewModel,
    uiState: SearchUiState,
) {
    val activeDialog = settingActions.activeDialog
    val onDismiss = { activeDialog.value = null }
    SpeedBumpAccessibilityDisclosureHost(settingActions.speedBumpToggle)
    AppLockAccessibilityDisclosureHost(settingActions.appLockToggle)
    when (activeDialog.value) {
        AppSettingRouteDialog.TOP_MATCHES_PRIORITY -> {
            val priorityItems =
                remember(uiState.topMatchesSectionOrder) {
                    uiState.topMatchesSectionOrder
                }
            PriorityReorderDialog(
                items = priorityItems,
                onItemsChange = { order ->
                    viewModel.applySettingsCommand(SettingsCommand.TopMatchesSectionOrder(order))
                },
                disabledSections = uiState.disabledTopMatchesSections,
                onItemEnabledChange = { section, enabled ->
                    viewModel.applySettingsCommand(SettingsCommand.TopMatchesSectionEnabled(section, enabled))
                },
                onDismiss = onDismiss,
                heightFraction = TOP_MATCHES_DRAWER_HEIGHT_FRACTION,
            )
        }
        AppSettingRouteDialog.APP_SUGGESTION_TABS ->
            AppSuggestionTabsDialog(
                enabledTabs = uiState.enabledAppSuggestionTabs,
                onTabEnabledChange = { tab, enabled ->
                    viewModel.applySettingsCommand(SettingsCommand.AppSuggestionTabEnabled(tab, enabled))
                },
                onDismiss = onDismiss,
            )
        AppSettingRouteDialog.LAUNCHER_ICON ->
            LauncherIconDrawer(
                selectedIcon = uiState.launcherAppIcon,
                onIconSelected = viewModel::setLauncherAppIcon,
                onDismissRequest = onDismiss,
            )
        AppSettingRouteDialog.PINNED_SECTIONS_ORDER ->
            PinnedSectionsOrderDrawer(
                homePinnedSectionOrder = uiState.homePinnedSectionOrder,
                pinnedAppShortcutsInAppGrid = uiState.pinnedAppShortcutsInAppGrid,
                onOrderChange = viewModel::setHomePinnedSectionOrder,
                onDismiss = onDismiss,
            )
        AppSettingRouteDialog.PINNED_NOTIFICATION_ITEMS -> {
            val context = LocalContext.current
            var items by remember(context) { mutableStateOf(PinnedNotifications.pinnedItems(context)) }
            PinnedNotificationItemsDialog(
                items = items,
                onRemove = { action ->
                    PinnedNotifications.remove(context, action)
                    items = items.filterNot { it.toJson() == action.toJson() }
                    if (items.isEmpty()) onDismiss()
                },
                onReorder = { reorderedItems ->
                    PinnedNotifications.reorder(context, reorderedItems)
                    items = reorderedItems
                },
                onDismiss = onDismiss,
            )
        }
        AppSettingRouteDialog.API_KEY_REQUIRED ->
            AiApiKeyRequiredDialog(
                onDismiss = onDismiss,
                onSetupKey = {
                    settingActions.onOpenAppSettingDestination(AppSettingsTarget.Page(SettingsDetailType.API_KEY_SETUP))
                },
            )
        AppSettingRouteDialog.AI_MODEL -> {
            val models =
                remember(uiState.activeLlmAvailableModels) {
                    uiState.activeLlmAvailableModels.distinctBy { it.id }.sortedBy { it.displayName.lowercase() }
                }
            ModelPickerDialog(
                selectedModelId = uiState.activeLlmModel,
                models = models,
                groundingEnabled = uiState.activeLlmGroundingEnabled,
                onGroundingChange = viewModel::setActiveLlmGroundingEnabled,
                onModelSelected = viewModel::setActiveLlmModel,
                onProviderModelSelected = viewModel::setLlmModel,
                onDismiss = onDismiss,
                showGroundingToggle = false,
                selectedProviderId = uiState.aiSearchLlmProviderId,
                modelsByProvider = uiState.availableLlmModelsByProvider,
                configuredProviderIds = uiState.llmApiKeyLast4ByProvider.keys,
            )
        }
        AppSettingRouteDialog.APP_LANGUAGE -> {
            val context = LocalContext.current
            val languageOptions = remember(context) { AppLanguageManager.getAvailableLanguages(context) }
            AppLanguagePickerDialog(
                selectedLanguageTag = AppLanguageManager.getSelectedLanguageTag(context),
                languageOptions = languageOptions,
                onDismiss = onDismiss,
                onLanguageSelected = { languageTag ->
                    onDismiss()
                    AppLanguageManager.setAppLanguage(context, languageTag)
                },
            )
        }
        AppSettingRouteDialog.EXPORT_SETTINGS,
        AppSettingRouteDialog.IMPORT_SETTINGS,
        null,
        -> Unit
    }

    // Composed outside the dialog switch so the import picker result survives dismissal.
    SettingsBackupFlow(
        request =
            when (activeDialog.value) {
                AppSettingRouteDialog.EXPORT_SETTINGS -> SettingsBackupRequest.EXPORT
                AppSettingRouteDialog.IMPORT_SETTINGS -> SettingsBackupRequest.IMPORT
                else -> null
            },
        onRequestHandled = onDismiss,
        onSettingsImported = viewModel::onSettingsImported,
    )

    settingActions.activeGestureDialog.value?.let { target ->
        // Keyed so tapping another gesture row starts a fresh dialog chain.
        key(target) {
            val gestureState = rememberGestureSettingsState(viewModel, initialTarget = target)
            GestureSettingsDialogs(gestureState, viewModel)
            LaunchedEffect(gestureState.isEditing) {
                if (!gestureState.isEditing) settingActions.activeGestureDialog.value = null
            }
        }
    }
}

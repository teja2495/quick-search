package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.aiSettings.AiProviderSettingsSection
import com.tk.quicksearch.settings.aiSettings.ApiKeySetupScreen
import com.tk.quicksearch.settings.gestureSettings.EdgeGestureSettingsSection
import com.tk.quicksearch.settings.gestureSettings.FloatingButtonSettingsSection
import com.tk.quicksearch.settings.gestureSettings.GesturesSettingsSection
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsCommand
import com.tk.quicksearch.settings.shared.SettingsSegmentNavigationRow
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenState
import com.tk.quicksearch.settings.toolsSettings.DateCalculatorInfoSection
import com.tk.quicksearch.settings.toolsSettings.UnitConverterInfoSection
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.aiSearch.supportsThinkingControl
import kotlinx.coroutines.launch

/** Content of the nested settings pages that scroll in the shared settings column. */
@Composable
internal fun ColumnScope.SettingsNestedDetailBody(
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    detailType: SettingsDetailType,
    scrollState: androidx.compose.foundation.ScrollState,
    atAGlanceSearchQuery: String,
    onNavigateToDetail: (SettingsDetailType) -> Unit,
    onNicknameRemoved: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    when (detailType) {
        SettingsDetailType.EXCLUDED_ITEMS -> {
            ExcludedItemScreen(
                modifier = Modifier.fillMaxWidth().weight(1f),
                suggestionExcludedApps = state.suggestionExcludedApps,
                resultExcludedApps = state.resultExcludedApps,
                excludedContacts = state.excludedContacts,
                excludedFiles = state.excludedFiles,
                excludedFileExtensions = state.excludedFileExtensions,
                excludedSettings = state.excludedSettings,
                excludedAppShortcuts = state.excludedAppShortcuts,
                onRemoveSuggestionExcludedApp = callbacks.onRemoveSuggestionExcludedApp,
                onRemoveResultExcludedApp = callbacks.onRemoveResultExcludedApp,
                onRemoveExcludedContact = callbacks.onRemoveExcludedContact,
                onRemoveExcludedFile = callbacks.onRemoveExcludedFile,
                onRemoveExcludedFileExtension = callbacks.onRemoveExcludedFileExtension,
                onRemoveExcludedSetting = callbacks.onRemoveExcludedSetting,
                onRemoveExcludedAppShortcut = callbacks.onRemoveExcludedAppShortcut,
                excludedOtherItems = state.excludedOtherItems,
                onRemoveExcludedOtherItem = callbacks.onRemoveExcludedOtherItem,
                showTitle = false,
                iconPackPackage = state.selectedIconPackPackage,
            )
        }

        SettingsDetailType.APP_SHORTCUTS -> Unit
        SettingsDetailType.DEVICE_SETTINGS -> Unit

        SettingsDetailType.CALLS_TEXTS -> {
            CallsTextsSettingsSection(
                primaryContactButton = state.primaryContactButton,
                secondaryContactButton = state.secondaryContactButton,
                onSetPrimaryContactButton = callbacks.onSetPrimaryContactButton,
                onSetSecondaryContactButton = callbacks.onSetSecondaryContactButton,
                directDialEnabled = state.directDialEnabled,
                onToggleDirectDial = callbacks.onToggleDirectDial,
                numberSearchEnabled = state.numberSearchEnabled,
                onToggleNumberSearch = callbacks.onToggleNumberSearch,
                hasCallPermission = PermissionHelper.checkCallPermission(context),
                contactsSectionEnabled = true,
                installedContactProviders = state.installedContactProviders,
                modifier = Modifier,
            )
        }

        SettingsDetailType.FILES -> {
            FileTypesSection(
                enabledFileTypes = state.enabledFileTypes,
                onToggleFileType = callbacks.onToggleFileType,
                showFolders = state.showFolders,
                onToggleFolders = { enabled ->
                    callbacks.onApplySettingsCommand(
                        SettingsCommand.Toggle(
                            key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.SHOW_FOLDERS,
                            enabled = enabled,
                        ),
                    )
                },
                booleanSettings = state.booleanSettings,
                onApplySettingsCommand = callbacks.onApplySettingsCommand,
                showSystemFiles = state.showSystemFiles,
                onToggleSystemFiles = { enabled ->
                    callbacks.onApplySettingsCommand(
                        SettingsCommand.Toggle(
                            key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.SHOW_SYSTEM_FILES,
                            enabled = enabled,
                        ),
                    )
                },
                folderWhitelistPatterns = state.folderWhitelistPatterns,
                onSetFolderWhitelistPatterns = callbacks.onSetFolderWhitelistPatterns,
                folderBlacklistPatterns = state.folderBlacklistPatterns,
                onSetFolderBlacklistPatterns = callbacks.onSetFolderBlacklistPatterns,
                excludedExtensions = state.excludedFileExtensions,
                onRemoveExcludedExtension = callbacks.onRemoveExcludedFileExtension,
                showTitle = false,
                modifier = Modifier,
            )
        }

        SettingsDetailType.GEMINI_API_CONFIG -> {
            SegmentedGroup(
                modifier = Modifier.padding(bottom = DesignTokens.SpacingLarge),
            ) {
                SettingsSegmentNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(R.string.settings_configure_api_key_title),
                            description = stringResource(R.string.settings_api_key_setup_nav_desc),
                            icon = MaterialSymbols.Key,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.API_KEY_SETUP)
                            },
                        ),
                    index = 0,
                    count = 1,
                )
            }

            if (state.hasApiKey) {
                AiProviderSettingsSection(
                    personalContext = state.personalContext,
                    aiSearchLlmProviderId = state.aiSearchLlmProviderId,
                    activeLlmModel = state.activeLlmModel,
                    activeLlmGroundingEnabled = state.activeLlmGroundingEnabled,
                    activeLlmThinkingEnabled = state.activeLlmThinkingEnabled,
                    activeLlmAvailableModels = state.activeLlmAvailableModels,
                    availableLlmModelsByProvider = state.availableLlmModelsByProvider,
                    apiKeyLast4ByProvider = state.llmApiKeyLast4ByProvider,
                    customAdvancedPayloadByProvider =
                        state.customLlmAdvancedPayloadByProvider,
                    onSetPersonalContext = callbacks.onSetPersonalContext,
                    onSetActiveLlmModel = callbacks.onSetActiveLlmModel,
                    onSetLlmModel = callbacks.onSetLlmModel,
                    onSetCustomAdvancedPayload =
                        callbacks.onSetCustomLlmAdvancedPayload,
                    onSetActiveLlmGroundingEnabled = callbacks.onSetActiveLlmGroundingEnabled,
                    onSetActiveLlmThinkingEnabled = callbacks.onSetActiveLlmThinkingEnabled,
                    onRefreshAvailableLlmModels = callbacks.onRefreshAvailableLlmModels,
                    showThinkingCheckbox =
                        supportsThinkingControl(
                            state.aiSearchLlmProviderId,
                            state.activeLlmModel,
                        ),
                    onRequestScrollToBottom = {
                        coroutineScope.launch {
                            scrollState.scrollTo(scrollState.maxValue)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SettingsDetailType.API_KEY_SETUP -> {
            ApiKeySetupScreen(
                apiKeyLast4ByProvider = state.llmApiKeyLast4ByProvider,
                customProviderBaseUrlByProvider = state.customLlmBaseUrlByProvider,
                isSavingApiKey = state.isSavingLlmApiKey,
                onSetApiKey = callbacks.onSetLlmApiKey,
                onAddCustomProvider = callbacks.onAddCustomLlmProvider,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SettingsDetailType.TOOLS -> Unit

        SettingsDetailType.NICKNAMES -> {
            NicknameItemsScreen(
                modifier = Modifier.fillMaxWidth(),
                onAfterRemove = onNicknameRemoved,
            )
        }

        SettingsDetailType.TRIGGERS -> {
            TriggerItemsScreen(modifier = Modifier.fillMaxWidth().weight(1f))
        }

        SettingsDetailType.GESTURES -> {
            GesturesSettingsSection(modifier = Modifier.fillMaxWidth())
        }

        SettingsDetailType.AT_A_GLANCE -> {
            AtAGlanceSettingsSection(modifier = Modifier.fillMaxWidth().weight(1f), searchQuery = atAGlanceSearchQuery)
        }

        SettingsDetailType.EDGE_GESTURE -> {
            EdgeGestureSettingsSection(modifier = Modifier.fillMaxWidth())
        }

        SettingsDetailType.FLOATING_BUTTON -> {
            FloatingButtonSettingsSection(modifier = Modifier.fillMaxWidth())
        }

        SettingsDetailType.UNIT_CONVERTER_INFO -> {
            UnitConverterInfoSection(modifier = Modifier.fillMaxWidth())
        }

        SettingsDetailType.DATE_CALCULATOR_INFO -> {
            DateCalculatorInfoSection(modifier = Modifier.fillMaxWidth())
        }

        else -> Unit
    }
}

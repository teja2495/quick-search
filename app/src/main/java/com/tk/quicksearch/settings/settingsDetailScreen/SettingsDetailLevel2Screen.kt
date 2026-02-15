package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.settings.shared.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsScreenState
import com.tk.quicksearch.ui.theme.DesignTokens
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
internal fun SettingsDetailLevel2Screen(
    modifier: Modifier = Modifier,
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    detailType: SettingsDetailType,
    appShortcutFocusShortcut: StaticShortcut? = null,
    appShortcutFocusPackageName: String? = null,
    onAppShortcutFocusHandled: () -> Unit = {},
) {
    if (!detailType.isLevel2()) return

    BackHandler(onBack = callbacks.onBack)
    val scrollState = rememberScrollState()
    var showClearAllConfirmation by remember { mutableStateOf(false) }
    var isScrollingDown by remember { mutableStateOf(false) }
    var lastScrollOffset by remember { mutableIntStateOf(0) }
    val scrollDeltaThreshold = 6

    val hasExcludedItems =
        state.suggestionExcludedApps.isNotEmpty() ||
            state.resultExcludedApps.isNotEmpty() ||
            state.excludedContacts.isNotEmpty() ||
            state.excludedFiles.isNotEmpty() ||
            state.excludedFileExtensions.isNotEmpty() ||
            state.excludedSettings.isNotEmpty() ||
            state.excludedAppShortcuts.isNotEmpty()

    LaunchedEffect(detailType, hasExcludedItems) {
        if (detailType == SettingsDetailType.EXCLUDED_ITEMS && !hasExcludedItems) {
            callbacks.onBack()
        }
    }

    LaunchedEffect(detailType, scrollState) {
        if (detailType != SettingsDetailType.APP_SHORTCUTS) return@LaunchedEffect
        snapshotFlow { scrollState.value }
            .map { offset ->
                val delta = offset - lastScrollOffset
                val direction = when {
                    delta > scrollDeltaThreshold -> true
                    delta < -scrollDeltaThreshold -> false
                    else -> isScrollingDown
                }
                lastScrollOffset = offset
                direction
            }.distinctUntilChanged()
            .collectLatest { down ->
                isScrollingDown = down
            }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailHeader(
                title = stringResource(detailType.titleResId()),
                onBack = callbacks.onBack,
            )

            if (detailType == SettingsDetailType.APP_MANAGEMENT) {
                AppManagementSettingsSection(
                    apps = state.allApps,
                    iconPackPackage = state.selectedIconPackPackage,
                    onRequestAppUninstall = callbacks.onRequestAppUninstall,
                    onOpenAppInfo = callbacks.onOpenAppInfo,
                    onRefreshApps = callbacks.onRefreshApps,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                start = DesignTokens.ContentHorizontalPadding,
                                end = DesignTokens.ContentHorizontalPadding,
                                bottom = DesignTokens.SectionTopPadding,
                            ),
                )
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(
                                start = DesignTokens.ContentHorizontalPadding,
                                end = DesignTokens.ContentHorizontalPadding,
                                bottom =
                                    if (detailType == SettingsDetailType.APP_SHORTCUTS) {
                                        96.dp
                                    } else {
                                        DesignTokens.SectionTopPadding
                                    },
                            ),
                ) {
                    when (detailType) {
                        SettingsDetailType.EXCLUDED_ITEMS -> {
                            ExcludedItemScreen(
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
                                showTitle = false,
                                iconPackPackage = state.selectedIconPackPackage,
                            )
                        }

                        SettingsDetailType.APP_SHORTCUTS -> {
                            AppShortcutsSettingsSection(
                                shortcuts = state.allAppShortcuts,
                                disabledShortcutIds = state.disabledAppShortcutIds,
                                iconPackPackage = state.selectedIconPackPackage,
                                onShortcutEnabledChange = callbacks.onToggleAppShortcutEnabled,
                                onShortcutNameClick = callbacks.onLaunchAppShortcut,
                                onDeleteCustomShortcut = callbacks.onDeleteCustomAppShortcut,
                                focusShortcut = appShortcutFocusShortcut,
                                focusPackageName = appShortcutFocusPackageName,
                                onFocusHandled = onAppShortcutFocusHandled,
                            )
                        }

                        SettingsDetailType.DEVICE_SETTINGS -> {
                            DeviceSettingsSettingsSection(
                                settings = state.allDeviceSettings,
                                onSettingClick = callbacks.onLaunchDeviceSetting,
                            )
                        }

                        SettingsDetailType.DIRECT_SEARCH_CONFIGURE -> {
                            DirectSearchConfigureSettingsSection(
                                personalContext = state.personalContext,
                                geminiModel = state.geminiModel,
                                geminiGroundingEnabled = state.geminiGroundingEnabled,
                                availableGeminiModels = state.availableGeminiModels,
                                onSetPersonalContext = callbacks.onSetPersonalContext,
                                onSetGeminiModel = callbacks.onSetGeminiModel,
                                onSetGeminiGroundingEnabled = callbacks.onSetGeminiGroundingEnabled,
                                onRefreshAvailableGeminiModels = callbacks.onRefreshAvailableGeminiModels,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        else -> Unit
                    }
                }
            }
        }

        if (detailType == SettingsDetailType.EXCLUDED_ITEMS) {
            FloatingActionButton(
                onClick = { showClearAllConfirmation = true },
                modifier =
                    Modifier
                        .align(androidx.compose.ui.Alignment.BottomEnd)
                        .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.settings_action_clear_all),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        if (detailType == SettingsDetailType.APP_SHORTCUTS) {
            ExtendedFloatingActionButton(
                onClick = callbacks.onOpenAddAppShortcutDialog,
                modifier =
                    Modifier
                        .align(androidx.compose.ui.Alignment.BottomEnd)
                        .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                expanded = !isScrollingDown,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                    )
                },
                text = { Text(text = stringResource(R.string.settings_app_shortcuts_add_button)) },
            )
        }

        if (showClearAllConfirmation && detailType == SettingsDetailType.EXCLUDED_ITEMS) {
            ClearAllConfirmationDialog(
                onConfirm = {
                    callbacks.onClearAllExclusions()
                    showClearAllConfirmation = false
                },
                onDismiss = { showClearAllConfirmation = false },
            )
        }
    }
}

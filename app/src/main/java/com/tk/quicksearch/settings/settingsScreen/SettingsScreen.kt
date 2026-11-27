package com.tk.quicksearch.settings.settingsScreen

import androidx.compose.runtime.collectAsState
import com.tk.quicksearch.settings.autoBackup.AutoBackupStore
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.rememberResumeRefreshKey
import com.tk.quicksearch.settings.shared.settingsScrollClip
import com.tk.quicksearch.search.searchScreen.LockScreenAccessibilityService
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenCallbacks as SharedSettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenState as SharedSettingsScreenState
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.verticalScroll
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.app.navigation.PageBackHandler
import com.tk.quicksearch.search.searchScreen.dialogs.ReleaseNotesDrawer
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.shared.permissions.LockScreenAccessibilityDisclosureDialog
import com.tk.quicksearch.shared.ui.components.TipBanner
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.AppLanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedToggleTile
import com.tk.quicksearch.shared.ui.components.AppTextButton

/**
 * Retrieves the app version name from the package manager.
 */
@Composable
fun getAppVersionName(): String? {
    val context = LocalContext.current
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        null
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    state: SharedSettingsScreenState,
    callbacks: SharedSettingsScreenCallbacks,
    isOverlayModeBlockedByLauncher: Boolean,
    shouldShowSettingsSearchTip: Boolean,
    onDismissSettingsSearchTip: () -> Unit,
    onNavigateToDetail: (SettingsDetailType) -> Unit,
    showReleaseNotesDialog: Boolean,
    releaseNotesVersionName: String?,
    onOpenReleaseNotes: () -> Unit,
    onReleaseNotesAcknowledged: () -> Unit,
    showAccessibilityPermissionDisclaimer: Boolean,
    onAccessibilityPermissionDisclaimerDismissed: () -> Unit,
    onSettingsImported: () -> Unit = {},
    pendingImportUri: String? = null,
    onPendingImportUriConsumed: () -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState =
        androidx.compose.foundation.rememberScrollState(),
) {
    PageBackHandler(onBack = callbacks.onBack)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val availableLanguages = remember(context) { AppLanguageManager.getAvailableLanguages(context) }
    val selectedLanguageLabel = AppLanguageManager.getSelectedLanguageLabel(context)
    val selectedLanguageTag = AppLanguageManager.getSelectedLanguageTag(context)
    var showImportWarningDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var pendingImportSourceUri by remember { mutableStateOf<Uri?>(null) }
    var showExportSelectionDialog by remember { mutableStateOf(false) }
    var exportSelectionState by remember { mutableStateOf(ExportSelectionState()) }
    val autoBackup by AutoBackupStore.state.collectAsState()
    val resumeRefreshKey = rememberResumeRefreshKey()
    // A backup can finish while Settings is in the background.
    LaunchedEffect(resumeRefreshKey) {
        withContext(Dispatchers.IO) { AutoBackupStore.refresh(context) }
    }
    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            importSettingsFromUri(
                context = context,
                uri = uri,
                onSuccess = onSettingsImported,
                coroutineScope = coroutineScope,
            )
        }

    LaunchedEffect(pendingImportUri) {
        val incomingUri = pendingImportUri ?: return@LaunchedEffect
        pendingImportSourceUri = Uri.parse(incomingUri)
        showImportWarningDialog = true
        onPendingImportUriConsumed()
    }

    SettingsScreenBackground(
        appTheme = state.appTheme,
        overlayThemeIntensity = state.overlayThemeIntensity,
        deviceThemeEnabled = state.deviceThemeEnabled,
        amoledThemeEnabled = state.amoledThemeEnabled,
        modifier = modifier,
    ) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
    ) {
        SettingsHeader(onBack = callbacks.onBack)

        Column(
            modifier =
                Modifier
                    .settingsContentWidth()
                    .fillMaxHeight()
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = DesignTokens.ContentHorizontalPadding)
                    .settingsScrollClip(scrollState)
                    .verticalScroll(scrollState),
        ) {
            if (shouldShowSettingsSearchTip) {
                TipBanner(
                    modifier = Modifier.padding(bottom = DesignTokens.SectionTopPadding),
                    text = stringResource(R.string.settings_search_tip_banner_message),
                    onDismiss = onDismissSettingsSearchTip,
                )
            }

            val overlayModeSubtitleRes =
                if (isOverlayModeBlockedByLauncher) {
                    R.string.settings_overlay_mode_desc_launcher_blocked
                } else {
                    R.string.settings_overlay_mode_desc
                }
            SegmentedGroup(modifier = Modifier.padding(bottom = DesignTokens.SectionTopPadding)) {
                SegmentedToggleTile(
                    title = stringResource(R.string.settings_overlay_mode_title),
                    description = stringResource(overlayModeSubtitleRes),
                    icon = MaterialSymbols.Layers,
                    checked = state.overlayModeEnabled,
                    enabled = !isOverlayModeBlockedByLauncher,
                    onCheckedChange = callbacks.onToggleOverlayMode,
                    index = 0,
                    count = 1,
                )
            }

            if (state.overlayModeEnabled && !state.hasSeenOverlayAssistantTip) {
                val tipText = stringResource(R.string.settings_overlay_assistant_tip)
                val setupNow = stringResource(R.string.settings_overlay_assistant_setup_now)
                val fullText = tipText + " " + setupNow
                val annotatedText =
                    buildAnnotatedString {
                        append(fullText)
                        val startIndex = fullText.indexOf(setupNow)
                        if (startIndex >= 0) {
                            val endIndex = startIndex + setupNow.length
                            addStyle(
                                style =
                                    SpanStyle(
                                        color = AppColors.LinkColor,
                                        textDecoration = TextDecoration.Underline,
                                    ),
                                start = startIndex,
                                end = endIndex,
                            )
                        }
                    }
                TipBanner(
                    modifier = Modifier.padding(bottom = DesignTokens.SectionTopPadding),
                    annotatedText = annotatedText,
                    onContentClick = { onNavigateToDetail(SettingsDetailType.LAUNCH_OPTIONS) },
                    onDismiss = callbacks.onDismissOverlayAssistantTip,
                )
            }

            val navigationItems =
                buildList {
                    add(
                        SettingsCardItem(
                            title = stringResource(R.string.settings_appearance_title),
                            description = stringResource(R.string.settings_appearance_desc),
                            icon = MaterialSymbols.Palette,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.APPEARANCE)
                            },
                        ),
                    )
                    add(
                        SettingsCardItem(
                            title = stringResource(R.string.settings_at_a_glance_title),
                            description = stringResource(R.string.settings_at_a_glance_desc),
                            icon = MaterialSymbols.Today,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.AT_A_GLANCE)
                            },
                        ),
                    )
                    add(
                        SettingsCardItem(
                            title = stringResource(R.string.settings_search_results_title),
                            description = stringResource(R.string.settings_search_results_desc),
                            icon = MaterialSymbols.Search,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.SEARCH_RESULTS)
                            },
                        ),
                    )
                    add(
                        SettingsCardItem(
                            title = stringResource(R.string.settings_app_shortcuts_filter_search_engines),
                            description = stringResource(R.string.settings_search_engines_desc),
                            icon = MaterialSymbols.ManageSearch,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.SEARCH_ENGINES)
                            },
                        ),
                    )
                    add(
                        SettingsCardItem(
                            title = stringResource(R.string.settings_tools_title),
                            description = stringResource(R.string.settings_tools_desc),
                            icon = MaterialSymbols.Build,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.TOOLS)
                            },
                        ),
                    )
                    add(
                        SettingsCardItem(
                            title = stringResource(R.string.common_ai_provider),
                            description = stringResource(R.string.settings_gemini_api_config_desc),
                            iconResId = R.drawable.direct_search,
                            actionOnPress = {
                                onNavigateToDetail(
                                    if (state.hasApiKey) {
                                        SettingsDetailType.GEMINI_API_CONFIG
                                    } else {
                                        SettingsDetailType.API_KEY_SETUP
                                    },
                                )
                            },
                        ),
                    )
                    add(
                        SettingsCardItem(
                            title = stringResource(R.string.settings_gestures_title),
                            description = stringResource(R.string.settings_gestures_desc),
                            icon = MaterialSymbols.Swipe,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.GESTURES)
                            },
                        ),
                    )
                    add(
                        SettingsCardItem(
                            title = stringResource(R.string.settings_more_options_title),
                            description = stringResource(R.string.settings_more_options_desc),
                            icon = MaterialSymbols.Tune,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.MORE_OPTIONS)
                            },
                        ),
                    )
                }

            SegmentedGroup(modifier = Modifier.padding(bottom = DesignTokens.SectionTopPadding)) {
                navigationItems.forEachIndexed { index, item ->
                    SettingsSegmentNavigationRow(item = item, index = index, count = navigationItems.size)
                }
            }

            val systemItems =
                listOf(
                    SettingsCardItem(
                        title = stringResource(R.string.settings_launch_options_title),
                        description = stringResource(R.string.settings_launch_options_desc),
                        icon = MaterialSymbols.RocketLaunch,
                        actionOnPress = {
                            onNavigateToDetail(SettingsDetailType.LAUNCH_OPTIONS)
                        },
                    ),
                    SettingsCardItem(
                        title = stringResource(R.string.settings_permissions_title),
                        description = stringResource(R.string.settings_permissions_desc),
                        icon = MaterialSymbols.AdminPanelSettings,
                        actionOnPress = {
                            onNavigateToDetail(SettingsDetailType.PERMISSIONS)
                        },
                    ),
                    SettingsCardItem(
                        title = stringResource(R.string.settings_app_language_title),
                        description =
                            stringResource(
                                R.string.settings_app_language_desc,
                                selectedLanguageLabel,
                            ),
                        icon = MaterialSymbols.Translate,
                        actionOnPress = {
                            showLanguageDialog = true
                        },
                    ),
                )
            // The backup tile closes this group.
            val systemCount = systemItems.size + 1
            SegmentedGroup(modifier = Modifier.padding(bottom = DesignTokens.SectionTopPadding)) {
                systemItems.forEachIndexed { index, item ->
                    SettingsSegmentNavigationRow(item = item, index = index, count = systemCount)
                }
                BackupRestoreRow(
                    index = systemCount - 1,
                    count = systemCount,
                    onImportClick = {
                        showImportWarningDialog = true
                    },
                    autoBackup = autoBackup,
                    onExportClick = {
                        coroutineScope.launch {
                            // API keys live in encrypted storage; keep keystore work off the main thread.
                            exportSelectionState =
                                withContext(Dispatchers.IO) { loadExportSelectionState(context) }
                            showExportSelectionDialog = true
                        }
                    },
                )
            }

            SettingsMoreOptions(
                onOpenReleaseNotes = onOpenReleaseNotes,
                onOpenFeaturesList = { onNavigateToDetail(SettingsDetailType.FEATURES_LIST) },
                onOpenOssLicenses = { onNavigateToDetail(SettingsDetailType.OPEN_SOURCE_LICENSES) },
            )

            SettingsVersionDisplay(
                modifier = Modifier.padding(top = DesignTokens.SectionTopPadding, bottom = DesignTokens.Spacing48 + DesignTokens.SpacingLarge),
            )
        }
    }
    }

    if (showReleaseNotesDialog) {
        ReleaseNotesDrawer(
            versionName = releaseNotesVersionName,
            onAcknowledge = onReleaseNotesAcknowledged,
            onViewAllFeatures = {
                onReleaseNotesAcknowledged()
                onNavigateToDetail(SettingsDetailType.FEATURES_LIST)
            },
        )
    }

    if (showAccessibilityPermissionDisclaimer) {
        LockScreenAccessibilityDisclosureDialog(
            onAgree = {
                onAccessibilityPermissionDisclaimerDismissed()
                LockScreenAccessibilityService.openAccessibilitySettings(context)
            },
            onDismiss = onAccessibilityPermissionDisclaimerDismissed,
        )
    }

    if (showImportWarningDialog) {
        AppAlertDialog(
            onDismissRequest = {
                showImportWarningDialog = false
            },
            title = {
                Text(text = stringResource(R.string.settings_backup_import_warning_title))
            },
            text = {
                Text(text = stringResource(R.string.settings_backup_import_warning_message))
            },
            confirmButton = {
                AppTextButton(
                    onClick = {
                        showImportWarningDialog = false
                        val externalUri = pendingImportSourceUri
                        pendingImportSourceUri = null
                        if (externalUri != null) {
                            importSettingsFromUri(
                                context = context,
                                uri = externalUri,
                                onSuccess = onSettingsImported,
                                coroutineScope = coroutineScope,
                            )
                        } else {
                            importLauncher.launch(arrayOf("*/*"))
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                AppTextButton(
                    onClick = {
                        showImportWarningDialog = false
                        pendingImportSourceUri = null
                    },
                ) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    if (showExportSelectionDialog) {
        SettingsExportDialog(
            selectionState = exportSelectionState,
            onSelectionStateChange = { exportSelectionState = it },
            onDismiss = {
                showExportSelectionDialog = false
            },
            onExport = {
                showExportSelectionDialog = false
                exportSettingsToDownloads(context, exportSelectionState, coroutineScope)
            },
        )
    }

    if (showLanguageDialog) {
        AppLanguagePickerDialog(
            selectedLanguageTag = selectedLanguageTag,
            languageOptions = availableLanguages,
            onDismiss = {
                showLanguageDialog = false
            },
            onLanguageSelected = { languageTag ->
                showLanguageDialog = false
                AppLanguageManager.setAppLanguage(context, languageTag)
            },
        )
    }
}

package com.tk.quicksearch.settings.settingsScreen

import android.content.ActivityNotFoundException
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenCallbacks as SharedSettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenState as SharedSettingsScreenState
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Upload
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.ButtonDefaults
import com.tk.quicksearch.settings.shared.SettingsCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.dialogs.ReleaseNotesDrawer
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.shared.featureFlags.FeatureFlag
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.permissions.LockScreenAccessibilityDisclosureDialog
import com.tk.quicksearch.shared.ui.components.TipBanner
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.AppLanguageManager
import com.tk.quicksearch.shared.util.AppLanguageOption
import com.tk.quicksearch.shared.util.FeedbackUtils
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    hasUsagePermission: Boolean,
    hasContactPermission: Boolean,
    hasFilePermission: Boolean,
    hasCallPermission: Boolean,
    shouldShowBanner: Boolean,
    onRequestUsagePermission: () -> Unit,
    onRequestContactPermission: () -> Unit,
    onRequestFilePermission: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onRequestCallPermission: () -> Unit,
    onDismissBanner: () -> Unit,
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
    BackHandler(onBack = callbacks.onBack)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    FeatureFlags.initialize(context)
    val availableLanguages = remember(context) { AppLanguageManager.getAvailableLanguages(context) }
    val selectedLanguageLabel = AppLanguageManager.getSelectedLanguageLabel(context)
    val selectedLanguageTag = AppLanguageManager.getSelectedLanguageTag(context)
    var showImportWarningDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var pendingImportSourceUri by remember { mutableStateOf<Uri?>(null) }
    var showExportSelectionDialog by remember { mutableStateOf(false) }
    var exportSelectionState by remember { mutableStateOf(ExportSelectionState()) }
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

        // Scrollable Content
        Column(
            modifier =
                Modifier
                    .settingsContentWidth()
                    .fillMaxHeight()
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(scrollState)
                    .padding(horizontal = DesignTokens.ContentHorizontalPadding),
        ) {
            if (shouldShowSettingsSearchTip) {
                TipBanner(
                    modifier = Modifier.padding(bottom = DesignTokens.SectionTopPadding),
                    text = stringResource(R.string.settings_search_tip_banner_message),
                    onDismiss = onDismissSettingsSearchTip,
                )
            }

            // Overlay Mode Card (top)
            val isOverlayModeBlockedByLauncher = context.isDefaultHomeApp()
            SettingsCard(
                modifier =
                    Modifier
                        .alpha(if (isOverlayModeBlockedByLauncher) 0.72f else 1f)
                        .fillMaxWidth()
                        .padding(bottom = DesignTokens.SectionTopPadding),
            ) {
                val overlayModeSubtitleRes =
                    if (isOverlayModeBlockedByLauncher) {
                        R.string.settings_overlay_mode_desc_launcher_blocked
                    } else {
                        R.string.settings_overlay_mode_desc
                    }
                Column {
                    SettingsToggleRow(
                        title = stringResource(R.string.settings_overlay_mode_title),
                        subtitleContent = {
                            Text(
                                text = stringResource(overlayModeSubtitleRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        checked = state.overlayModeEnabled,
                        enabled = !isOverlayModeBlockedByLauncher,
                        onCheckedChange = callbacks.onToggleOverlayMode,
                        leadingIcon = Icons.Rounded.Layers,
                        isFirstItem = true,
                        isLastItem = true,
                        showDivider = false,
                    )
                }
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

            // Search Results and Search Engines Card
            val navigationItems =
                buildList {
                    add(
                        SettingsCardItem(
                            title = stringResource(R.string.settings_appearance_title),
                            description = stringResource(R.string.settings_appearance_desc),
                            icon = Icons.Rounded.Palette,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.APPEARANCE)
                            },
                        ),
                    )
                    add(
                        SettingsCardItem(
                            title = stringResource(R.string.settings_search_results_title),
                            description = stringResource(R.string.settings_search_results_desc),
                            icon = Icons.Rounded.Search,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.SEARCH_RESULTS)
                            },
                        ),
                    )
                    add(
                        SettingsCardItem(
                            title = stringResource(R.string.settings_app_shortcuts_filter_search_engines),
                            description = stringResource(R.string.settings_search_engines_desc),
                            icon = Icons.AutoMirrored.Rounded.ManageSearch,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.SEARCH_ENGINES)
                            },
                        ),
                    )
                    add(
                        SettingsCardItem(
                            title = stringResource(R.string.settings_tools_title),
                            description = stringResource(R.string.settings_tools_desc),
                            icon = Icons.Rounded.Build,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.TOOLS)
                            },
                        ),
                    )
                    add(
                        SettingsCardItem(
                            title = stringResource(R.string.settings_at_a_glance_title),
                            description = stringResource(R.string.settings_at_a_glance_desc),
                            icon = Icons.Rounded.Today,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.AT_A_GLANCE)
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
                            icon = Icons.Rounded.Swipe,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.GESTURES)
                            },
                        ),
                    )
                    add(
                        SettingsCardItem(
                            title = stringResource(R.string.settings_more_options_title),
                            description = stringResource(R.string.settings_more_options_desc),
                            icon = Icons.Rounded.Tune,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.MORE_OPTIONS)
                            },
                        ),
                    )
                }

            SettingsCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = DesignTokens.SectionTopPadding),
            ) {
                Column {
                    navigationItems.forEachIndexed { index, item ->
                        SettingsNavigationRow(
                            item = item,
                            contentPadding =
                                PaddingValues(
                                    horizontal = DesignTokens.SpacingXXLarge,
                                    vertical = DesignTokens.SpacingLarge,
                                ),
                        )

                        if (index < navigationItems.lastIndex) {
                            HorizontalDivider(
                                color = AppColors.SettingsDivider,
                            )
                        }
                    }
                }
            }

            SettingsCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = DesignTokens.SectionTopPadding),
            ) {
                Column {
                    SettingsNavigationRow(
                        item =
                            SettingsCardItem(
                                title = stringResource(R.string.settings_launch_options_title),
                                description = stringResource(R.string.settings_launch_options_desc),
                                icon = Icons.Rounded.RocketLaunch,
                                actionOnPress = {
                                    onNavigateToDetail(SettingsDetailType.LAUNCH_OPTIONS)
                                },
                            ),
                        contentPadding =
                            PaddingValues(
                                horizontal = DesignTokens.SpacingXXLarge,
                                vertical = DesignTokens.SpacingLarge,
                            ),
                    )

                    HorizontalDivider(
                        color = AppColors.SettingsDivider,
                    )

                    SettingsNavigationRow(
                        item =
                            SettingsCardItem(
                                title = stringResource(R.string.settings_permissions_title),
                                description = stringResource(R.string.settings_permissions_desc),
                                icon = Icons.Rounded.AdminPanelSettings,
                                actionOnPress = {
                                    onNavigateToDetail(SettingsDetailType.PERMISSIONS)
                                },
                            ),
                        contentPadding =
                            PaddingValues(
                                horizontal = DesignTokens.SpacingXXLarge,
                                vertical = DesignTokens.SpacingLarge,
                            ),
                    )

                    HorizontalDivider(
                        color = AppColors.SettingsDivider,
                    )

                    SettingsNavigationRow(
                        item =
                            SettingsCardItem(
                                title = stringResource(R.string.settings_app_language_title),
                                description =
                                    stringResource(
                                        R.string.settings_app_language_desc,
                                        selectedLanguageLabel,
                                    ),
                                icon = Icons.Rounded.Translate,
                                actionOnPress = {
                                    showLanguageDialog = true
                                },
                            ),
                        contentPadding =
                            PaddingValues(
                                horizontal = DesignTokens.SpacingXXLarge,
                                vertical = DesignTokens.SpacingLarge,
                            ),
                    )

                    HorizontalDivider(
                        color = AppColors.SettingsDivider,
                    )

                    BackupRestoreRow(
                        onImportClick = {
                            showImportWarningDialog = true
                        },
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
            }

            // More Options Section
            SettingsMoreOptions(
                onOpenReleaseNotes = onOpenReleaseNotes,
                onOpenFeaturesList = { onNavigateToDetail(SettingsDetailType.FEATURES_LIST) },
                onOpenOssLicenses = { onNavigateToDetail(SettingsDetailType.OPEN_SOURCE_LICENSES) },
            )

            // App Version
            SettingsVersionDisplay(
                modifier = Modifier.padding(top = DesignTokens.Spacing40, bottom = 60.dp),
                onFeatureFlagsChanged = onSettingsImported,
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
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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
                TextButton(
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
                TextButton(
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

package com.tk.quicksearch.settings.settingsScreen

import android.content.ActivityNotFoundException
import com.tk.quicksearch.settings.shared.SettingsScreenCallbacks as SharedSettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsScreenState as SharedSettingsScreenState
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.ButtonDefaults
import com.tk.quicksearch.settings.shared.SettingsCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.BasePreferences
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.shared.featureFlags.FeatureFlag
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.ui.components.TipBanner
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.FeedbackUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    onSettingsImported: () -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState =
        androidx.compose.foundation.rememberScrollState(),
) {
    BackHandler(onBack = callbacks.onBack)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showImportWarningDialog by remember { mutableStateOf(false) }
    var showExportSelectionDialog by remember { mutableStateOf(false) }
    var exportSelectionState by remember { mutableStateOf(ExportSelectionState()) }
    val userPrefs =
        remember(context) {
            context.getSharedPreferences(BasePreferences.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        }
    val isSearchHistoryEnabledForExport =
        remember(userPrefs) {
            userPrefs.getBoolean(BasePreferences.KEY_RECENT_QUERIES_ENABLED, true)
        }
    val hasPinnedItemsForExport =
        remember(userPrefs) {
            listOf(
                BasePreferences.KEY_PINNED,
                BasePreferences.KEY_PINNED_CONTACT_IDS,
                BasePreferences.KEY_PINNED_FILE_URIS,
                BasePreferences.KEY_PINNED_SETTINGS,
                BasePreferences.KEY_PINNED_CALENDAR_EVENT_IDS,
                BasePreferences.KEY_PINNED_APP_SHORTCUTS,
            ).any { key ->
                userPrefs.getStringSet(key, emptySet()).orEmpty().isNotEmpty()
            }
        }
    FeatureFlags.initialize(context)

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch(Dispatchers.IO) {
                val isSuccess =
                    runCatching {
                        SettingsBackupManager.exportToUri(
                            context = context,
                            outputUri = uri,
                            options = exportSelectionState.toExportOptions(),
                        )
                    }.isSuccess
                withContext(Dispatchers.Main) {
                    val messageResId =
                        if (isSuccess) {
                            R.string.settings_backup_export_success
                        } else {
                            R.string.settings_backup_export_failed
                        }
                    Toast.makeText(context, context.getString(messageResId), Toast.LENGTH_SHORT).show()
                }
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch(Dispatchers.IO) {
                val isSuccess =
                    runCatching {
                        SettingsBackupManager.importFromUri(context, uri)
                    }.isSuccess
                withContext(Dispatchers.Main) {
                    val messageResId =
                        if (isSuccess) {
                            R.string.settings_backup_import_success
                        } else {
                            R.string.settings_backup_import_failed
                        }
                    Toast
                        .makeText(
                            context,
                            context.getString(messageResId),
                            Toast.LENGTH_SHORT,
                        ).show()
                    if (isSuccess) {
                        onSettingsImported()
                    }
                }
            }
        }

    SettingsScreenBackground(
        appTheme = state.appTheme,
        overlayThemeIntensity = state.overlayThemeIntensity,
        deviceThemeEnabled = state.deviceThemeEnabled,
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
            SettingsCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = DesignTokens.SectionTopPadding),
            ) {
                Column {
                    SettingsToggleRow(
                        title = stringResource(R.string.settings_overlay_mode_title),
                        subtitle = stringResource(R.string.settings_overlay_mode_desc),
                        checked = state.overlayModeEnabled,
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
                            title = stringResource(R.string.settings_backup_export_option_gemini_title),
                            description = stringResource(R.string.settings_gemini_api_config_desc),
                            iconResId = R.drawable.direct_search,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.GEMINI_API_CONFIG)
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
                                title = stringResource(R.string.settings_more_options_title),
                                description = stringResource(R.string.settings_more_options_desc),
                                icon = Icons.Rounded.Tune,
                                actionOnPress = {
                                    onNavigateToDetail(SettingsDetailType.MORE_OPTIONS)
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

                    BackupRestoreRow(
                        onImportClick = {
                            showImportWarningDialog = true
                        },
                        onExportClick = {
                            exportSelectionState =
                                ExportSelectionState(
                                    includeSettings = true,
                                    includeSearchHistory = isSearchHistoryEnabledForExport,
                                    includePinnedItems = hasPinnedItemsForExport,
                                    includeShortcuts = true,
                                    includeSearchEngines = true,
                                    includeGeminiApi = false,
                                    showSearchHistoryOption = isSearchHistoryEnabledForExport,
                                    showPinnedItemsOption = hasPinnedItemsForExport,
                                )
                            showExportSelectionDialog = true
                        },
                    )
                }
            }

            // More Options Section
            SettingsMoreOptions(
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
                        importLauncher.launch(arrayOf("*/*"))
                    },
                ) {
                    Text(text = stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportWarningDialog = false
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
                val defaultName = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                exportLauncher.launch("quick-search-settings-$defaultName.quicksearch")
            },
        )
    }
}

/**
 * More options section with rating, feedback, GitHub, and features.
 */
@Composable
fun SettingsMoreOptions(
    modifier: Modifier = Modifier,
    onOpenFeaturesList: () -> Unit = {},
    onOpenOssLicenses: () -> Unit = {},
) {
    val context = LocalContext.current

    val onSendFeedback = {
        FeedbackUtils.launchFeedbackEmail(context, null)
    }

    val onRateApp = {
        val packageName = context.packageName
        try {
            // Try to open Google Play Store app
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=$packageName")
                    setPackage("com.android.vending")
                }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback to web browser if Play Store app is not available
            try {
                val intent =
                    Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Handle case where browser is not available
            }
        }
    }

    val onOpenGitHub = {
        val url = "https://github.com/teja2495/quick-search"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle case where browser is not available
        }
    }

    val onOpenFeatures = {
        onOpenFeaturesList()
    }

    val feedbackItems =
        listOf(
            SettingsCardItem(
                title = stringResource(R.string.settings_feedback_send_title),
                description = stringResource(R.string.settings_feedback_send_desc),
                icon = Icons.Rounded.Email,
                actionOnPress = onSendFeedback,
            ),
            SettingsCardItem(
                title = stringResource(R.string.settings_feedback_rate_title),
                description = stringResource(R.string.settings_feedback_rate_desc),
                iconResId = R.drawable.google_play,
                actionOnPress = onRateApp,
            ),
            SettingsCardItem(
                title = stringResource(R.string.settings_feedback_github_title),
                description = stringResource(R.string.settings_feedback_github_desc),
                iconResId = R.drawable.ic_github,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                actionOnPress = onOpenGitHub,
            ),
            SettingsCardItem(
                title = stringResource(R.string.settings_all_quick_search_features),
                description = stringResource(R.string.settings_all_quick_search_features_desc),
                icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                actionOnPress = onOpenFeatures,
            ),
        )

    SettingsCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            feedbackItems.forEachIndexed { index, item ->
                SettingsNavigationRow(
                    item = item,
                    contentPadding =
                        PaddingValues(
                            horizontal = DesignTokens.SpacingXXLarge,
                            vertical = DesignTokens.SpacingLarge,
                        ),
                )

                if (index < feedbackItems.lastIndex) {
                    HorizontalDivider(
                        color = AppColors.SettingsDivider,
                    )
                }
            }

            HorizontalDivider(
                color = AppColors.SettingsDivider,
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenOssLicenses)
                        .padding(
                            horizontal = DesignTokens.SpacingXXLarge,
                            vertical = DesignTokens.SpacingLarge,
                        ),
            ) {
                Text(
                    text = stringResource(R.string.settings_open_source_licenses_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BackupRestoreRow(
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.SpacingXXLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        Text(
            text = stringResource(R.string.settings_backup_restore_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.settings_backup_restore_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            OutlinedButton(
                onClick = onImportClick,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
                Text(text = stringResource(R.string.settings_backup_import_button))
            }
            OutlinedButton(
                onClick = onExportClick,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Upload,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
                Text(text = stringResource(R.string.settings_backup_export_button))
            }
        }
    }
}

/**
 * Header component for the settings screen.
 */
@Composable
fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.ContentHorizontalPadding,
                    vertical = DesignTokens.HeaderVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.desc_navigate_back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        androidx.compose.foundation.layout
            .Spacer(modifier = Modifier.width(DesignTokens.HeaderIconSpacing))
        Text(
            text = stringResource(R.string.settings_backup_export_option_settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Displays the app version and developer info in a card at the bottom of the settings screen.
 */
@Composable
fun SettingsVersionDisplay(
    modifier: Modifier = Modifier,
    onFeatureFlagsChanged: () -> Unit = {},
) {
    val context = LocalContext.current
    val versionName = getAppVersionName() ?: "1.2.2"
    val developerName = stringResource(R.string.settings_feedback_developer_name)
    val developerDesc = stringResource(R.string.settings_feedback_developer_desc, developerName)

    val annotatedDeveloperDesc =
        buildAnnotatedString {
            val parts = developerDesc.split(developerName)
            if (parts.size > 1) {
                append(parts[0])
                withStyle(
                    style =
                        SpanStyle(
                            color = AppColors.LinkColor,
                            fontWeight = FontWeight.Medium,
                        ),
                ) {
                    append(developerName)
                }
                append(parts[1])
            } else {
                append(developerDesc)
            }
        }

    var versionTapCount by remember { mutableIntStateOf(0) }
    val versionTapInteractionSource = remember { MutableInteractionSource() }
    val hasNewFeaturesAvailable = FeatureFlag.entries.isNotEmpty()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.SpacingXXLarge,
                    vertical = DesignTokens.SpacingXLarge,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.settings_feedback_developer_title, stringResource(R.string.app_name), versionName),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.combinedClickable(
                    interactionSource = versionTapInteractionSource,
                    indication = null,
                    onClick = {
                        versionTapCount += 1
                        if (versionTapCount < 5) {
                            return@combinedClickable
                        }
                        versionTapCount = 0

                        if (FeatureFlags.isAnyEnabled()) {
                            FeatureFlags.setAll(context, enabled = false)
                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.settings_beta_features_disabled),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            onFeatureFlagsChanged()
                            return@combinedClickable
                        }

                        if (!hasNewFeaturesAvailable) {
                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.settings_beta_features_unavailable),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            return@combinedClickable
                        }

                        FeatureFlags.setAll(context, enabled = true)
                        Toast
                            .makeText(
                                context,
                                context.getString(R.string.settings_beta_features_enabled),
                                Toast.LENGTH_SHORT,
                            ).show()
                        onFeatureFlagsChanged()
                    },
                    onLongClick = {
                        FeedbackUtils.launchFeedbackEmailWithCrashLog(context)
                    },
                ),
        )

        androidx.compose.foundation.layout
            .Spacer(modifier = Modifier.height(DesignTokens.SpacingXSmall))

        Text(
            text = annotatedDeveloperDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.clickable {
                    val url = "https://hihello.com/p/e11b6338-b4a5-49d8-93c8-03ac219de738"
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                    }
                },
        )
    }
}

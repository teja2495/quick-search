package com.tk.quicksearch.settings.settingsScreen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.ui.components.TipBanner
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.FeedbackUtils
import com.tk.quicksearch.util.InAppBrowserUtils
import com.tk.quicksearch.util.hapticToggle
import kotlinx.coroutines.launch

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
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    hasUsagePermission: Boolean,
    hasContactPermission: Boolean,
    hasFilePermission: Boolean,
    hasCallPermission: Boolean,
    shouldShowBanner: Boolean,
    onRequestUsagePermission: () -> Unit,
    onRequestContactPermission: () -> Unit,
    onRequestFilePermission: () -> Unit,
    onRequestCallPermission: () -> Unit,
    onDismissBanner: () -> Unit,
    onNavigateToDetail: (SettingsDetailType) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState =
        androidx.compose.foundation.rememberScrollState(),
) {
    BackHandler(onBack = callbacks.onBack)
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding(),
    ) {
        SettingsHeader(onBack = callbacks.onBack)

        // Scrollable Content
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = DesignTokens.ContentHorizontalPadding),
        ) {
            // Overlay Mode Card (top)
            ElevatedCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = DesignTokens.SectionTopPadding),
                shape = MaterialTheme.shapes.extraLarge,
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
                                        color = MaterialTheme.colorScheme.primary,
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
                listOf(
                    SettingsCardItem(
                        title = stringResource(R.string.settings_appearance_title),
                        description = stringResource(R.string.settings_appearance_desc),
                        icon = Icons.Rounded.Palette,
                        actionOnPress = {
                            onNavigateToDetail(SettingsDetailType.APPEARANCE)
                        },
                    ),
                    SettingsCardItem(
                        title = stringResource(R.string.settings_search_results_title),
                        description = stringResource(R.string.settings_search_results_desc),
                        icon = Icons.Rounded.Search,
                        actionOnPress = {
                            onNavigateToDetail(SettingsDetailType.SEARCH_RESULTS)
                        },
                    ),
                    SettingsCardItem(
                        title = stringResource(R.string.settings_search_engines_title),
                        description = stringResource(R.string.settings_search_engines_desc),
                        icon = Icons.AutoMirrored.Rounded.ManageSearch,
                        actionOnPress = {
                            onNavigateToDetail(SettingsDetailType.SEARCH_ENGINES)
                        },
                    ),
                    SettingsCardItem(
                        title = stringResource(R.string.settings_calls_texts_title),
                        description = stringResource(R.string.settings_calls_texts_desc),
                        icon = Icons.Rounded.Phone,
                        actionOnPress = {
                            onNavigateToDetail(SettingsDetailType.CALLS_TEXTS)
                        },
                    ),
                    SettingsCardItem(
                        title = stringResource(R.string.settings_file_types_title),
                        description = stringResource(R.string.settings_files_desc),
                        icon = Icons.Rounded.Folder,
                        actionOnPress = {
                            onNavigateToDetail(SettingsDetailType.FILES)
                        },
                    ),
                    SettingsCardItem(
                        title = stringResource(R.string.settings_launch_options_title),
                        description = stringResource(R.string.settings_launch_options_desc),
                        icon = Icons.Rounded.RocketLaunch,
                        actionOnPress = {
                            onNavigateToDetail(SettingsDetailType.LAUNCH_OPTIONS)
                        },
                    ),
                )

            ElevatedCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = DesignTokens.SectionTopPadding),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column {
                    navigationItems.forEachIndexed { index, item ->
                        SettingsNavigationRow(
                            item = item,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        )

                        if (index < navigationItems.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }

            // More Options Section
            SettingsMoreOptions(
                onNavigateToPermissions = {
                    onNavigateToDetail(SettingsDetailType.PERMISSIONS)
                },
            )

            // App Version
            SettingsVersionDisplay(modifier = Modifier.padding(top = 40.dp, bottom = 60.dp))
        }
    }
}

/**
 * Single navigation card that matches the Search Engines section style.
 */
@Composable
fun NavigationSectionCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        SettingsNavigationRow(
            item =
                SettingsCardItem(
                    title = title,
                    description = description,
                    actionOnPress = onClick,
                ),
            contentPadding = contentPadding,
        )
    }
}

/**
 * More options section with navigation options for permissions, rating, feedback, GitHub, and features.
 */
@Composable
fun SettingsMoreOptions(
    modifier: Modifier = Modifier,
    onNavigateToPermissions: () -> Unit = {},
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
        val url = "https://github.com/teja2495/quick-search/blob/main/FEATURES.md"
        InAppBrowserUtils.openUrl(context, url)
    }

    val onOpenOssLicenses = {
        try {
            OssLicensesMenuActivity.setActivityTitle(
                context.getString(R.string.settings_open_source_licenses_title),
            )
            context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
        } catch (e: Exception) {
        }
    }

    val feedbackItems =
        listOf(
            SettingsCardItem(
                title = stringResource(R.string.settings_permissions_title),
                description = stringResource(R.string.settings_permissions_desc),
                icon = Icons.Rounded.AdminPanelSettings,
                actionOnPress = onNavigateToPermissions,
            ),
            SettingsCardItem(
                title = stringResource(R.string.settings_feedback_rate_title),
                description = stringResource(R.string.settings_feedback_rate_desc),
                iconResId = R.drawable.google_play,
                actionOnPress = onRateApp,
            ),
            SettingsCardItem(
                title = stringResource(R.string.settings_feedback_send_title),
                description = stringResource(R.string.settings_feedback_send_desc),
                icon = Icons.Rounded.Email,
                actionOnPress = onSendFeedback,
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

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column {
            feedbackItems.forEachIndexed { index, item ->
                SettingsNavigationRow(
                    item = item,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                )

                if (index < feedbackItems.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenOssLicenses)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
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
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Displays the app version and developer info in a card at the bottom of the settings screen.
 */
@Composable
fun SettingsVersionDisplay(modifier: Modifier = Modifier) {
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
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
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

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.settings_feedback_developer_title, stringResource(R.string.app_name), versionName),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        androidx.compose.foundation.layout
            .Spacer(modifier = Modifier.height(4.dp))

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

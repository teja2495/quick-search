package com.tk.quicksearch.settings.settingsScreen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.settings.SettingsSpacing
import com.tk.quicksearch.settings.searchEnginesScreen.SearchEngineAppearanceCard
import com.tk.quicksearch.settings.settingsScreen.permissions.PermissionsSection
import com.tk.quicksearch.settings.settingsScreen.permissions.UsagePermissionBanner
import kotlinx.coroutines.launch


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
        onMessagingAppSelected: (MessagingApp) -> Unit,
        onRequestContactPermission: () -> Unit,
        onRequestFilePermission: () -> Unit,
        onRequestCallPermission: () -> Unit,
        onDismissBanner: () -> Unit,
        onNavigateToDetail: (SettingsDetailType) -> Unit,
        scrollState: androidx.compose.foundation.ScrollState =
                androidx.compose.foundation.rememberScrollState()
) {
    BackHandler(onBack = callbacks.onBack)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showIconPackDialog by remember { mutableStateOf(false) }
    val selectedIconPackLabel =
            remember(state.selectedIconPackPackage, state.availableIconPacks) {
                state.availableIconPacks
                        .firstOrNull { it.packageName == state.selectedIconPackPackage }
                        ?.label
                        ?: context.getString(R.string.settings_icon_pack_option_system)
            }

    Column(
            modifier =
                    modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .safeDrawingPadding()
    ) {
        SettingsHeader(onBack = callbacks.onBack)

        // Scrollable Content
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(horizontal = SettingsSpacing.contentHorizontalPadding)
        ) {
            // Usage Permission Banner (at the top)
            // Show banner only if usage access permission is missing and user hasn't dismissed it
            // twice
            if (!hasUsagePermission && shouldShowBanner) {
                UsagePermissionBanner(
                        onRequestPermission = onRequestUsagePermission,
                        onDismiss = onDismissBanner,
                        onCardClick = {
                            onNavigateToDetail(SettingsDetailType.PERMISSIONS)
                        },
                        modifier = Modifier.padding(bottom = SettingsSpacing.sectionTopPadding)
                )
            }

            // Search Results and Search Engines Card
            CombinedSearchNavigationCard(
                    searchResultsTitle = stringResource(R.string.settings_search_results_title),
                    searchResultsDescription = stringResource(R.string.settings_search_results_desc),
                    searchEnginesTitle = stringResource(R.string.settings_search_engines_title),
                    searchEnginesDescription = stringResource(R.string.settings_search_engines_desc),
                    appearanceTitle = stringResource(R.string.settings_appearance_title),
                    appearanceDescription = stringResource(R.string.settings_appearance_desc),
                    callsTextsTitle = stringResource(R.string.settings_calls_texts_title),
                    callsTextsDescription = stringResource(R.string.settings_calls_texts_desc),
                    filesTitle = stringResource(R.string.settings_file_types_title),
                    filesDescription = stringResource(R.string.settings_files_desc),
                    launchOptionsTitle = stringResource(R.string.settings_launch_options_title),
                    launchOptionsDescription = stringResource(R.string.settings_launch_options_desc),
                    onSearchResultsClick = {
                        onNavigateToDetail(SettingsDetailType.SEARCH_RESULTS)
                    },
                    onSearchEnginesClick = {
                        onNavigateToDetail(SettingsDetailType.SEARCH_ENGINES)
                    },
                    onAppearanceClick = {
                        onNavigateToDetail(SettingsDetailType.APPEARANCE)
                    },
                    onCallsTextsClick = {
                        onNavigateToDetail(SettingsDetailType.CALLS_TEXTS)
                    },
                    onFilesClick = {
                        onNavigateToDetail(SettingsDetailType.FILES)
                    },
                    onLaunchOptionsClick = {
                        onNavigateToDetail(SettingsDetailType.LAUNCH_OPTIONS)
                    },
                    modifier = Modifier.padding(bottom = SettingsSpacing.sectionTopPadding)
            )


            // Feedback Section
            FeedbackSection(
                onNavigateToPermissions = {
                    onNavigateToDetail(SettingsDetailType.PERMISSIONS)
                }
            )

            // App Version
            SettingsVersionDisplay(modifier = Modifier.padding(top = 40.dp, bottom = 60.dp))
        }

        if (showIconPackDialog) {
            IconPackPickerDialog(
                    availableIconPacks = state.availableIconPacks,
                    selectedPackage = state.selectedIconPackPackage,
                    onSelect = { packageName ->
                        callbacks.onSelectIconPack(packageName)
                        showIconPackDialog = false
                    },
                    onDismiss = { showIconPackDialog = false }
            )
        }
    }
}

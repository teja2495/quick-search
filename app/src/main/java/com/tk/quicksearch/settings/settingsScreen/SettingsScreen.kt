package com.tk.quicksearch.settings.settingsScreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.settings.searchEnginesScreen.SearchEngineAppearanceCard
import com.tk.quicksearch.settings.settingsScreen.CalculatorToggleCard
import com.tk.quicksearch.settings.settingsScreen.CombinedAppearanceCard
import com.tk.quicksearch.settings.settingsScreen.CombinedSearchEnginesCard
import com.tk.quicksearch.settings.settingsScreen.CombinedSettingsNavigationCard
import com.tk.quicksearch.settings.settingsScreen.NavigationSectionCard
import com.tk.quicksearch.settings.settingsScreen.SettingsHeader
import com.tk.quicksearch.settings.settingsScreen.SettingsVersionDisplay
import com.tk.quicksearch.settings.SettingsSpacing
import com.tk.quicksearch.settings.settingsScreen.IconPackPickerDialog
import com.tk.quicksearch.settings.settingsScreen.MessagingSection
import com.tk.quicksearch.settings.settingsScreen.FeedbackSection
import com.tk.quicksearch.settings.settingsScreen.FileTypesSection
import com.tk.quicksearch.settings.settingsScreen.permissions.PermissionsSection
import com.tk.quicksearch.settings.settingsScreen.permissions.UsagePermissionBanner
import com.tk.quicksearch.settings.settingsScreen.SectionSettingsSection
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
    scrollState: androidx.compose.foundation.ScrollState = androidx.compose.foundation.rememberScrollState()
) {
    BackHandler(onBack = callbacks.onBack)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showIconPackDialog by remember { mutableStateOf(false) }
    val selectedIconPackLabel = remember(state.selectedIconPackPackage, state.availableIconPacks) {
        state.availableIconPacks.firstOrNull { it.packageName == state.selectedIconPackPackage }?.label
            ?: context.getString(R.string.settings_icon_pack_option_system)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        SettingsHeader(onBack = callbacks.onBack)

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = SettingsSpacing.contentHorizontalPadding)
        ) {
            // Usage Permission Banner (at the top)
            // Show banner only if usage access permission is missing and user hasn't dismissed it twice
            if (!hasUsagePermission && shouldShowBanner) {
                UsagePermissionBanner(
                    onRequestPermission = onRequestUsagePermission,
                    onDismiss = onDismissBanner,
                    onCardClick = {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    },
                    modifier = Modifier.padding(bottom = SettingsSpacing.sectionTopPadding)
                )
            }

            // Search Sections Section
            SectionSettingsSection(
                sectionOrder = state.sectionOrder,
                disabledSections = state.disabledSections,
                onToggleSection = callbacks.onToggleSection,
                onReorderSections = callbacks.onReorderSections
            )

            // Calculator Toggle
            CalculatorToggleCard(
                enabled = state.calculatorEnabled,
                onToggle = callbacks.onToggleCalculator,
                modifier = Modifier.padding(top = 12.dp)
            )

            // Internet Search Section
            Text(
                text = stringResource(R.string.settings_internet_search_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = SettingsSpacing.sectionTopPadding)
                    .padding(bottom = SettingsSpacing.sectionTitleBottomPadding)
            )

            // Combined Search Engines and Web Suggestions Card
            CombinedSearchEnginesCard(
                searchEnginesTitle = stringResource(R.string.settings_search_engines_title),
                searchEnginesDescription = stringResource(R.string.settings_search_engines_desc),
                onSearchEnginesClick = { onNavigateToDetail(SettingsDetailType.SEARCH_ENGINES) },
                webSuggestionsEnabled = state.webSuggestionsEnabled,
                onWebSuggestionsToggle = callbacks.onToggleWebSuggestions,
                webSuggestionsCount = state.webSuggestionsCount,
                onWebSuggestionsCountChange = callbacks.onWebSuggestionsCountChange,
                recentQueriesEnabled = state.recentQueriesEnabled,
                onRecentQueriesToggle = callbacks.onToggleRecentQueries,
                recentQueriesCount = state.recentQueriesCount,
                onRecentQueriesCountChange = callbacks.onRecentQueriesCountChange
            )

            // Appearance Section
            Text(
                text = stringResource(R.string.settings_app_labels_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = SettingsSpacing.sectionTopPadding)
                    .padding(bottom = SettingsSpacing.sectionTitleBottomPadding)
            )
            
            val hasIconPacks = state.availableIconPacks.isNotEmpty()
            CombinedAppearanceCard(
                keyboardAlignedLayout = state.keyboardAlignedLayout,
                onToggleKeyboardAlignedLayout = callbacks.onToggleKeyboardAlignedLayout,
                showWallpaperBackground = state.showWallpaperBackground,
                wallpaperBackgroundAlpha = state.wallpaperBackgroundAlpha,
                wallpaperBlurRadius = state.wallpaperBlurRadius,
                onToggleShowWallpaperBackground = { enabled ->
                    if (enabled && !hasFilePermission) {
                        // Request files permission when user tries to enable wallpaper
                        onRequestFilePermission()
                    } else {
                        callbacks.onToggleShowWallpaperBackground(enabled)
                    }
                },
                onWallpaperBackgroundAlphaChange = callbacks.onWallpaperBackgroundAlphaChange,
                onWallpaperBlurRadiusChange = callbacks.onWallpaperBlurRadiusChange,
                hasFilePermission = hasFilePermission,
                iconPackTitle = stringResource(R.string.settings_icon_pack_title),
                iconPackDescription = if (hasIconPacks) {
                    stringResource(R.string.settings_icon_pack_selected_label, selectedIconPackLabel)
                } else {
                    stringResource(R.string.settings_icon_pack_empty)
                },
                onIconPackClick = {
                    if (hasIconPacks) {
                        showIconPackDialog = true
                    } else {
                        callbacks.onSearchIconPacks()
                    }
                },
                onRefreshIconPacks = {
                    callbacks.onRefreshIconPacks()
                    coroutineScope.launch {
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_refreshing_icon_packs),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SearchEngineAppearanceCard(
                isSearchEngineCompactMode = state.isSearchEngineCompactMode,
                onToggleSearchEngineCompactMode = callbacks.onToggleSearchEngineCompactMode
            )

            // Contacts Section
            MessagingSection(
                messagingApp = state.messagingApp,
                onSetMessagingApp = callbacks.onSetMessagingApp,
                directDialEnabled = state.directDialEnabled,
                onToggleDirectDial = callbacks.onToggleDirectDial,
                hasCallPermission = hasCallPermission,
                contactsSectionEnabled = SearchSection.CONTACTS !in state.disabledSections,
                isWhatsAppInstalled = state.isWhatsAppInstalled,
                isTelegramInstalled = state.isTelegramInstalled,
                onMessagingAppSelected = onMessagingAppSelected,
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )

            // Files Section
            FileTypesSection(
                enabledFileTypes = state.enabledFileTypes,
                onToggleFileType = callbacks.onToggleFileType,
                excludedExtensions = state.excludedFileExtensions,
                onRemoveExcludedExtension = callbacks.onRemoveExcludedFileExtension,
                filesSectionEnabled = SearchSection.FILES !in state.disabledSections,
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )

            // More Options Section
            Text(
                text = stringResource(R.string.settings_more_options_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = SettingsSpacing.sectionTopPadding)
                    .padding(bottom = SettingsSpacing.sectionTitleBottomPadding)
            )

            // Combined Excluded Items and Additional Settings Card
            CombinedSettingsNavigationCard(
                excludedItemsTitle = stringResource(R.string.settings_excluded_items_title),
                excludedItemsDescription = stringResource(R.string.settings_excluded_items_desc),
                additionalSettingsTitle = stringResource(R.string.settings_additional_settings_title),
                additionalSettingsDescription = stringResource(R.string.settings_additional_settings_desc),
                onExcludedItemsClick = {
                    val hasItems = state.suggestionExcludedApps.isNotEmpty() ||
                                   state.resultExcludedApps.isNotEmpty() ||
                                   state.excludedContacts.isNotEmpty() ||
                                   state.excludedFiles.isNotEmpty() ||
                                   state.excludedSettings.isNotEmpty() ||
                                   state.excludedAppShortcuts.isNotEmpty()
                    if (hasItems) {
                        onNavigateToDetail(SettingsDetailType.EXCLUDED_ITEMS)
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_excluded_items_empty),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onAdditionalSettingsClick = { onNavigateToDetail(SettingsDetailType.ADDITIONAL_SETTINGS) },
                contentPadding = SettingsSpacing.singleCardPadding
            )

            // Permissions Section (at the bottom)
            PermissionsSection(
                hasUsagePermission = hasUsagePermission,
                hasContactPermission = hasContactPermission,
                hasFilePermission = hasFilePermission,
                hasCallPermission = hasCallPermission,
                onRequestUsagePermission = onRequestUsagePermission,
                onRequestContactPermission = onRequestContactPermission,
                onRequestFilePermission = onRequestFilePermission,
                onRequestCallPermission = onRequestCallPermission,
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )

            // Feedback Section
            FeedbackSection(
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )

            // App Version
            SettingsVersionDisplay(
                modifier = Modifier.padding(top = 40.dp, bottom = 60.dp)
            )
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

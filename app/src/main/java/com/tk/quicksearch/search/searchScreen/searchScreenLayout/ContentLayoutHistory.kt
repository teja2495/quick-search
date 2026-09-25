package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.SearchHistoryTab
import com.tk.quicksearch.search.searchHistory.SearchHistorySection
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.search.searchScreen.ContactsSectionParams
import com.tk.quicksearch.search.searchScreen.FilesSectionParams
import com.tk.quicksearch.search.searchScreen.AppShortcutsSectionParams
import com.tk.quicksearch.search.searchScreen.SettingsSectionParams
import com.tk.quicksearch.search.searchScreen.CalendarSectionParams
import com.tk.quicksearch.search.searchScreen.NotesSectionParams
import com.tk.quicksearch.search.searchScreen.RemindersSectionParams
import com.tk.quicksearch.search.searchScreen.PinnedNonAppItemsSection
import com.tk.quicksearch.R
import com.tk.quicksearch.app.startup.StartupTrace

@Composable
internal fun HomeSearchHistoryBlock(
    state: SearchUiState,
    renderingState: SectionRenderingState,
    isHomeCalendarExpanded: Boolean,
    hasQuery: Boolean,
    appearedHomeContentKeys: MutableSet<String>,
    hasAtAGlanceSection: Boolean,
    effectiveContactsParams: ContactsSectionParams,
    effectiveFilesParams: FilesSectionParams,
    effectiveSettingsParams: SettingsSectionParams,
    effectiveAppShortcutsParams: AppShortcutsSectionParams,
    effectiveCalendarParams: CalendarSectionParams,
    effectiveNotesParams: NotesSectionParams,
    effectiveRemindersParams: RemindersSectionParams?,
    notesParams: NotesSectionParams,
    onRecentQueryClick: (RecentSearchEntry.Query) -> Unit,
    onDeleteRecentItem: (RecentSearchEntry) -> Unit,
    onClearRecentItems: () -> Unit,
    searchHistoryExpanded: Boolean,
    onSearchHistoryExpandedChange: (Boolean) -> Unit,
    searchHistoryCollapseRequestKey: Int,
    expandedCardMaxHeight: Dp,
    effectiveShowWallpaperBackground: Boolean,
    isOverlayPresentation: Boolean,
    searchHistorySelectedTab: SearchHistoryTab,
    onSearchHistorySelectedTabChange: (SearchHistoryTab) -> Unit,
    showPinnedNonAppItems: Boolean,
    pinnedNonAppItemsRendered: Boolean,
    onPinnedNonAppItemsRendered: () -> Unit,
    userPreferences: UserAppPreferences,
    pinnedCalendarEventsForPinnedBlock: List<com.tk.quicksearch.search.models.CalendarEventInfo>,
) {
        if (isHomeCalendarExpanded) return
        LaunchedEffect(Unit) { StartupTrace.mark("QS.Home.SearchHistoryRendered") }
        HomeLoadingAnimatedContent(
            animationKey = "home-search-history",
            enabled = !hasQuery,
            appearedKeys = appearedHomeContentKeys,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                if (
                    shouldShowSearchHistoryTitle(hasAtAGlanceSection)
                ) {
                    Text(
                        text = stringResource(R.string.recent_queries_toggle_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = homeTextColor(),
                        modifier = Modifier.padding(horizontal = DesignTokens.SpacingLarge),
                    )
                }
                SearchHistorySection(
                    items = state.recentItems,
                    callingApp =
                        effectiveContactsParams.callingApp
                            ?: CallingApp.CALL,
                    messagingApp =
                        effectiveContactsParams.messagingApp
                            ?: MessagingApp
                                .MESSAGES,
                    onRecentQueryClick =
                    onRecentQueryClick,
                    onContactClick =
                        effectiveContactsParams
                            .onContactClick,
                    onShowContactMethods =
                        effectiveContactsParams
                            .onShowContactMethods,
                    onCallContact =
                        effectiveContactsParams
                            .onCallContact,
                    onSmsContact =
                        effectiveContactsParams.onSmsContact,
                    onContactMethodClick =
                        effectiveContactsParams
                            .onContactMethodClick,
                    getPrimaryContactCardAction =
                        effectiveContactsParams
                            .getPrimaryContactCardAction,
                    getSecondaryContactCardAction =
                        effectiveContactsParams
                            .getSecondaryContactCardAction,
                    onPrimaryActionLongPress =
                        effectiveContactsParams
                            .onPrimaryActionLongPress,
                    onSecondaryActionLongPress =
                        effectiveContactsParams
                            .onSecondaryActionLongPress,
                    onCustomAction =
                        effectiveContactsParams
                            .onCustomAction,
                    onFileClick =
                        effectiveFilesParams.onFileClick,
                    onSettingClick =
                        effectiveSettingsParams
                            .onSettingClick,
                    onAppShortcutClick =
                        effectiveAppShortcutsParams
                            .onShortcutClick,
                    onNoteClick = notesParams.onNoteClick,
                    onDeleteRecentItem =
                    onDeleteRecentItem,
                    onClearRecentItems = onClearRecentItems,
                    isExpanded = searchHistoryExpanded,
                    collapsedItemCount = state.recentQueriesDisplayCount,
                    reverseCollapsedItems = state.oneHandedMode,
                    onExpandedChange = onSearchHistoryExpandedChange,
                    collapseRequestKey = searchHistoryCollapseRequestKey,
                    expandedCardMaxHeight = expandedCardMaxHeight,
                    showWallpaperBackground =
                        effectiveShowWallpaperBackground,
                    isOverlayPresentation = isOverlayPresentation,
                    showInlineCollapseButton = false,
                    selectedTab = searchHistorySelectedTab,
                    onSelectedTabChange = onSearchHistorySelectedTabChange,
                    modifier = Modifier.fillMaxWidth(),
                    )
                if (showPinnedNonAppItems && !pinnedNonAppItemsRendered) {
                    UnifiedPinnedItemsBlock(
                        userPreferences = userPreferences,
                        showWallpaperBackground = effectiveShowWallpaperBackground,
                    ) {
                        PinnedNonAppItemsSection(
                            pinnedItemOrder = state.pinnedNonAppItemOrder,
                            contacts = renderingState.pinnedContacts,
                            files = renderingState.pinnedFiles,
                            appShortcuts = renderingState.pinnedAppShortcuts,
                            settings = renderingState.pinnedSettings,
                            calendarEvents = pinnedCalendarEventsForPinnedBlock,
                            notes = renderingState.pinnedNotes,
                            reminders = renderingState.pinnedReminders,
                            contactsParams = effectiveContactsParams,
                            filesParams = effectiveFilesParams,
                            appShortcutsParams = effectiveAppShortcutsParams,
                            settingsParams = effectiveSettingsParams,
                            calendarParams = effectiveCalendarParams,
                            notesParams = effectiveNotesParams,
                            remindersParams = effectiveRemindersParams,
                            showWallpaperBackground = effectiveShowWallpaperBackground,
                            modifier = Modifier.fillMaxWidth(),
                            )
                    }
                    onPinnedNonAppItemsRendered()
                }
            }
        }
    }

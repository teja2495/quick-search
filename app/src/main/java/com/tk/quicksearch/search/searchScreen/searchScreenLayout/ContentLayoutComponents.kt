package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import com.tk.quicksearch.search.searchHistory.SearchHistorySection
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.search.searchScreen.ContactsSectionParams
import com.tk.quicksearch.search.searchScreen.FilesSectionParams
import com.tk.quicksearch.search.searchScreen.AppShortcutsSectionParams
import com.tk.quicksearch.search.searchScreen.SettingsSectionParams
import com.tk.quicksearch.search.searchScreen.NotesSectionParams
import com.tk.quicksearch.search.searchScreen.RemindersSectionParams
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.R

/**
 * Fades home sections in when their asynchronously loaded data first arrives. The section is laid
 * out at its final height from the first frame, so neighbouring sections never slide as late
 * content appears; only its opacity animates.
 */
@Composable
internal fun HomeLoadingAnimatedContent(
    animationKey: String,
    enabled: Boolean,
    appearedKeys: MutableSet<String>,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }

    val shouldAnimate = remember(animationKey) { appearedKeys.add(animationKey) }
    if (!shouldAnimate) {
        content()
        return
    }

    var visible by remember(animationKey) { mutableStateOf(false) }
    LaunchedEffect(animationKey) {
        visible = true
    }
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = HomeSectionFadeDurationMillis),
        label = "homeSectionFade",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = contentAlpha },
        verticalArrangement = Arrangement.spacedBy(HomeSectionContentSpacing),
    ) {
        content()
    }
}

private const val HomeSectionFadeDurationMillis = 180
private val HomeSectionContentSpacing = 14.dp

internal fun hasMoreResults(
    renderingState: SectionRenderingState,
    sectionContext: SectionRenderContext,
): Boolean =
    (sectionContext.shouldRenderApps && renderingState.displayApps.isNotEmpty()) ||
        (sectionContext.shouldRenderAppShortcuts && sectionContext.appShortcutsList.isNotEmpty()) ||
        (sectionContext.shouldRenderContacts && sectionContext.contactsList.isNotEmpty()) ||
        (sectionContext.shouldRenderFiles && sectionContext.filesList.isNotEmpty()) ||
        (sectionContext.shouldRenderSettings && sectionContext.settingsList.isNotEmpty()) ||
        (sectionContext.shouldRenderAppSettings && sectionContext.appSettingsList.isNotEmpty()) ||
        (sectionContext.shouldRenderCalendar && sectionContext.calendarEventsList.isNotEmpty()) ||
        (sectionContext.shouldRenderNotes && sectionContext.notesList.isNotEmpty()) ||
        (sectionContext.shouldRenderReminders && sectionContext.remindersList.isNotEmpty())

internal fun SearchSection.supportsPinnedHomeCollapse(): Boolean =
    when (this) {
        SearchSection.APPS, SearchSection.APP_SETTINGS -> false
        SearchSection.APP_SHORTCUTS,
        SearchSection.CONTACTS,
        SearchSection.FILES,
        SearchSection.SETTINGS,
        SearchSection.CALENDAR,
        SearchSection.REMINDERS,
        SearchSection.NOTES,
        -> true
    }

@Composable
internal fun UnifiedPinnedItemsBlock(
    userPreferences: UserAppPreferences,
    showWallpaperBackground: Boolean,
    content: @Composable () -> Unit,
) {
    var isExpanded by rememberSaveable {
        mutableStateOf(userPreferences.isUnifiedPinnedItemsExpanded())
    }
    val interactionSource = remember { MutableInteractionSource() }
    val toggleExpanded = {
        val newExpanded = !isExpanded
        isExpanded = newExpanded
        userPreferences.setUnifiedPinnedItemsExpanded(newExpanded)
    }

    val headerContent: @Composable (Modifier) -> Unit = { modifier ->
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = toggleExpanded,
                )
                .padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingXXSmall,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_suggestions_tab_pinned),
                style = MaterialTheme.typography.titleSmall,
                color = homeTextColor(),
            )
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = stringResource(
                    if (isExpanded) R.string.desc_collapse else R.string.desc_expand,
                ),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(DesignTokens.IconSizeSmall),
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXXSmall),
    ) {
        if (isExpanded) {
            headerContent(Modifier)
        } else {
            SearchResultCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp),
                showWallpaperBackground = showWallpaperBackground,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    headerContent(
                        Modifier.padding(horizontal = DesignTokens.SpacingLarge),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            content()
        }
    }
}

internal val ALIAS_RECENT_ELIGIBLE_SECTIONS =
    setOf(
        SearchSection.APP_SHORTCUTS,
        SearchSection.FILES,
        SearchSection.CONTACTS,
        SearchSection.SETTINGS,
        SearchSection.APP_SETTINGS,
        SearchSection.NOTES,
    )

internal fun ItemPriorityConfig.ItemType.toSearchSectionOrNull(): SearchSection? =
    SearchSectionRegistry.sectionForItemType(this)

internal fun sectionAliasPermissionMessageRes(
    state: SearchUiState,
    section: SearchSection,
    isSectionAliasMode: Boolean,
): Int? {
    if (!isSectionAliasMode) return null
    return when (section) {
        SearchSection.CONTACTS ->
            if (state.contactsSectionState is ContactsSectionVisibility.NoPermission) {
                R.string.contacts_section_permission_subtitle
            } else {
                null
            }
        SearchSection.FILES ->
            if (state.filesSectionState is FilesSectionVisibility.NoPermission) {
                R.string.files_section_permission_subtitle
            } else {
                null
            }
        SearchSection.CALENDAR ->
            if (state.calendarSectionState is CalendarSectionVisibility.NoPermission) {
                R.string.calendar_section_permission_subtitle
            } else {
                null
            }
        else -> null
    }
}

@Composable
internal fun AliasRecentItemsSection(
    items: List<com.tk.quicksearch.search.searchHistory.RecentSearchItem>,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    settingsParams: SettingsSectionParams,
    appShortcutsParams: AppShortcutsSectionParams,
    notesParams: NotesSectionParams,
    remindersParams: RemindersSectionParams? = null,
    onRecentQueryClick: (RecentSearchEntry.Query) -> Unit,
    onDeleteRecentItem: (RecentSearchEntry) -> Unit,
    expandedCardMaxHeight: Dp,
    showWallpaperBackground: Boolean,
    isOverlayPresentation: Boolean,
) {
    SearchHistorySection(
        items = items,
        callingApp = contactsParams.callingApp ?: CallingApp.CALL,
        messagingApp = contactsParams.messagingApp ?: MessagingApp.MESSAGES,
        onRecentQueryClick = onRecentQueryClick,
        onContactClick = contactsParams.onContactClick,
        onShowContactMethods = contactsParams.onShowContactMethods,
        onCallContact = contactsParams.onCallContact,
        onSmsContact = contactsParams.onSmsContact,
        onContactMethodClick = contactsParams.onContactMethodClick,
        getPrimaryContactCardAction = contactsParams.getPrimaryContactCardAction,
        getSecondaryContactCardAction = contactsParams.getSecondaryContactCardAction,
        onPrimaryActionLongPress = contactsParams.onPrimaryActionLongPress,
        onSecondaryActionLongPress = contactsParams.onSecondaryActionLongPress,
        onCustomAction = contactsParams.onCustomAction,
        onFileClick = filesParams.onFileClick,
        onSettingClick = settingsParams.onSettingClick,
        onAppShortcutClick = appShortcutsParams.onShortcutClick,
        onNoteClick = notesParams.onNoteClick,
        onAppSettingClick = settingsParams.onAppSettingClick,
        onAppSettingToggle = settingsParams.onAppSettingToggle,
        isAppSettingToggleChecked = settingsParams.isAppSettingToggleChecked,
        appSettingPhoneAppGridColumns = settingsParams.appSettingPhoneAppGridColumns,
        onAppSettingPhoneAppGridColumnsChange = settingsParams.onAppSettingPhoneAppGridColumnsChange,
        appSettingAppResultRowCount = settingsParams.appSettingAppResultRowCount,
        onAppSettingAppResultRowCountChange = settingsParams.onAppSettingAppResultRowCountChange,
        onDeleteRecentItem = onDeleteRecentItem,
        showInlineCollapseButton = false,
        expandedCardMaxHeight = expandedCardMaxHeight,
        showWallpaperBackground = showWallpaperBackground,
        isOverlayPresentation = isOverlayPresentation,
        alwaysExpanded = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

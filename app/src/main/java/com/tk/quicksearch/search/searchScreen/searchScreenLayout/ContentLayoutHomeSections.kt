package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.settings.settingsDetailScreen.PriorityReorderDialog
import com.tk.quicksearch.settings.settingsDetailScreen.withHiddenPinnedSectionsRestored
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.R

@Composable
internal fun HomePinnedSection(
    section: SearchSection,
    showSectionedPinnedHeaders: Boolean,
    userPreferences: UserAppPreferences,
    effectiveShowWallpaperBackground: Boolean,
    onShowOrderDialog: () -> Unit,
    content: @Composable () -> Unit,
) {
        if (!showSectionedPinnedHeaders || !section.supportsPinnedHomeCollapse()) {
            content()
            return
        }

        var isExpanded by rememberSaveable(section.name) {
            mutableStateOf(userPreferences.isHomePinnedSectionExpanded(section))
        }
        val interactionSource = remember { MutableInteractionSource() }
        val metadata = SearchSectionUiMetadataRegistry.metadataFor(section)
        val sectionIcon = metadata.settingsIcon
        val headerGestures =
            Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    val newExpanded = !isExpanded
                    isExpanded = newExpanded
                    userPreferences.setHomePinnedSectionExpanded(section, newExpanded)
                },
                onLongClick = { onShowOrderDialog() },
            )
        val headerContent: @Composable (Modifier) -> Unit = { modifier ->
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = DesignTokens.SpacingLarge,
                        vertical = DesignTokens.SpacingXXSmall,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!isExpanded) {
                        Icon(
                            imageVector = sectionIcon,
                            contentDescription = null,
                            tint = homeTextColor(),
                            modifier = Modifier.size(DesignTokens.IconSizeSmall),
                        )
                    }
                    Text(
                        text = stringResource(metadata.sectionLabelRes),
                        style = MaterialTheme.typography.titleSmall,
                        color = homeTextColor(),
                    )
                }
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
                headerContent(headerGestures)
            } else {
                SearchResultCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    showWallpaperBackground = effectiveShowWallpaperBackground,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .then(headerGestures),
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

@Composable
internal fun HomePinnedSectionOrderDialog(
    showPinnedSectionOrderDialog: Boolean,
    state: SearchUiState,
    onHomePinnedSectionOrderChange: (List<SearchSection>) -> Unit,
    onDismiss: () -> Unit,
) {
    if (showPinnedSectionOrderDialog) {
        val pinnedSectionOrderItems =
            state.homePinnedSectionOrder.filter { section ->
                FeatureFlags.isSearchSectionEnabled(section) &&
                    !(state.pinnedAppShortcutsInAppGrid && section == SearchSection.APP_SHORTCUTS)
            }
        PriorityReorderDialog(
            items = pinnedSectionOrderItems,
            onItemsChange = { order ->
                onHomePinnedSectionOrderChange(
                    withHiddenPinnedSectionsRestored(order, state.homePinnedSectionOrder),
                )
            },
            onDismiss = onDismiss,
            titleRes = R.string.settings_pinned_sections_order_title,
            infoRes = R.string.settings_pinned_sections_order_dialog_info,
        )
    }

}

internal fun homePinnedSectionHasItems(
        section: SearchSection,
        sectionContext: SectionRenderContext,
    ): Boolean =
        when (section) {
            SearchSection.APP_SHORTCUTS ->
                sectionContext.shouldRenderAppShortcuts && sectionContext.appShortcutsList.isNotEmpty()
            SearchSection.CONTACTS ->
                sectionContext.shouldRenderContacts && sectionContext.contactsList.isNotEmpty()
            SearchSection.FILES ->
                sectionContext.shouldRenderFiles && sectionContext.filesList.isNotEmpty()
            SearchSection.SETTINGS ->
                sectionContext.shouldRenderSettings &&
                    !sectionContext.isAppSettingsExpanded &&
                    sectionContext.settingsList.isNotEmpty()
            SearchSection.CALENDAR ->
                (sectionContext.shouldRenderCalendar && sectionContext.calendarEventsList.isNotEmpty()) ||
                    (sectionContext.isHomeScreenCalendarMode &&
                        sectionContext.todayCalendarEventsList.isNotEmpty())
            SearchSection.REMINDERS ->
                sectionContext.shouldRenderReminders && sectionContext.remindersList.isNotEmpty()
            SearchSection.NOTES ->
                sectionContext.shouldRenderNotes && sectionContext.notesList.isNotEmpty()
            SearchSection.APPS, SearchSection.APP_SETTINGS -> true
        }

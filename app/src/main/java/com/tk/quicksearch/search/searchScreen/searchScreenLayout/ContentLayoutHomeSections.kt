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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import java.util.Locale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.settings.settingsDetailScreen.PriorityReorderDialog
import com.tk.quicksearch.settings.appearanceSettings.withHiddenPinnedSectionsRestored
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.search.searchScreen.shared.LocalCardlessStackSpacing
import com.tk.quicksearch.search.searchScreen.shared.EdgeDistances
import com.tk.quicksearch.search.searchScreen.shared.cardlessDividerColor
import com.tk.quicksearch.search.searchScreen.shared.leftOr
import com.tk.quicksearch.search.searchScreen.shared.rightOr
import com.tk.quicksearch.search.searchScreen.shared.trackEdgeDistances
import com.tk.quicksearch.R

/**
 * Side inset for a collapsed pinned-section header. Flat cards bleed [DesignTokens.SpacingSmall]
 * outward, so the smaller inset lines the header up with section titles.
 */
@Composable
internal fun collapsedPinnedHeaderInset(): Dp =
    if (LocalSearchCardLayoutEnabled.current) DesignTokens.SpacingLarge else DesignTokens.SpacingSmall

/**
 * The divider above a flat header sits [HomeSectionContentSpacing] minus half the stack spacing
 * (8dp) above it, its own divider half the stack spacing (6dp) below. Shifting 1dp of padding from
 * top to bottom centers the title between the two.
 */
private val CompactHeaderCentering = 1.dp

/**
 * Draws a flat header row's divider across the full screen width (the results column bleeds to the
 * edges, see [compactEdgeBleed]), half the stack spacing below the row like flat card dividers.
 */
@Composable
private fun Modifier.compactHeaderDivider(showWallpaperBackground: Boolean): Modifier {
    val dividerColor = cardlessDividerColor(showWallpaperBackground)
    val below = LocalCardlessStackSpacing.current / 2
    val edges = remember { EdgeDistances() }
    return trackEdgeDistances(edges).drawWithContent {
        drawContent()
        val left = edges.leftOr(0).toFloat()
        val right = edges.rightOr(0).toFloat()
        val stroke = DesignTokens.BorderWidth.toPx()
        val y = size.height + below.toPx() - stroke / 2
        drawLine(dividerColor, Offset(-left, y), Offset(size.width + right, y), strokeWidth = stroke)
    }
}

/** Flat header titles: small, letter-spaced caps. */
@Composable
private fun compactHeaderTitleStyle(): TextStyle =
    MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp)

/**
 * Title row of a home section: optional leading icon, title, and an expand chevron when
 * [isExpanded] is set. With the card layout off it becomes a flat row with a small caps title and a
 * divider below it, sized like the rows it heads.
 */
@Composable
internal fun HomeSectionHeaderRow(
    title: String,
    leadingIcon: ImageVector?,
    isExpanded: Boolean?,
    showWallpaperBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    val compactHeader = !LocalSearchCardLayoutEnabled.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (compactHeader) {
                    Modifier.compactHeaderDivider(showWallpaperBackground)
                } else {
                    Modifier
                },
            )
            .padding(
                start = DesignTokens.SpacingLarge,
                end = DesignTokens.SpacingLarge,
                top = if (compactHeader) DesignTokens.SpacingXSmall - CompactHeaderCentering else DesignTokens.SpacingXXSmall,
                bottom = if (compactHeader) DesignTokens.SpacingXSmall + CompactHeaderCentering else DesignTokens.SpacingXXSmall,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = homeTextColor(),
                    modifier = Modifier.size(DesignTokens.IconSizeSmall),
                )
            }
            Text(
                text = if (compactHeader) title.uppercase(Locale.getDefault()) else title,
                style = if (compactHeader) compactHeaderTitleStyle() else MaterialTheme.typography.titleSmall,
                color = homeTextColor(),
            )
        }
        if (isExpanded != null) {
            Icon(
                imageVector = if (isExpanded) MaterialSymbols.ExpandLess else MaterialSymbols.ExpandMore,
                contentDescription = stringResource(
                    if (isExpanded) R.string.desc_collapse else R.string.desc_expand,
                ),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(DesignTokens.IconSizeSmall),
            )
        }
    }
}

/**
 * Plain home section title (At a Glance, Search History). With the card layout off it uses the flat
 * header row so it matches the collapsible section headers.
 */
@Composable
internal fun HomeSectionTitle(
    title: String,
    showWallpaperBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    if (LocalSearchCardLayoutEnabled.current) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = homeTextColor(),
            modifier = Modifier.padding(horizontal = DesignTokens.SpacingLarge).then(modifier),
        )
    } else {
        HomeSectionHeaderRow(
            title = title,
            leadingIcon = null,
            isExpanded = null,
            showWallpaperBackground = showWallpaperBackground,
            modifier = modifier,
        )
    }
}

/**
 * Expanding body of a collapsible home section. The gap below the header lives inside the animated
 * content so it shrinks with it, instead of snapping away when the collapse finishes.
 */
@Composable
internal fun HomeSectionCollapsibleContent(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    // Flat headers draw their divider in this gap, so it matches the spacing between home items.
    val headerGap =
        if (LocalSearchCardLayoutEnabled.current) DesignTokens.SpacingXXSmall else HomeSectionContentSpacing
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        Box(modifier = Modifier.padding(top = headerGap)) {
            content()
        }
    }
}

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
        val compactHeader = !LocalSearchCardLayoutEnabled.current
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
            HomeSectionHeaderRow(
                title = stringResource(metadata.sectionLabelRes),
                leadingIcon = sectionIcon.takeIf { !isExpanded && !compactHeader },
                isExpanded = isExpanded,
                showWallpaperBackground = effectiveShowWallpaperBackground,
                modifier = modifier,
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isExpanded || compactHeader) {
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
                            Modifier.padding(horizontal = collapsedPinnedHeaderInset()),
                        )
                    }
                }
            }
            HomeSectionCollapsibleContent(visible = isExpanded, content = content)
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
            SearchSection.BOOKMARKS ->
                sectionContext.shouldRenderBookmarks && sectionContext.bookmarksList.isNotEmpty()
            // Noted items appear only in Top matches.
            SearchSection.NOTED -> false
            SearchSection.APPS, SearchSection.APP_SETTINGS -> true
        }

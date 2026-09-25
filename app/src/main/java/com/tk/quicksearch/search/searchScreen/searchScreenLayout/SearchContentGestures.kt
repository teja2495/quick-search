package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.search.searchScreen.ExpandedSection
import com.tk.quicksearch.search.searchScreen.ContactsSectionParams
import com.tk.quicksearch.search.searchScreen.FilesSectionParams
import com.tk.quicksearch.search.searchScreen.AppShortcutsSectionParams
import com.tk.quicksearch.search.searchScreen.SettingsSectionParams
import com.tk.quicksearch.search.searchScreen.AppsSectionParams
import com.tk.quicksearch.search.searchScreen.CalendarSectionParams
import com.tk.quicksearch.search.searchScreen.NotesSectionParams
import com.tk.quicksearch.search.searchScreen.RemindersSectionParams
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.hasAnySearchResults
import com.tk.quicksearch.search.searchScreen.appThemeResultCardColor
import com.tk.quicksearch.search.searchScreen.appThemeDividerColor
import com.tk.quicksearch.search.searchScreen.appThemeActionColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayActionColor
import com.tk.quicksearch.search.searchScreen.components.CollapseButton
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import com.tk.quicksearch.search.searchHistory.SearchHistoryTab
import com.tk.quicksearch.search.searchHistory.SearchHistorySection
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.searchEngines.compact.NoResultsSearchEngineCards
import com.tk.quicksearch.search.webSuggestions.WebSuggestionsSection
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.calculator.CalculatorResult
import com.tk.quicksearch.tools.aiSearch.AiSearchResult
import kotlin.math.min
import com.tk.quicksearch.search.other.OtherSearchItemId
import com.tk.quicksearch.widgetsPanel.HomeAddWidgetSheet

@Composable
internal fun rememberSearchContentOverscrollConnection(
    bottomOneHandedOverscrollEnabled: Boolean,
    launcherOverscrollUpEnabled: Boolean,
    launcherOverscrollDownEnabled: Boolean,
    onBottomOneHandedOverscrollUp: () -> Unit,
    onLauncherOverscrollUp: () -> Unit,
    onLauncherOverscrollDown: () -> Unit,
): NestedScrollConnection {
    return remember(
            bottomOneHandedOverscrollEnabled,
            launcherOverscrollUpEnabled,
            launcherOverscrollDownEnabled,
            onBottomOneHandedOverscrollUp,
            onLauncherOverscrollUp,
            onLauncherOverscrollDown,
        ) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (
                        bottomOneHandedOverscrollEnabled &&
                            source == NestedScrollSource.UserInput &&
                            available.y < -OVERSCROLL_FOCUS_THRESHOLD_PX
                    ) {
                        onBottomOneHandedOverscrollUp()
                    }
                    if (
                        launcherOverscrollUpEnabled &&
                            source == NestedScrollSource.UserInput &&
                            available.y < -OVERSCROLL_FOCUS_THRESHOLD_PX
                    ) {
                        onLauncherOverscrollUp()
                    }
                    if (
                        launcherOverscrollDownEnabled &&
                            source == NestedScrollSource.UserInput &&
                            available.y > OVERSCROLL_FOCUS_THRESHOLD_PX
                    ) {
                        onLauncherOverscrollDown()
                    }
                    return Offset.Zero
                }
            }
        }
}

internal fun Modifier.searchHistoryExpandedTabSwipe(
    enabled: Boolean,
    canSwitchTabs: Boolean,
    selectedTab: SearchHistoryTab,
    onTabSelected: (SearchHistoryTab) -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(selectedTab, canSwitchTabs) {
        var totalHorizontalDrag = 0f
        detectHorizontalDragGestures(
            onDragStart = { totalHorizontalDrag = 0f },
            onHorizontalDrag = { change, dragAmount ->
                totalHorizontalDrag += dragAmount
                change.consume()
            },
            onDragEnd = {
                if (canSwitchTabs) {
                    when {
                        totalHorizontalDrag <= -SEARCH_HISTORY_TAB_SWIPE_THRESHOLD_PX &&
                            selectedTab != SearchHistoryTab.RECENTLY_OPENED ->
                            onTabSelected(SearchHistoryTab.RECENTLY_OPENED)

                        totalHorizontalDrag >= SEARCH_HISTORY_TAB_SWIPE_THRESHOLD_PX &&
                            selectedTab != SearchHistoryTab.SEARCHES ->
                            onTabSelected(SearchHistoryTab.SEARCHES)
                    }
                }
                totalHorizontalDrag = 0f
            },
            onDragCancel = { totalHorizontalDrag = 0f },
        )
    }
}

internal const val ExpansionTransitionDurationMillis = 220
internal val ExpansionTransitionOffset = 16.dp

/** Flips its phase whenever the expansion key changes, so each change restarts the transition. */
internal class ExpansionPhaseTracker {
    private var lastKey: Any? = null
    private var phase = true

    fun phaseFor(key: Any): Boolean {
        if (lastKey != null && key != lastKey) phase = !phase
        lastKey = key
        return phase
    }
}
internal const val SEARCH_HISTORY_TAB_SWIPE_THRESHOLD_PX = 64f
internal const val OVERSCROLL_FOCUS_THRESHOLD_PX = 24f

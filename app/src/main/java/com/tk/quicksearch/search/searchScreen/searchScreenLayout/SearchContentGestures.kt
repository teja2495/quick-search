package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.search.searchScreen.hasAnySearchResults
import com.tk.quicksearch.search.searchScreen.appThemeResultCardColor
import com.tk.quicksearch.search.searchScreen.appThemeDividerColor
import com.tk.quicksearch.search.searchScreen.appThemeActionColor
import com.tk.quicksearch.search.searchHistory.SearchHistoryTab
import com.tk.quicksearch.searchEngines.*
import kotlin.math.min

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

internal const val SEARCH_HISTORY_TAB_SWIPE_THRESHOLD_PX = 64f
internal const val OVERSCROLL_FOCUS_THRESHOLD_PX = 24f

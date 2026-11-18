package com.tk.quicksearch.search.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.util.Locale
import kotlin.math.roundToInt
import com.tk.quicksearch.shared.ui.theme.AppShapes

private val LetterPopupSize = 80.dp
private val LetterPopupPointerWidth = 14.dp
private val LetterPopupPointerHeight = 22.dp
private val LetterPopupScrollbarGap = DesignTokens.SpacingXSmall

@Composable
internal fun AllAppsScrollLetterPopup(
        apps: List<AppInfo>,
        gridState: LazyGridState,
        scrollbarDragFraction: Float?,
        modifier: Modifier = Modifier,
) {
    val firstVisibleIndex by remember {
        derivedStateOf { gridState.firstVisibleItemIndex }
    }
    val metrics by remember {
        derivedStateOf { scrollbarMetrics(gridState) }
    }
    val letter = apps.getOrNull(firstVisibleIndex)?.let { appSeekLetter(it.appName) }
    // Only shown while the scrollbar is dragged, not during regular list scrolling.
    val visible = letter != null && scrollbarDragFraction != null
    val density = LocalDensity.current
    val bubbleColor = MaterialTheme.colorScheme.primaryContainer
    val letterColor = MaterialTheme.colorScheme.onPrimaryContainer

    BoxWithConstraints(
            modifier = modifier.fillMaxWidth().fillMaxHeight().zIndex(1f),
    ) {
        AnimatedVisibility(
                visible = visible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        ) {
            val resolvedLetter = letter ?: return@AnimatedVisibility
            val trackHeightPx = constraints.maxHeight.toFloat()
            val popupHeightPx = with(density) { LetterPopupSize.toPx() }
            val thumbOffsetPx =
                    letterPopupThumbCenterY(
                            trackHeightPx = trackHeightPx,
                            metrics = metrics,
                            scrollFraction = scrollbarDragFraction,
                            minThumbPx = with(density) { DesignTokens.Spacing48.toPx() },
                    )
            val yPx =
                    (thumbOffsetPx - popupHeightPx / 2f)
                            .coerceIn(0f, (trackHeightPx - popupHeightPx).coerceAtLeast(0f))
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                Row(
                        modifier =
                                Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(end = LetterPopupScrollbarGap)
                                        .offset { IntOffset(0, yPx.roundToInt()) }
                                        .height(LetterPopupSize),
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                Box(
                        modifier =
                                Modifier
                                        .size(LetterPopupSize)
                                        .shadow(DesignTokens.ElevationLevel3, AppShapes.full)
                                        .clip(AppShapes.full)
                                        .background(bubbleColor),
                        contentAlignment = Alignment.Center,
                ) {
                    Text(
                            text = resolvedLetter.toString(),
                            color = letterColor,
                            style = MaterialTheme.typography.displaySmall,
                    )
                }
                ScrollbarPointer(
                        color = bubbleColor,
                        modifier =
                                Modifier
                                        .offset(x = (-2).dp)
                                        .width(LetterPopupPointerWidth)
                                        .height(LetterPopupPointerHeight),
                )
                }
            }
        }
    }
}

@Composable
private fun ScrollbarPointer(
        color: Color,
        modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val path =
                Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, size.height / 2f)
                    lineTo(0f, size.height)
                    close()
                }
        drawPath(path, color)
    }
}

internal fun letterPopupThumbCenterY(
        trackHeightPx: Float,
        metrics: LazyGridScrollbarMetrics?,
        minThumbPx: Float,
        scrollFraction: Float? = null,
): Float {
    if (trackHeightPx <= 0f || metrics == null) return trackHeightPx / 2f
    val thumbHeightPx = thumbHeightPx(trackHeightPx, metrics, minThumbPx)
    val maxOffsetPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)
    return maxOffsetPx * (scrollFraction ?: metrics.scrollFraction) + thumbHeightPx / 2f
}

internal val AlphabetSeekLetters: List<Char> = buildList {
    add('#')
    addAll('A'..'Z')
}

internal fun appSeekLetter(appName: String): Char {
    val first = appName.trim().firstOrNull() ?: return '#'
    val mapped = first.toString().uppercase(Locale.ROOT).firstOrNull() ?: return '#'
    return if (mapped in 'A'..'Z') mapped else '#'
}

internal fun firstIndexBySeekLetter(appNames: List<String>): Map<Char, Int> {
    val map = linkedMapOf<Char, Int>()
    appNames.forEachIndexed { index, name ->
        val letter = appSeekLetter(name)
        if (letter !in map) {
            map[letter] = index
        }
    }
    return map
}

internal fun indexForSeekLetter(
        letter: Char,
        firstIndexByLetter: Map<Char, Int>,
): Int? {
    firstIndexByLetter[letter]?.let { return it }
    val start = AlphabetSeekLetters.indexOf(letter)
    if (start < 0) return firstIndexByLetter.values.minOrNull()
    for (i in start until AlphabetSeekLetters.size) {
        firstIndexByLetter[AlphabetSeekLetters[i]]?.let { return it }
    }
    for (i in start downTo 0) {
        firstIndexByLetter[AlphabetSeekLetters[i]]?.let { return it }
    }
    return null
}

internal fun letterAtTrackPosition(
        y: Float,
        trackHeightPx: Float,
        letters: List<Char>,
): Char {
    if (letters.isEmpty() || trackHeightPx <= 0f) return 'A'
    val index = ((y / trackHeightPx) * letters.size).toInt().coerceIn(0, letters.lastIndex)
    return letters[index]
}

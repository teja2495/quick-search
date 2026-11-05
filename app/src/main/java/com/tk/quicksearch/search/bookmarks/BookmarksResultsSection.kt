package com.tk.quicksearch.search.bookmarks

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.bookmarks.BookmarkFaviconStore
import com.tk.quicksearch.search.data.bookmarks.BookmarkUrls
import com.tk.quicksearch.search.models.BookmarkInfo
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandButton
import com.tk.quicksearch.search.searchScreen.components.ExpandButtonDivider
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.search.searchScreen.components.resultRowVerticalPadding
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.search.searchScreen.shared.compactEdgeToEdge
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ROW_MIN_HEIGHT_DP = 52

/** Callbacks and styling for the Bookmarks search section, provided once by the search screen. */
data class BookmarksSectionParams(
    val onBookmarkClick: (BookmarkInfo) -> Unit,
    val onExpandClick: () -> Unit,
    val showWallpaperBackground: Boolean,
    val expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
)

/**
 * Bookmarks params travel by composition local rather than through every layout layer's
 * parameter list; null outside the search screen.
 */
val LocalBookmarksSectionParams = compositionLocalOf<BookmarksSectionParams?> { null }

@Composable
fun BookmarksResultsSection(
    bookmarks: List<BookmarkInfo>,
    isExpanded: Boolean,
    showExpandControls: Boolean,
    params: BookmarksSectionParams,
    allowInternalScroll: Boolean = true,
) {
    if (bookmarks.isEmpty()) return

    val collapsed = bookmarks.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
    val overlayDividerColor = LocalOverlayDividerColor.current
    val scrollState = rememberScrollState()
    val shouldUseInternalScroll = isExpanded && allowInternalScroll

    ExpandableResultsCard(
        resultCount = bookmarks.size,
        hasAdditionalResults = bookmarks.size > collapsed.size,
        isExpanded = isExpanded,
        showAllResults = false,
        showExpandControls = showExpandControls,
        expandedCardMaxHeight = params.expandedCardMaxHeight,
        constrainExpandedHeight = allowInternalScroll,
        hasScrollableContent = shouldUseInternalScroll && scrollState.maxValue > 0,
        fillExpandedHeight = false,
        showWallpaperBackground = params.showWallpaperBackground,
        overlayCardColor = LocalOverlayResultCardColor.current,
    ) { contentModifier, cardState ->
        val displayed = if (cardState.displayAsExpanded) bookmarks else collapsed
        Column(
            modifier =
                contentModifier.then(
                    if (shouldUseInternalScroll) Modifier.verticalScroll(scrollState) else Modifier,
                ),
        ) {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = DesignTokens.SpacingMedium, vertical = 4.dp),
            ) {
                displayed.forEachIndexed { index, bookmark ->
                    key(bookmark.bookmarkId) {
                        BookmarkRow(
                            bookmark = bookmark,
                            onClick = params.onBookmarkClick,
                            isPredicted = false,
                        )
                        if (index < displayed.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.compactEdgeToEdge().fillMaxWidth(),
                                color =
                                    overlayDividerColor
                                        ?: if (params.showWallpaperBackground) {
                                            AppColors.WallpaperDivider
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                            )
                        }
                    }
                }

                if (cardState.shouldShowExpandButton) {
                    ExpandButtonDivider(params.showWallpaperBackground)
                    ExpandButton(
                        onClick = params.onExpandClick,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        textResId = R.string.action_expand_more_bookmarks,
                    )
                }
            }
        }
    }
}

@Composable
internal fun BookmarkRow(
    bookmark: BookmarkInfo,
    onClick: (BookmarkInfo) -> Unit,
    isPredicted: Boolean,
) {
    val rowView = LocalView.current
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT_DP.dp)
                .topPredictedRowContainer(isTopPredicted = isPredicted)
                .clickable {
                    hapticConfirm(rowView)()
                    onClick(bookmark)
                }
                .topPredictedRowContentPadding()
                .padding(vertical = resultRowVerticalPadding(12.dp)),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookmarkFavicon(bookmark = bookmark, modifier = Modifier.padding(start = 7.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = rememberQueryHighlightedText(bookmark.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = remember(bookmark.url) { BookmarkUrls.displayHost(bookmark.url) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The bookmark's saved favicon, or a globe while it loads or when the site has none. */
@Composable
fun BookmarkFavicon(
    bookmark: BookmarkInfo,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val context = LocalContext.current
    val favicon by
        produceState<ImageBitmap?>(null, bookmark.bookmarkId, bookmark.faviconVersion) {
            value =
                if (!bookmark.hasFavicon) {
                    null
                } else {
                    withContext(Dispatchers.IO) {
                        BookmarkFaviconStore(context)
                            .loadBitmap(bookmark.bookmarkId, bookmark.faviconVersion)
                            ?.asImageBitmap()
                    }
                }
        }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        val bitmap = favicon
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(size).clip(RoundedCornerShape(6.dp)),
            )
        } else {
            Icon(
                imageVector = MaterialSymbols.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size),
            )
        }
    }
}

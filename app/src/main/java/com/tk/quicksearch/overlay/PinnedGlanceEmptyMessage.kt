package com.tk.quicksearch.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import kotlinx.coroutines.delay

// Pinned items and At a Glance come from many independent sources, so emptiness is read from the
// measured content: with nothing in it, the search content is only its own padding.
private val EMPTY_CONTENT_MAX_HEIGHT = 64.dp

/** The popup card never gets shorter than this, so it doesn't open as a strip and then grow. */
internal val PinnedGlanceMinHeight = 140.dp

// Items can land a frame or two after opening; waiting keeps the text from flashing. The card
// already has its minimum height, so only the text fades in.
private const val EMPTY_SETTLE_MS = 250L

/**
 * "Nothing to show" for the notch's Pinned & At a Glance popup when it has no items, centered in
 * the card's [PinnedGlanceMinHeight].
 *
 * @param contentHeightPx measured height of the popup's search content, or -1 before layout.
 */
@Composable
internal fun PinnedGlanceEmptyMessage(contentHeightPx: Int) {
    val emptyMaxPx = with(LocalDensity.current) { EMPTY_CONTENT_MAX_HEIGHT.roundToPx() }
    val looksEmpty = contentHeightPx in 0 until emptyMaxPx
    var showMessage by remember { mutableStateOf(false) }
    LaunchedEffect(looksEmpty) {
        if (looksEmpty) {
            delay(EMPTY_SETTLE_MS)
            showMessage = true
        } else {
            showMessage = false
        }
    }
    AnimatedVisibility(visible = showMessage, enter = fadeIn(), exit = fadeOut()) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = PinnedGlanceMinHeight)
                .padding(top = OverlayBubbleArrowHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.overlay_pinned_glance_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    }
}

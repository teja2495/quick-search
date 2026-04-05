package com.tk.quicksearch.shared.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.tk.quicksearch.shared.ui.theme.AppColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tk.quicksearch.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A bottom-anchored popup dialog with a header and scrollable dark content card.
 *
 * Use this as the shell for any feature that needs a bottom popup with a titled header and a list
 * of actions or options. Pass feature-specific content via the [content] slot.
 *
 * @param onDismiss Called when the popup should be dismissed.
 * @param title Composable for the header title area. Fills the space between [leadingContent] and
 *   the close button.
 * @param modifier Modifier applied to the popup surface.
 * @param leadingContent Optional composable rendered before the title (e.g. an avatar or icon).
 * @param fixedTopContent Optional fixed content rendered at the top of the card, above the
 *   scrollable content.
 * @param maxInnerCardHeight Optional max height for the inner content card. If null, defaults to
 *   72% of screen height.
 * @param content Content rendered inside the scrollable dark card.
 */
@Composable
fun AppBottomPopup(
    onDismiss: () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    fixedTopContent: (@Composable () -> Unit)? = null,
    showFixedTopDivider: Boolean = true,
    maxInnerCardHeight: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val defaultMaxCardHeight = LocalConfiguration.current.screenHeightDp.dp * 0.72f
    val resolvedMaxCardHeight = maxInnerCardHeight ?: defaultMaxCardHeight
    val offsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val dismissThresholdPx = with(LocalDensity.current) { 150.dp.toPx() }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, offsetY.value.roundToInt()) }
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                coroutineScope.launch {
                                    offsetY.snapTo((offsetY.value + delta).coerceAtLeast(0f))
                                }
                            },
                            onDragStopped = {
                                if (offsetY.value >= dismissThresholdPx) {
                                    onDismiss()
                                } else {
                                    coroutineScope.launch {
                                        offsetY.animateTo(0f, spring())
                                    }
                                }
                            },
                        )
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = AppColors.getDrawerContainerColor(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (leadingContent != null) {
                            leadingContent()
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            title()
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.dialog_cancel),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().heightIn(max = resolvedMaxCardHeight),
                        colors = CardDefaults.cardColors(containerColor = AppColors.getSettingsCardContainerColor()),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        if (fixedTopContent == null) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(scrollState)
                                        .padding(
                                            start = 16.dp,
                                            top = 20.dp,
                                            end = 16.dp,
                                            bottom = 24.dp,
                                        ),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                content = content,
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxWidth().heightIn(max = resolvedMaxCardHeight)) {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = 16.dp,
                                                top = 20.dp,
                                                end = 16.dp,
                                                bottom = 12.dp,
                                            ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    fixedTopContent()
                                }
                                if (showFixedTopDivider) {
                                    HorizontalDivider()
                                }
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .weight(1f, fill = false)
                                            .verticalScroll(scrollState)
                                            .padding(
                                                start = 16.dp,
                                                top = 12.dp,
                                                end = 16.dp,
                                                bottom = 24.dp,
                                            ),
                                    verticalArrangement = Arrangement.spacedBy(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    content = content,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

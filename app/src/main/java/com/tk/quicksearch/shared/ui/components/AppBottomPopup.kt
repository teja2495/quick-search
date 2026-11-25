package com.tk.quicksearch.shared.ui.components

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.tk.quicksearch.shared.ui.theme.AppMotion

/**
 * A bottom-anchored popup dialog with a header and scrollable content card.
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
 * @param contentBottomPadding Padding below the scrollable content, inside the card.
 * @param contentHorizontalPadding Padding on both sides of the scrollable content, inside the card.
 * @param contentTopPadding Padding above scrollable content when there is no fixed top content.
 * @param bottomPadding Space between the popup's content and its bottom edge.
 * @param animateSizeChanges Animate content-driven height changes while keeping the bottom anchored.
 * @param showCloseButton Whether the header shows a close button after the title.
 * @param bottomOverlay Optional content drawn over the popup's bottom edge, clipped to its shape.
 * @param content Content rendered inside the scrollable card.
 */
@Composable
fun AppBottomPopup(
    onDismiss: () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AppColors.DialogBackground,
    contentCardColor: Color = AppColors.getSettingsCardContainerColor(),
    leadingContent: (@Composable () -> Unit)? = null,
    aboveCardContent: (@Composable () -> Unit)? = null,
    fixedTopContent: (@Composable () -> Unit)? = null,
    showFixedTopDivider: Boolean = true,
    maxInnerCardHeight: Dp? = null,
    innerCardHeight: Dp? = null,
    drawerHeight: Dp? = null,
    contentSpacing: Dp = 24.dp,
    bottomPadding: Dp = DesignTokens.SpacingXXLarge,
    contentTopPadding: Dp = 20.dp,
    contentBottomPadding: Dp = 24.dp,
    contentHorizontalPadding: Dp = 16.dp,
    contentScrollable: Boolean = true,
    bottomOverlay: (@Composable BoxScope.() -> Unit)? = null,
    showCloseButton: Boolean = true,
    animateSizeChanges: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val defaultMaxCardHeight = LocalConfiguration.current.screenHeightDp.dp * 0.72f
    val resolvedMaxCardHeight = maxInnerCardHeight ?: defaultMaxCardHeight
    val offsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val dismissThresholdPx = with(LocalDensity.current) { 150.dp.toPx() }
    val hostKeyboardOpen = rememberHostKeyboardOpen()
    PreserveHostKeyboardStateEffect()
    ReportOpenPopupEffect()

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .safeDrawingPaddingIgnoringHostKeyboard(hostKeyboardOpen),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // The scrim is a sibling of the sheet. Back and the header close action handle dismissal
            // for accessibility without adding an invisible focus stop to the dialog.
            Box(Modifier.matchParentSize().pointerInput(onDismiss) {
                detectTapGestures(onTap = { onDismiss() })
            })
            Surface(
                modifier =
                    modifier
                        .widthIn(max = DesignTokens.ItemMenuMaxWidth)
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
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
                                        offsetY.animateTo(0f, AppMotion.defaultSpatial())
                                    }
                                }
                            },
                        )
                        .padding(DesignTokens.SpacingSmall),
                shape = AppShapes.extraLarge,
                color = containerColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Box {
                    Column(
                        modifier =
                            Modifier
                                .then(
                                    if (animateSizeChanges) {
                                        Modifier.animateContentSize(
                                            animationSpec = AppMotion.defaultSpatial(),
                                            alignment = Alignment.BottomCenter,
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .fillMaxWidth()
                                .padding(
                                    start = DesignTokens.ContentHorizontalPadding,
                                    top = DesignTokens.SpacingLarge,
                                    end = DesignTokens.ContentHorizontalPadding,
                                    bottom = bottomPadding,
                                )
                                .then(
                                    if (drawerHeight != null) Modifier.height(drawerHeight)
                                    else Modifier,
                                ),
                        verticalArrangement = Arrangement.spacedBy(contentSpacing),
                    ) {
                        AppDrawerHeader(
                            onClose = onDismiss.takeIf { showCloseButton },
                            leadingContent = leadingContent,
                            title = title,
                        )

                        aboveCardContent?.invoke()

                        Card(
                            modifier =
                                Modifier.fillMaxWidth().weight(1f, fill = drawerHeight != null).then(
                                    when {
                                        drawerHeight != null -> Modifier
                                        innerCardHeight != null -> Modifier.height(innerCardHeight)
                                        else -> Modifier.heightIn(max = resolvedMaxCardHeight)
                                    },
                                ),
                            colors = CardDefaults.cardColors(containerColor = contentCardColor),
                            shape = AppShapes.large,
                        ) {
                            if (fixedTopContent == null) {
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (contentScrollable) Modifier.verticalScroll(scrollState)
                                                    else Modifier.fillMaxSize(),
                                                )
                                            .padding(
                                                start = contentHorizontalPadding,
                                                top = contentTopPadding,
                                                end = contentHorizontalPadding,
                                                bottom = contentBottomPadding,
                                            ),
                                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXLarge),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    content = content,
                                )
                            } else {
                                Column(
                                    modifier =
                                        Modifier.fillMaxWidth().then(
                                            if (innerCardHeight != null) Modifier.height(innerCardHeight)
                                            else Modifier.heightIn(max = resolvedMaxCardHeight),
                                        ),
                                ) {
                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    start = DesignTokens.SpacingLarge,
                                                    top = DesignTokens.SpacingXLarge,
                                                    end = DesignTokens.SpacingLarge,
                                                    bottom = DesignTokens.SpacingMedium,
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
                                                .weight(1f, fill = !contentScrollable)
                                                .then(
                                                    if (contentScrollable) Modifier.verticalScroll(scrollState)
                                                    else Modifier,
                                                )
                                                .padding(
                                                    start = DesignTokens.SpacingLarge,
                                                    top = DesignTokens.SpacingMedium,
                                                    end = DesignTokens.SpacingLarge,
                                                    bottom = contentBottomPadding,
                                                ),
                                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXLarge),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        content = content,
                                    )
                                }
                            }
                        }
                    }
                    bottomOverlay?.invoke(this)
                }
            }
            // Dialogs are separate windows, so screen-level overlays (e.g. the undo snackbar)
            // must be drawn here to appear above the popup.
            LocalPopupOverlayContent.current?.invoke(this)
        }
    }
}

/** Content drawn above every [AppBottomPopup], in the popup's own window. */
val LocalPopupOverlayContent = staticCompositionLocalOf<(@Composable BoxScope.() -> Unit)?> { null }

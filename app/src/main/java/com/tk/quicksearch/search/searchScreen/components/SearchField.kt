package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticStrong
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PersistentSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    dismissKeyboardBeforeSettingsClick: Boolean = false,
    enabledTargets: List<SearchTarget>,
    onSearchAction: () -> Unit,
    shouldUseNumberKeyboard: Boolean,
    detectedShortcutTarget: SearchTarget? = null,
    showWelcomeAnimation: Boolean = false,
    opaqueBackground: Boolean = false,
    onClearDetectedShortcut: () -> Unit = {},
    onWelcomeAnimationCompleted: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    val searchBarBackground =
        if (opaqueBackground) {
            AppColors.DialogBackground
        } else {
            AppColors.SearchBarBackground
        }
    // Light color for icons and text on dark grey background
    val iconAndTextColor = DesignTokens.ColorSearchText

    // Local text field value maintains cursor position even when state query changes from voice
    // input.
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(query, TextRange(query.length)))
    }

    LaunchedEffect(query) {
        if (query != textFieldValue.text) {
            textFieldValue =
                textFieldValue.copy(
                    text = query,
                    selection = TextRange(query.length),
                )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Animation state
    // We use a linear progression 0 -> 1 to scan the gradient exactly once
    val animationProgress = remember { Animatable(0f) }

    val glowAlpha = remember { Animatable(0f) }
    // If we aren't showing the welcome animation, start with the standard UI (0.3f alpha)
    val borderAlpha =
        remember {
            Animatable(
                if (showWelcomeAnimation) 0f else DesignTokens.SearchFieldBorderAlphaDefault,
            )
        }

    LaunchedEffect(showWelcomeAnimation) {
        if (showWelcomeAnimation) {
            // Setup Start State
            glowAlpha.snapTo(1f)
            borderAlpha.snapTo(0f)
            animationProgress.snapTo(0f)

            // Phase 1: Animate the gradient flow (0 -> 1)
            // This scans the colors and arrives at the end (White)
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(DesignTokens.AnimationDurationLong, easing = LinearEasing),
            )

            // Phase 2: Arrived at White. Make it permanent.
            // We DO NOT snap border to 1f. We rely on the White Brush from Phase 1 to
            // hold the
            // white state.
            // This maintains the "Glow" look during the hold.

            // Hold for a tiny beat (imperceptible, just ensures scan completion)
            delay(DesignTokens.AnimationDurationMicro.toLong())

            // Phase 3: Dissipate Heat / Cool Down
            // Quicker fade out (500ms) to prevent lingering
            launch {
                glowAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(DesignTokens.AnimationDurationFast, easing = LinearOutSlowInEasing),
                )
            }
            launch {
                borderAlpha.animateTo(
                    targetValue = DesignTokens.SearchFieldBorderAlphaDefault,
                    animationSpec = tween(DesignTokens.AnimationDurationFast, easing = LinearOutSlowInEasing),
                )
            }

            // Wait for fade out to complete, then reset the animation flag
            delay(DesignTokens.AnimationDurationFast.toLong())
            onWelcomeAnimationCompleted?.invoke()
        }
    }
    // Palettes are centralized in AppColors to keep color tokens out of feature files.
    val activeColors = AppColors.SearchFieldGooglePalette

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer() // GPU acceleration
                .drawBehind {
                    val alpha = glowAlpha.value
                    if (alpha > 0f) {
                        val strokeWidth = DesignTokens.SearchFieldBorderWidth.toPx()
                        val cornerRadiusVal = DesignTokens.Spacing28.toPx()

                        // Calculate gradient movement based on animation
                        // progress
                        // We want to SCAN the gradient from Start (Colors)
                        // to End
                        // (White)
                        // At t=0, we want offset=0 (Start of colors aligned
                        // with left
                        // edge)
                        // At t=1, we want to look at the End (White).
                        // So we slide the brush to the LEFT (negative
                        // offset) until the
                        // end is visible.

                        val gradientWidth = size.width * DesignTokens.SearchFieldGradientWidthMultiplier

                        // xOffset moves from 0 down to -3*width.
                        // At -3*width, the brush starts 3 screens to the
                        // left.
                        // The visible part [0, width] is at offset +3*width
                        // = [3*width,
                        // 4*width] of the gradient.
                        // This is the last 25% of the gradient, which is
                        // White.
                        val xOffset =
                            -(animationProgress.value * size.width * DesignTokens.SearchFieldGradientTravelMultiplier)

                        val brush =
                            Brush.linearGradient(
                                colors = activeColors,
                                start = Offset(xOffset, 0f),
                                end =
                                    Offset(
                                        xOffset +
                                            gradientWidth,
                                        0f,
                                    ),
                                // Tilt slightly for more dynamic
                                // look? No,
                                // straight looks cleaner for border
                            )

                        // 1. Draw "Outer Glow" (Simulated Blur)
                        // We draw wider, lower alpha strokes behind
                        drawRoundRect(
                            brush = brush,
                            cornerRadius =
                                CornerRadius(cornerRadiusVal),
                            style =
                                Stroke(
                                    width = strokeWidth * 4f,
                                ), // Wide spill
                            alpha = alpha * 0.3f, // Low opacity
                        )
                        drawRoundRect(
                            brush = brush,
                            cornerRadius =
                                CornerRadius(cornerRadiusVal),
                            style =
                                Stroke(
                                    width = strokeWidth * 2f,
                                ), // Medium spill
                            alpha = alpha * 0.5f,
                        )

                        // 2. Draw "Core" sharp line
                        drawRoundRect(
                            brush = brush,
                            cornerRadius =
                                CornerRadius(cornerRadiusVal),
                            style = Stroke(width = strokeWidth),
                            alpha = alpha,
                        )
                    }
                }.border(
                    width = DesignTokens.SearchFieldBorderWidth,
                    color = AppColors.DialogText.copy(alpha = borderAlpha.value),
                    shape = DesignTokens.ShapeXXLarge,
                ).clip(DesignTokens.ShapeXXLarge)
                .background(searchBarBackground),
    ) {
        TextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onQueryChange(newValue.text)
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { keyEvent ->
                        if (
                            keyEvent.type == KeyEventType.KeyDown &&
                                keyEvent.key == Key.Backspace &&
                                textFieldValue.text.isEmpty() &&
                                detectedShortcutTarget != null
                        ) {
                            onClearDetectedShortcut()
                            true
                        } else {
                            false
                        }
                    }
                    .animateContentSize(),
            shape = DesignTokens.ShapeXXLarge,
            placeholder = {
                Text(
                    text = stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.titleMedium,
                    color = iconAndTextColor.copy(alpha = DesignTokens.SearchFieldPlaceholderAlpha),
                )
            },
            textStyle =
                MaterialTheme.typography.titleMedium.copy(color = iconAndTextColor),
            singleLine = false,
            maxLines = 3,
            leadingIcon = {
                if (detectedShortcutTarget != null) {
                    SearchTargetIcon(
                        target = detectedShortcutTarget,
                        iconSize = DesignTokens.IconSize,
                        style = IconRenderStyle.ADVANCED,
                        modifier = Modifier.padding(start = DesignTokens.SpacingSmall),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription =
                            stringResource(R.string.desc_search_icon),
                        tint = iconAndTextColor,
                        modifier =
                            Modifier.padding(
                                start = DesignTokens.SpacingXSmall,
                            ),
                    )
                }
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                    modifier =
                        Modifier.padding(end = DesignTokens.SpacingXSmall),
                ) {
                    // Show X icon when query is not empty, otherwise show
                    // settings icon
                    // (whether shortcut is detected or not when query is empty)
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClearQuery) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription =
                                    stringResource(
                                        R.string
                                            .desc_clear_search,
                                    ),
                                tint = iconAndTextColor,
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                hapticStrong(view)()
                                if (dismissKeyboardBeforeSettingsClick) {
                                    view.clearFocus()
                                    keyboardController?.hide()
                                }
                                onSettingsClick()
                            },
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Rounded.Settings,
                                contentDescription =
                                    stringResource(
                                        R.string
                                            .desc_open_settings,
                                    ),
                                tint = iconAndTextColor,
                            )
                        }
                    }
                }
            },
            keyboardOptions =
                KeyboardOptions(
                    imeAction = ImeAction.Search,
                    keyboardType =
                        if (shouldUseNumberKeyboard) {
                            KeyboardType.Number
                        } else {
                            KeyboardType.Text
                        },
                ),
            keyboardActions =
                KeyboardActions(
                    onSearch = {
                        onSearchAction()
                        if (query.isNotBlank()) {
                            // Only hide keyboard if the first engine is
                            // not
                            // DIRECT_ANSWER
                            val firstTarget =
                                enabledTargets.firstOrNull()
                            val keepKeyboard =
                                (
                                    firstTarget as?
                                        SearchTarget.Engine
                                )?.engine ==
                                    SearchEngine.DIRECT_SEARCH
                            if (!keepKeyboard) {
                                keyboardController?.hide()
                            }
                        }
                    },
                ),
            colors =
                TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedTextColor = iconAndTextColor,
                    unfocusedTextColor = iconAndTextColor,
                ),
        )
    }
}

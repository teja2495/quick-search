package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.Shortcut
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchToolType
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticStrong
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private const val AliasIconMorphDurationMs = 260
private const val AliasIconMorphEndScale = 0.5f
private const val LeadingIconEnterDurationMs = 180
private const val LeadingIconExitDurationMs = 120
private const val LeadingIconEnterDelayMs = 40
private const val LeadingIconEnterInitialScale = 0.88f
private const val LeadingIconExitTargetScale = 0.88f
private val AliasMorphTextStartPadding = DesignTokens.Spacing48 + DesignTokens.SpacingXSmall
private val AliasMorphHorizontalTravel = DesignTokens.Spacing28
private val AliasMorphVerticalTravel = DesignTokens.SpacingXXSmall

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PersistentSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    dismissKeyboardBeforeSettingsClick: Boolean = false,
    enabledTargets: List<SearchTarget>,
    shortcutCodes: Map<String, String> = emptyMap(),
    shortcutEnabled: Map<String, Boolean> = emptyMap(),
    isSearchEngineAliasSuffixEnabled: Boolean = true,
    onSearchAction: () -> Unit,
    shouldUseNumberKeyboard: Boolean,
    detectedShortcutTarget: SearchTarget? = null,
    detectedAliasSearchSection: SearchSection? = null,
    activeToolType: SearchToolType? = null,
    isCalculatorMode: Boolean = false,
    placeholderText: String,
    showWelcomeAnimation: Boolean = false,
    opaqueBackground: Boolean = false,
    autoFocusOnStart: Boolean = false,
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
    val isAliasDetected =
        detectedShortcutTarget != null || detectedAliasSearchSection != null || activeToolType != null
    val aliasVisualTransformation =
        rememberAliasHighlightVisualTransformation(
            enabledTargets = enabledTargets,
            shortcutCodes = shortcutCodes,
            shortcutEnabled = shortcutEnabled,
            isSearchEngineAliasSuffixEnabled = isSearchEngineAliasSuffixEnabled && !isAliasDetected,
            highlightColor = MaterialTheme.colorScheme.primary,
        )
    val leadingIconState =
        when {
            activeToolType == SearchToolType.UNIT_CONVERTER -> LeadingIconState.UnitConverter
            activeToolType == SearchToolType.CALCULATOR || isCalculatorMode -> LeadingIconState.Calculator
            detectedShortcutTarget != null -> LeadingIconState.Shortcut(detectedShortcutTarget)
            detectedAliasSearchSection != null -> LeadingIconState.Section(detectedAliasSearchSection)
            else -> LeadingIconState.Search
        }
    val activePrefixAliases =
        remember(shortcutCodes, shortcutEnabled) {
            shortcutCodes.entries
                .asSequence()
                .filter { (id, code) -> code.isNotBlank() && (shortcutEnabled[id] != false) }
                .map { (_, code) -> code.trim().lowercase(Locale.getDefault()) }
                .filter { it.isNotEmpty() }
                .toSet()
        }

    // Local text field value maintains cursor position even when state query changes from voice
    // input.
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(query, TextRange(query.length)))
    }
    var hasLaidOutSearchField by remember { mutableStateOf(false) }
    val aliasMorphProgress = remember { Animatable(1f) }
    var aliasMorphText by remember { mutableStateOf<String?>(null) }
    var previousLeadingIconState by remember { mutableStateOf(leadingIconState) }

    LaunchedEffect(query, leadingIconState) {
        val previousText = textFieldValue.text
        if (query != previousText) {
            val shouldAnimateAliasMorph =
                leadingIconState !is LeadingIconState.Search &&
                    previousLeadingIconState != leadingIconState
            if (shouldAnimateAliasMorph) {
                detectConsumedPrefixAlias(
                    previousText = previousText,
                    currentQuery = query,
                    activePrefixAliases = activePrefixAliases,
                )?.let { consumedAlias ->
                    aliasMorphText = consumedAlias
                }
            }
            textFieldValue =
                textFieldValue.copy(
                    text = query,
                    selection = TextRange(query.length),
                )
        }
        previousLeadingIconState = leadingIconState
    }

    LaunchedEffect(aliasMorphText) {
        if (aliasMorphText == null) return@LaunchedEffect
        aliasMorphProgress.snapTo(0f)
        aliasMorphProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = AliasIconMorphDurationMs, easing = LinearOutSlowInEasing),
        )
        aliasMorphText = null
    }

    LaunchedEffect(autoFocusOnStart, hasLaidOutSearchField) {
        if (autoFocusOnStart && hasLaidOutSearchField) {
            // Wait for a rendered frame before focusing to avoid startup contention.
            withFrameNanos { }
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    if (autoFocusOnStart) {
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
    val density = LocalDensity.current
    val aliasMorphHorizontalTravelPx = with(density) { AliasMorphHorizontalTravel.toPx() }
    val aliasMorphVerticalTravelPx = with(density) { AliasMorphVerticalTravel.toPx() }

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
                    .onGloballyPositioned {
                        if (!hasLaidOutSearchField) {
                            hasLaidOutSearchField = true
                        }
                    }
                    .onPreviewKeyEvent { keyEvent ->
                        if (
                                keyEvent.type == KeyEventType.KeyDown &&
                                keyEvent.key == Key.Backspace &&
                                textFieldValue.text.isEmpty() &&
                                (detectedShortcutTarget != null ||
                                    detectedAliasSearchSection != null ||
                                    isCalculatorMode)
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
                    text = placeholderText,
                    style = MaterialTheme.typography.titleMedium,
                    color = iconAndTextColor.copy(alpha = DesignTokens.SearchFieldPlaceholderAlpha),
                )
            },
            textStyle =
                MaterialTheme.typography.titleMedium.copy(color = iconAndTextColor),
            singleLine = false,
            maxLines = 3,
            leadingIcon = {
                AnimatedContent(
                    targetState = leadingIconState,
                    transitionSpec = {
                        (
                            fadeIn(
                                animationSpec =
                                    tween(
                                        durationMillis = LeadingIconEnterDurationMs,
                                        delayMillis = LeadingIconEnterDelayMs,
                                        easing = LinearOutSlowInEasing,
                                    ),
                            ) +
                                scaleIn(
                                    animationSpec =
                                        tween(
                                            durationMillis = LeadingIconEnterDurationMs,
                                            delayMillis = LeadingIconEnterDelayMs,
                                            easing = LinearOutSlowInEasing,
                                        ),
                                    initialScale = LeadingIconEnterInitialScale,
                                )
                            )
                            .togetherWith(
                                fadeOut(
                                    animationSpec = tween(durationMillis = LeadingIconExitDurationMs),
                                ) +
                                    scaleOut(
                                        animationSpec = tween(durationMillis = LeadingIconExitDurationMs),
                                        targetScale = LeadingIconExitTargetScale,
                                    ),
                            )
                    },
                    label = "search_bar_leading_icon",
                ) { currentIconState ->
                    SearchBarLeadingIcon(
                        iconState = currentIconState,
                        iconTint = iconAndTextColor,
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
            visualTransformation = aliasVisualTransformation,
        )

        val animatedAliasText = aliasMorphText
        if (animatedAliasText != null) {
            val progress = aliasMorphProgress.value.coerceIn(0f, 1f)
            val scale = 1f - ((1f - AliasIconMorphEndScale) * progress)
            Text(
                text = animatedAliasText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = AliasMorphTextStartPadding)
                        .graphicsLayer {
                            translationX = -aliasMorphHorizontalTravelPx * progress
                            translationY = -aliasMorphVerticalTravelPx * progress
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - progress
                        },
            )
        }
    }
}

@Composable
private fun SearchBarLeadingIcon(
    iconState: LeadingIconState,
    iconTint: Color,
) {
    when (iconState) {
        LeadingIconState.Calculator -> {
            Icon(
                imageVector = Icons.Rounded.Calculate,
                contentDescription = stringResource(R.string.calculator_toggle_title),
                tint = iconTint,
                modifier = Modifier.padding(start = DesignTokens.SpacingXSmall),
            )
        }

        LeadingIconState.UnitConverter -> {
            Icon(
                imageVector = Icons.Rounded.Straighten,
                contentDescription = stringResource(R.string.unit_converter_toggle_title),
                tint = iconTint,
                modifier = Modifier.padding(start = DesignTokens.SpacingXSmall),
            )
        }

        is LeadingIconState.Shortcut -> {
            SearchTargetIcon(
                target = iconState.target,
                iconSize = DesignTokens.IconSize,
                style = IconRenderStyle.ADVANCED,
                modifier = Modifier.padding(start = DesignTokens.SpacingSmall),
            )
        }

        is LeadingIconState.Section -> {
            Icon(
                imageVector =
                    when (iconState.section) {
                        SearchSection.APPS -> Icons.Rounded.Apps
                        SearchSection.APP_SHORTCUTS -> Icons.AutoMirrored.Rounded.Shortcut
                        SearchSection.CONTACTS -> Icons.Rounded.Person
                        SearchSection.FILES -> Icons.AutoMirrored.Rounded.InsertDriveFile
                        SearchSection.SETTINGS -> Icons.Rounded.Settings
                        SearchSection.CALENDAR -> Icons.Rounded.CalendarMonth
                    },
                contentDescription = stringResource(R.string.desc_search_icon),
                tint = iconTint,
                modifier = Modifier.padding(start = DesignTokens.SpacingXSmall),
            )
        }

        LeadingIconState.Search -> {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = stringResource(R.string.desc_search_icon),
                tint = iconTint,
                modifier = Modifier.padding(start = DesignTokens.SpacingXSmall),
            )
        }
    }
}

private sealed interface LeadingIconState {
    data object Search : LeadingIconState

    data object Calculator : LeadingIconState

    data object UnitConverter : LeadingIconState

    data class Shortcut(
        val target: SearchTarget,
    ) : LeadingIconState

    data class Section(
        val section: SearchSection,
    ) : LeadingIconState
}

private fun detectConsumedPrefixAlias(
    previousText: String,
    currentQuery: String,
    activePrefixAliases: Set<String>,
): String? {
    if (activePrefixAliases.isEmpty()) return null
    val queryWithNoLeadingWhitespace = previousText.trimStart()
    if (queryWithNoLeadingWhitespace.isEmpty()) return null
    val separatorIndex = queryWithNoLeadingWhitespace.indexOfFirst { it.isWhitespace() }
    if (separatorIndex <= 0) return null

    val aliasToken = queryWithNoLeadingWhitespace.substring(0, separatorIndex)
    val aliasRemainder = queryWithNoLeadingWhitespace.substring(separatorIndex).trimStart()
    if (aliasRemainder != currentQuery) return null

    val normalizedAlias = aliasToken.lowercase(Locale.getDefault())
    if (normalizedAlias !in activePrefixAliases) return null
    return aliasToken
}

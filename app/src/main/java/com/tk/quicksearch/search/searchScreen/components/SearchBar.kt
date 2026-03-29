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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchToolType
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
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
private const val LightSearchBarShadowAmbientAlpha = 0.38f
private const val LightSearchBarShadowSpotAlpha = 0.62f
private val AliasMorphTextStartPadding = DesignTokens.Spacing48 + DesignTokens.SpacingXSmall
private val AliasMorphHorizontalTravel = DesignTokens.Spacing28
private val AliasMorphVerticalTravel = DesignTokens.SpacingXXSmall

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PersistentSearchBar(
    query: String,
    selectRetainedQuery: Boolean,
    onSelectRetainedQueryHandled: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    dismissKeyboardBeforeSettingsClick: Boolean = false,
    enabledTargets: List<SearchTarget>,
    shortcutCodes: Map<String, String> = emptyMap(),
    shortcutEnabled: Map<String, Boolean> = emptyMap(),
    isSearchEngineAliasSuffixEnabled: Boolean = true,
    onSearchAction: () -> Boolean,
    shouldUseNumberKeyboard: Boolean,
    detectedShortcutTarget: SearchTarget? = null,
    detectedAliasSearchSection: SearchSection? = null,
    activeToolType: SearchToolType? = null,
    isCalculatorMode: Boolean = false,
    placeholderText: String,
    showWelcomeAnimation: Boolean = false,
    showWallpaperBackground: Boolean = false,
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
    val accentColor = AppColors.Accent
    val iconAndTextColor = AppColors.SearchBarTextAndIcon
    val isDarkTheme = LocalAppIsDarkTheme.current
    val lightWallpaperSearchBar = !isDarkTheme && showWallpaperBackground
    val searchBarIconColor = AppColors.SecondaryIconTint
    val isAliasDetected =
        detectedShortcutTarget != null || detectedAliasSearchSection != null || activeToolType != null
    val aliasVisualTransformation =
        rememberAliasHighlightVisualTransformation(
            enabledTargets = enabledTargets,
            shortcutCodes = shortcutCodes,
            shortcutEnabled = shortcutEnabled,
            isSearchEngineAliasSuffixEnabled = isSearchEngineAliasSuffixEnabled && !isAliasDetected,
            highlightColor = AppColors.LinkColor,
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

    LaunchedEffect(selectRetainedQuery, query) {
        if (!selectRetainedQuery) return@LaunchedEffect
        if (query.isNotEmpty()) {
            textFieldValue =
                textFieldValue.copy(
                    text = query,
                    selection = TextRange(0, query.length),
                )
        }
        onSelectRetainedQueryHandled()
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
    val borderAlpha =
        remember {
            Animatable(
                if (showWelcomeAnimation) 0f else 1f,
            )
        }

    LaunchedEffect(showWelcomeAnimation, query.isEmpty()) {
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
                    targetValue = 1f,
                    animationSpec = tween(DesignTokens.AnimationDurationFast, easing = LinearOutSlowInEasing),
                )
            }

            // Wait for fade out to complete, then reset the animation flag
            delay(DesignTokens.AnimationDurationFast.toLong())
            onWelcomeAnimationCompleted?.invoke()
        } else if (query.isEmpty()) {
            // Temporarily disabled: idle border alpha pulse (fade in/out), not the welcome gradient.
            // while (true) {
            //     borderAlpha.animateTo(
            //         targetValue = 0.60f,
            //         animationSpec = tween(durationMillis = 4000, easing = LinearOutSlowInEasing)
            //     )
            //     borderAlpha.animateTo(
            //         targetValue = 0f,
            //         animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing)
            //     )
            // }
            borderAlpha.snapTo(1f)
        } else {
            borderAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = DesignTokens.AnimationDurationFast, easing = LinearOutSlowInEasing)
            )
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
                .graphicsLayer {
                    if (!isDarkTheme) {
                        val elevationDp =
                            if (lightWallpaperSearchBar) {
                                DesignTokens.ElevationLevel5
                            } else {
                                DesignTokens.ElevationLevel4
                            }
                        shadowElevation = with(density) { elevationDp.toPx() }
                        shape = DesignTokens.ShapeXXLarge
                        ambientShadowColor = accentColor.copy(alpha = LightSearchBarShadowAmbientAlpha)
                        spotShadowColor = accentColor.copy(alpha = LightSearchBarShadowSpotAlpha)
                    } else {
                        shadowElevation = 0f
                    }
                }
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
                }.then(
                    if (lightWallpaperSearchBar) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = DesignTokens.SearchFieldRestingOutlineWidth,
                            color =
                                accentColor.copy(
                                    alpha =
                                        (borderAlpha.value * DesignTokens.SearchFieldAccentOutlineAlpha)
                                            .coerceIn(0f, 1f),
                                ),
                            shape = DesignTokens.ShapeXXLarge,
                        )
                    },
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
                        when {
                            keyEvent.type == KeyEventType.KeyDown &&
                                keyEvent.key == Key.Backspace &&
                                textFieldValue.text.isEmpty() &&
                                (detectedShortcutTarget != null ||
                                    detectedAliasSearchSection != null ||
                                    activeToolType != null ||
                                    isCalculatorMode) -> {
                                onClearDetectedShortcut()
                                true
                            }
                            keyEvent.type == KeyEventType.KeyUp &&
                                (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) -> {
                                val keepKeyboardFromAction = onSearchAction()
                                if (!keepKeyboardFromAction && query.isNotBlank()) {
                                    val firstTarget = enabledTargets.firstOrNull()
                                    val keepKeyboard =
                                        (firstTarget as? SearchTarget.Engine)?.engine ==
                                            SearchEngine.DIRECT_SEARCH
                                    if (!keepKeyboard) {
                                        keyboardController?.hide()
                                    }
                                }
                                true
                            }
                            else -> false
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
                        iconTint = searchBarIconColor,
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
                    if (isAliasDetected || query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (query.isNotEmpty()) {
                                    onClearQuery()
                                } else if (isAliasDetected) {
                                    onClearDetectedShortcut()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription =
                                    stringResource(
                                        R.string
                                            .desc_clear_search,
                                    ),
                                tint = accentColor,
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
                                tint = searchBarIconColor,
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
                        val keepKeyboardFromAction = onSearchAction()
                        if (!keepKeyboardFromAction && query.isNotBlank()) {
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
                    unfocusedIndicatorColor = AppColors.AppBackgroundTransparent,
                    focusedIndicatorColor = AppColors.AppBackgroundTransparent,
                    disabledIndicatorColor = AppColors.AppBackgroundTransparent,
                    focusedContainerColor = AppColors.AppBackgroundTransparent,
                    unfocusedContainerColor = AppColors.AppBackgroundTransparent,
                    disabledContainerColor = AppColors.AppBackgroundTransparent,
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
                color = AppColors.LinkColor,
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
            if (iconState.section == SearchSection.APP_SETTINGS) {
                val appIconResult = rememberAppIcon(
                    packageName = "com.tk.quicksearch",
                    iconPackPackage = null,
                )
                val bitmap = appIconResult.bitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(R.string.desc_search_icon),
                        modifier = Modifier
                            .padding(start = DesignTokens.SpacingSmall)
                            .size(DesignTokens.IconSize),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.desc_search_icon),
                        tint = iconTint,
                        modifier = Modifier.padding(start = DesignTokens.SpacingXSmall),
                    )
                }
            } else {
                Icon(
                    imageVector =
                        when (iconState.section) {
                            SearchSection.APPS -> Icons.Rounded.Apps
                            SearchSection.APP_SHORTCUTS -> Icons.AutoMirrored.Rounded.Shortcut
                            SearchSection.CONTACTS -> Icons.Rounded.Person
                            SearchSection.FILES -> Icons.AutoMirrored.Rounded.InsertDriveFile
                            SearchSection.SETTINGS -> Icons.Rounded.Settings
                            SearchSection.CALENDAR -> Icons.Rounded.CalendarMonth
                            SearchSection.APP_SETTINGS -> Icons.Rounded.Settings
                        },
                    contentDescription = stringResource(R.string.desc_search_icon),
                    tint = iconTint,
                    modifier = Modifier.padding(start = DesignTokens.SpacingXSmall),
                )
            }
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

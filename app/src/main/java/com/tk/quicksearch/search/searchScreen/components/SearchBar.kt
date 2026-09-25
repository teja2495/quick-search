package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.rounded.Shortcut
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchToolType
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.app.startup.StartupTrace
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.util.hapticStrong
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
private const val AliasIconMorphDurationMs = 260
private const val LeadingIconEnterDurationMs = 180
private const val LeadingIconExitDurationMs = 120
private const val LeadingIconEnterDelayMs = 40
private const val LeadingIconEnterInitialScale = 0.88f
private const val LeadingIconExitTargetScale = 0.88f
private const val PlaceholderHintTransitionDurationMs = 700
private const val PlaceholderHintTransitionDelayMs = 120
private const val LightSearchBarShadowAmbientAlpha = 0.38f
private const val LightSearchBarShadowSpotAlpha = 0.62f
/** Resting radius of the standalone bar, matching [DesignTokens.ShapeXXLarge]. */
private val DefaultSearchBarCornerRadius = DesignTokens.Spacing28

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
    showSettingsIcon: Boolean = true,
    dismissKeyboardBeforeSettingsClick: Boolean = false,
    enabledTargets: List<SearchTarget>,
    shortcutCodes: Map<String, String> = emptyMap(),
    shortcutEnabled: Map<String, Boolean> = emptyMap(),
    triggerWords: Collection<String> = emptyList(),
    isSearchEngineAliasSuffixEnabled: Boolean = true,
    onSearchAction: () -> Boolean,
    onMoveTopResultSelectionUp: (() -> Boolean)? = null,
    onMoveTopResultSelectionDown: (() -> Boolean)? = null,
    shouldUseNumberKeyboard: Boolean,
    detectedShortcutTarget: SearchTarget? = null,
    detectedAliasSearchSection: SearchSection? = null,
    isCurrencyConverterAliasMode: Boolean = false,
    isWorldClockAliasMode: Boolean = false,
    isDictionaryAliasMode: Boolean = false,
    isWeatherAliasMode: Boolean = false,
    detectedCustomToolId: String? = null,
    detectedTaskerIntentId: String? = null,
    activeToolType: SearchToolType? = null,
    isCalculatorMode: Boolean = false,
    placeholderText: String,
    showWelcomeAnimation: Boolean = false,
    showWallpaperBackground: Boolean = false,
    opaqueBackground: Boolean = false,
    forceRestingOutline: Boolean = false,
    autoFocusOnStart: Boolean = false,
    releaseFocusOnLeave: Boolean = false,
    restoreKeyboardOnEnter: Boolean = false,
    onRestoreKeyboardHandled: () -> Unit = {},
    startupSurfaceReady: Boolean = true,
    onClearDetectedShortcut: () -> Unit = {},
    onSectionSelected: (SearchSection) -> Unit = {},
    onWelcomeAnimationCompleted: (() -> Unit)? = null,
    onPressWhileKeyboardClosed: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    transparentBackground: Boolean = false,
    cornerRadius: Dp = DefaultSearchBarCornerRadius,
    modifier: Modifier = Modifier,
) {
    val barShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val focusRequester = focusRequester ?: remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    val searchBarBackground =
        if (transparentBackground) {
            Color.Transparent
        } else if (opaqueBackground) {
            AppColors.DialogBackground
        } else {
            AppColors.getSearchBarBackground(showWallpaperBackground)
        }
    val accentColor = AppColors.Accent
    val iconAndTextColor = AppColors.getSearchBarTextAndIconColor(showWallpaperBackground)
    val isDarkTheme = LocalAppIsDarkTheme.current
    val lightWallpaperSearchBar = !isDarkTheme && showWallpaperBackground
    val searchBarIconColor = AppColors.getSearchBarSecondaryIconTint(showWallpaperBackground)
    val isAliasDetected =
            detectedShortcutTarget != null ||
            detectedAliasSearchSection != null ||
            activeToolType != null ||
            isCurrencyConverterAliasMode ||
            isWorldClockAliasMode ||
            isDictionaryAliasMode ||
            isWeatherAliasMode ||
            detectedCustomToolId != null
            || detectedTaskerIntentId != null
    val aliasVisualTransformation =
        rememberAliasHighlightVisualTransformation(
            enabledTargets = enabledTargets,
            shortcutCodes = shortcutCodes,
            shortcutEnabled = shortcutEnabled,
            triggerWords = triggerWords,
            isSearchEngineAliasSuffixEnabled = isSearchEngineAliasSuffixEnabled && !isAliasDetected,
            highlightColor = AppColors.LinkColor,
        )
    val leadingIconState =
        when {
            activeToolType == SearchToolType.UNIT_CONVERTER -> LeadingIconState.UnitConverter
            activeToolType == SearchToolType.CALCULATOR || isCalculatorMode -> LeadingIconState.Calculator
            isCurrencyConverterAliasMode -> LeadingIconState.CurrencyConverter
            isWorldClockAliasMode -> LeadingIconState.WorldClock
            isDictionaryAliasMode -> LeadingIconState.Dictionary
            isWeatherAliasMode -> LeadingIconState.Weather
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
    var localInputAwaitingStateAck by remember { mutableStateOf<String?>(null) }
    var hasLaidOutSearchField by remember { mutableStateOf(false) }
    var showSectionMenu by remember { mutableStateOf(false) }
    val aliasMorphProgress = remember { Animatable(1f) }
    var aliasMorphText by remember { mutableStateOf<String?>(null) }
    var previousLeadingIconState by remember { mutableStateOf(leadingIconState) }
    var hasCompletedStartupAutoFocus by remember { mutableStateOf(!autoFocusOnStart) }
    val searchBarInteractionSource = remember { MutableInteractionSource() }
    fun submitSearchAction() {
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
    }

    LaunchedEffect(query, leadingIconState) {
        val previousText = textFieldValue.text
        val shouldDeferSync =
            shouldDeferTextFieldValueSync(
                stateQuery = query,
                localText = previousText,
                localInputAwaitingStateAck = localInputAwaitingStateAck,
            )
        if (query == localInputAwaitingStateAck) {
            localInputAwaitingStateAck = null
        }
        if (
            query != previousText &&
                !shouldDeferSync
        ) {
            localInputAwaitingStateAck = null
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

    // Pending-ack deferral only protects in-flight typing/voice input. Once the surface stops, the
    // ViewModel is authoritative (e.g. clear-query-on-launch runs in onStop), so drop any stale ack
    // and resync on return in case a clear was deferred before the stop.
    val latestQuery by rememberUpdatedState(query)
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> localInputAwaitingStateAck = null
                    Lifecycle.Event.ON_START -> {
                        localInputAwaitingStateAck = null
                        if (textFieldValue.text != latestQuery) {
                            textFieldValue =
                                TextFieldValue(latestQuery, TextRange(latestQuery.length))
                        }
                    }
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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

    LaunchedEffect(searchBarInteractionSource) {
        searchBarInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                onPressWhileKeyboardClosed()
            }
        }
    }

    LaunchedEffect(autoFocusOnStart, hasLaidOutSearchField) {
        if (!autoFocusOnStart) {
            hasCompletedStartupAutoFocus = true
            return@LaunchedEffect
        }
        if (autoFocusOnStart && hasLaidOutSearchField) {
            focusRequester.requestFocus()
            keyboardController?.show()
            hasCompletedStartupAutoFocus = true
        }
    }
    if (autoFocusOnStart || releaseFocusOnLeave) {
        DisposableEffect(lifecycleOwner, autoFocusOnStart, releaseFocusOnLeave) {
            val observer =
                LifecycleEventObserver { _, event ->
                    when (event) {
                        // Leaving for another app must drop the focus here rather than on the way
                        // back: the window is declared stateAlwaysVisible, so a field that is still
                        // focused when the surface becomes visible again re-raises the keyboard
                        // before a resume-time release could suppress it. Only a full stop counts —
                        // a transient pause (permission dialog, notification shade) leaves a
                        // deliberately opened keyboard alone.
                        Lifecycle.Event.ON_STOP ->
                            if (releaseFocusOnLeave && !autoFocusOnStart) {
                                view.clearFocus()
                                keyboardController?.hide()
                            }

                        Lifecycle.Event.ON_RESUME ->
                            if (autoFocusOnStart && hasCompletedStartupAutoFocus) {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }

                        else -> Unit
                    }
                }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }
    // Navigating to settings/widgets and back rebuilds this field from scratch, so a keyboard the
    // user opened by hand is only restored when the ViewModel says it was open on the way out.
    LaunchedEffect(restoreKeyboardOnEnter, hasLaidOutSearchField) {
        if (!restoreKeyboardOnEnter || !hasLaidOutSearchField) return@LaunchedEffect
        focusRequester.requestFocus()
        keyboardController?.show()
        onRestoreKeyboardHandled()
    }

    // Welcome animation: the gradient scans across the border once, ending on its white tail,
    // then the glow fades out while the resting border fades in.
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
            glowAlpha.snapTo(1f)
            borderAlpha.snapTo(0f)
            animationProgress.snapTo(0f)

            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(DesignTokens.AnimationDurationLong, easing = LinearEasing),
            )

            // The white end of the gradient holds the glow; the border is not snapped to opaque here.
            delay(DesignTokens.AnimationDurationMicro.toLong())

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

            delay(DesignTokens.AnimationDurationFast.toLong())
            onWelcomeAnimationCompleted?.invoke()
        } else if (query.isEmpty()) {
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
                    // The inset strip already paints the card behind a transparent bar, so an
                    // accent-tinted shadow would only wash colour across the bar's interior.
                    if (!isDarkTheme && !transparentBackground) {
                        val elevationDp =
                            if (lightWallpaperSearchBar) {
                                DesignTokens.ElevationLevel5
                            } else {
                                DesignTokens.ElevationLevel4
                            }
                        shadowElevation = with(density) { elevationDp.toPx() }
                        shape = barShape
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
                        val cornerRadiusVal = cornerRadius.toPx()

                        val gradientWidth = size.width * DesignTokens.SearchFieldGradientWidthMultiplier

                        // Slides the brush left so the border ends on the gradient's white tail.
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
                            )

                        // Wide, faint strokes behind the border fake a blurred glow.
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
                    if (lightWallpaperSearchBar && !forceRestingOutline) {
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
                            shape = barShape,
                        )
                    },
                ).clip(barShape)
                .background(searchBarBackground),
    ) {
        TextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                if (textFieldValue.text.isEmpty() && newValue.text.isNotEmpty()) {
                    StartupTrace.mark("QS.Home.FirstInputAccepted")
                }
                // Selection/composition-only changes never produce a query update, so marking them
                // as awaiting ack would leave a stale ack that swallows a later external clear.
                if (newValue.text != textFieldValue.text) {
                    localInputAwaitingStateAck = newValue.text
                }
                textFieldValue = newValue
                onQueryChange(newValue.text)
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            StartupTrace.mark("QS.Home.SearchFieldFocused")
                        }
                    }
                    .onGloballyPositioned {
                        if (!hasLaidOutSearchField) {
                            hasLaidOutSearchField = true
                        }
                    }
                    .onPreviewKeyEvent { keyEvent ->
                        when {
                            keyEvent.type == KeyEventType.KeyDown &&
                                (keyEvent.key == Key.Backspace || keyEvent.key == Key.Delete) &&
                                textFieldValue.text.isEmpty() &&
                                (detectedShortcutTarget != null ||
                                    detectedAliasSearchSection != null ||
                                    isCurrencyConverterAliasMode ||
                                    isWorldClockAliasMode ||
                                    isDictionaryAliasMode ||
                                    isWeatherAliasMode ||
                                    detectedCustomToolId != null ||
                                    detectedTaskerIntentId != null ||
                                    activeToolType != null ||
                                    isCalculatorMode) -> {
                                onClearDetectedShortcut()
                                true
                            }
                            keyEvent.type == KeyEventType.KeyDown &&
                                keyEvent.key == Key.DirectionUp -> {
                                onMoveTopResultSelectionUp?.invoke() == true
                            }
                            keyEvent.type == KeyEventType.KeyDown &&
                                keyEvent.key == Key.DirectionDown -> {
                                onMoveTopResultSelectionDown?.invoke() == true
                            }
                            keyEvent.type == KeyEventType.KeyDown &&
                                (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) -> {
                                if (!keyEvent.isShiftPressed) {
                                    submitSearchAction()
                                    true
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    }
                    .animateContentSize(),
            shape = barShape,
            placeholder = {
                AnimatedContent(
                    targetState = placeholderText,
                    transitionSpec = {
                        fadeIn(
                            animationSpec =
                                tween(
                                    durationMillis = PlaceholderHintTransitionDurationMs,
                                    delayMillis = PlaceholderHintTransitionDelayMs,
                                    easing = LinearOutSlowInEasing,
                                ),
                        ) togetherWith
                            fadeOut(
                                animationSpec =
                                    tween(
                                        durationMillis = PlaceholderHintTransitionDurationMs,
                                        easing = FastOutSlowInEasing,
                                    ),
                            )
                    },
                    label = "SearchPlaceholderHint",
                ) { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        color = iconAndTextColor.copy(alpha = DesignTokens.SearchFieldPlaceholderAlpha),
                    )
                }
            },
            textStyle =
                MaterialTheme.typography.titleMedium.copy(color = iconAndTextColor),
            singleLine = true,
            maxLines = 1,
            leadingIcon = {
                Box {
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
                        Box(
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showSectionMenu = true },
                            ),
                        ) {
                            SearchBarLeadingIcon(
                                iconState = currentIconState,
                                iconTint = searchBarIconColor,
                            )
                        }
                    }
                    SearchBarSectionMenu(
                        expanded = showSectionMenu,
                        onDismiss = { showSectionMenu = false },
                        onSectionSelected = { section ->
                            showSectionMenu = false
                            onSectionSelected(section)
                        },
                    )
                }
            },
            trailingIcon = {
                SearchBarTrailingIcon(
                    showClear = isAliasDetected || query.isNotEmpty(),
                    showSettings = showSettingsIcon,
                    accentColor = accentColor,
                    iconColor = searchBarIconColor,
                    onClear = {
                        if (query.isNotEmpty()) {
                            localInputAwaitingStateAck = null
                            onClearQuery()
                        } else if (isAliasDetected) {
                            onClearDetectedShortcut()
                        }
                    },
                    onSettings = {
                        hapticStrong(view)()
                        if (dismissKeyboardBeforeSettingsClick) {
                            view.clearFocus()
                            keyboardController?.hide()
                        }
                        onSettingsClick()
                    },
                )
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
                        submitSearchAction()
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
            interactionSource = searchBarInteractionSource,
        )

        SearchBarAliasMorphText(
            text = aliasMorphText,
            progress = aliasMorphProgress.value,
            horizontalTravelPx = aliasMorphHorizontalTravelPx,
            verticalTravelPx = aliasMorphVerticalTravelPx,
        )
    }
}

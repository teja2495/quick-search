package com.tk.quicksearch.search.searchScreen.components

import com.tk.quicksearch.searchEngines.AliasValidator.splitAliasCodes
import com.tk.quicksearch.R
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.search.searchScreen.insetCardBackdrop
import com.tk.quicksearch.search.searchScreen.insetCardBackground
import com.tk.quicksearch.onboarding.LocalWelcomeMorph
import com.tk.quicksearch.onboarding.welcomeMorphTarget
import com.tk.quicksearch.searchEngines.inline.insetCardGlow
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tk.quicksearch.searchEngines.SearchEngine
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.tools.inlineSearch.SearchToolType
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchBarStyle
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.app.HomeActivity
import com.tk.quicksearch.app.navigation.LocalRootPageSettled
import com.tk.quicksearch.app.startup.StartupTrace
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.util.hapticStrong
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
private const val AliasIconMorphDurationMs = AppMotion.DurationStandard

/** Accent stops after the palette, so the welcome sweep comes to rest on the accent. */
private const val SweepAccentTailStops = 14
private const val LeadingIconEnterDurationMs = AppMotion.DurationShort
private const val LeadingIconExitDurationMs = AppMotion.DurationQuick
private const val LeadingIconEnterDelayMs = 40
private const val LeadingIconEnterInitialScale = 0.88f
private const val LeadingIconExitTargetScale = 0.88f
private const val PlaceholderHintTransitionDurationMs = AppMotion.DurationExtraLong
private const val PlaceholderHintTransitionDelayMs = 120
internal const val LightSearchBarShadowAmbientAlpha = 0.38f
internal const val LightSearchBarShadowSpotAlpha = 0.62f
/** Resting radius of the standalone bar, matching [AppShapes.extraLarge]. */
private val DefaultSearchBarCornerRadius = DesignTokens.Spacing28

private val AliasMorphHorizontalTravel = DesignTokens.Spacing28
private val AliasMorphVerticalTravel = DesignTokens.SpacingXSmall



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
    onVoiceClick: () -> Unit = {},
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
    searchBarStyle: SearchBarStyle = SearchBarStyle(),
    modifier: Modifier = Modifier,
) {
    val barShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val focusRequester = focusRequester ?: remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val context = LocalContext.current

    val searchBarBackground =
        if (transparentBackground) {
            Color.Transparent
        } else if (opaqueBackground) {
            AppColors.DialogBackground
        } else {
            searchBarStyle.backgroundColor(AppColors.getSearchBarBackground(showWallpaperBackground))
        }
    val accentColor = AppColors.Accent
    val restingBorderColor = searchBarStyle.borderColor(accentColor)
    val contrastBackground = searchBarContrastBackground(
        searchBarStyle,
        showWallpaperBackground,
        opaqueBackground = opaqueBackground && !transparentBackground,
    )
    val iconAndTextColor = AppColors.getSearchBarTextAndIconColor(showWallpaperBackground).readableOn(contrastBackground)
    val placeholderColor = iconAndTextColor.copy(alpha = DesignTokens.SearchFieldPlaceholderAlpha)
        .readableOn(contrastBackground)
    val accentIconColor = accentColor.readableOn(contrastBackground, minimumContrast = 3f)
    val isDarkTheme = LocalAppIsDarkTheme.current
    val lightWallpaperSearchBar = !isDarkTheme && showWallpaperBackground
    val searchBarIconColor = AppColors.getSearchBarSecondaryIconTint(showWallpaperBackground).readableOn(contrastBackground, 3f)
    val isAliasDetected =
        isSearchBarAliasActive(
            detectedShortcutTarget = detectedShortcutTarget,
            detectedAliasSearchSection = detectedAliasSearchSection,
            activeToolType = activeToolType,
            isCurrencyConverterAliasMode = isCurrencyConverterAliasMode,
            isWorldClockAliasMode = isWorldClockAliasMode,
            isDictionaryAliasMode = isDictionaryAliasMode,
            isWeatherAliasMode = isWeatherAliasMode,
            detectedCustomToolId = detectedCustomToolId,
            detectedTaskerIntentId = detectedTaskerIntentId,
        )
    val aliasOutputTransformation =
        rememberAliasHighlightOutputTransformation(
            enabledTargets = enabledTargets,
            shortcutCodes = shortcutCodes,
            shortcutEnabled = shortcutEnabled,
            triggerWords = triggerWords,
            isSearchEngineAliasSuffixEnabled = isSearchEngineAliasSuffixEnabled && !isAliasDetected,
            highlightColor = AppColors.LinkColor.readableOn(contrastBackground),
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
                .flatMap { (_, code) -> splitAliasCodes(code) }
                .toSet()
        }

    // Local text field state maintains cursor position even when state query changes from voice
    // input.
    val textFieldState = rememberTextFieldState(query, TextRange(query.length))
    var localInputAwaitingStateAck by remember { mutableStateOf<String?>(null) }
    var hasLaidOutSearchField by remember { mutableStateOf(false) }
    var showSectionMenu by remember { mutableStateOf(false) }
    val aliasMorphProgress = remember { Animatable(1f) }
    var aliasMorphText by remember { mutableStateOf<String?>(null) }
    var previousLeadingIconState by remember { mutableStateOf(leadingIconState) }
    var hasCompletedStartupAutoFocus by remember(autoFocusOnStart) { mutableStateOf(!autoFocusOnStart) }
    val searchBarInteractionSource = remember { MutableInteractionSource() }
    // A caret with no way to type is noise: hide it while the soft keyboard is closed, unless a
    // hardware keyboard can still type into the field.
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val isImeVisible by remember(imeInsets, density) {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    val hasHardwareKeyboard =
        LocalConfiguration.current.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
    val showCursor = isImeVisible || hasHardwareKeyboard
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
        val previousText = textFieldState.text.toString()
        // A consumed prefix alias shrinks the query to the typed text's suffix; apply it right away
        // instead of treating it as a stale state echo.
        val isConsumedAlias =
            leadingIconState !is LeadingIconState.Search &&
                detectConsumedPrefixAlias(
                    previousText = previousText,
                    currentQuery = query,
                    activePrefixAliases = activePrefixAliases,
                ) != null
        val shouldDeferSync =
            !isConsumedAlias &&
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
            textFieldState.setTextAndPlaceCursorAtEnd(query)
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
                        if (textFieldState.text.toString() != latestQuery) {
                            textFieldState.setTextAndPlaceCursorAtEnd(latestQuery)
                        }
                    }
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // User edits reach the ViewModel from here. Programmatic syncs set the text to the current
    // query, so they never echo back; selection-only changes don't change the text at all.
    LaunchedEffect(textFieldState) {
        var previousText = textFieldState.text.toString()
        snapshotFlow { textFieldState.text.toString() }.collect { text ->
            if (previousText.isEmpty() && text.isNotEmpty()) {
                StartupTrace.mark("QS.Home.FirstInputAccepted")
            }
            previousText = text
            if (text == latestQuery) return@collect
            localInputAwaitingStateAck = text
            onQueryChange(text)
        }
    }

    LaunchedEffect(selectRetainedQuery, query) {
        if (!selectRetainedQuery) return@LaunchedEffect
        if (query.isNotEmpty()) {
            textFieldState.setTextAndSelectAll(query)
        }
        onSelectRetainedQueryHandled()
    }

    LaunchedEffect(aliasMorphText) {
        if (aliasMorphText == null) return@LaunchedEffect
        aliasMorphProgress.snapTo(0f)
        aliasMorphProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = AliasIconMorphDurationMs, easing = AppMotion.EmphasizedDecelerateEasing),
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

    val pageSettled = LocalRootPageSettled.current
    val latestPageSettled = rememberUpdatedState(pageSettled)
    val skipLaunchAutoFocus = LocalSearchReturningFromInAppPage.current
    LaunchedEffect(autoFocusOnStart, hasLaidOutSearchField, pageSettled) {
        if (!pageSettled || hasCompletedStartupAutoFocus) return@LaunchedEffect
        if (!autoFocusOnStart || skipLaunchAutoFocus) {
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
                            if (
                                autoFocusOnStart &&
                                hasCompletedStartupAutoFocus &&
                                latestPageSettled.value &&
                                (context as? HomeActivity)?.consumeResumeAutoFocusSuppression() != true
                            ) {
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
    // The IME changes Home's layout. Wait until the page settles before restoring it, and leave
    // the request pending if a swipe only previews Home and is then cancelled.
    LaunchedEffect(restoreKeyboardOnEnter, hasLaidOutSearchField, pageSettled) {
        if (!restoreKeyboardOnEnter || !hasLaidOutSearchField || !pageSettled) return@LaunchedEffect
        focusRequester.requestFocus()
        keyboardController?.show()
        onRestoreKeyboardHandled()
    }

    val borderSweep =
        rememberSearchBarBorderSweep(
            play = showWelcomeAnimation && LocalWelcomeMorph.current?.isActive != true,
            queryIsEmpty = query.isEmpty(),
            onCompleted = onWelcomeAnimationCompleted,
        )
    // Palettes are centralized in AppColors; the sweep settles on the accent, the resting border's color.
    val activeColors = remember(accentColor) { AppColors.SearchFieldGooglePalette + List(SweepAccentTailStops) { accentColor } }
    val aliasMorphHorizontalTravelPx = with(density) { AliasMorphHorizontalTravel.toPx() }
    val aliasMorphVerticalTravelPx = with(density) { AliasMorphVerticalTravel.toPx() }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .welcomeMorphTarget(cornerRadius, searchBarBackground)
                .then(if (transparentBackground) Modifier else Modifier.insetCardGlow(restingBorderColor, barShape))
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
                    val alpha = borderSweep.glowAlpha
                    if (alpha > 0f) {
                        val strokeWidth = DesignTokens.SearchFieldBorderWidth.toPx()
                        val cornerRadiusVal = cornerRadius.toPx()

                        val gradientWidth = size.width * DesignTokens.SearchFieldGradientWidthMultiplier

                        // Slides the brush left so the border ends on the gradient's accent tail.
                        val xOffset =
                            -(borderSweep.progress * size.width * DesignTokens.SearchFieldGradientTravelMultiplier)

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
                            cornerRadius = CornerRadius(cornerRadiusVal),
                            style = Stroke(width = strokeWidth * 4f), // Wide spill
                            alpha = alpha * 0.3f, // Low opacity
                        )
                        drawRoundRect(
                            brush = brush,
                            cornerRadius = CornerRadius(cornerRadiusVal),
                            style =
                                Stroke(
                                    width = strokeWidth * 2f,
                                ), // Medium spill
                            alpha = alpha * 0.5f,
                        )

                        // 2. Draw "Core" sharp line
                        drawRoundRect(
                            brush = brush,
                            cornerRadius = CornerRadius(cornerRadiusVal),
                            style = Stroke(width = strokeWidth),
                            alpha = alpha,
                        )
                    }
                }.then(
                    if (
                        searchBarStyle.borderWidthDp <= 0f ||
                        (lightWallpaperSearchBar && !forceRestingOutline && !searchBarStyle.hasCustomBorder)
                    ) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = searchBarStyle.borderWidth(),
                            color =
                                restingBorderColor.copy(
                                    alpha = (borderSweep.borderAlpha * restingBorderColor.alpha).coerceIn(0f, 1f),
                                ),
                            shape = barShape,
                        )
                    },
                ).clip(barShape)
                .insetCardBackdrop(!transparentBackground && !opaqueBackground, barShape, anywhere = true)
                .background(insetCardBackground(!opaqueBackground, searchBarBackground, anywhere = true)),
    ) {
        TextField(
            state = textFieldState,
            inputTransformation = SearchQueryInputTransformation,
            outputTransformation = aliasOutputTransformation,
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
                                textFieldState.text.isEmpty() &&
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
                                    easing = AppMotion.EmphasizedDecelerateEasing,
                                ),
                        ) togetherWith
                            fadeOut(
                                animationSpec =
                                    tween(
                                        durationMillis = PlaceholderHintTransitionDurationMs,
                                        easing = AppMotion.EmphasizedEasing,
                                    ),
                            )
                    },
                    label = "SearchPlaceholderHint",
                ) { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        color = placeholderColor,
                    )
                }
            },
            textStyle =
                MaterialTheme.typography.titleMedium.copy(color = iconAndTextColor),
            lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = SearchFieldMaxLines),
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
                                            easing = AppMotion.EmphasizedDecelerateEasing,
                                        ),
                                ) +
                                    scaleIn(
                                        animationSpec =
                                            tween(
                                                durationMillis = LeadingIconEnterDurationMs,
                                                delayMillis = LeadingIconEnterDelayMs,
                                                easing = AppMotion.EmphasizedDecelerateEasing,
                                            ),
                                        initialScale = LeadingIconEnterInitialScale,
                                    )
                                )
                                .togetherWith(
                                    fadeOut(
                                        animationSpec = tween(durationMillis = LeadingIconExitDurationMs, easing = AppMotion.EmphasizedAccelerateEasing),
                                    ) +
                                        scaleOut(
                                            animationSpec = tween(durationMillis = LeadingIconExitDurationMs, easing = AppMotion.EmphasizedAccelerateEasing),
                                            targetScale = LeadingIconExitTargetScale,
                                        ),
                                )
                        },
                        label = "search_bar_leading_icon",
                    ) { currentIconState ->
                        AppIconButton(
                            onClick = { showSectionMenu = true },
                            tooltip = stringResource(R.string.search_section_menu_title),
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
                        onSectionSelected = onSectionSelected,
                    )
                }
            },
            trailingIcon = {
                SearchBarTrailingIcon(
                    showClear = isAliasDetected || query.isNotEmpty(),
                    showSettings = showSettingsIcon && !searchBarStyle.showMicIcon,
                    showMic = searchBarStyle.showMicIcon,
                    accentColor = accentIconColor,
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
                    onMic = {
                        hapticStrong(view)()
                        onVoiceClick()
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
            onKeyboardAction = { submitSearchAction() },
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
                    cursorColor = if (showCursor) accentIconColor else Color.Transparent,
                ),
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

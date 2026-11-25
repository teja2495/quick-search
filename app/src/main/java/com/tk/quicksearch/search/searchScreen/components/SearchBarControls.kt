package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.components.AppPlainTooltip
import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionUiMetadataRegistry
import com.tk.quicksearch.tools.inlineSearch.SearchToolType
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticStrong
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import com.tk.quicksearch.shared.ui.theme.AppShapes

@Composable
internal fun SearchBarLeadingIcon(
    iconState: LeadingIconState,
    iconTint: Color,
) {
    when (iconState) {
        LeadingIconState.Calculator -> {
            Icon(
                imageVector = MaterialSymbols.Calculate,
                contentDescription = stringResource(R.string.calculator_toggle_title),
                tint = iconTint,
                modifier = Modifier.padding(start = DesignTokens.SpacingXSmall),
            )
        }

        LeadingIconState.UnitConverter -> {
            Icon(
                imageVector = MaterialSymbols.Straighten,
                contentDescription = stringResource(R.string.unit_converter_info_title),
                tint = iconTint,
                modifier = Modifier.padding(start = DesignTokens.SpacingXSmall),
            )
        }

        LeadingIconState.CurrencyConverter -> {
            Icon(
                imageVector = MaterialSymbols.CurrencyExchange,
                contentDescription = stringResource(R.string.currency_converter_toggle_title),
                tint = iconTint,
                modifier = Modifier.padding(start = DesignTokens.SpacingXSmall),
            )
        }

        LeadingIconState.WorldClock -> {
            Icon(
                imageVector = MaterialSymbols.AccessTime,
                contentDescription = stringResource(R.string.world_clock_toggle_title),
                tint = iconTint,
                modifier = Modifier.padding(start = DesignTokens.SpacingXSmall),
            )
        }

        LeadingIconState.Dictionary -> {
            Icon(
                imageVector = MaterialSymbols.MenuBook,
                contentDescription = stringResource(R.string.dictionary_toggle_title),
                tint = iconTint,
                modifier = Modifier.padding(start = DesignTokens.SpacingXSmall),
            )
        }

        LeadingIconState.Weather -> {
            Icon(
                imageVector = MaterialSymbols.Cloud,
                contentDescription = stringResource(R.string.weather_toggle_title),
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
                        contentDescription = stringResource(R.string.common_search),
                        modifier = Modifier
                            .padding(start = DesignTokens.SpacingSmall)
                            .size(DesignTokens.IconSize),
                    )
                } else {
                    Icon(
                        imageVector = MaterialSymbols.Settings,
                        contentDescription = stringResource(R.string.common_search),
                        tint = iconTint,
                        modifier = Modifier.padding(start = DesignTokens.SpacingXSmall),
                    )
                }
            } else {
                Icon(
                    imageVector =
                        SearchSectionUiMetadataRegistry.metadataFor(iconState.section).searchBarIcon,
                    contentDescription = stringResource(R.string.common_search),
                    tint = iconTint,
                    modifier = Modifier.padding(start = DesignTokens.SpacingXSmall),
                )
            }
        }

        LeadingIconState.Search -> {
            Icon(
                imageVector = MaterialSymbols.Search,
                contentDescription = stringResource(R.string.common_search),
                tint = iconTint,
                modifier = Modifier.padding(start = DesignTokens.SpacingXSmall),
            )
        }
    }
}

internal sealed interface LeadingIconState {
    data object Search : LeadingIconState

    data object Calculator : LeadingIconState

    data object UnitConverter : LeadingIconState

    data object CurrencyConverter : LeadingIconState

    data object WorldClock : LeadingIconState

    data object Dictionary : LeadingIconState

    data object Weather : LeadingIconState

    data class Shortcut(
        val target: SearchTarget,
    ) : LeadingIconState

    data class Section(
        val section: SearchSection,
    ) : LeadingIconState
}

/**
 * The IME can send a newer composing value before the ViewModel-backed [stateQuery] has been
 * rendered. Do not replace that value with its stale prefix while it is awaiting that state
 * acknowledgment: doing so cancels the IME composition and can leave voice transcription
 * truncated. Clear actions and other external query updates continue through the normal
 * synchronization path.
 */
internal fun shouldDeferTextFieldValueSync(
    stateQuery: String,
    localText: String,
    localInputAwaitingStateAck: String?,
): Boolean =
    localInputAwaitingStateAck == localText &&
        stateQuery.length < localText.length &&
        localText.startsWith(stateQuery)

internal fun detectConsumedPrefixAlias(
    previousText: String,
    currentQuery: String,
    activePrefixAliases: Set<String>,
): String? {
    if (activePrefixAliases.isEmpty()) return null
    val queryWithNoLeadingWhitespace = previousText.trimStart()
    if (queryWithNoLeadingWhitespace.isEmpty()) return null
    // Without a separator the whole text is the alias (aliases that trigger without a space).
    val separatorIndex =
        queryWithNoLeadingWhitespace
            .indexOfFirst { it.isWhitespace() }
            .let { if (it < 0) queryWithNoLeadingWhitespace.length else it }
    if (separatorIndex <= 0) return null

    val aliasToken = queryWithNoLeadingWhitespace.substring(0, separatorIndex)
    val aliasRemainder = queryWithNoLeadingWhitespace.substring(separatorIndex).trimStart()
    if (aliasRemainder != currentQuery) return null

    val normalizedAlias = aliasToken.lowercase(Locale.getDefault())
    if (normalizedAlias !in activePrefixAliases) return null
    return aliasToken
}

/** True while an alias, tool or shortcut owns the bar, which swaps its trailing icon to clear. */
internal fun isSearchBarAliasActive(
    detectedShortcutTarget: SearchTarget?,
    detectedAliasSearchSection: SearchSection?,
    activeToolType: SearchToolType?,
    isCurrencyConverterAliasMode: Boolean,
    isWorldClockAliasMode: Boolean,
    isDictionaryAliasMode: Boolean,
    isWeatherAliasMode: Boolean,
    detectedCustomToolId: String?,
    detectedTaskerIntentId: String?,
): Boolean =
    detectedShortcutTarget != null ||
        detectedAliasSearchSection != null ||
        activeToolType != null ||
        isCurrencyConverterAliasMode ||
        isWorldClockAliasMode ||
        isDictionaryAliasMode ||
        isWeatherAliasMode ||
        detectedCustomToolId != null ||
        detectedTaskerIntentId != null

/** True while search is rebuilt after returning from Settings or Widgets; skips launch auto-focus. */
internal val LocalSearchReturningFromInAppPage = staticCompositionLocalOf { false }

/** Outlives the Search page so the Open Keyboard pill returns from Settings/Widgets without re-animating. */
internal class OpenKeyboardPillMemory {
    var visibleWhenLeft = false
}

internal val LocalOpenKeyboardPillMemory = staticCompositionLocalOf { OpenKeyboardPillMemory() }

private enum class SearchBarTrailingAction { Clear, Mic, Settings, None }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SearchBarTrailingIcon(
    showClear: Boolean,
    showSettings: Boolean,
    showMic: Boolean,
    accentColor: Color,
    iconColor: Color,
    onClear: () -> Unit,
    onSettings: () -> Unit,
    onMic: () -> Unit,
) {
    val action =
        when {
            showClear -> SearchBarTrailingAction.Clear
            showMic -> SearchBarTrailingAction.Mic
            showSettings -> SearchBarTrailingAction.Settings
            else -> SearchBarTrailingAction.None
        }
    // The old icon shrinks away, then the new one grows in from the same spot. The scale is applied
    // only when drawing: a scaleIn/scaleOut layer could leave the button's hit-test transform stuck
    // at scale 0 after the animation, so taps on the visible icon fell through to the text field.
    AnimatedContent(
        targetState = action,
        transitionSpec = {
            fadeIn(tween(TrailingIconGrowMs, delayMillis = TrailingIconShrinkMs, easing = AppMotion.EmphasizedDecelerateEasing))
                .togetherWith(fadeOut(tween(TrailingIconShrinkMs, easing = AppMotion.EmphasizedAccelerateEasing)))
                .using(SizeTransform(clip = false))
        },
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(end = DesignTokens.SpacingXSmall),
        label = "SearchBarTrailingIcon",
    ) { target ->
        val drawScale by transition.animateFloat(
            transitionSpec = {
                if (targetState == EnterExitState.Visible) {
                    tween(TrailingIconGrowMs, delayMillis = TrailingIconShrinkMs, easing = AppMotion.EmphasizedEasing)
                } else {
                    tween(TrailingIconShrinkMs, easing = AppMotion.EmphasizedEasing)
                }
            },
            label = "SearchBarTrailingIconScale",
        ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
        Box(Modifier.drawWithContent { scale(drawScale) { this@drawWithContent.drawContent() } }) {
        when (target) {
            SearchBarTrailingAction.Clear ->
                AppIconButton(onClick = onClear, tooltip = stringResource(R.string.desc_clear_search)) {
                    Icon(
                        imageVector = MaterialSymbols.Close,
                        contentDescription = stringResource(R.string.desc_clear_search),
                        tint = accentColor,
                    )
                }

            SearchBarTrailingAction.Mic ->
                AppIconButton(onClick = onMic, tooltip = stringResource(R.string.desc_voice_search_icon)) {
                    Icon(
                        imageVector = MaterialSymbols.Mic,
                        contentDescription = stringResource(R.string.desc_voice_search_icon),
                        tint = iconColor,
                    )
                }

            SearchBarTrailingAction.Settings -> {
                val context = LocalContext.current
                val view = LocalView.current
                val settingsLabel = stringResource(R.string.desc_open_settings)
                AppPlainTooltip(text = settingsLabel, preserveLongPress = true) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .size(DesignTokens.Spacing48)
                                .clip(AppShapes.full)
                                .combinedClickable(
                                    hapticFeedbackEnabled = false,
                                    role = Role.Button,
                                    onLongClickLabel = stringResource(R.string.desc_open_device_settings),
                                    onLongClick = {
                                        hapticStrong(view)()
                                        openDeviceSettings(context)
                                    },
                                    onClick = onSettings,
                                ),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Settings,
                            contentDescription = settingsLabel,
                            tint = iconColor,
                        )
                    }
                }
            }

            SearchBarTrailingAction.None -> Unit
        }
        }
    }
}

private const val TrailingIconShrinkMs = AppMotion.DurationExit
private const val TrailingIconGrowMs = AppMotion.DurationShort

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.SearchBarAliasMorphText(
    text: String?,
    progress: Float,
    horizontalTravelPx: Float,
    verticalTravelPx: Float,
) {
        if (text != null) {
            val progress = progress.coerceIn(0f, 1f)
            val scale = 1f - ((1f - AliasIconMorphEndScale) * progress)
            // graphicsLayer translation is absolute, so travel toward the leading icon in either direction.
            val travelSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.LinkColor,
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = AliasMorphTextStartPadding)
                        .graphicsLayer {
                            translationX = -horizontalTravelPx * progress * travelSign
                            translationY = -verticalTravelPx * progress
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - progress
                        },
            )
        }
}

private const val AliasIconMorphEndScale = 0.5f

private val AliasMorphTextStartPadding = DesignTokens.Spacing48 + DesignTokens.SpacingXSmall

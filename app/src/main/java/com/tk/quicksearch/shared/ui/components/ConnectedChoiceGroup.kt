package com.tk.quicksearch.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.connectedButtonShape

/** One option of a [ConnectedChoiceGroup]: a label with an optional icon or color swatch. */
data class ChoiceOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
    val swatch: Color? = null,
)

/**
 * Material 3 Expressive connected button group for a single choice: buttons 2dp apart, fully round
 * at the group's ends and slightly round between, the selected one filled with the accent and
 * morphing fully round. A pressed button tightens its inner corners. Options without an icon or
 * swatch show a check while selected.
 *
 * Built on stable APIs (material3 1.4 keeps `ToggleButton`/`ButtonGroup` internal) with the same
 * shape behavior. Unselected buttons are an accent tint rather than a surface color, so they read
 * on the settings tile over wallpaper, custom image, AMOLED, and Material You alike.
 *
 * [fillWidth] spreads the options across the row, each button's share proportional to its label so
 * a long label gets the room a short one doesn't need. When that doesn't fit, only the selected
 * plain option keeps room for its check (the widths animate on selection), and labels stay on one
 * line, shrinking a little only when the row is still too narrow. Otherwise each button wraps its label, for a group placed
 * at the end of a row. [onSelect] also gets taps on the selected option, so a Custom
 * option can reopen its picker; [haptic] plays the toggle haptic on a change, off for callers that
 * play their own.
 */
@Composable
fun <T> ConnectedChoiceGroup(
    options: List<ChoiceOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = true,
    enabled: Boolean = true,
    haptic: Boolean = true,
) {
    val view = LocalView.current
    val labelStyle = MaterialTheme.typography.labelLarge
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Natural width of each button. Plain options reserve the check's slot whether or not they're
    // selected, so changing the selection doesn't shift the buttons.
    val labelWidths =
        remember(options, labelStyle, density) {
            options.map { option ->
                with(density) { textMeasurer.measure(option.label, labelStyle, maxLines = 1).size.width.toDp() }
            }
        }
    val reservedWidths =
        options.mapIndexed { index, option ->
            val leading = if (option.swatch != null) ChoiceSwatchSize else ChoiceIconSize
            labelWidths[index] + leading + ChoiceIconSpacing + ChoicePadding * 2
        }
    // When the row is too narrow for that, only the selected plain option keeps the check's slot,
    // so the room goes to the labels instead of empty slots and long labels stay whole.
    val compactWidths =
        options.mapIndexed { index, option ->
            val hasLeading = option.icon != null || option.swatch != null || option.value == selected
            val leading =
                when {
                    option.swatch != null -> ChoiceSwatchSize + ChoiceIconSpacing
                    hasLeading -> ChoiceIconSize + ChoiceIconSpacing
                    else -> 0.dp
                }
            labelWidths[index] + leading + ChoicePadding * 2
        }
    BoxWithConstraints(modifier = modifier.then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)) {
        val available = maxWidth - DesignTokens.ConnectedGroupGap * (options.size - 1).coerceAtLeast(0)
        val compact = fillWidth && reservedWidths.fold(0.dp) { sum, width -> sum + width } > available
        // Labels never ellipsize: when even the smallest font can't keep them on one line, they
        // wrap at that size and the buttons grow taller.
        val minScale = ChoiceMinFontSize.value / labelStyle.fontSize.value
        val wrapLabels =
            compact &&
                options.indices.fold(0.dp) { sum, index ->
                    sum + compactWidths[index] - labelWidths[index] * (1f - minScale)
                } > available
        val targetWeights = (if (compact) compactWidths else reservedWidths).map { it.value }
        Row(
            modifier =
                Modifier
                    .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                    .selectableGroup()
                    .alpha(if (enabled) 1f else ChoiceDisabledAlpha),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.ConnectedGroupGap),
        ) {
            options.forEachIndexed { index, option ->
                val weight by animateFloatAsState(
                    targetValue = targetWeights[index],
                    animationSpec = AppMotion.fastSpatial(),
                    label = "choiceWeight",
                )
                ChoiceButton(
                    option = option,
                    checked = option.value == selected,
                    isStart = index == 0,
                    isEnd = index == options.lastIndex,
                    enabled = enabled,
                    wrapLabel = wrapLabels,
                    onClick = {
                        if (haptic && option.value != selected) hapticToggle(view)()
                        onSelect(option.value)
                    },
                    modifier = if (fillWidth) Modifier.weight(weight) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun <T> ChoiceButton(
    option: ChoiceOption<T>,
    checked: Boolean,
    isStart: Boolean,
    isEnd: Boolean,
    enabled: Boolean,
    wrapLabel: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape =
        connectedButtonShape(
            interactionSource,
            isStart = isStart,
            isEnd = isEnd,
            outer = ChoiceRound,
            selected = checked,
        )
    val container by animateColorAsState(
        if (checked) AppColors.Accent else AppColors.AccentTintTonal,
        label = "choiceContainer",
    )
    val content by animateColorAsState(
        if (checked) AppColors.OnAccent else MaterialTheme.colorScheme.onSurface,
        label = "choiceContent",
    )
    Row(
        modifier =
            modifier
                .heightIn(min = ChoiceHeight)
                .clip(shape)
                .background(container, shape)
                .selectable(
                    selected = checked,
                    enabled = enabled,
                    role = Role.RadioButton,
                    interactionSource = interactionSource,
                    indication = ripple(color = content),
                    onClick = onClick,
                )
                .padding(horizontal = ChoicePadding),
        horizontalArrangement = Arrangement.spacedBy(ChoiceIconSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val leadingIcon = option.icon ?: if (checked && option.swatch == null) MaterialSymbols.Check else null
        when {
            option.swatch != null -> ChoiceSwatch(option.swatch)
            leadingIcon != null ->
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(ChoiceIconSize),
                )
        }
        // One line, shrinking toward ChoiceMinFontSize when the row is too narrow; past
        // that ([wrapLabel]) the label wraps at the minimum size. Never ellipsized.
        val labelStyle = MaterialTheme.typography.labelLarge
        if (wrapLabel) {
            BasicText(
                text = option.label,
                style =
                    labelStyle.copy(
                        color = content,
                        textAlign = TextAlign.Center,
                        fontSize = ChoiceMinFontSize,
                        lineHeight = ChoiceMinFontSize * 1.3f,
                    ),
                modifier = Modifier.padding(vertical = 6.dp),
            )
        } else {
            BasicText(
                text = option.label,
                style = labelStyle.copy(color = content, textAlign = TextAlign.Center),
                maxLines = 1,
                softWrap = false,
                autoSize =
                    TextAutoSize.StepBased(
                        minFontSize = ChoiceMinFontSize,
                        maxFontSize = labelStyle.fontSize,
                        stepSize = 0.5.sp,
                    ),
            )
        }
    }
}

/** A color dot; outlined so white and black stay visible on either fill. */
@Composable
private fun ChoiceSwatch(color: Color) {
    Box(
        modifier =
            Modifier
                .size(ChoiceSwatchSize)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}

private val ChoiceHeight: Dp = 48.dp
private val ChoiceRound: Dp = ChoiceHeight / 2
private val ChoicePadding: Dp = 12.dp
private val ChoiceIconSize: Dp = 18.dp
private val ChoiceIconSpacing: Dp = 8.dp
private val ChoiceMinFontSize = 11.sp
private val ChoiceSwatchSize: Dp = 16.dp
private const val ChoiceDisabledAlpha = 0.5f

/**
 * A segmented tile with a `titleMedium` heading over a full-width [ConnectedChoiceGroup], for a
 * single choice that needs the whole tile width (edge gesture side, activation).
 */
@Composable
fun <T> ChoiceTile(
    title: String,
    options: List<ChoiceOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    index: Int,
    count: Int,
) {
    SegmentedTile(index = index, count = count) {
        SegmentedTileContent {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ConnectedChoiceGroup(options = options, selected = selected, onSelect = onSelect)
        }
    }
}

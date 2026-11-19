package com.tk.quicksearch.shared.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.AppShapes

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
 * Built on Material's [ToggleButton] with the connected button group shapes. Unselected buttons are an accent tint rather than a surface color, so they read
 * on the settings tile over wallpaper, custom image, AMOLED, and Material You alike.
 *
 * [fillWidth] spreads the options across the row, each button's share proportional to its label so
 * a long label gets the room a short one doesn't need. When that doesn't fit, only the selected
 * plain option keeps room for its check. If labels still do not fit, the group stacks vertically
 * at the Material type size. [onSelect] also gets taps on the selected option, so a Custom
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
    fun select(value: T) {
        if (haptic && value != selected) hapticToggle(view)()
        onSelect(value)
    }
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
            (labelWidths[index] + leading + ChoiceIconSpacing + ChoicePadding * 2).coerceAtLeast(DesignTokens.MinTouchTarget)
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
            (labelWidths[index] + leading + ChoicePadding * 2).coerceAtLeast(DesignTokens.MinTouchTarget)
        }
    BoxWithConstraints(modifier = modifier.then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)) {
        val available = maxWidth - ButtonGroupDefaults.ConnectedSpaceBetween * (options.size - 1).coerceAtLeast(0)
        val compact = reservedWidths.fold(0.dp, Dp::plus) > available
        if (compactWidths.fold(0.dp, Dp::plus) > available) {
            Column(
                modifier = Modifier
                    .widthIn(max = if (fillWidth) maxWidth else maxWidth / 2)
                    .fillMaxWidth()
                    .selectableGroup()
                    .alpha(if (enabled) 1f else ChoiceDisabledAlpha),
                verticalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                options.forEach { option ->
                    ChoiceButton(
                        option = option,
                        checked = option.value == selected,
                        isStart = true,
                        isEnd = true,
                        enabled = enabled,
                        onClick = { select(option.value) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            return@BoxWithConstraints
        }
        val targetWeights = (if (compact) compactWidths else reservedWidths).map { it.value }
        Row(
            modifier =
                Modifier
                    .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                    .selectableGroup()
                    .alpha(if (enabled) 1f else ChoiceDisabledAlpha),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
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
                    onClick = { select(option.value) },
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonShapes = connectedButtonShapes(isStart = isStart, isEnd = isEnd)
    val container = if (checked) AppColors.Accent else AppColors.AccentTintTonal
    val content = if (checked) AppColors.OnAccent else MaterialTheme.colorScheme.onSurface
    ToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        modifier =
            modifier
                .widthIn(min = DesignTokens.MinTouchTarget)
                .heightIn(min = ChoiceHeight)
                .semantics { role = Role.RadioButton },
        enabled = enabled,
        shapes =
            ToggleButtonShapes(
                shape = buttonShapes.shape,
                pressedShape = buttonShapes.pressedShape,
                checkedShape = ButtonGroupDefaults.connectedButtonCheckedShape,
            ),
        // Disabled keeps the selection visible; the whole group dims instead.
        colors =
            ToggleButtonDefaults.colors(
                containerColor = AppColors.AccentTintTonal,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = container,
                disabledContentColor = content,
                checkedContainerColor = AppColors.Accent,
                checkedContentColor = AppColors.OnAccent,
            ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = ChoicePadding),
    ) {
        Row(
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
                        modifier = Modifier.size(ChoiceIconSize),
                    )
            }
            Text(
                text = option.label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = DesignTokens.SpacingSmall),
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
                .clip(AppShapes.full)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline, AppShapes.full),
    )
}

private val ChoiceHeight: Dp = 48.dp
private val ChoiceRound: Dp = ChoiceHeight / 2
private val ChoicePadding: Dp = 12.dp
private val ChoiceIconSize: Dp = 18.dp
private val ChoiceIconSpacing: Dp = 8.dp
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
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ConnectedChoiceGroup(options = options, selected = selected, onSelect = onSelect)
        }
    }
}

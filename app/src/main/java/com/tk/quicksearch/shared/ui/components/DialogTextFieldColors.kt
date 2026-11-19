package com.tk.quicksearch.shared.ui.components

import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.AppColors
import kotlinx.coroutines.flow.drop

@Composable
fun dialogTextFieldColors(
    unfocusedIndicatorColor: Color = MaterialTheme.colorScheme.outline,
): TextFieldColors =
    TextFieldDefaults.colors(
        focusedContainerColor = AppColors.AccentTintTonal,
        unfocusedContainerColor = AppColors.AccentTintSubtle,
        disabledContainerColor = AppColors.AccentTintFaint,
        errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = AppColors.TintStrongAlpha),
        focusedIndicatorColor = AppColors.Accent,
        unfocusedIndicatorColor = unfocusedIndicatorColor,
        focusedLabelColor = AppColors.Accent,
        cursorColor = AppColors.Accent,
    )

/** Transparent fields with a Material focus indicator for text inputs stacked as rows of a settings card. */
@Composable
fun cardTextFieldColors(): TextFieldColors =
    TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

/**
 * A transparent [TextField] for a row of a settings card. With a [placeholder], the label stays
 * floated so the hint is visible before the field is focused. The label keeps a fixed gap above
 * the input so focusing never changes the field's height.
 */
@Composable
fun CardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
) {
    val state = rememberTextFieldState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    // Bridge the caller's String to the field's own state: edits flow out, outside changes flow in.
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .drop(1)
            .collect { latestOnValueChange(it) }
    }
    LaunchedEffect(value) {
        if (state.text.toString() != value) state.setTextAndPlaceCursorAtEnd(value)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val alwaysFloatLabel = placeholder != null
    // A single-line resting label is centered with its gap included, so nudge it back down by
    // half the gap until it floats. Offset is draw-only, so this never resizes the field.
    val restingLabelOffset by animateDpAsState(
        targetValue =
            if (singleLine && !alwaysFloatLabel && !focused && value.isEmpty()) {
                CardTextFieldLabelGap / 2
            } else {
                0.dp
            },
        animationSpec = tween(durationMillis = CARD_TEXT_FIELD_LABEL_ANIMATION_MS, easing = AppMotion.EmphasizedEasing),
        label = "cardTextFieldRestingLabelOffset",
    )
    TextField(
        state = state,
        modifier = modifier.animateContentSize(),
        enabled = enabled,
        labelPosition = TextFieldLabelPosition.Inside(isAlwaysMinimized = alwaysFloatLabel),
        label = {
            Text(
                text = label,
                modifier = Modifier.offset(y = restingLabelOffset).padding(bottom = CardTextFieldLabelGap),
            )
        },
        placeholder = placeholder?.let {
            { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        },
        lineLimits =
            if (singleLine) {
                TextFieldLineLimits.SingleLine
            } else {
                TextFieldLineLimits.MultiLine(minHeightInLines = minLines, maxHeightInLines = maxLines)
            },
        colors = cardTextFieldColors(),
        interactionSource = interactionSource,
    )
}

private val CardTextFieldLabelGap = 6.dp

// Matches the Material 3 label float duration.
private const val CARD_TEXT_FIELD_LABEL_ANIMATION_MS = AppMotion.DurationExit

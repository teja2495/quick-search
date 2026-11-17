package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.AppColors

/*
 * The app's buttons. Every button outside this file goes through these wrappers instead of the
 * Material composables, so a new design language (shapes, colors, sizes, press morphs) changes the
 * defaults here once instead of at every call site. The parameters mirror Material 3's.
 */

/** High-emphasis filled button (Material `Button`). */
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Button(onClick, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)
}

/** Medium-emphasis tonal button (Material `FilledTonalButton`). */
@Composable
fun AppTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    FilledTonalButton(onClick, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)
}

/** Medium-emphasis outlined button (Material `OutlinedButton`). */
@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(onClick, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)
}

/** Low-emphasis text button (Material `TextButton`), e.g. dialog actions. */
@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(onClick, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)
}

/**
 * Tonal action button inside a settings tile, such as Reset or Edit: an accent tint with accent
 * content (or [contentColor], e.g. the error color for Delete). [small] is for buttons beside text.
 */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentColor: Color = AppColors.Accent,
    small: Boolean = false,
) {
    AppTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = if (small) TonalButtonSmallHeight else TonalButtonHeight),
        enabled = enabled,
        shape = CircleShape,
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = contentColor.copy(alpha = AppColors.TintTonalAlpha),
                contentColor = contentColor,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            ),
        contentPadding =
            if (small) {
                PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            } else {
                ButtonDefaults.ContentPadding
            },
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(end = if (small) 6.dp else ButtonDefaults.IconSpacing)
                        .size(if (small) 16.dp else ButtonDefaults.IconSize),
            )
        }
        Text(
            text = text,
            style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

private val TonalButtonHeight: Dp = 48.dp
private val TonalButtonSmallHeight: Dp = 32.dp

package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.pressMorphPillShape

/*
 * The app's buttons. Every button outside this file goes through these wrappers instead of the
 * Material composables. Callers pick a semantic [AppButtonTone] and [AppButtonSize]; the wrappers
 * own shape, colors, elevation, padding and the press morph. A new design language (or a new
 * Material button API) is a change to this file, not to call sites.
 */

/** What a button's color means. The style (filled, tonal, outlined, text) decides how it is applied. */
@Immutable
sealed interface AppButtonTone {
    /** The theme accent: the default for every button. */
    data object Accent : AppButtonTone

    /** Quiet secondary color, for actions inside result cards (AI answers, converters). */
    data object Secondary : AppButtonTone

    /** Destructive actions such as Delete. */
    data object Error : AppButtonTone

    /** De-emphasized actions such as Reset beside a primary action. */
    data object Neutral : AppButtonTone

    /**
     * A caller color, e.g. text over wallpaper or a snackbar's action color. [onColor] is the
     * content color on a filled container; unspecified picks black or white for contrast.
     */
    data class Custom(
        val color: Color,
        val onColor: Color = Color.Unspecified,
    ) : AppButtonTone
}

/** Material 3 Expressive button sizes: height, padding, icon size and the corner it squares to when pressed. */
enum class AppButtonSize(
    internal val height: Dp,
    internal val horizontalPadding: Dp,
    internal val iconSize: Dp,
    internal val iconSpacing: Dp,
    internal val pressedCorner: Dp,
) {
    ExtraSmall(
        height = DesignTokens.ButtonHeightExtraSmall,
        horizontalPadding = DesignTokens.SpacingMedium,
        iconSize = DesignTokens.ButtonIconSizeExtraSmall,
        iconSpacing = DesignTokens.SpacingXSmall,
        pressedCorner = DesignTokens.ButtonPressedCornerSmall,
    ),
    Small(
        height = DesignTokens.ButtonHeightSmall,
        horizontalPadding = DesignTokens.SpacingXXLarge,
        iconSize = DesignTokens.ButtonIconSizeSmall,
        iconSpacing = DesignTokens.SpacingSmall,
        pressedCorner = DesignTokens.ButtonPressedCornerSmall,
    ),
    Medium(
        height = DesignTokens.FloatingActionHeight,
        horizontalPadding = DesignTokens.SpacingXXLarge,
        iconSize = DesignTokens.IconSize,
        iconSpacing = DesignTokens.SpacingSmall,
        pressedCorner = DesignTokens.ButtonPressedCornerMedium,
    ),
}

/** High-emphasis filled button. */
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: AppButtonTone = AppButtonTone.Accent,
    size: AppButtonSize = AppButtonSize.Small,
    icon: ImageVector? = null,
    contentPadding: PaddingValues? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val color = tone.color()
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = size.height),
        enabled = enabled,
        shape = pressMorphPillShape(source, size.pressedCorner),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = tone.onColor(color)),
        contentPadding = contentPadding ?: size.padding(icon != null),
        interactionSource = source,
    ) { ButtonContent(icon, size, content) }
}

/** Medium-emphasis tonal button: a tint of the tone's color under content in that color. */
@Composable
fun AppTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: AppButtonTone = AppButtonTone.Accent,
    size: AppButtonSize = AppButtonSize.Small,
    icon: ImageVector? = null,
    contentPadding: PaddingValues? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = size.height),
        enabled = enabled,
        shape = pressMorphPillShape(source, size.pressedCorner),
        colors = tone.tonalColors(),
        contentPadding = contentPadding ?: size.padding(icon != null),
        interactionSource = source,
    ) { ButtonContent(icon, size, content) }
}

/** Medium-emphasis outlined button. [showBorder] off lets a caller draw its own outline (welcome glow). */
@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: AppButtonTone = AppButtonTone.Accent,
    size: AppButtonSize = AppButtonSize.Small,
    icon: ImageVector? = null,
    showBorder: Boolean = true,
    contentPadding: PaddingValues? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = size.height),
        enabled = enabled,
        shape = pressMorphPillShape(source, size.pressedCorner),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tone.color()),
        border = if (showBorder) ButtonDefaults.outlinedButtonBorder(enabled) else null,
        contentPadding = contentPadding ?: size.padding(icon != null),
        interactionSource = source,
    ) { ButtonContent(icon, size, content) }
}

/** Low-emphasis text button, e.g. dialog actions. */
@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: AppButtonTone = AppButtonTone.Accent,
    size: AppButtonSize = AppButtonSize.Small,
    icon: ImageVector? = null,
    contentPadding: PaddingValues? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = pressMorphPillShape(source, size.pressedCorner),
        colors = ButtonDefaults.textButtonColors(contentColor = tone.color()),
        contentPadding =
            contentPadding ?: if (icon != null) {
                ButtonDefaults.TextButtonWithIconContentPadding
            } else {
                ButtonDefaults.TextButtonContentPadding
            },
        interactionSource = source,
    ) { ButtonContent(icon, size, content) }
}

/**
 * Tonal action button inside a settings tile, such as Reset or Edit: an accent tint with accent
 * content (or another [tone], e.g. [AppButtonTone.Error] for Delete). [small] is for buttons beside text.
 */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    tone: AppButtonTone = AppButtonTone.Accent,
    small: Boolean = false,
) {
    AppTonalButton(
        onClick = onClick,
        modifier = if (small) modifier else modifier.heightIn(min = DesignTokens.TonalSettingsButtonHeight),
        enabled = enabled,
        tone = tone,
        size = if (small) AppButtonSize.ExtraSmall else AppButtonSize.Small,
        icon = icon,
    ) {
        Text(
            text = text,
            style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RowScope.ButtonContent(
    icon: ImageVector?,
    size: AppButtonSize,
    content: @Composable RowScope.() -> Unit,
) {
    if (icon != null) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(size.iconSize))
        Spacer(Modifier.width(size.iconSpacing))
    }
    content()
}

private fun AppButtonSize.padding(hasIcon: Boolean): PaddingValues =
    PaddingValues(
        start = if (hasIcon) horizontalPadding - DesignTokens.SpacingSmall else horizontalPadding,
        end = horizontalPadding,
        top = DesignTokens.SpacingXSmall,
        bottom = DesignTokens.SpacingXSmall,
    )

@Composable
private fun AppButtonTone.color(): Color =
    when (this) {
        AppButtonTone.Accent -> AppColors.Accent
        AppButtonTone.Secondary -> MaterialTheme.colorScheme.onSecondaryContainer
        AppButtonTone.Error -> MaterialTheme.colorScheme.error
        AppButtonTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        is AppButtonTone.Custom -> color
    }

@Composable
private fun AppButtonTone.onColor(container: Color): Color =
    when (this) {
        AppButtonTone.Accent -> AppColors.OnAccent
        AppButtonTone.Secondary -> MaterialTheme.colorScheme.secondaryContainer
        AppButtonTone.Error -> MaterialTheme.colorScheme.onError
        AppButtonTone.Neutral -> MaterialTheme.colorScheme.surface
        is AppButtonTone.Custom ->
            onColor.takeIf { it != Color.Unspecified }
                ?: if (container.luminance() > DesignTokens.DarkModeLuminanceThreshold) Color.Black else Color.White
    }

@Composable
internal fun AppButtonTone.tonalColors(): ButtonColors {
    val content = color()
    val container =
        when (this) {
            AppButtonTone.Secondary ->
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = AppColors.SecondaryTonalContainerAlpha)
            else -> content.copy(alpha = AppColors.TintTonalAlpha)
        }
    return ButtonDefaults.filledTonalButtonColors(containerColor = container, contentColor = content)
}

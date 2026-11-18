package com.tk.quicksearch.shared.ui.components

import androidx.compose.animation.core.tween
import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.shared.ui.theme.pressMorphSegmentShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Shape
import com.tk.quicksearch.shared.ui.theme.AppShapes

/**
 * True inside a [SegmentedTile]. Shared rows such as [SettingsToggleRow] then
 * drop their dividers, since tiles are separated by their gap, and use the tile's inset.
 */
val LocalInSegmentedTile = staticCompositionLocalOf { false }

/**
 * Decoration a host applies to every segmented group header and tile, such as the welcome screen's
 * exit animation; identity elsewhere.
 */
val LocalSegmentedItemModifier = staticCompositionLocalOf<(Modifier) -> Modifier> { { it } }

/** Side and vertical inset of a row inside a [SegmentedTile]. */
val SegmentedTileContentPadding: Dp = DesignTokens.SpacingLarge

/**
 * Side inset of a [SettingsToggleRow] in a tile: lines its title up with free-form
 * [SegmentedTileContent], whose items keep their own 4dp inset.
 */
val SegmentedTileToggleHorizontalPadding: Dp = DesignTokens.SpacingXLarge

/**
 * Material 3 Expressive segmented settings list: each row is its own tile on the settings card
 * color, [DesignTokens.SegmentedListGap] apart, with large outer corners on the group and small
 * corners between tiles. A pressed tile rounds its inner corners toward the outer radius.
 *
 * The tile color comes from [AppColors.getSettingsCardContainerColor], so wallpaper, custom image,
 * AMOLED, and Material You stay correct without branching here.
 */
@Composable
fun SegmentedGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Column(
                modifier = LocalSegmentedItemModifier.current(Modifier).padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingMedium,
                ),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Accent,
                    modifier = Modifier.semantics { heading() },
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
            content = content,
        )
    }
}

/**
 * One tile at [index] of [count] in a [SegmentedGroup] (or any segmented list). [containerColor]
 * overrides the tile color, e.g. to highlight a tile being dragged. [backdrop] draws behind the
 * tile in its shape (search results use the frosted card backdrop); [contentPadding] insets the
 * content; [pressMorph] off keeps the inner corners fixed while pressed. [shape] replaces the
 * index-based shape, for tiles that animate their own corners.
 */
@Composable
fun SegmentedTile(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    containerColor: Color = AppColors.getSettingsCardContainerColor(),
    backdrop: @Composable (Modifier, Shape) -> Modifier = { m, _ -> m },
    contentPadding: PaddingValues = PaddingValues(),
    pressMorph: Boolean = true,
    shape: Shape? = null,
    content: @Composable () -> Unit,
) {
    val shape =
        shape
            ?: if (pressMorph) pressMorphSegmentShape(interactionSource, index, count) else AppShapes.segment(index, count)
    Box(
        modifier =
            modifier
                .then(LocalSegmentedItemModifier.current(Modifier))
                .fillMaxWidth()
                .then(backdrop(Modifier, shape))
                .clip(shape)
                .background(containerColor, shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(role = Role.Button, interactionSource = interactionSource, indication = androidx.compose.material3.ripple(), onClick = onClick)
                    } else {
                        Modifier
                    },
                ).padding(contentPadding),
    ) {
        CompositionLocalProvider(LocalInSegmentedTile provides true, content = content)
    }
}

/**
 * The settings switch: Material 3 size (52x32dp, never scaled), a check in the thumb while on, and
 * an unchecked track tinted from onSurface so it reads on tiles over wallpaper, custom image, and
 * AMOLED. Pass [onCheckedChange] null when the whole row toggles.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = appSwitchColors(),
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = appSwitchThumbContent(checked),
        colors = colors,
    )
}

/** Colors of [AppSwitch]; callers on special surfaces override the disabled colors. */
@Composable
fun appSwitchColors(): SwitchColors =
    SwitchDefaults.colors(
        uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.TintLightAlpha),
        uncheckedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.DisabledContentAlpha),
        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.DisabledContentAlpha),
    )

/** Check icon in a switch thumb while on, the Material 3 Expressive switch. */
@Composable
fun appSwitchThumbContent(checked: Boolean): (@Composable () -> Unit)? =
    if (checked) {
        {
            Icon(
                imageVector = MaterialSymbols.Check,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        }
    } else {
        null
    }

/**
 * Icon, title, and supporting text laid out inside a [SegmentedTile]. Pass [Alignment.Top] as
 * [verticalAlignment] for three-line content, as Material 3 top-aligns those list items.
 */
@Composable
fun SegmentedTileRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    /** Animates [description] in and out when toggled, for rows that reveal it on expand. */
    descriptionVisible: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = SegmentedTileMinHeight)
                .padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
        verticalAlignment = verticalAlignment,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        leading?.invoke()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall / 2),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description != null) {
                AnimatedVisibility(
                    visible = descriptionVisible,
                    enter = expandVertically(AppMotion.fastSpatial()) + fadeIn(tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedDecelerateEasing)),
                    exit = shrinkVertically(AppMotion.fastSpatial()) + fadeOut(tween(AppMotion.DurationExit, easing = AppMotion.EmphasizedAccelerateEasing)),
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        trailing?.invoke(this)
    }
}

/** A switch tile with a tonal icon badge; the whole tile toggles. */
@Composable
fun SegmentedToggleTile(
    title: String,
    description: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    count: Int,
    enabled: Boolean = true,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    SegmentedTile(
        index = index,
        count = count,
        interactionSource = interactionSource,
        modifier =
            Modifier
                .alpha(if (enabled) 1f else SegmentedTileDisabledAlpha),
    ) {
        SegmentedTileRow(
            title = title,
            description = description,
            modifier =
                Modifier.toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    interactionSource = interactionSource,
                    indication = androidx.compose.material3.ripple(),
                    onValueChange = { newValue ->
                        hapticToggle(view)()
                        onCheckedChange(newValue)
                    },
                ),
            leading = { TonalIconBadge(icon = icon) },
            trailing = {
                AppSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
            },
        )
    }
}

@Composable
fun TonalIconBadge(icon: ImageVector) {
    TonalIconBadgeContainer {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(TonalIconBadgeIconSize),
        )
    }
}

/**
 * The accent-tinted container of a [TonalIconBadge], for custom badge content. Circular at
 * [TonalIconBadgeSize] by default; smaller list badges pass their own [size] and [shape].
 */
@Composable
fun TonalIconBadgeContainer(
    size: Dp = TonalIconBadgeSize,
    shape: Shape = AppShapes.full,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(shape)
                .background(AppColors.AccentTintTonal),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private val SegmentedTileMinHeight = 72.dp
val TonalIconBadgeSize = 40.dp
val TonalIconBadgeIconSize = DesignTokens.IconSize
private const val SegmentedTileDisabledAlpha = 0.72f

/**
 * Inset for free-form content in a [SegmentedTile] (sliders, pickers, previews). Content that
 * keeps the old cards' inner 4dp offset lines up with toggle rows at 20dp.
 */
@Composable
fun SegmentedTileContent(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = SegmentedTileContentPadding, vertical = SegmentedTileContentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

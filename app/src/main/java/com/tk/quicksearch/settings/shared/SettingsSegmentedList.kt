package com.tk.quicksearch.settings.shared

import com.tk.quicksearch.onboarding.welcomeExitItem
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle

/**
 * True inside a [SettingsSegment]. Shared rows ([SettingsToggleRow], [SettingsCheckboxRow]) then
 * drop their dividers, since tiles are separated by their gap, and use the tile's inset.
 */
val LocalSettingsSegmented = staticCompositionLocalOf { false }

/** Side and vertical inset of a row inside a [SettingsSegment]. */
val SettingsSegmentRowPadding: Dp = DesignTokens.SpacingLarge

/**
 * Side inset of a [SettingsToggleRow] in a tile: lines its title up with free-form
 * [SettingsSegmentContent], whose items keep their own 4dp inset.
 */
val SettingsSegmentToggleHorizontalPadding: Dp = DesignTokens.SpacingXLarge

/**
 * Material 3 Expressive segmented settings list: each row is its own tile on the settings card
 * color, [DesignTokens.SegmentedListGap] apart, with large outer corners on the group and small
 * corners between tiles. A pressed tile rounds its inner corners toward the outer radius.
 *
 * The tile color comes from [AppColors.getSettingsCardContainerColor], so wallpaper, custom image,
 * AMOLED, and Material You stay correct without branching here.
 */
@Composable
fun SettingsSegmentGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Column(
                modifier = Modifier.welcomeExitItem().padding(
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
 * One tile at [index] of [count] in a [SettingsSegmentGroup]. [containerColor] overrides the tile
 * color, e.g. to highlight a tile being dragged.
 */
@Composable
fun SettingsSegment(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    containerColor: Color = AppColors.getSettingsCardContainerColor(),
    content: @Composable () -> Unit,
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val outer = DesignTokens.SegmentedListOuterCorner
    val inner by animateDpAsState(
        targetValue = if (pressed) SettingsSegmentPressedInnerCorner else DesignTokens.SegmentedListInnerCorner,
        animationSpec = DesignTokens.expressiveFastSpatialSpring(),
        label = "settingsSegmentInnerCorner",
    )
    val top = if (index <= 0) outer else inner
    val bottom = if (index >= count - 1) outer else inner
    val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    Box(
        modifier =
            modifier
                .welcomeExitItem()
                .fillMaxWidth()
                .clip(shape)
                .background(containerColor, shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(interactionSource = interactionSource, indication = androidx.compose.material3.ripple(), onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
    ) {
        CompositionLocalProvider(LocalSettingsSegmented provides true, content = content)
    }
}

/**
 * The settings switch: Material 3 size (52x32dp, never scaled), a check in the thumb while on, and
 * an unchecked track tinted from onSurface so it reads on tiles over wallpaper, custom image, and
 * AMOLED. Pass [onCheckedChange] null when the whole row toggles.
 */
@Composable
fun SettingsSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = settingsSwitchColors(),
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = settingsSwitchThumbContent(checked),
        colors = colors,
    )
}

/** Colors of [SettingsSwitch]; callers on special surfaces override the disabled colors. */
@Composable
fun settingsSwitchColors(): SwitchColors =
    SwitchDefaults.colors(
        uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        uncheckedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    )

/** Check icon in a switch thumb while on, the Material 3 Expressive switch. */
@Composable
fun settingsSwitchThumbContent(checked: Boolean): (@Composable () -> Unit)? =
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
 * Icon, title, and supporting text laid out inside a [SettingsSegment]. Pass [Alignment.Top] as
 * [verticalAlignment] for three-line content, as Material 3 top-aligns those list items.
 */
@Composable
fun SettingsSegmentRow(
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
                .heightIn(min = SettingsSegmentMinHeight)
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
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXXSmall / 2),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description != null) {
                AnimatedVisibility(
                    visible = descriptionVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
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

/** A [SettingsCardItem] as a tappable tile with a tonal icon badge. */
@Composable
fun SettingsSegmentNavigationRow(
    item: SettingsCardItem,
    index: Int,
    count: Int,
) {
    SettingsSegment(
        index = index,
        count = count,
        onClick = if (item.isEnabled) item.actionOnPress else null,
    ) {
        SettingsSegmentRow(
            title = item.title,
            description = item.description,
            leading = { SettingsIconBadge(item) },
        )
    }
}

/** A switch tile with a tonal icon badge; the whole tile toggles. */
@Composable
fun SettingsSegmentToggleRow(
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
    SettingsSegment(
        index = index,
        count = count,
        interactionSource = interactionSource,
        modifier =
            Modifier
                .alpha(if (enabled) 1f else SettingsSegmentDisabledAlpha),
    ) {
        SettingsSegmentRow(
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
            leading = { SettingsIconBadge(icon = icon) },
            trailing = {
                SettingsSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
            },
        )
    }
}

/**
 * Circular accent-tinted badge behind a settings icon. Vector icons take the accent; drawables
 * with their own colors (brand marks) keep them unless the item sets a tint.
 */
@Composable
fun SettingsIconBadge(item: SettingsCardItem) {
    SettingsIconBadgeContainer {
        when {
            item.iconBitmap != null ->
                Image(
                    bitmap = item.iconBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(SettingsIconBadgeIconSize),
                    contentScale = ContentScale.Fit,
                )
            item.icon != null ->
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.iconTint ?: AppColors.Accent,
                    modifier = Modifier.size(SettingsIconBadgeIconSize),
                )
            item.iconResId != null ->
                Icon(
                    painter = painterResource(id = item.iconResId),
                    contentDescription = null,
                    tint = item.iconTint ?: Color.Unspecified,
                    modifier = Modifier.size(SettingsIconBadgeIconSize),
                )
        }
    }
}

@Composable
fun SettingsIconBadge(icon: ImageVector) {
    SettingsIconBadgeContainer {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(SettingsIconBadgeIconSize),
        )
    }
}

@Composable
private fun SettingsIconBadgeContainer(content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier
                .size(SettingsIconBadgeSize)
                .clip(CircleShape)
                .background(AppColors.Accent.copy(alpha = SettingsIconBadgeAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Inner corner of a pressed segmented tile. */
private val SettingsSegmentPressedInnerCorner: Dp = 16.dp
private val SettingsSegmentMinHeight = 72.dp
private val SettingsIconBadgeSize = 40.dp
private val SettingsIconBadgeIconSize = 22.dp
private const val SettingsIconBadgeAlpha = 0.14f
private const val SettingsSegmentDisabledAlpha = 0.72f

/**
 * Inset for free-form content in a [SettingsSegment] (sliders, pickers, previews). Content that
 * keeps the old cards' inner 4dp offset lines up with toggle rows at 20dp.
 */
@Composable
fun SettingsSegmentContent(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsSegmentRowPadding, vertical = SettingsSegmentRowPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

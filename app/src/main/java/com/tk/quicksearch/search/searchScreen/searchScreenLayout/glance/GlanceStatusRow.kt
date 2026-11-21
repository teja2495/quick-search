package com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.mutableStateOf
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuDivider
import com.tk.quicksearch.shared.ui.components.AppButtonSize
import com.tk.quicksearch.shared.ui.components.AppButtonTone
import com.tk.quicksearch.shared.ui.components.AppOutlinedButton
import com.tk.quicksearch.shared.ui.components.AppButton
import com.tk.quicksearch.shared.ui.components.AppIconButton
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
import com.tk.quicksearch.search.searchHistory.QUERY_ROW_ICON_SIZE
import com.tk.quicksearch.search.searchHistory.QUERY_ROW_VERTICAL_PADDING
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.AppShapes

internal val GlanceDismissButtonSize = 28.dp

/** Same height as a search history row, so the home cards line up. */
private val GlanceRowMinHeight = (QUERY_ROW_ICON_SIZE + QUERY_ROW_VERTICAL_PADDING * 2).dp

@Composable
internal fun glanceRowMinHeight(cardHeight: Dp): Dp =
    if (LocalSearchCardLayoutEnabled.current) cardHeight else DesignTokens.MinTouchTarget

@Composable
internal fun glanceRowVerticalPadding(): Dp =
    if (LocalSearchCardLayoutEnabled.current) DesignTokens.SpacingMedium else DesignTokens.SpacingXSmall

/** Descriptions wrap in full rather than cutting off. */
internal const val GlanceTextMaxLines = Int.MAX_VALUE

/** A counter that bumps every time the screen resumes, for glances that re-read their source then. */
@Composable
internal fun rememberResumeRefreshKey(): Int {
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return refreshKey
}

/** Colors for the trailing value pill of a [GlanceStatusRow]. */
internal class GlancePillColors(
    val container: Color,
    val content: Color,
)

@Composable
internal fun glanceNeutralPillColors() =
    GlancePillColors(
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer,
    )

/**
 * The common At a Glance row layout, matching [LowBatteryRow]: a 24dp icon, a title with an
 * optional subtitle and extra content below it, an optional value pill, and an optional [trailing]
 * action or dismiss button outside the row's tap target, centered or, with [dismissAtTop], in the
 * top right corner. [pillWidthText] is the widest value the
 * pill can show; the pill always reserves its width, so a changing value doesn't shift the row.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GlanceStatusRow(
    icon: @Composable () -> Unit,
    title: String,
    /** Null leaves the row without a tap target; long press still opens its menu. */
    onClick: (() -> Unit)?,
    /**
     * The row's own long-press options, given a callback that closes the menu. The menu always
     * ends with the At a Glance settings item.
     */
    menuItems: (@Composable ColumnScope.(closeMenu: () -> Unit) -> Unit)? = null,
    /** Large text above the title, such as the distance to a navigation's next turn. */
    headline: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    titleMaxLines: Int = GlanceTextMaxLines,
    subtitle: String? = null,
    subtitleStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    pillText: String? = null,
    pillColors: GlancePillColors = glanceNeutralPillColors(),
    pillWidthText: String? = null,
    /** Shown before the pill's text, such as a navigation turn arrow. */
    pillIcon: (@Composable () -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    dismissAtTop: Boolean = false,
    belowText: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    iconSize: Dp = DesignTokens.IconSize,
) {
    val verticalPadding = glanceRowVerticalPadding()
    val openSettings = rememberOpenAtAGlanceSettings()
    var showMenu by remember { mutableStateOf(false) }
    val hasMenu = menuItems != null || openSettings != null
    val showOptionsLabel = stringResource(R.string.desc_show_options)
    val haptics = LocalHapticFeedback.current
    Row(
        modifier =
            Modifier.fillMaxWidth().heightIn(min = glanceRowMinHeight(GlanceRowMinHeight)).padding(
                start = DesignTokens.GlanceRowHorizontalPadding,
                end = if (onDismiss == null) DesignTokens.GlanceRowHorizontalPadding else 0.dp,
                top = verticalPadding,
                bottom = verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier =
                Modifier.weight(1f).then(
                    when {
                        onClick != null ->
                            Modifier.combinedClickable(
                                role = Role.Button,
                                onClick = onClick,
                                onLongClickLabel = showOptionsLabel,
                                onLongClick = if (hasMenu) ({ showMenu = true }) else null,
                            )
                        // No tap action, so no ripple on tap: only the long press opens the menu.
                        hasMenu ->
                            Modifier
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showMenu = true
                                        },
                                    )
                                }.semantics {
                                    onLongClick(label = showOptionsLabel) {
                                        showMenu = true
                                        true
                                    }
                                }
                        else -> Modifier
                    },
                ),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) { icon() }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.TextColumnSpacing),
            ) {
                if (headline != null) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = titleColor,
                        maxLines = GlanceTextMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = title,
                    style = titleStyle,
                    color = titleColor,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = subtitleStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = GlanceTextMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                belowText?.invoke()
            }
            if (pillText != null) {
                Box(
                    modifier =
                        Modifier
                            .clip(AppShapes.full)
                            .background(pillColors.container)
                            .padding(horizontal = DesignTokens.SpacingMedium, vertical = DesignTokens.SpacingXSmall),
                    contentAlignment = Alignment.Center,
                ) {
                    if (pillWidthText != null) {
                        Text(
                            text = pillWidthText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.alpha(0f).clearAndSetSemantics {},
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (pillIcon != null) {
                            CompositionLocalProvider(LocalContentColor provides pillColors.content) { pillIcon() }
                        }
                        Text(
                            text = pillText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = pillColors.content,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (hasMenu) {
                AppDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    val closeMenu = { showMenu = false }
                    menuItems?.invoke(this, closeMenu)
                    if (openSettings != null) {
                        if (menuItems != null) AppDropdownMenuDivider()
                        AtAGlanceSettingsMenuItem(openSettings = openSettings, closeMenu = closeMenu)
                    }
                }
            }
        }
        if (trailing != null) {
            Box(modifier = Modifier.padding(start = DesignTokens.SpacingSmall)) { trailing() }
        }
        if (onDismiss != null) {
            AppIconButton(
                onClick = onDismiss,
                tooltip = stringResource(R.string.common_close),
                modifier = Modifier.size(GlanceDismissButtonSize),
                anchorModifier = if (dismissAtTop) Modifier.align(Alignment.Top) else Modifier,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * A pill button for a row's own actions, such as a timer notification's Pause or Stop. Outlined by
 * default; with [colors] it is filled like a [GlanceStatusRow] value pill, with an optional [icon].
 */
@Composable
internal fun GlanceActionChip(
    label: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    colors: GlancePillColors? = null,
) {
    val content: @Composable RowScope.() -> Unit = {
        Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (colors != null) {
        AppButton(
            onClick = onClick,
            tone = AppButtonTone.Custom(colors.container, colors.content),
            size = AppButtonSize.ExtraSmall,
            icon = icon,
            content = content,
        )
    } else {
        AppOutlinedButton(
            onClick = onClick,
            tone = AppButtonTone.Custom(MaterialTheme.colorScheme.onSurface),
            size = AppButtonSize.ExtraSmall,
            icon = icon,
            content = content,
        )
    }
}

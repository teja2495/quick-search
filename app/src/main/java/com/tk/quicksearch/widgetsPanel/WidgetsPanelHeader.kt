package com.tk.quicksearch.widgetsPanel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppButtonTone
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.components.AppIconButtonStyle
import com.tk.quicksearch.shared.ui.components.AppTonalButton
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.homeTextColor

private const val HEADER_ACTION_FADE_MS = AppMotion.DurationExit
private const val HEADER_ACTION_INITIAL_SCALE = 0.85f

@Composable
internal fun WidgetsPanelHeader(
    hidden: Boolean,
    inEditMode: Boolean,
    layoutLocked: Boolean?,
    onLayoutLockChange: (Boolean) -> Unit,
    onAddWidget: () -> Unit,
    onExitEditMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val foreground = homeTextColor()
    val actionColors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = foreground.copy(alpha = AppColors.TintLightAlpha),
        contentColor = foreground,
        disabledContainerColor = foreground.copy(alpha = AppColors.TintFaintAlpha),
        disabledContentColor = foreground.copy(alpha = AppColors.DisabledContentAlpha),
    )
    // Fades out while a widget is dragged, keeping its space so the pages don't change size.
    val visibility by animateFloatAsState(
        targetValue = if (hidden) 0f else 1f,
        animationSpec = tween(HEADER_ACTION_FADE_MS, easing = AppMotion.EmphasizedEasing),
        label = "widgetsPanelHeaderVisibility",
    )
    Row(
        // A fixed minimum height keeps the pages the same size when the lock button hides.
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = visibility }
                .then(if (hidden) Modifier.clearAndSetSemantics { } else Modifier)
                .padding(vertical = DesignTokens.SpacingSmall)
                .heightIn(min = DesignTokens.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.widgets_panel_title),
            style = MaterialTheme.typography.headlineSmall,
            color = foreground,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (!inEditMode && layoutLocked != null) {
            WidgetLayoutLockButton(
                locked = layoutLocked,
                actionColors = actionColors,
                onCheckedChange = onLayoutLockChange,
            )
            Spacer(modifier = Modifier.width(DesignTokens.SpacingXSmall))
        }
        // Swap the add button for Done with a fade and springy scale instead of a hard cut.
        AnimatedContent(
            targetState = inEditMode,
            transitionSpec = {
                (fadeIn(tween(HEADER_ACTION_FADE_MS, easing = AppMotion.EmphasizedDecelerateEasing)) +
                    scaleIn(initialScale = HEADER_ACTION_INITIAL_SCALE, animationSpec = AppMotion.fastSpatial()))
                    .togetherWith(fadeOut(tween(HEADER_ACTION_FADE_MS, easing = AppMotion.EmphasizedAccelerateEasing)))
                    .using(SizeTransform(clip = false))
            },
            contentAlignment = Alignment.CenterEnd,
            label = "widgetPanelHeaderAction",
        ) { editing ->
            if (editing) {
                AppTonalButton(
                    onClick = onExitEditMode,
                    tone = AppButtonTone.Custom(foreground),
                    icon = MaterialSymbols.Check,
                ) {
                    Text(text = stringResource(R.string.dialog_done))
                }
            } else {
                AppIconButton(
                    onClick = onAddWidget,
                    tooltip = stringResource(R.string.common_action_add),
                    style = AppIconButtonStyle.Tonal,
                    enabled = layoutLocked == false,
                    modifier = Modifier.size(DesignTokens.Spacing40),
                    colors = actionColors,
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Add,
                        contentDescription = stringResource(R.string.common_action_add),
                        modifier = Modifier.size(DesignTokens.IconSize),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetLayoutLockButton(
    locked: Boolean,
    actionColors: IconButtonColors,
    onCheckedChange: (Boolean) -> Unit,
) {
    val lockLabel = stringResource(R.string.widgets_panel_lock_layout)
    val tooltip = if (locked) stringResource(R.string.widgets_panel_unlock_layout) else lockLabel
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
    ) {
        FilledTonalIconToggleButton(
            checked = locked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.minimumInteractiveComponentSize().size(DesignTokens.Spacing40),
            colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                containerColor = actionColors.containerColor,
                contentColor = actionColors.contentColor,
                checkedContainerColor = actionColors.containerColor,
                checkedContentColor = actionColors.contentColor,
            ),
        ) {
            Icon(
                imageVector = if (locked) MaterialSymbols.Lock else MaterialSymbols.LockOpen,
                contentDescription = tooltip,
                modifier = Modifier.size(DesignTokens.IconSize),
            )
        }
    }
}

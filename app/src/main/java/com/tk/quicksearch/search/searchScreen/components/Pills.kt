package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.ui.graphics.RectangleShape
import com.tk.quicksearch.search.searchScreen.insetCardBackdrop
import com.tk.quicksearch.search.searchScreen.insetCardBackground
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.searchEngines.extendToScreenEdges
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.ui.theme.LocalDeviceDynamicColorsActive

private val PhoneCallPillIconSize = 14.dp

@Composable
internal fun KeyboardSwitchPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = LocalAppIsDarkTheme.current
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val labelColor = if (isDarkTheme) Color.White else Color.Black
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor =
        if (isDarkTheme) {
            AppColors.Accent.copy(alpha = 0.22f)
        } else {
            Color.Black.copy(alpha = 0.1f)
        }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = DesignTokens.ShapeFull,
        color = backgroundColor,
        border = BorderStroke(DesignTokens.KeyboardPillBorderStrokeWidth, borderColor),
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(
                        horizontal = DesignTokens.SpacingMedium,
                        vertical = DesignTokens.SpacingXSmall,
                    ).height(DesignTokens.IconSize),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun PhoneCallPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDarkTheme = LocalAppIsDarkTheme.current
    val phoneAppIcon =
        remember(context) {
            runCatching {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                val resolveInfo = context.packageManager.resolveActivity(dialIntent, 0)
                resolveInfo?.loadIcon(context.packageManager)?.toBitmap()?.asImageBitmap()
            }.getOrNull()
        }
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor =
        if (isDarkTheme) {
            AppColors.Accent.copy(alpha = 0.22f)
        } else {
            Color.Black.copy(alpha = 0.1f)
        }

    Surface(
        modifier = modifier,
        onClick = onClick,
        interactionSource = interactionSource,
        shape = DesignTokens.ShapeFull,
        color = if (isDarkTheme) Color.Black else Color.White,
        border = BorderStroke(DesignTokens.KeyboardPillBorderStrokeWidth, borderColor),
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(
                        horizontal = DesignTokens.SpacingMedium,
                        vertical = DesignTokens.SpacingXSmall,
                    )
                    .height(DesignTokens.IconSize),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (phoneAppIcon != null) {
                Image(
                    painter = BitmapPainter(phoneAppIcon),
                    contentDescription = stringResource(R.string.contact_method_call_label),
                    modifier = Modifier.size(PhoneCallPillIconSize),
                )
            } else {
                Icon(
                    imageVector = MaterialSymbols.Call,
                    contentDescription = stringResource(R.string.contact_method_call_label),
                    tint = AppColors.ActionPhone,
                    modifier = Modifier.size(PhoneCallPillIconSize),
                )
            }
            Spacer(modifier = Modifier.size(DesignTokens.SpacingXSmall))
            Text(
                text = stringResource(R.string.contact_method_call_label),
                style = MaterialTheme.typography.labelSmall,
                color = if (isDarkTheme) Color.White else Color.Black,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun SetAlarmPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = ClockActionPill(
    icon = MaterialSymbols.Alarm,
    label = stringResource(R.string.set_alarm_action_label),
    onClick = onClick,
    modifier = modifier,
)

@Composable
internal fun AllAppsPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ClockActionPill(
            icon = MaterialSymbols.Apps,
            label = stringResource(R.string.settings_app_shortcuts_filter_all_apps),
            onClick = onClick,
        )
    }
}

@Composable
internal fun StartTimerPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = ClockActionPill(
    icon = MaterialSymbols.Timer,
    label = stringResource(R.string.start_timer_action_label),
    onClick = onClick,
    modifier = modifier,
)

@Composable
internal fun CreateReminderPill(
    onClick: () -> Unit,
    useShortLabel: Boolean = false,
    modifier: Modifier = Modifier,
) = ClockActionPill(
    icon = MaterialSymbols.Notifications,
    label = stringResource(if (useShortLabel) R.string.reminder_name_hint else R.string.app_setting_create_reminder),
    onClick = onClick,
    modifier = modifier,
)

@Composable
private fun ClockActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = LocalAppIsDarkTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor =
        if (isDarkTheme) {
            AppColors.Accent.copy(alpha = 0.22f)
        } else {
            Color.Black.copy(alpha = 0.1f)
        }

    Surface(
        modifier = modifier,
        onClick = onClick,
        interactionSource = interactionSource,
        shape = DesignTokens.ShapeFull,
        color = if (isDarkTheme) Color.Black else Color.White,
        border = BorderStroke(DesignTokens.KeyboardPillBorderStrokeWidth, borderColor),
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(
                        horizontal = DesignTokens.SpacingMedium,
                        vertical = DesignTokens.SpacingXSmall,
                    )
                    .height(DesignTokens.IconSize),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(PhoneCallPillIconSize),
            )
            Spacer(modifier = Modifier.size(DesignTokens.SpacingXSmall))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDarkTheme) Color.White else Color.Black,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun OpenKeyboardAction(
    text: String,
    onClick: () -> Unit,
    onVoiceClick: () -> Unit,
    showWallpaperBackground: Boolean,
    modifier: Modifier = Modifier,
    showMic: Boolean = true,
) {
    val isDarkTheme = LocalAppIsDarkTheme.current
    val backgroundColor =
        insetCardBackground(true, AppColors.getSearchEngineSectionBackground(showWallpaperBackground))
    val labelColor =
        if (showWallpaperBackground && LocalDeviceDynamicColorsActive.current) {
            MaterialTheme.colorScheme.onSurface
        } else if (isDarkTheme) {
            Color.White
        } else {
            Color.Black
        }
    Surface(
        modifier = modifier.fillMaxWidth().insetCardBackdrop(true, RectangleShape),
        color = backgroundColor,
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 75.dp)) {
            Row(
                modifier = Modifier.matchParentSize().clickable(onClick = onClick),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Keyboard,
                    contentDescription = null,
                    tint = labelColor,
                    modifier = Modifier.size(DesignTokens.IconSize),
                )
                Spacer(modifier = Modifier.size(DesignTokens.SpacingSmall))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = labelColor,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (showMic) {
                Surface(
                    onClick = onVoiceClick,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = DesignTokens.SpacingXLarge).size(40.dp),
                    shape = CircleShape,
                    color = labelColor.copy(alpha = 0.1f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = MaterialSymbols.Mic,
                            contentDescription = stringResource(R.string.desc_voice_search_icon),
                            tint = labelColor,
                            modifier = Modifier.size(DesignTokens.IconSize),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun OverlayExpandPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = DesignTokens.ShapeFull,
        color = AppColors.OverlayMedium,
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(
                        horizontal = DesignTokens.SpacingMedium,
                        vertical = DesignTokens.SpacingXSmall,
                    ).height(DesignTokens.IconSize),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.size(DesignTokens.SpacingXSmall))
            Icon(
                imageVector = MaterialSymbols.ExpandMore,
                contentDescription = stringResource(R.string.desc_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
internal fun NumberKeyboardOperatorPills(
    onOperatorClick: (String) -> Unit,
    isOverlayPresentation: Boolean = false,
    extendToScreenEdges: Boolean = true,
    showWallpaperBackground: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val operators = remember { listOf("+", "-", "×", "/", "(", ")") }
    val isDarkTheme = LocalAppIsDarkTheme.current
    val containerBackgroundColor =
        when {
            showWallpaperBackground ->
                if (isDarkTheme) Color.Black else Color.White
            isOverlayPresentation -> MaterialTheme.colorScheme.surface
            MaterialTheme.colorScheme.surface == MaterialTheme.colorScheme.background &&
                MaterialTheme.colorScheme.background.red < 0.1f &&
                MaterialTheme.colorScheme.background.green < 0.1f &&
                MaterialTheme.colorScheme.background.blue < 0.1f ->
                AppColors.SearchBarBackground
            else -> MaterialTheme.colorScheme.surface
        }
    val operatorChipColor = AppColors.KeyboardPillBackground
    val operatorChipTextColor = AppColors.KeyboardPillText
    val operatorChipBorderColor = AppColors.SearchChromeOutlineBorder

    Surface(
        modifier = if (extendToScreenEdges) modifier.extendToScreenEdges() else modifier,
        color = containerBackgroundColor,
        shape = RoundedCornerShape(ZeroCornerSize),
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(
                        start = DesignTokens.SpacingLarge,
                        end = DesignTokens.SpacingLarge,
                        top = 9.dp,
                        bottom = 7.dp,
                    ),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            operators.forEach { operator ->
                Surface(
                    modifier =
                        Modifier.weight(1f).clickable { onOperatorClick(operator) },
                    shape = DesignTokens.ShapeFull,
                    color = operatorChipColor,
                    border = BorderStroke(DesignTokens.KeyboardPillBorderStrokeWidth, operatorChipBorderColor),
                    tonalElevation = DesignTokens.ElevationLevel0,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .padding(
                                    vertical = 2.dp,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = operator,
                            style = MaterialTheme.typography.titleMedium,
                            color = operatorChipTextColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

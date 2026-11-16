package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.ui.graphics.RectangleShape
import com.tk.quicksearch.search.searchScreen.insetCardBackdrop
import com.tk.quicksearch.search.searchScreen.insetCardBackground
import android.content.Intent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.searchEngines.extendToScreenEdges
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.ui.theme.LocalDeviceDynamicColorsActive

private val QuickActionIconSize = 16.dp

/** Space above the number keyboard operator row, inside its band. */
internal val NumberKeyboardOperatorRowTopPadding = 9.dp
private val QuickActionButtonHeight = 32.dp
private val QuickActionPressedCorner = 8.dp
private val QuickActionMinFontSize = 10.sp

/** Container opacity of the search quick actions (50% transparent). */
private const val QuickActionTranslucentAlpha = 0.5f

/**
 * Material 3 Expressive extra-small button for the search screen's quick actions (Number Keyboard,
 * Call, Set Alarm, Start Timer, Reminder, All Apps): 32dp tall, fully round corners that square to
 * 8dp on press on the expressive fast spatial spring. Filled tonal
 * (`secondaryContainer`/`onSecondaryContainer`); the search quick actions draw the container 50%
 * transparent, while All Apps keeps it opaque. Content padding is symmetric so the icon and
 * label stay centered in the button. The label shrinks (down to 10sp) rather than wrapping or
 * ellipsizing, so a row of quick actions always fits on one line.
 */
@Composable
private fun QuickActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    translucent: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) QuickActionPressedCorner else QuickActionButtonHeight / 2,
        animationSpec = DesignTokens.expressiveFastSpatialSpring(),
        label = "quickActionCorner",
    )
    val labelStyle = MaterialTheme.typography.labelLarge
    Surface(
        modifier = modifier.height(QuickActionButtonHeight),
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(corner),
        color =
            MaterialTheme.colorScheme.secondaryContainer.let { container ->
                if (translucent) container.copy(alpha = container.alpha * QuickActionTranslucentAlpha) else container
            },
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DesignTokens.SpacingLarge),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
            }
            Text(
                text = label,
                style = labelStyle,
                maxLines = 1,
                // Without this the label wraps onto a clipped second line instead of auto-shrinking.
                softWrap = false,
                autoSize =
                    TextAutoSize.StepBased(
                        minFontSize = QuickActionMinFontSize,
                        maxFontSize = labelStyle.fontSize,
                    ),
            )
        }
    }
}

@Composable
private fun QuickActionVectorIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(QuickActionIconSize),
    )
}

@Composable
internal fun KeyboardSwitchPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
) = QuickActionButton(
    label = text,
    onClick = onClick,
    modifier = modifier,
    leadingIcon = if (showIcon) ({ QuickActionVectorIcon(MaterialSymbols.Keyboard) }) else null,
)

@Composable
internal fun PhoneCallPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
) {
    val context = LocalContext.current
    val phoneAppIcon =
        remember(context) {
            runCatching {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                val resolveInfo = context.packageManager.resolveActivity(dialIntent, 0)
                resolveInfo?.loadIcon(context.packageManager)?.toBitmap()?.asImageBitmap()
            }.getOrNull()
        }
    QuickActionButton(
        label = stringResource(R.string.contact_method_call_label),
        onClick = onClick,
        modifier = modifier,
        leadingIcon = if (!showIcon) null else {
            {
            if (phoneAppIcon != null) {
                Image(
                    painter = BitmapPainter(phoneAppIcon),
                    contentDescription = null,
                    modifier = Modifier.size(QuickActionIconSize),
                )
            } else {
                Icon(
                    imageVector = MaterialSymbols.Call,
                    contentDescription = null,
                    tint = AppColors.ActionPhone,
                    modifier = Modifier.size(QuickActionIconSize),
                )
            }
            }
        },
    )
}

@Composable
internal fun SetAlarmPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
) = ClockActionPill(
    icon = MaterialSymbols.Alarm,
    label = stringResource(R.string.set_alarm_action_label),
    onClick = onClick,
    modifier = modifier,
    showIcon = showIcon,
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
            translucent = false,
        )
    }
}

@Composable
internal fun StartTimerPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
) = ClockActionPill(
    icon = MaterialSymbols.Timer,
    label = stringResource(R.string.start_timer_action_label),
    onClick = onClick,
    modifier = modifier,
    showIcon = showIcon,
)

@Composable
internal fun CreateReminderPill(
    onClick: () -> Unit,
    useShortLabel: Boolean = false,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
) = ClockActionPill(
    icon = MaterialSymbols.Notifications,
    label = stringResource(if (useShortLabel) R.string.reminder_name_hint else R.string.app_setting_create_reminder),
    onClick = onClick,
    modifier = modifier,
    showIcon = showIcon,
)

@Composable
private fun ClockActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    translucent: Boolean = true,
) = QuickActionButton(
    label = label,
    onClick = onClick,
    modifier = modifier,
    leadingIcon = if (showIcon) ({ QuickActionVectorIcon(icon) }) else null,
    translucent = translucent,
)

/**
 * Home's bottom keyboard strip: a Material 3 Expressive connected button group. Voice search and
 * Open Keyboard sit 2dp apart with pill outer corners and tight inner corners; a pressed button
 * squares its corners on the expressive fast spatial spring. Buttons take a light tint of the strip's
 * foreground, so they stay neutral over every background (theme gradient, wallpaper, custom image,
 * AMOLED, Material You).
 */
@Composable
internal fun OpenKeyboardAction(
    text: String,
    onClick: () -> Unit,
    onVoiceClick: () -> Unit,
    showWallpaperBackground: Boolean,
    modifier: Modifier = Modifier,
    showMic: Boolean = true,
    extendBehindNavigationBar: Boolean = false,
    padTop: Boolean = false,
) {
    val backgroundColor =
        insetCardBackground(true, AppColors.getSearchEngineSectionBackground(showWallpaperBackground))
    // Buttons are tinted from the strip's own foreground, so they sit in the same neutral family as
    // the strip and the cards above it in every theme instead of introducing a new hue.
    val contentColor =
        if (showWallpaperBackground && LocalDeviceDynamicColorsActive.current) {
            MaterialTheme.colorScheme.onSurface
        } else if (LocalAppIsDarkTheme.current) {
            Color.White
        } else {
            Color.Black
        }
    val buttonColors = ConnectedButtonColors(container = contentColor.copy(alpha = ConnectedButtonTintAlpha), content = contentColor)
    // The strip ends at the navigation bar inset; carry its colour on down to the screen edge so it
    // reads as one surface behind the gesture bar instead of stopping short above it.
    val navigationBarBottom =
        if (extendBehindNavigationBar) WindowInsets.navigationBars.getBottom(LocalDensity.current) else 0
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    if (navigationBarBottom > 0) {
                        drawRect(
                            color = backgroundColor,
                            topLeft = Offset(0f, size.height),
                            size = Size(size.width, navigationBarBottom.toFloat()),
                        )
                    }
                }.insetCardBackdrop(true, RectangleShape),
        color = backgroundColor,
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    // An engine strip above already ends with its own bottom padding; without one
                    // ([padTop]) the band needs the same space above the buttons as below them.
                    .padding(
                        start = DesignTokens.ContentHorizontalPadding,
                        top = if (padTop) DesignTokens.SpacingXLarge else 0.dp,
                        end = DesignTokens.ContentHorizontalPadding,
                        bottom = DesignTokens.SpacingXLarge,
                    ),
            horizontalArrangement = Arrangement.spacedBy(ConnectedButtonGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showMic) {
                ConnectedGroupButton(
                    onClick = onVoiceClick,
                    roundStart = true,
                    roundEnd = false,
                    colors = buttonColors,
                    modifier = Modifier.width(VoiceButtonWidth),
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Mic,
                        contentDescription = stringResource(R.string.desc_voice_search_icon),
                        modifier = Modifier.size(DesignTokens.IconSize),
                    )
                }
            }
            ConnectedGroupButton(
                onClick = onClick,
                roundStart = !showMic,
                roundEnd = true,
                colors = buttonColors,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Keyboard,
                    contentDescription = null,
                    modifier = Modifier.size(DesignTokens.IconSize),
                )
                Spacer(modifier = Modifier.size(DesignTokens.SpacingSmall))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
            }
        }
    }
}

private data class ConnectedButtonColors(val container: Color, val content: Color)

private const val ConnectedButtonTintAlpha = 0.1f
private val ConnectedButtonGap = 2.dp
private val ConnectedButtonHeight = 56.dp
private val VoiceButtonWidth = 72.dp
private val ConnectedOuterCorner = 28.dp
private val ConnectedOuterCornerPressed = 16.dp
private val ConnectedInnerCorner = 8.dp
private val ConnectedInnerCornerPressed = 4.dp

@Composable
private fun ConnectedGroupButton(
    onClick: () -> Unit,
    roundStart: Boolean,
    roundEnd: Boolean,
    colors: ConnectedButtonColors,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val cornerSpec = DesignTokens.expressiveFastSpatialSpring<Dp>()
    val outer by animateDpAsState(
        targetValue = if (pressed) ConnectedOuterCornerPressed else ConnectedOuterCorner,
        animationSpec = cornerSpec,
        label = "connectedOuterCorner",
    )
    val inner by animateDpAsState(
        targetValue = if (pressed) ConnectedInnerCornerPressed else ConnectedInnerCorner,
        animationSpec = cornerSpec,
        label = "connectedInnerCorner",
    )
    val start = if (roundStart) outer else inner
    val end = if (roundEnd) outer else inner
    Surface(
        onClick = onClick,
        modifier = modifier.height(ConnectedButtonHeight),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end),
        color = colors.container,
        contentColor = colors.content,
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = DesignTokens.SpacingLarge),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
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
    // Drawn inside a host surface (the bottom search card), whose background shows through.
    transparentBackground: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val operators = remember { listOf("+", "-", "×", "/", "(", ")") }
    val isDarkTheme = LocalAppIsDarkTheme.current
    val containerBackgroundColor =
        when {
            transparentBackground -> Color.Transparent
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
                        // Inside a host card the row above already ends with its own spacing.
                        top = if (transparentBackground) 0.dp else NumberKeyboardOperatorRowTopPadding,
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

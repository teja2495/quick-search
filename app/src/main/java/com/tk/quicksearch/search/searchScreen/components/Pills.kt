package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.tk.quicksearch.shared.ui.components.AppButtonTone
import com.tk.quicksearch.shared.ui.theme.AppShapes
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.floor
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.tools.tickTick.TickTickTaskHandler
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.searchEngines.extendToScreenEdges
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.pressMorphButtonShape
import com.tk.quicksearch.shared.ui.components.ConnectedButtonRow

private val QuickActionIconSize = 16.dp

/** Space above the number keyboard operator row, inside its band. */
internal val NumberKeyboardOperatorRowTopPadding = DesignTokens.SpacingSmall
private val QuickActionMinFontSize = 10.sp

/** Auto-size step in sp, shared by the labels and the size they report to [QuickActionLabelSizing]. */
private const val AutoSizeStep = 0.25f

/** Container opacity of the search quick actions (50% transparent). */
private const val QuickActionTranslucentAlpha = 0.5f

/**
 * Shared label size for one row of quick actions. Each label auto-shrinks on its own, so a longer
 * label would otherwise end up smaller than its neighbours; every label reports the size it settled
 * on and the row caps all of them at the smallest. Recreate it when the row's buttons change.
 */
@Stable
internal class QuickActionLabelSizing {
    var fontSize by mutableStateOf(TextUnit.Unspecified)
        private set

    fun report(size: TextUnit) {
        if (!size.isSp) return
        if (!fontSize.isSpecified || size < fontSize) fontSize = size
    }
}

internal val LocalQuickActionLabelSizing = staticCompositionLocalOf<QuickActionLabelSizing?> { null }

/**
 * Material 3 Expressive extra-small button for the search screen's quick actions (Number Keyboard,
 * Call, Set Alarm, Start Timer, Reminder, All Apps): 32dp minimum visual height, round corners that square to
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
    val labelStyle = MaterialTheme.typography.labelLarge
    val labelSizing = LocalQuickActionLabelSizing.current
    val sharedFontSize = labelSizing?.fontSize ?: TextUnit.Unspecified
    val maxFontSize =
        if (sharedFontSize.isSpecified && sharedFontSize < labelStyle.fontSize) sharedFontSize else labelStyle.fontSize
    val textMeasurer = rememberTextMeasurer()
    val fullLabelWidth =
        remember(label, labelStyle, textMeasurer) {
            if (labelSizing == null) 0 else textMeasurer.measure(label, labelStyle, maxLines = 1, softWrap = false).size.width
        }
    Surface(
        modifier = modifier.minimumInteractiveComponentSize().heightIn(min = DesignTokens.ButtonHeightExtraSmall),
        onClick = onClick,
        interactionSource = interactionSource,
        shape = pressMorphButtonShape(interactionSource, DesignTokens.ButtonHeightExtraSmall, DesignTokens.ButtonPressedCornerSmall),
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
                        maxFontSize = maxFontSize,
                        stepSize = AutoSizeStep.sp,
                    ),
                onTextLayout = { result ->
                    // The layout result keeps the unshrunk style, so derive the size that fits the
                    // label's width; text width scales linearly with font size.
                    val available = result.layoutInput.constraints.maxWidth
                    if (labelSizing != null && available != Constraints.Infinity && fullLabelWidth > 0) {
                        val fullSize = labelStyle.fontSize.value
                        val fitted =
                            if (fullLabelWidth <= available) {
                                fullSize
                            } else {
                                floor(fullSize * available / fullLabelWidth / AutoSizeStep) * AutoSizeStep
                            }
                        labelSizing.report(fitted.coerceAtLeast(QuickActionMinFontSize.value).sp)
                    }
                },
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
internal fun TickTickTaskPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
) {
    val appIcon = rememberAppIcon(TickTickTaskHandler.PACKAGE_NAME).bitmap
    QuickActionButton(
        label = stringResource(R.string.ticktick_app_name),
        onClick = onClick,
        modifier = modifier,
        leadingIcon = if (!showIcon) null else {
            {
                if (appIcon != null) {
                    Image(
                        painter = BitmapPainter(appIcon),
                        contentDescription = null,
                        modifier = Modifier.size(QuickActionIconSize),
                    )
                } else {
                    QuickActionVectorIcon(MaterialSymbols.CheckCircle)
                }
            }
        },
    )
}

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
    val contentColor = MaterialTheme.colorScheme.onSurface
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
        ConnectedButtonRow(
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
            tone = AppButtonTone.Custom(contentColor),
        ) {
            if (showMic) {
                button(onClick = onVoiceClick, width = VoiceButtonWidth) {
                    Icon(
                        imageVector = MaterialSymbols.Mic,
                        contentDescription = stringResource(R.string.desc_voice_search_icon),
                        modifier = Modifier.size(DesignTokens.IconSize),
                    )
                }
            }
            button(onClick = onClick) {
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


private val VoiceButtonWidth = 72.dp

@Composable
internal fun OverlayExpandPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        shape = AppShapes.full,
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
            )
            Spacer(modifier = Modifier.size(DesignTokens.SpacingXSmall))
            Icon(
                imageVector = MaterialSymbols.ExpandMore,
                contentDescription = stringResource(R.string.desc_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DesignTokens.ButtonIconSizeExtraSmall),
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
    val containerBackgroundColor =
        when {
            transparentBackground -> Color.Transparent
            // Same frosted band as the engine strip it sits under.
            showWallpaperBackground ->
                insetCardBackground(true, AppColors.getSearchEngineSectionBackground(true))
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
        modifier =
            (if (extendToScreenEdges) modifier.extendToScreenEdges() else modifier)
                .insetCardBackdrop(showWallpaperBackground && !transparentBackground, RectangleShape),
        color = containerBackgroundColor,
        shape = AppShapes.none,
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().padding(
                start = DesignTokens.SpacingLarge,
                end = DesignTokens.SpacingLarge,
                top = if (transparentBackground) 0.dp else NumberKeyboardOperatorRowTopPadding,
                bottom = DesignTokens.SpacingSmall,
            ),
        ) {
            val spacing = DesignTokens.SpacingXSmall
            val keyWidth = ((maxWidth - spacing * operators.lastIndex) / operators.size)
                .coerceAtLeast(DesignTokens.MinTouchTarget)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                operators.forEach { operator ->
                    Surface(
                        onClick = { onOperatorClick(operator) },
                        modifier = Modifier.width(keyWidth).semantics { role = Role.Button },
                        shape = AppShapes.full,
                        color = operatorChipColor,
                        border = BorderStroke(DesignTokens.KeyboardPillBorderStrokeWidth, operatorChipBorderColor),
                    ) {
                        Box(
                            modifier = Modifier.height(DesignTokens.MinTouchTarget),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = operator,
                                style = MaterialTheme.typography.titleMedium,
                                color = operatorChipTextColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

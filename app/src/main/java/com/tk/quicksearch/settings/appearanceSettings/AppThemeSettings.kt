package com.tk.quicksearch.settings.AppearanceSettings

import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.os.Build
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tk.quicksearch.R
import com.tk.quicksearch.app.HomeActivity
import com.tk.quicksearch.search.core.AccentColorMode
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.search.searchScreen.AppThemeColors
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.widgets.WidgetConfigScreen.components.WidgetColorPickerDialog
import kotlin.math.roundToInt

@Composable
fun AppThemeCard(
        selectedTheme: AppTheme,
        overlayThemeIntensity: Float,
        onThemeSelected: (AppTheme) -> Unit,
        onOverlayThemeIntensityChange: (Float) -> Unit,
        backgroundSource: BackgroundSource,
        onSetBackgroundSource: (BackgroundSource) -> Unit,
        appThemeMode: AppThemeMode,
        onSetAppThemeMode: (AppThemeMode) -> Unit,
        hasWallpaperPermission: Boolean,
        themedIconsEnabled: Boolean,
        onThemedIconsToggle: (Boolean) -> Unit,
        deviceThemeEnabled: Boolean,
        onDeviceThemeToggle: (Boolean) -> Unit,
        amoledThemeEnabled: Boolean,
        onAmoledThemeToggle: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val canShowSystemWallpaperBackdrop =
            (context as? HomeActivity)?.canShowSystemWallpaperBackdrop == true
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val useMonoThemeFallback =
            backgroundSource == BackgroundSource.SYSTEM_WALLPAPER &&
                    !hasWallpaperPermission &&
                    !canShowSystemWallpaperBackdrop
    val effectiveBackgroundSource =
            if (useMonoThemeFallback) BackgroundSource.THEME else backgroundSource
    val effectiveSelectedTheme =
            if (useMonoThemeFallback) AppTheme.MONOCHROME else selectedTheme
    val isThemeSourceSelected = effectiveBackgroundSource == BackgroundSource.THEME
    val showThemedIconsToggle = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val showMaterialYouToggle = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val showAmoledThemeToggle =
            appThemeMode == AppThemeMode.DARK &&
                    !deviceThemeEnabled &&
                    isThemeSourceSelected &&
                    effectiveSelectedTheme == AppTheme.MONOCHROME

    val minIntensity = UiPreferences.MIN_OVERLAY_THEME_INTENSITY
    val maxIntensity = UiPreferences.MAX_OVERLAY_THEME_INTENSITY
    val intensityStep = UiPreferences.OVERLAY_THEME_INTENSITY_STEP
    val intensitySteps = (UiPreferences.OVERLAY_THEME_INTENSITY_DELTA_STEPS * 2) - 1
    var lastToneStep by remember {
        mutableStateOf(
                ((overlayThemeIntensity - minIntensity) / intensityStep)
                        .roundToInt()
                        .coerceIn(0, UiPreferences.OVERLAY_THEME_INTENSITY_DELTA_STEPS * 2),
        )
    }

    val themeOptions =
            remember {
                listOf(
                        AppThemeOption(
                                theme = AppTheme.MONOCHROME,
                                labelRes = R.string.settings_app_theme_monochrome,
                        ),
                        AppThemeOption(
                                theme = AppTheme.FOREST,
                                labelRes = R.string.settings_app_theme_forest,
                        ),
                        AppThemeOption(
                                theme = AppTheme.AURORA,
                                labelRes = R.string.settings_app_theme_aurora,
                        ),
                        AppThemeOption(
                                theme = AppTheme.SUNSET,
                                labelRes = R.string.settings_app_theme_sunset,
                        ),
                )
            }

    SettingsCard(
            modifier = modifier.fillMaxWidth(),
    ) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                    text = stringResource(R.string.settings_app_theme_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                            modifier = Modifier.fillMaxWidth().selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                            verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppModeOption(
                                label = stringResource(R.string.common_theme_light),
                                icon = Icons.Rounded.LightMode,
                                selected = appThemeMode == AppThemeMode.LIGHT,
                                onClick = { onSetAppThemeMode(AppThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f),
                        )
                        AppModeOption(
                                label = stringResource(R.string.common_theme_dark),
                                icon = Icons.Rounded.DarkMode,
                                selected = appThemeMode == AppThemeMode.DARK,
                                onClick = { onSetAppThemeMode(AppThemeMode.DARK) },
                                modifier = Modifier.weight(1f),
                        )
                        AppModeOption(
                                label = stringResource(R.string.common_theme_system),
                                icon = Icons.Rounded.Settings,
                                selected = appThemeMode == AppThemeMode.SYSTEM,
                                onClick = { onSetAppThemeMode(AppThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f),
                        )
                    }

                    HorizontalDivider(
                            color = AppColors.SettingsDivider,
                            modifier = Modifier.padding(horizontal = 75.dp, vertical = 12.dp),
                    )

                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        themeOptions.forEach { option ->
                            val selectedThemeForDisplay =
                                    if (deviceThemeEnabled) AppTheme.MONOCHROME else effectiveSelectedTheme
                            val isSelected = selectedThemeForDisplay == option.theme && isThemeSourceSelected
                            val showSelection = isSelected && !deviceThemeEnabled
                            val interactionSource = remember { MutableInteractionSource() }
                            Column(
                                    modifier =
                                            Modifier.weight(1f)
                                                    .clickable(
                                                            interactionSource = interactionSource,
                                                            indication = null,
                                                    ) {
                                                        if (deviceThemeEnabled) {
                                                            Toast.makeText(
                                                                    context,
                                                                    context.getString(R.string.settings_device_theme_blocked_toast),
                                                                    Toast.LENGTH_SHORT,
                                                            ).show()
                                                        } else {
                                                            if (!isSelected) {
                                                                hapticToggle(view)()
                                                            }
                                                            onThemeSelected(option.theme)
                                                            onSetBackgroundSource(BackgroundSource.THEME)
                                                        }
                                                    },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .height(44.dp)
                                                        .clip(MaterialTheme.shapes.medium)
                                                        .background(
                                                                Brush.linearGradient(
                                                                        colors =
                                                                                if (
                                                                                        amoledThemeEnabled &&
                                                                                                option.theme == AppTheme.MONOCHROME &&
                                                                                                isDarkMode
                                                                                ) {
                                                                                    listOf(
                                                                                            Color.Black,
                                                                                            Color.Black,
                                                                                            Color.Black,
                                                                                            Color.Black,
                                                                                    )
                                                                                } else {
                                                                                    AppThemeColors(
                                                                                            theme = option.theme,
                                                                                            isDarkMode = isDarkMode,
                                                                                            intensity = overlayThemeIntensity,
                                                                                    )
                                                                                },
                                                                ),
                                                        )
                                                        .border(
                                                                width = DesignTokens.BorderWidth,
                                                                color = AppColors.SettingsDivider,
                                                                shape = MaterialTheme.shapes.medium,
                                                        ),
                                        contentAlignment = Alignment.Center,
                                ) {
                                    if (showSelection) {
                                        Box(
                                                modifier =
                                                        Modifier.size(22.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.primary),
                                                contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                }
                                Text(
                                        text = stringResource(option.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color =
                                                if (showSelection) {
                                                    MaterialTheme.colorScheme.onSurface
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    if (isThemeSourceSelected && !deviceThemeEnabled) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                    imageVector = Icons.Rounded.LightMode,
                                    contentDescription =
                                            stringResource(R.string.settings_overlay_theme_tone_lighter),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Slider(
                                    value = overlayThemeIntensity,
                                    onValueChange = { value ->
                                        val step =
                                                ((value - minIntensity) / intensityStep)
                                                        .roundToInt()
                                                        .coerceIn(
                                                                0,
                                                                UiPreferences.OVERLAY_THEME_INTENSITY_DELTA_STEPS *
                                                                        2,
                                                        )
                                        if (step != lastToneStep) {
                                            hapticToggle(view)()
                                            lastToneStep = step
                                        }
                                        onOverlayThemeIntensityChange(value)
                                    },
                                    valueRange = minIntensity..maxIntensity,
                                    steps = intensitySteps,
                                    modifier = Modifier.weight(1f),
                            )
                            Icon(
                                    imageVector = Icons.Rounded.DarkMode,
                                    contentDescription =
                                            stringResource(R.string.settings_overlay_theme_tone_darker),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

            }

            if (showMaterialYouToggle) {
                SettingsToggleRow(
                        title = stringResource(R.string.settings_device_theme_title),
                        subtitle = stringResource(R.string.settings_device_theme_desc),
                        checked = deviceThemeEnabled,
                        onCheckedChange = onDeviceThemeToggle,
                        horizontalPadding = DesignTokens.SpacingSmall,
                        isLastItem = !showAmoledThemeToggle && !showThemedIconsToggle,
                        showDivider = showAmoledThemeToggle || showThemedIconsToggle,
                )
            }

            if (showAmoledThemeToggle) {
                SettingsToggleRow(
                        title = stringResource(R.string.settings_amoled_theme_title),
                        subtitle = stringResource(R.string.settings_amoled_theme_desc),
                        checked = amoledThemeEnabled,
                        onCheckedChange = onAmoledThemeToggle,
                        horizontalPadding = DesignTokens.SpacingSmall,
                        extraVerticalPadding = (-4).dp,
                        isLastItem = !showThemedIconsToggle,
                        showDivider = showThemedIconsToggle,
                )
            }

            if (showThemedIconsToggle) {
                SettingsToggleRow(
                        title = stringResource(R.string.settings_themed_icons_title),
                        subtitle = stringResource(R.string.settings_themed_icons_desc),
                        checked = themedIconsEnabled,
                        onCheckedChange = onThemedIconsToggle,
                        horizontalPadding = DesignTokens.SpacingSmall,
                        extraVerticalPadding = (-4).dp,
                        isLastItem = true,
                        showDivider = false,
                )
            }
        }
    }
}

@Composable
internal fun AppModeOption(
        label: String,
        icon: ImageVector,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        swatchColor: Color? = null,
        iconLabelSpacing: Dp = DesignTokens.SpacingXSmall,
        compactLabel: Boolean = false,
        enabled: Boolean = true,
) {
    val contentAlpha = if (enabled) 1f else 0.5f
    val borderColor =
            if (selected) MaterialTheme.colorScheme.primary
            else AppColors.SettingsDivider
    val backgroundColor =
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else Color.Transparent

    Column(
            modifier =
                    modifier
                            .clip(MaterialTheme.shapes.large)
                            .background(color = backgroundColor, shape = MaterialTheme.shapes.large)
                            .border(
                                    width = DesignTokens.BorderWidth,
                                    color = borderColor,
                                    shape = MaterialTheme.shapes.large,
                            )
                            .selectable(
                                    selected = selected,
                                    enabled = enabled,
                                    onClick = onClick,
                                    role = Role.RadioButton,
                            )
                            .padding(
                                    horizontal = DesignTokens.ChipHorizontalPadding,
                                    vertical = DesignTokens.ChipVerticalPadding,
                            )
                            .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(iconLabelSpacing),
    ) {
        if (swatchColor != null) {
            Box(
                    modifier =
                            Modifier.size(24.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline,
                                            shape = CircleShape,
                                    ),
            )
        } else {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
                text = label,
                style =
                        if (compactLabel) {
                            MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                        } else {
                            MaterialTheme.typography.labelSmall
                        },
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun OverlaySourceBox(
        modifier: Modifier = Modifier,
        selected: Boolean,
        label: String,
        enabled: Boolean = true,
        hasImage: Boolean = false,
        onClick: () -> Unit,
        content: @Composable BoxScope.() -> Unit,
) {
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(52.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(Color.Transparent)
                                .clickable(onClick = onClick)
                                .then(
                                        if (!enabled) {
                                            Modifier.alpha(0.45f)
                                        } else {
                                            Modifier
                                        },
                                )
                                .then(
                                        if (!hasImage || isDarkMode) {
                                            Modifier.border(
                                                    width = DesignTokens.BorderWidth,
                                                    color = AppColors.SettingsDivider,
                                                    shape = MaterialTheme.shapes.medium,
                                            )
                                        } else {
                                            Modifier
                                        },
                                ),
                contentAlignment = Alignment.Center,
        ) {
            content()
            if (selected) {
                Box(
                        modifier =
                                Modifier.size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                ) {
                    Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color =
                        if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class AppThemeOption(
        val theme: AppTheme,
        val labelRes: Int,
)

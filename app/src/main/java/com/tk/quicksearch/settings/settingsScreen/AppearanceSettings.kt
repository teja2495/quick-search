package com.tk.quicksearch.settings.settingsScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.settings.SettingsToggleRow
import com.tk.quicksearch.util.hapticToggle


/**
 * Combined card for keyboard alignment and icon pack settings.
 */
@Composable
fun CombinedLayoutIconCard(
    keyboardAlignedLayout: Boolean,
    onToggleKeyboardAlignedLayout: (Boolean) -> Unit,
    iconPackTitle: String,
    iconPackDescription: String,
    onIconPackClick: () -> Unit,
    onRefreshIconPacks: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Results alignment toggle
            SettingsToggleRow(
                title = stringResource(R.string.settings_layout_option_bottom_title),
                subtitle = stringResource(R.string.settings_layout_option_bottom_desc),
                checked = keyboardAlignedLayout,
                onCheckedChange = onToggleKeyboardAlignedLayout,
                showDivider = false,
                extraVerticalPadding = 8.dp,
            )

            // Divider
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Icon Pack Section (with navigation)
            val hasIconPacks = iconPackDescription != stringResource(R.string.settings_icon_pack_empty)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onIconPackClick)
                    .padding(
                        start = 24.dp,
                        top = 16.dp,
                        end = 24.dp,
                        bottom = if (hasIconPacks) 16.dp else 20.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = iconPackTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = iconPackDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (hasIconPacks) Icons.Rounded.ChevronRight else Icons.Rounded.Refresh,
                    contentDescription = if (hasIconPacks) stringResource(R.string.desc_navigate_forward) else stringResource(R.string.settings_refresh_icon_packs),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .then(
                            if (!hasIconPacks) Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onRefreshIconPacks
                            )
                            else Modifier
                        )
                )
            }
        }
    }
}

/**
 * Combined appearance card with wallpaper background settings.
 */
@Composable
fun CombinedAppearanceCard(
    showWallpaperBackground: Boolean,
    wallpaperBackgroundAlpha: Float,
    wallpaperBlurRadius: Float,
    onToggleShowWallpaperBackground: (Boolean) -> Unit,
    onWallpaperBackgroundAlphaChange: (Float) -> Unit,
    onWallpaperBlurRadiusChange: (Float) -> Unit,
    hasFilePermission: Boolean = true,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Wallpaper background toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Always call the callback - it will handle permission request if needed
                        onToggleShowWallpaperBackground(!showWallpaperBackground)
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_wallpaper_background_toggle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = showWallpaperBackground && hasFilePermission,
                    onCheckedChange = { enabled ->
                        hapticToggle(view)()
                        onToggleShowWallpaperBackground(enabled)
                    }
                )
            }

            if (showWallpaperBackground && hasFilePermission) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_wallpaper_transparency_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Slider(
                            value = wallpaperBackgroundAlpha,
                            onValueChange = onWallpaperBackgroundAlphaChange,
                            valueRange = 0f..1f,
                            steps = 9,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(wallpaperBackgroundAlpha * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.widthIn(min = 48.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.settings_wallpaper_blur_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Slider(
                            value = wallpaperBlurRadius,
                            onValueChange = onWallpaperBlurRadiusChange,
                            valueRange = 0f..UiPreferences.MAX_WALLPAPER_BLUR_RADIUS,
                            steps = 7,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${((wallpaperBlurRadius / UiPreferences.MAX_WALLPAPER_BLUR_RADIUS) * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.widthIn(min = 48.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
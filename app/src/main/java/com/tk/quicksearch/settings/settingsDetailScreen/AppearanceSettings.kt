package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.IconPackInfo
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.settings.searchEnginesScreen.SearchEngineAppearanceCard
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.util.hapticToggle
import kotlin.math.roundToInt

/** Combined card for keyboard alignment and icon pack settings. */
@Composable
fun CombinedLayoutIconCard(
        oneHandedMode: Boolean,
        onToggleOneHandedMode: (Boolean) -> Unit,
        iconPackTitle: String,
        iconPackDescription: String,
        onIconPackClick: () -> Unit,
        onRefreshIconPacks: () -> Unit = {},
        modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column {
            // Results alignment toggle
            SettingsToggleRow(
                    title = stringResource(R.string.settings_layout_option_bottom_title),
                    subtitle = stringResource(R.string.settings_layout_option_bottom_desc),
                    checked = oneHandedMode,
                    onCheckedChange = onToggleOneHandedMode,
                    isFirstItem = true,
                    extraVerticalPadding = 8.dp,
            )

            // Icon Pack Section (with navigation)
            val hasIconPacks =
                    iconPackDescription != stringResource(R.string.settings_icon_pack_empty)
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
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
                        imageVector =
                                if (hasIconPacks) Icons.Rounded.ChevronRight
                                else Icons.Rounded.Refresh,
                        contentDescription =
                                if (hasIconPacks) stringResource(R.string.desc_navigate_forward)
                                else stringResource(R.string.settings_refresh_icon_packs),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                                Modifier.padding(start = 8.dp)
                                        .then(
                                                if (!hasIconPacks)
                                                        Modifier.clickable(
                                                                interactionSource =
                                                                        remember {
                                                                            MutableInteractionSource()
                                                                        },
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

/** Combined appearance card with wallpaper background settings. */
@Composable
fun CombinedAppearanceCard(
        showWallpaperBackground: Boolean,
        wallpaperBackgroundAlpha: Float,
        wallpaperBlurRadius: Float,
        onToggleShowWallpaperBackground: (Boolean) -> Unit,
        onWallpaperBackgroundAlphaChange: (Float) -> Unit,
        onWallpaperBlurRadiusChange: (Float) -> Unit,
        hasFilePermission: Boolean = true,
        hasWallpaperPermission: Boolean = true,
        wallpaperAvailable: Boolean = false,
        modifier: Modifier = Modifier
) {
    val view = LocalView.current
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column {
            // Wallpaper background toggle
            val wallpaperEnabled =
                    showWallpaperBackground &&
                            hasFilePermission &&
                            (hasWallpaperPermission || wallpaperAvailable)

            SettingsToggleRow(
                    title = stringResource(R.string.settings_wallpaper_background_toggle),
                    checked = wallpaperEnabled,
                    onCheckedChange = onToggleShowWallpaperBackground,
                    isFirstItem = true,
                    isLastItem = true
            )

            if (showWallpaperBackground &&
                            hasFilePermission &&
                            (hasWallpaperPermission || wallpaperAvailable)
            ) {
                var lastAlphaStep by remember {
                    mutableStateOf((wallpaperBackgroundAlpha * 9).roundToInt().coerceIn(0, 9))
                }
                var lastBlurStep by remember {
                    mutableStateOf(
                            (wallpaperBlurRadius / UiPreferences.MAX_WALLPAPER_BLUR_RADIUS * 7)
                                    .roundToInt()
                                    .coerceIn(0, 7)
                    )
                }
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(
                                                start = 24.dp,
                                                end = 24.dp,
                                                top = 0.dp,
                                                bottom = 16.dp
                                        )
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
                                onValueChange = { v ->
                                    val step = (v * 9).roundToInt().coerceIn(0, 9)
                                    if (step != lastAlphaStep) {
                                        hapticToggle(view)()
                                        lastAlphaStep = step
                                    }
                                    onWallpaperBackgroundAlphaChange(v)
                                },
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
                                onValueChange = { v ->
                                    val step =
                                            (v / UiPreferences.MAX_WALLPAPER_BLUR_RADIUS * 7)
                                                    .roundToInt()
                                                    .coerceIn(0, 7)
                                    if (step != lastBlurStep) {
                                        hapticToggle(view)()
                                        lastBlurStep = step
                                    }
                                    onWallpaperBlurRadiusChange(v)
                                },
                                valueRange = 0f..UiPreferences.MAX_WALLPAPER_BLUR_RADIUS,
                                steps = 7,
                                modifier = Modifier.weight(1f)
                        )
                        Text(
                                text =
                                        "${((wallpaperBlurRadius / UiPreferences.MAX_WALLPAPER_BLUR_RADIUS) * 100).toInt()}%",
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

/** Complete appearance settings section with all appearance-related components and dialogs. */
@Composable
fun AppearanceSettingsSection(
        oneHandedMode: Boolean,
        onToggleOneHandedMode: (Boolean) -> Unit,
        showWallpaperBackground: Boolean,
        wallpaperBackgroundAlpha: Float,
        wallpaperBlurRadius: Float,
        onToggleShowWallpaperBackground: (Boolean) -> Unit,
        onWallpaperBackgroundAlphaChange: (Float) -> Unit,
        onWallpaperBlurRadiusChange: (Float) -> Unit,
        isSearchEngineCompactMode: Boolean,
        onToggleSearchEngineCompactMode: (Boolean) -> Unit,
        selectedIconPackPackage: String?,
        availableIconPacks: List<IconPackInfo>,
        onSelectIconPack: (String?) -> Unit,
        onRefreshIconPacks: () -> Unit,
        onSearchIconPacks: () -> Unit,
        hasFilePermission: Boolean = true,
        hasWallpaperPermission: Boolean = true,
        wallpaperAvailable: Boolean = false,
        modifier: Modifier = Modifier
) {
    val appearanceContext = androidx.compose.ui.platform.LocalContext.current
    var showIconPackDialog by remember { mutableStateOf(false) }

    val hasIconPacks = availableIconPacks.isNotEmpty()
    val selectedIconPackLabel =
            remember(selectedIconPackPackage, availableIconPacks) {
                availableIconPacks.firstOrNull { it.packageName == selectedIconPackPackage }?.label
                        ?: appearanceContext.getString(R.string.settings_icon_pack_option_system)
            }

    Column(modifier = modifier) {
        // Wallpaper Background Card
        CombinedAppearanceCard(
                showWallpaperBackground = showWallpaperBackground,
                wallpaperBackgroundAlpha = wallpaperBackgroundAlpha,
                wallpaperBlurRadius = wallpaperBlurRadius,
                onToggleShowWallpaperBackground = onToggleShowWallpaperBackground,
                onWallpaperBackgroundAlphaChange = onWallpaperBackgroundAlphaChange,
                onWallpaperBlurRadiusChange = onWallpaperBlurRadiusChange,
                hasFilePermission = hasFilePermission,
                hasWallpaperPermission = hasWallpaperPermission,
                wallpaperAvailable = wallpaperAvailable
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Engine Style Card
        SearchEngineAppearanceCard(
                isSearchEngineCompactMode = isSearchEngineCompactMode,
                onToggleSearchEngineCompactMode = onToggleSearchEngineCompactMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        // One-Handed Mode and Icon Pack Card
        CombinedLayoutIconCard(
                oneHandedMode = oneHandedMode,
                onToggleOneHandedMode = onToggleOneHandedMode,
                iconPackTitle =
                        androidx.compose.ui.res.stringResource(R.string.settings_icon_pack_title),
                iconPackDescription =
                        if (hasIconPacks) {
                            androidx.compose.ui.res.stringResource(
                                    R.string.settings_icon_pack_selected_label,
                                    selectedIconPackLabel
                            )
                        } else {
                            androidx.compose.ui.res.stringResource(
                                    R.string.settings_icon_pack_empty
                            )
                        },
                onIconPackClick = {
                    if (hasIconPacks) {
                        showIconPackDialog = true
                    } else {
                        onSearchIconPacks()
                    }
                },
                onRefreshIconPacks = onRefreshIconPacks
        )
    }

    // Icon Pack Picker Dialog
    if (showIconPackDialog) {
        IconPackPickerDialog(
                availableIconPacks = availableIconPacks,
                selectedPackage = selectedIconPackPackage,
                onSelect = { packageName: String? ->
                    onSelectIconPack(packageName)
                    showIconPackDialog = false
                },
                onDismiss = { showIconPackDialog = false }
        )
    }
}

@Composable
private fun IconPackPickerDialog(
        availableIconPacks: List<IconPackInfo>,
        selectedPackage: String?,
        onSelect: (String?) -> Unit,
        onDismiss: () -> Unit
) {
    AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                        text =
                                androidx.compose.ui.res.stringResource(
                                        R.string.settings_icon_pack_picker_title
                                )
                )
            },
            text = {
                Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (availableIconPacks.isEmpty()) {
                        Text(
                                text =
                                        androidx.compose.ui.res.stringResource(
                                                R.string.settings_icon_pack_empty
                                        ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconPackOptionRow(
                                    label =
                                            androidx.compose.ui.res.stringResource(
                                                    R.string.settings_icon_pack_option_system
                                            ),
                                    packageName = null,
                                    selected = selectedPackage == null,
                                    onClick = { onSelect(null) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            availableIconPacks.forEach { pack ->
                                IconPackOptionRow(
                                        label = pack.label,
                                        packageName = pack.packageName,
                                        selected = selectedPackage == pack.packageName,
                                        onClick = { onSelect(pack.packageName) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(androidx.compose.ui.res.stringResource(R.string.dialog_done))
                }
            }
    )
}

@Composable
private fun IconPackOptionRow(
        label: String,
        packageName: String?,
        selected: Boolean,
        onClick: () -> Unit
) {
    val iconBitmap = packageName?.let { rememberAppIcon(packageName = it).bitmap }
    Row(
            modifier =
                    Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        if (iconBitmap != null) {
            Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(24.dp))
        } else {
            Icon(
                    imageVector = Icons.Rounded.Android,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
        )
    }
}

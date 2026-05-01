package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.IconPackInfo
import com.tk.quicksearch.search.core.LauncherAppIcon
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.settings.AppearanceSettings.FontSizeCard
import com.tk.quicksearch.settings.AppearanceSettings.IconPackPickerDialog
import com.tk.quicksearch.settings.AppearanceSettings.AppIconCard
import com.tk.quicksearch.settings.AppearanceSettings.AppLauncherIconCard
import com.tk.quicksearch.settings.AppearanceSettings.AppThemeCard
import com.tk.quicksearch.settings.AppearanceSettings.WallpaperCard
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.settings.searchEnginesScreen.SearchEngineAppearanceCard

/** Complete appearance settings section with all appearance-related components and dialogs. */
@Composable
fun AppearanceSettingsSection(
        oneHandedMode: Boolean,
        onToggleOneHandedMode: (Boolean) -> Unit,
        bottomSearchBarEnabled: Boolean,
        onToggleBottomSearchBar: (Boolean) -> Unit,
        wallpaperBackgroundAlpha: Float,
        wallpaperBlurRadius: Float,
        onWallpaperBackgroundAlphaChange: (Float) -> Unit,
        onWallpaperBlurRadiusChange: (Float) -> Unit,
        appTheme: AppTheme,
        overlayThemeIntensity: Float,
        fontScaleMultiplier: Float,
        useSystemFont: Boolean,
        onSetAppTheme: (AppTheme) -> Unit,
        onOverlayThemeIntensityChange: (Float) -> Unit,
        appThemeMode: com.tk.quicksearch.search.core.AppThemeMode,
        onSetAppThemeMode: (com.tk.quicksearch.search.core.AppThemeMode) -> Unit,
        onFontScaleMultiplierChange: (Float) -> Unit,
        onUseSystemFontChange: (Boolean) -> Unit,
        backgroundSource: BackgroundSource,
        customImageUri: String?,
        onSetBackgroundSource: (BackgroundSource) -> Unit,
        onPickCustomImage: () -> Unit,
        onRequestWallpaperPermission: () -> Unit,
        isSearchEngineCompactMode: Boolean,
        searchEngineCompactRowCount: Int,
        hasEnabledSearchEngines: Boolean,
        onToggleSearchEngineCompactMode: (Boolean) -> Unit,
        onSetSearchEngineCompactRowCount: (Int) -> Unit,
        selectedIconPackPackage: String?,
        availableIconPacks: List<IconPackInfo>,
        maskUnsupportedIconPackIcons: Boolean,
        showAppLabels: Boolean,
        onToggleAppLabels: (Boolean) -> Unit,
        phoneAppGridColumns: Int = com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS,
        onSetPhoneAppGridColumns: (Int) -> Unit = {},
        onSelectIconPack: (String?) -> Unit,
        onSetMaskUnsupportedIconPackIcons: (Boolean) -> Unit,
        onRefreshIconPacks: () -> Unit,
        onSearchIconPacks: () -> Unit,
        appIconShape: AppIconShape,
        onSetAppIconShape: (AppIconShape) -> Unit,
        launcherAppIcon: LauncherAppIcon,
        onSetLauncherAppIcon: (LauncherAppIcon) -> Unit,
        themedIconsEnabled: Boolean,
        onThemedIconsToggle: (Boolean) -> Unit,
        deviceThemeEnabled: Boolean,
        onDeviceThemeToggle: (Boolean) -> Unit,
        wallpaperAccentEnabled: Boolean,
        onWallpaperAccentToggle: (Boolean) -> Unit,
        hasWallpaperPermission: Boolean = true,
        modifier: Modifier = Modifier,
) {
    val appearanceContext = androidx.compose.ui.platform.LocalContext.current
    var showIconPackDialog by remember { mutableStateOf(false) }

    val hasIconPacks = availableIconPacks.isNotEmpty()
    val systemIconPackLabel = stringResource(R.string.settings_icon_pack_option_system)
    val selectedIconPackLabel =
            remember(selectedIconPackPackage, availableIconPacks, systemIconPackLabel) {
                availableIconPacks.firstOrNull { it.packageName == selectedIconPackPackage }?.label
                        ?: systemIconPackLabel
            }

    Column(modifier = modifier) {
        FontSizeCard(
                fontScaleMultiplier = fontScaleMultiplier,
                onFontScaleMultiplierChange = onFontScaleMultiplierChange,
                useSystemFont = useSystemFont,
                onUseSystemFontChange = onUseSystemFontChange,
        )

        Spacer(modifier = Modifier.height(16.dp))

        AppThemeCard(
                selectedTheme = appTheme,
                overlayThemeIntensity = overlayThemeIntensity,
                onThemeSelected = onSetAppTheme,
                onOverlayThemeIntensityChange = onOverlayThemeIntensityChange,
                backgroundSource = backgroundSource,
                onSetBackgroundSource = onSetBackgroundSource,
                appThemeMode = appThemeMode,
                onSetAppThemeMode = onSetAppThemeMode,
                hasWallpaperPermission = hasWallpaperPermission,
                themedIconsEnabled = themedIconsEnabled,
                onThemedIconsToggle = onThemedIconsToggle,
                deviceThemeEnabled = deviceThemeEnabled,
                onDeviceThemeToggle = onDeviceThemeToggle,
        )

        Spacer(modifier = Modifier.height(16.dp))

        WallpaperCard(
                wallpaperBackgroundAlpha = wallpaperBackgroundAlpha,
                wallpaperBlurRadius = wallpaperBlurRadius,
                onWallpaperBackgroundAlphaChange = onWallpaperBackgroundAlphaChange,
                onWallpaperBlurRadiusChange = onWallpaperBlurRadiusChange,
                backgroundSource = backgroundSource,
                customImageUri = customImageUri,
                onSetBackgroundSource = onSetBackgroundSource,
                onPickCustomImage = onPickCustomImage,
                hasWallpaperPermission = hasWallpaperPermission,
                onRequestWallpaperPermission = onRequestWallpaperPermission,
                wallpaperAccentEnabled = wallpaperAccentEnabled,
                onWallpaperAccentToggle = onWallpaperAccentToggle,
                deviceThemeEnabled = deviceThemeEnabled,
        )

        Spacer(modifier = Modifier.height(16.dp))

        AppLauncherIconCard(
                launcherAppIcon = launcherAppIcon,
                onSetLauncherAppIcon = onSetLauncherAppIcon,
                appTheme = appTheme,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (hasEnabledSearchEngines) {
            // Search Engine Style Card
            SearchEngineAppearanceCard(
                    isSearchEngineCompactMode = isSearchEngineCompactMode,
                    onToggleSearchEngineCompactMode = onToggleSearchEngineCompactMode,
                    compactRowCount = searchEngineCompactRowCount,
                    onSetCompactRowCount = onSetSearchEngineCompactRowCount,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // One-Handed Mode and Search Bar Card
        SettingsCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsToggleRow(
                        title = stringResource(R.string.settings_layout_option_bottom_title),
                        subtitle = stringResource(R.string.settings_layout_option_bottom_desc),
                        checked = oneHandedMode,
                        onCheckedChange = onToggleOneHandedMode,
                        isFirstItem = true,
                        extraVerticalPadding = 8.dp,
                )
                SettingsToggleRow(
                        title = stringResource(R.string.settings_bottom_searchbar_title),
                        subtitle = stringResource(R.string.settings_bottom_searchbar_desc),
                        checked = bottomSearchBarEnabled,
                        onCheckedChange = onToggleBottomSearchBar,
                        extraVerticalPadding = 8.dp,
                        showDivider = false,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Grid Columns, App Labels, and Icon Pack Card
        AppIconCard(
                showAppLabels = showAppLabels,
                onToggleAppLabels = onToggleAppLabels,
                phoneAppGridColumns = phoneAppGridColumns,
                onSetPhoneAppGridColumns = onSetPhoneAppGridColumns,
                iconPackTitle =
                        androidx.compose.ui.res.stringResource(R.string.settings_icon_pack_title),
                iconPackDescription =
                        if (hasIconPacks) {
                            androidx.compose.ui.res.stringResource(
                                    R.string.settings_icon_pack_selected_label,
                                    selectedIconPackLabel,
                            )
                        } else {
                            androidx.compose.ui.res.stringResource(
                                    R.string.settings_icon_pack_empty,
                            )
                        },
                onIconPackClick = {
                    if (hasIconPacks) {
                        showIconPackDialog = true
                    } else {
                        onSearchIconPacks()
                    }
                },
                onRefreshIconPacks = onRefreshIconPacks,
                appIconShape = appIconShape,
                onSetAppIconShape = onSetAppIconShape,
        )
    }

    // Icon Pack Picker Dialog
    if (showIconPackDialog) {
        IconPackPickerDialog(
                availableIconPacks = availableIconPacks,
                selectedPackage = selectedIconPackPackage,
                maskUnsupportedIcons = maskUnsupportedIconPackIcons,
                onSelect = { packageName: String? ->
                    onSelectIconPack(packageName)
                    showIconPackDialog = false
                },
                onMaskUnsupportedIconsChange = onSetMaskUnsupportedIconPackIcons,
                onDismiss = { showIconPackDialog = false },
        )
    }
}

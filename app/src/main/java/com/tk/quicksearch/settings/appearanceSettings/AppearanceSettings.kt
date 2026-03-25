package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.IconPackInfo
import com.tk.quicksearch.search.core.OverlayGradientTheme
import com.tk.quicksearch.settings.AppearanceSettings.FontSizeCard
import com.tk.quicksearch.settings.AppearanceSettings.IconPackPickerDialog
import com.tk.quicksearch.settings.AppearanceSettings.CombinedLayoutIconCard
import com.tk.quicksearch.settings.AppearanceSettings.OverlayThemeCard
import com.tk.quicksearch.settings.AppearanceSettings.WallpaperCard
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
        overlayGradientTheme: OverlayGradientTheme,
        overlayThemeIntensity: Float,
        fontScaleMultiplier: Float,
        onSetOverlayGradientTheme: (OverlayGradientTheme) -> Unit,
        onOverlayThemeIntensityChange: (Float) -> Unit,
        appThemeMode: com.tk.quicksearch.search.core.AppThemeMode,
        onSetAppThemeMode: (com.tk.quicksearch.search.core.AppThemeMode) -> Unit,
        onFontScaleMultiplierChange: (Float) -> Unit,
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
        showAppLabels: Boolean,
        onToggleAppLabels: (Boolean) -> Unit,
        onSelectIconPack: (String?) -> Unit,
        onRefreshIconPacks: () -> Unit,
        onSearchIconPacks: () -> Unit,
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
        )

        Spacer(modifier = Modifier.height(16.dp))

        OverlayThemeCard(
                selectedTheme = overlayGradientTheme,
                overlayThemeIntensity = overlayThemeIntensity,
                onThemeSelected = onSetOverlayGradientTheme,
                onOverlayThemeIntensityChange = onOverlayThemeIntensityChange,
                backgroundSource = backgroundSource,
                onSetBackgroundSource = onSetBackgroundSource,
                appThemeMode = appThemeMode,
                onSetAppThemeMode = onSetAppThemeMode,
                hasWallpaperPermission = hasWallpaperPermission,
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

        // One-Handed Mode and Icon Pack Card
        CombinedLayoutIconCard(
                oneHandedMode = oneHandedMode,
                onToggleOneHandedMode = onToggleOneHandedMode,
                showAppLabels = showAppLabels,
                onToggleAppLabels = onToggleAppLabels,
                bottomSearchBarEnabled = bottomSearchBarEnabled,
                onToggleBottomSearchBar = onToggleBottomSearchBar,
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
                onDismiss = { showIconPackDialog = false },
        )
    }
}

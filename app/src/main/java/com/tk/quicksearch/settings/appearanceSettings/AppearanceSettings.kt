package com.tk.quicksearch.settings.appearanceSettings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.search.data.preferences.SwipeDirection
import com.tk.quicksearch.search.data.preferences.actionKey
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.AppIconShape
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.IconPackInfo
import com.tk.quicksearch.app.LauncherAppIcon
import com.tk.quicksearch.search.core.SearchBarStyle
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.AccentColorMode
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.BasePreferences
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.settings.settingsDetailScreen.PriorityReorderDialog
import com.tk.quicksearch.shared.ui.components.AnimatedSegmentedTiles
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.settings.searchEngineSettings.SearchEngineAppearanceCard
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** Complete appearance settings section with all appearance-related components and dialogs. */
@Composable
fun AppearanceSettingsSection(
        oneHandedMode: Boolean,
        onToggleOneHandedMode: (Boolean) -> Unit,
        bottomSearchBarEnabled: Boolean,
        onToggleBottomSearchBar: (Boolean) -> Unit,
        compactLayoutEnabled: Boolean,
        onToggleCompactLayout: (Boolean) -> Unit,
        unifiedPinnedItemsEnabled: Boolean,
        onToggleUnifiedPinnedItems: (Boolean) -> Unit,
        homePinnedSectionOrder: List<SearchSection>,
        onHomePinnedSectionOrderChange: (List<SearchSection>) -> Unit,
        pinnedAppShortcutsInAppGrid: Boolean,
        onTogglePinnedAppShortcutsInAppGrid: (Boolean) -> Unit,
        showStatusBar: Boolean,
        onToggleShowStatusBar: (Boolean) -> Unit,
        searchChromeBlurEnabled: Boolean,
        onToggleSearchChromeBlur: (Boolean) -> Unit,
        settingsIconEnabled: Boolean,
        onToggleSettingsIcon: (Boolean) -> Unit,
        wallpaperBackgroundAlpha: Float,
        wallpaperBlurRadius: Float,
        onWallpaperBackgroundAlphaChange: (Float) -> Unit,
        onWallpaperBlurRadiusChange: (Float) -> Unit,
        appTheme: AppTheme,
        overlayThemeIntensity: Float,
        fontScaleMultiplier: Float,
        useSystemFont: Boolean,
        homeTextColorOverride: com.tk.quicksearch.search.core.HomeTextColor?,
        onSetAppTheme: (AppTheme) -> Unit,
        onOverlayThemeIntensityChange: (Float) -> Unit,
        appThemeMode: com.tk.quicksearch.search.core.AppThemeMode,
        onSetAppThemeMode: (com.tk.quicksearch.search.core.AppThemeMode) -> Unit,
        onFontScaleMultiplierChange: (Float) -> Unit,
        onUseSystemFontChange: (Boolean) -> Unit,
        onHomeTextColorChange: (com.tk.quicksearch.search.core.HomeTextColor) -> Unit,
        backgroundSource: BackgroundSource,
        customImageUri: String?,
        onSetBackgroundSource: (BackgroundSource) -> Unit,
        onPickCustomImage: () -> Unit,
        onRemoveCustomImage: () -> Unit,
        onRequestWallpaperPermission: () -> Unit,
        isSearchEngineCompactMode: Boolean,
        searchEngineCompactRowCount: Int,
        enabledSearchEngines: List<SearchTarget>,
        onToggleSearchEngineCompactMode: (Boolean) -> Unit,
        onSetSearchEngineCompactRowCount: (Int) -> Unit,
        selectedIconPackPackage: String?,
        availableIconPacks: List<IconPackInfo>,
        maskUnsupportedIconPackIcons: Boolean,
        showAppLabels: Boolean,
        onToggleAppLabels: (Boolean) -> Unit,
        phoneAppGridColumns: Int = com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS,
        onSetPhoneAppGridColumns: (Int) -> Unit = {},
        appIconSizeStep: Int = com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_APP_ICON_SIZE_STEP,
        onSetAppIconSizeStep: (Int) -> Unit = {},
        onSelectIconPack: (String?) -> Unit,
        onResetAllIcons: () -> Unit,
        onSetMaskUnsupportedIconPackIcons: (Boolean) -> Unit,
        onRefreshIconPacks: () -> Unit,
        onSearchIconPacks: () -> Unit,
        appIconShape: AppIconShape,
        onSetAppIconShape: (AppIconShape) -> Unit,
        onSetAppIconCornerPercent: (Int) -> Unit = {},
        launcherAppIcon: LauncherAppIcon,
        onSetLauncherAppIcon: (LauncherAppIcon) -> Unit,
        searchBarStyle: SearchBarStyle,
        onSetSearchBarStyle: (SearchBarStyle) -> Unit,
        themedIconsEnabled: Boolean,
        onThemedIconsToggle: (Boolean) -> Unit,
        deviceThemeEnabled: Boolean,
        onDeviceThemeToggle: (Boolean) -> Unit,
        amoledThemeEnabled: Boolean,
        onAmoledThemeToggle: (Boolean) -> Unit,
        accentColorMode: com.tk.quicksearch.search.core.AccentColorMode,
        customAccentColorArgb: Int,
        onAccentColorModeChange: (com.tk.quicksearch.search.core.AccentColorMode) -> Unit,
        onCustomAccentColorChange: (Int) -> Unit,
        hasWallpaperPermission: Boolean = true,
        wallpaperAvailable: Boolean = false,
        modifier: Modifier = Modifier,
) {
    val appearanceContext = androidx.compose.ui.platform.LocalContext.current
    val gesturePreferences =
        remember(appearanceContext) { UserAppPreferences(appearanceContext.applicationContext) }
    var settingsIconRequired by remember {
        mutableStateOf(gesturePreferences.actionFor(SwipeDirection.LEFT) != SwipeGestureAction.SETTINGS)
    }
    DisposableEffect(gesturePreferences) {
        val preferences =
            appearanceContext.applicationContext.getSharedPreferences(
                BasePreferences.PREFS_NAME,
                Context.MODE_PRIVATE,
            )
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SwipeDirection.LEFT.actionKey) {
                settingsIconRequired =
                    gesturePreferences.actionFor(SwipeDirection.LEFT) != SwipeGestureAction.SETTINGS
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    var themeCardExpanded by remember { mutableStateOf(gesturePreferences.isAppearanceCardExpanded(CARD_THEME)) }
    var backgroundCardExpanded by remember { mutableStateOf(gesturePreferences.isAppearanceCardExpanded(CARD_BACKGROUND)) }
    var searchBarCardExpanded by remember { mutableStateOf(gesturePreferences.isAppearanceCardExpanded(CARD_SEARCH_BAR)) }
    var searchEnginesCardExpanded by remember { mutableStateOf(gesturePreferences.isAppearanceCardExpanded(CARD_SEARCH_ENGINES)) }
    var showIconPackDialog by remember { mutableStateOf(false) }
    var showPinnedSectionOrderDialog by rememberSaveable { mutableStateOf(false) }
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
                homeTextColorOverride = homeTextColorOverride,
                onHomeTextColorChange = onHomeTextColorChange,
                backgroundSource = backgroundSource,
                customImageUri = customImageUri,
                compactLayoutEnabled = compactLayoutEnabled,
        )

        Spacer(modifier = Modifier.height(DesignTokens.SpacingLarge))

        AppThemeCard(
                selectedTheme = appTheme,
                overlayThemeIntensity = overlayThemeIntensity,
                onThemeSelected = { theme ->
                    // Over a wallpaper or custom image, a new theme takes over the accent color.
                    val switchAccentToTheme =
                            theme != appTheme &&
                                    (accentColorMode == AccentColorMode.FROM_WALLPAPER ||
                                            (backgroundSource != BackgroundSource.THEME &&
                                                    accentColorMode != AccentColorMode.NONE))
                    onSetAppTheme(theme)
                    if (switchAccentToTheme) onAccentColorModeChange(AccentColorMode.NONE)
                },
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
                amoledThemeEnabled = amoledThemeEnabled,
                onAmoledThemeToggle = onAmoledThemeToggle,
                expanded = themeCardExpanded,
                onExpandedChange = { expanded ->
                    themeCardExpanded = expanded
                    gesturePreferences.setAppearanceCardExpanded(CARD_THEME, expanded)
                },
        )

        Spacer(modifier = Modifier.height(DesignTokens.SpacingLarge))

        WallpaperCard(
                wallpaperBackgroundAlpha = wallpaperBackgroundAlpha,
                wallpaperBlurRadius = wallpaperBlurRadius,
                onWallpaperBackgroundAlphaChange = onWallpaperBackgroundAlphaChange,
                onWallpaperBlurRadiusChange = onWallpaperBlurRadiusChange,
                backgroundSource = backgroundSource,
                customImageUri = customImageUri,
                onSetBackgroundSource = onSetBackgroundSource,
                onPickCustomImage = onPickCustomImage,
                onRemoveCustomImage = onRemoveCustomImage,
                hasWallpaperPermission = hasWallpaperPermission,
                wallpaperAvailable = wallpaperAvailable,
                onRequestWallpaperPermission = onRequestWallpaperPermission,
                accentColorMode = accentColorMode,
                customAccentColorArgb = customAccentColorArgb,
                onAccentColorModeChange = onAccentColorModeChange,
                onCustomAccentColorChange = onCustomAccentColorChange,
                appTheme = appTheme,
                overlayThemeIntensity = overlayThemeIntensity,
                amoledThemeEnabled = amoledThemeEnabled,
                deviceThemeEnabled = deviceThemeEnabled,
                expanded = backgroundCardExpanded,
                onExpandedChange = { expanded ->
                    backgroundCardExpanded = expanded
                    gesturePreferences.setAppearanceCardExpanded(CARD_BACKGROUND, expanded)
                },
        )

        Spacer(modifier = Modifier.height(DesignTokens.SpacingLarge))

        SearchBarStyleCard(
                searchBarStyle = searchBarStyle,
                onSetSearchBarStyle = onSetSearchBarStyle,
                showWallpaperBackground = backgroundSource != BackgroundSource.THEME,
                settingsIconEnabled = settingsIconEnabled,
                settingsIconRequired = settingsIconRequired,
                onToggleSettingsIcon = onToggleSettingsIcon,
                searchChromeBlurEnabled = searchChromeBlurEnabled,
                onToggleSearchChromeBlur = onToggleSearchChromeBlur,
                fixedShape = SearchBarStyle.hasFixedShape(bottomSearchBarEnabled, isSearchEngineCompactMode),
                enabledSearchEngines = enabledSearchEngines,
                searchEngineCompactRowCount = searchEngineCompactRowCount,
                appIconShape = appIconShape,
                iconPackPackage = selectedIconPackPackage,
                expanded = searchBarCardExpanded,
                onExpandedChange = { expanded ->
                    searchBarCardExpanded = expanded
                    gesturePreferences.setAppearanceCardExpanded(CARD_SEARCH_BAR, expanded)
                },
        )

        Spacer(modifier = Modifier.height(DesignTokens.SpacingLarge))

        if (enabledSearchEngines.isNotEmpty()) {
            SearchEngineAppearanceCard(
                    isSearchEngineCompactMode = isSearchEngineCompactMode,
                    onToggleSearchEngineCompactMode = onToggleSearchEngineCompactMode,
                    compactRowCount = searchEngineCompactRowCount,
                    onSetCompactRowCount = onSetSearchEngineCompactRowCount,
                    expanded = searchEnginesCardExpanded,
                    onExpandedChange = { expanded ->
                        searchEnginesCardExpanded = expanded
                        gesturePreferences.setAppearanceCardExpanded(CARD_SEARCH_ENGINES, expanded)
                    },
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLarge))
        }

        SegmentedGroup {
            val layoutToggles =
                    listOf(
                            Triple(R.string.settings_layout_option_bottom_title, R.string.settings_layout_option_bottom_desc, oneHandedMode to onToggleOneHandedMode),
                            Triple(R.string.settings_bottom_searchbar_title, R.string.settings_bottom_searchbar_desc, bottomSearchBarEnabled to onToggleBottomSearchBar),
                            Triple(R.string.settings_compact_layout_title, R.string.settings_compact_layout_desc, compactLayoutEnabled to onToggleCompactLayout),
                            Triple(R.string.settings_show_status_bar_title, R.string.settings_show_status_bar_desc, showStatusBar to onToggleShowStatusBar),
                    )
            // Alternate launcher icons are adaptive icons (mipmap-anydpi-v26 only).
            val showLauncherIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            val offset = if (showLauncherIcon) 1 else 0
            val tileCount = layoutToggles.size + offset
            if (showLauncherIcon) {
                AppLauncherIconTile(
                        launcherAppIcon = launcherAppIcon,
                        onSetLauncherAppIcon = onSetLauncherAppIcon,
                        index = 0,
                        count = tileCount,
                )
            }
            layoutToggles.forEachIndexed { index, (titleRes, descRes, toggle) ->
                SegmentedTile(index = index + offset, count = tileCount) {
                    SettingsToggleRow(
                            title = stringResource(titleRes),
                            subtitle = stringResource(descRes),
                            checked = toggle.first,
                            onCheckedChange = toggle.second,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(DesignTokens.SpacingLarge))

        SegmentedGroup {
            AnimatedSegmentedTiles {
                tile {
                    SettingsToggleRow(
                            title = stringResource(R.string.settings_unified_pinned_items_title),
                            subtitle = stringResource(R.string.settings_unified_pinned_items_desc),
                            checked = unifiedPinnedItemsEnabled,
                            onCheckedChange = onToggleUnifiedPinnedItems,
                    )
                }
                tile(visible = !unifiedPinnedItemsEnabled, onClick = { showPinnedSectionOrderDialog = true }) {
                    SegmentedTileRow(
                        title = stringResource(R.string.settings_pinned_sections_order_title),
                        description = stringResource(R.string.settings_pinned_sections_order_desc),
                        trailing = {
                            Icon(
                                imageVector = MaterialSymbols.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                tile {
                    SettingsToggleRow(
                            title = stringResource(R.string.settings_pinned_app_shortcuts_in_app_grid_title),
                            subtitle = stringResource(R.string.settings_pinned_app_shortcuts_in_app_grid_desc),
                            checked = pinnedAppShortcutsInAppGrid,
                            onCheckedChange = onTogglePinnedAppShortcutsInAppGrid,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(DesignTokens.SpacingLarge))

        AppIconCard(
                showAppLabels = showAppLabels,
                onToggleAppLabels = onToggleAppLabels,
                phoneAppGridColumns = phoneAppGridColumns,
                onSetPhoneAppGridColumns = onSetPhoneAppGridColumns,
                appIconSizeStep = appIconSizeStep,
                onSetAppIconSizeStep = onSetAppIconSizeStep,
                iconPackTitle =
                        androidx.compose.ui.res.stringResource(R.string.settings_icon_pack_title),
                iconPackDescription =
                        if (hasIconPacks) {
                            androidx.compose.ui.res.stringResource(
                                    R.string.settings_app_language_desc,
                                    selectedIconPackLabel,
                            )
                        } else {
                            androidx.compose.ui.res.stringResource(
                                    R.string.settings_icon_pack_description_no_packs,
                            )
                        },
                onIconPackClick = {
                    onRefreshIconPacks()
                    showIconPackDialog = true
                },
                appIconShape = appIconShape,
                onSetAppIconShape = onSetAppIconShape,
                onSetAppIconCornerPercent = onSetAppIconCornerPercent,
        )
    }

    if (showPinnedSectionOrderDialog) {
        PinnedSectionsOrderDrawer(
                homePinnedSectionOrder = homePinnedSectionOrder,
                pinnedAppShortcutsInAppGrid = pinnedAppShortcutsInAppGrid,
                onOrderChange = onHomePinnedSectionOrderChange,
                onDismiss = { showPinnedSectionOrderDialog = false },
        )
    }

    if (showIconPackDialog) {
        IconPackPickerDialog(
                availableIconPacks = availableIconPacks,
                selectedPackage = selectedIconPackPackage,
                maskUnsupportedIcons = maskUnsupportedIconPackIcons,
                onSelect = { packageName: String? -> onSelectIconPack(packageName) },
                onMaskUnsupportedIconsChange = onSetMaskUnsupportedIconPackIcons,
                onDownloadIconPacks = onSearchIconPacks,
                onResetAllIcons = onResetAllIcons,
                onDismiss = { showIconPackDialog = false },
        )
    }
}

private const val CARD_THEME = "theme"
private const val CARD_BACKGROUND = "background"
private const val CARD_SEARCH_BAR = "search_bar"
private const val CARD_SEARCH_ENGINES = "search_engines"

@Composable
internal fun PinnedSectionsOrderDrawer(
    homePinnedSectionOrder: List<SearchSection>,
    pinnedAppShortcutsInAppGrid: Boolean,
    onOrderChange: (List<SearchSection>) -> Unit,
    onDismiss: () -> Unit,
) {
    val visibleOrder =
        remember(homePinnedSectionOrder, pinnedAppShortcutsInAppGrid) {
            homePinnedSectionOrder.filter { section ->
                    section != SearchSection.NOTED &&
                    !(pinnedAppShortcutsInAppGrid && section == SearchSection.APP_SHORTCUTS)
            }
        }
    PriorityReorderDialog(
        items = visibleOrder,
        onItemsChange = { order ->
            onOrderChange(withHiddenPinnedSectionsRestored(order, homePinnedSectionOrder))
        },
        onDismiss = onDismiss,
        titleRes = R.string.settings_pinned_sections_order_title,
        infoRes = R.string.settings_pinned_sections_order_dialog_info,
    )
}

/** Keeps sections hidden from the reorder dialog at their saved positions. */
internal fun withHiddenPinnedSectionsRestored(
        visibleOrder: List<SearchSection>,
        fullOrder: List<SearchSection>,
): List<SearchSection> {
    val result = visibleOrder.toMutableList()
    fullOrder.forEachIndexed { index, section ->
        if (section !in visibleOrder) result.add(index.coerceAtMost(result.size), section)
    }
    return result
}

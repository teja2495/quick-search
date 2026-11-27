package com.tk.quicksearch.search.apps

import com.tk.quicksearch.R
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.ui.components.AppSortControls
import com.tk.quicksearch.shared.ui.components.SortRowIconButtonSize
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.tk.quicksearch.search.apps.swipeGestures.appSwipeGestures
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.apps.notificationDots.AppNotificationBadge
import com.tk.quicksearch.search.apps.notificationDots.NotificationBadges
import com.tk.quicksearch.search.apps.privateSpace.privateSpaceItems
import com.tk.quicksearch.search.apps.privateSpace.rememberPrivateSpaceController
import com.tk.quicksearch.search.common.AddToHomeHandler
import com.tk.quicksearch.search.apps.AppIconShape
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.shared.ui.components.AppExtendedFloatingActionButton
import com.tk.quicksearch.shared.ui.components.AppFabTone
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.components.AppIconButtonStyle
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.getAppGridColumns
import com.tk.quicksearch.shared.util.hapticConfirm

@Composable
internal fun AllAppsDrawer(
        title: String,
        apps: List<AppInfo>,
        onDismiss: () -> Unit,
        onAppClick: (AppInfo) -> Unit,
        onAppShortcutClick: (StaticShortcut) -> Unit,
        onAppInfoClick: (AppInfo) -> Unit,
        onUninstallClick: (AppInfo) -> Unit,
        onHideApp: (AppInfo) -> Unit,
        onDisableAppShortcut: (StaticShortcut) -> Unit = {},
        onPinApp: (AppInfo) -> Unit,
        onUnpinApp: (AppInfo) -> Unit,
        onNicknameClick: (AppInfo) -> Unit,
        onTriggerClick: (AppInfo) -> Unit,
        onOpenInSplitScreen: (AppInfo) -> Unit,
        getAppNickname: (String) -> String?,
        getAppTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        pinnedPackageNames: Set<String>,
        shortcutsByPackage: Map<String, List<StaticShortcut>>,
        phoneColumnOverride: Int,
        appIconSizeStep: Int,
        iconPackPackage: String?,
        appIconShape: AppIconShape,
        notificationBadges: NotificationBadges = NotificationBadges.None,
        hasUsagePermission: Boolean = false,
) {
    val dialogColumns = getAppGridColumns(phoneColumnOverride)
    val context = LocalContext.current
    val view = LocalView.current
    val addToHomeHandler = remember(context) { AddToHomeHandler(context) }
    val privateSpace = rememberPrivateSpaceController()
    val privateSpaceState by privateSpace.state.collectAsState()
    val uninstallableApps = remember(apps, privateSpaceState.apps, context.packageName) {
        (apps.asSequence() + privateSpaceState.apps.asSequence())
            .filter { !it.isSystemApp && it.packageName != context.packageName }
            .associateBy(AppInfo::launchCountKey)
    }
    val batchUninstall = rememberAllAppsBatchUninstall(uninstallableApps)
    var isSelecting by remember { mutableStateOf(false) }
    var selectedKeys by remember { mutableStateOf(emptySet<String>()) }
    val currentSelection = remember(selectedKeys, uninstallableApps) { selectedKeys.intersect(uninstallableApps.keys) }
    LaunchedEffect(uninstallableApps) { selectedKeys = selectedKeys.intersect(uninstallableApps.keys) }
    val toggleSelection: (AppInfo) -> Unit = { app ->
        val key = app.launchCountKey()
        if (key in uninstallableApps) {
            selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
        }
    }
    // Apps arrive sorted by name; the other orders are sorted off the main thread.
    var sortOption by rememberSaveable { mutableStateOf(AppSortOption.NAME) }
    var isSortAscending by rememberSaveable { mutableStateOf(true) }
    val sortOptions = remember(hasUsagePermission) { availableAppSortOptions(hasUsagePermission) }
    LaunchedEffect(sortOptions) { if (sortOption !in sortOptions) sortOption = AppSortOption.NAME }
    // Package lookups for size, dates and API level load only once such an order is picked.
    var sortMetadata by remember(apps) { mutableStateOf<Map<String, AppSortMetadata>?>(null) }
    // The order a list was sorted with travels with it, so scrolling to the top waits for the new list.
    // Not keyed on apps: an install or removal keeps the current order until its re-sort lands,
    // instead of flashing the name order and jumping to the top.
    var sortedApps by remember { mutableStateOf(apps to (AppSortOption.NAME to true)) }
    val displayedApps = sortedApps.first
    val gridState = rememberLazyGridState()
    // Material fade through: the old order fades out, the list swaps and returns to the top while
    // hidden, then the new order fades and settles in.
    val gridReveal = remember { Animatable(1f) }
    LaunchedEffect(apps, sortOption, isSortAscending) {
        val metadata =
                if (sortOption.needsMetadata) {
                    sortMetadata
                            ?: withContext(Dispatchers.IO) {
                                apps.associate { app ->
                                    ensureActive()
                                    app.launchCountKey() to loadAppSortMetadata(context.applicationContext, app)
                                }
                            }.also { sortMetadata = it }
                } else {
                    emptyMap()
                }
        val order = sortOption to isSortAscending
        val sorted =
                withContext(Dispatchers.Default) {
                    val sorted =
                            if (sortOption == AppSortOption.NAME) {
                                apps
                            } else {
                                val locale = Locale.getDefault()
                                apps.sortedWith(appSortComparator(sortOption, metadata) { it.appName.lowercase(locale) })
                            }
                    if (isSortAscending) sorted else sorted.asReversed()
                }
        val orderChanged = sortedApps.second != order
        if (orderChanged) {
            gridReveal.animateTo(
                    0f,
                    tween(AppMotion.DurationQuick, easing = AppMotion.EmphasizedAccelerateEasing),
            )
        }
        sortedApps = sorted to order
        if (orderChanged) gridState.scrollToItem(0)
        // Always finish visible, even when a cancelled run left the grid faded out.
        gridReveal.animateTo(
                1f,
                tween(AppMotion.DurationStandard, easing = AppMotion.EmphasizedDecelerateEasing),
        )
    }
    // Load every icon in the background up front so they are already cached when scrolled to.
    LaunchedEffect(apps, iconPackPackage, appIconShape) {
        prefetchAppIconRequests(
                context = context.applicationContext,
                requests = apps.map { AppIconRequest(it.packageName, it.userHandleId) },
                iconPackPackage = iconPackPackage,
                forceCircularMask = appIconShape == AppIconShape.CIRCLE,
                parallelism = AllAppsDialogIconPrefetchParallelism,
        )
    }
    AppPickerDrawer(
            title = title,
            onDismiss = onDismiss,
            // Opening the drawer closes the Home keyboard; following its inset would grow the
            // sheet and relayout the grid on every frame of that animation.
            avoidKeyboard = false,
            header = {
                AppSortControls(
                    options = sortOptions,
                    selectedOption = sortOption,
                    optionLabel = { stringResource(it.labelResId) },
                    onOptionSelected = {
                        hapticConfirm(view)()
                        sortOption = it
                    },
                    isAscending = isSortAscending,
                    onAscendingChange = {
                        hapticConfirm(view)()
                        isSortAscending = it
                    },
                ) {
                    val selectionAction =
                        stringResource(if (isSelecting) R.string.dialog_cancel else R.string.all_apps_select_to_uninstall)
                    AppIconButton(
                        onClick = {
                            hapticConfirm(view)()
                            isSelecting = !isSelecting
                            selectedKeys = emptySet()
                        },
                        tooltip = selectionAction,
                        enabled = !batchUninstall.isBusy && (isSelecting || uninstallableApps.isNotEmpty()),
                        style = if (isSelecting) AppIconButtonStyle.Filled else AppIconButtonStyle.Tonal,
                        modifier = Modifier.size(SortRowIconButtonSize),
                    ) {
                        Icon(
                            imageVector = if (isSelecting) MaterialSymbols.Close else MaterialSymbols.DeleteOutline,
                            contentDescription = selectionAction,
                        )
                    }
                }
            },
    ) {
        var scrollbarDragFraction by remember { mutableStateOf<Float?>(null) }
        Box(
            modifier =
                Modifier.fillMaxWidth().graphicsLayer {
                    val reveal = gridReveal.value
                    alpha = reveal
                    val scale = SortRevealStartScale + (1f - SortRevealStartScale) * reveal
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 0f)
                },
        ) {
            LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(dialogColumns),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        bottom = if (currentSelection.isEmpty()) 0.dp else
                            DesignTokens.FloatingActionHeight + DesignTokens.SpacingLarge * 2,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(AllAppsDialogRowSpacing),
            ) {
                items(
                        items = displayedApps,
                        key = { app -> app.launchCountKey() },
                ) { app ->
                    AllAppsDialogGridItem(
                            app = app,
                            appIconSizeStep = appIconSizeStep,
                            iconPackPackage = iconPackPackage,
                            appIconShape = appIconShape,
                            notificationBadges = notificationBadges,
                            onClick = { if (isSelecting) toggleSelection(app) else onAppClick(app) },
                            selectedForUninstall = if (isSelecting) app.launchCountKey() in currentSelection else null,
                            uninstallSelectionEnabled = app.launchCountKey() in uninstallableApps && !batchUninstall.isBusy,
                            shortcuts = shortcutsByPackage[app.packageName].orEmpty(),
                            appActions =
                                    AppActions(
                                            onClick = { onAppClick(app) },
                                            onShortcutClick = onAppShortcutClick,
                                            onAppInfoClick = { onAppInfoClick(app) },
                                            onUninstallClick = { onUninstallClick(app) },
                                            onHideApp = { onHideApp(app) },
                                            onDisableAppShortcut = onDisableAppShortcut,
                                            onPinApp = { onPinApp(app) },
                                            onUnpinApp = { onUnpinApp(app) },
                                            onNicknameClick = { onNicknameClick(app) },
                                            onTriggerClick = { onTriggerClick(app) },
                                            onAddToHome = { addToHomeHandler.addAppToHome(app) },
                                            onOpenInSplitScreen = { onOpenInSplitScreen(app) },
                                    ),
                            appState =
                                    AppState(
                                            hasNickname =
                                                    !getAppNickname(app.launchCountKey())
                                                            .isNullOrBlank(),
                                            hasTrigger =
                                                    getAppTrigger(app.launchCountKey())
                                                            ?.word
                                                            ?.isNotBlank() == true,
                                            isPinned =
                                                    pinnedPackageNames.contains(
                                                            app.launchCountKey(),
                                                    ),
                                            showUninstall =
                                                    !app.isSystemApp &&
                                                            app.packageName != context.packageName,
                                            showAppLabel = true,
                                            isOverlayPresentation = false,
                                    ),
                    )
                }
                // Private Space apps get no options menu: pinning, nicknames, and the
                // like would persist them outside the locked profile.
                privateSpaceItems(
                        state = privateSpaceState,
                        onLockedChange = privateSpace::setLocked,
                ) { app ->
                    AllAppsDialogGridItem(
                            app = app,
                            appIconSizeStep = appIconSizeStep,
                            iconPackPackage = iconPackPackage,
                            appIconShape = appIconShape,
                            notificationBadges = NotificationBadges.None,
                            onClick = {
                                if (isSelecting) {
                                    toggleSelection(app)
                                } else {
                                    onDismiss()
                                    privateSpace.launch(context, app)
                                }
                            },
                            selectedForUninstall = if (isSelecting) app.launchCountKey() in currentSelection else null,
                            uninstallSelectionEnabled = app.launchCountKey() in uninstallableApps && !batchUninstall.isBusy,
                    )
                }
            }
            // Letters only make sense for the alphabetical order.
            if (sortOption == AppSortOption.NAME) {
                AllAppsScrollLetterPopup(
                        apps = displayedApps,
                        gridState = gridState,
                        scrollbarDragFraction = scrollbarDragFraction,
                )
            }
            LazyGridVerticalScrollbar(
                    state = gridState,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    // Sit the thumb in the drawer's end padding, clear of the last column.
                    thumbEndOutset = DesignTokens.ContentHorizontalPadding - DesignTokens.SpacingSmall,
                    onDragFractionChange = { scrollbarDragFraction = it },
            )
            if (currentSelection.isNotEmpty()) {
                AppExtendedFloatingActionButton(
                    onClick = {
                        val selectedApps = (displayedApps + privateSpaceState.apps)
                            .filter { it.launchCountKey() in currentSelection }
                            .distinctBy(AppInfo::launchCountKey)
                        batchUninstall.start(context, selectedApps) {
                            selectedKeys = emptySet()
                            isSelecting = false
                        }
                    },
                    text = stringResource(R.string.settings_app_uninstall_selected_format, currentSelection.size),
                    icon = MaterialSymbols.DeleteOutline,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(DesignTokens.SpacingLarge),
                    tone = AppFabTone.Error,
                    enabled = !batchUninstall.isBusy,
                )
            }
        }
    }
}

/** Grid scale at the start of the sort fade-through; it grows to full size as it fades in. */
private const val SortRevealStartScale = 0.96f

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AllAppsDialogGridItem(
        app: AppInfo,
        appIconSizeStep: Int,
        iconPackPackage: String?,
        appIconShape: AppIconShape,
        notificationBadges: NotificationBadges,
        onClick: () -> Unit,
        shortcuts: List<StaticShortcut> = emptyList(),
        // Null for apps without an options menu or swipe gestures (Private Space).
        appActions: AppActions? = null,
        appState: AppState? = null,
        selectedForUninstall: Boolean? = null,
        uninstallSelectionEnabled: Boolean = false,
) {
    val view = LocalView.current
    val context = LocalContext.current
    var showOptions by remember { mutableStateOf(false) }
    val sizeScale = remember(appIconSizeStep) { UiPreferences.appIconSizeScale(appIconSizeStep) }
    val iconSize = remember(sizeScale) { RegularAppIconSize * sizeScale }
    val iconSurfaceSize = remember(sizeScale) { AllAppsDialogIconSurfaceSize * sizeScale }
    val isSelecting = selectedForUninstall != null
    val isSelected = selectedForUninstall == true
    val colors = MaterialTheme.colorScheme
    val tileColor by animateColorAsState(
        targetValue = if (isSelected) colors.secondaryContainer else colors.secondaryContainer.copy(alpha = 0f),
        animationSpec = AppMotion.fastSpatial(),
        label = "uninstallTileColor",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) SelectedAppIconScale else 1f,
        animationSpec = AppMotion.fastSpatial(),
        label = "uninstallIconScale",
    )
    val handleClick = {
        hapticConfirm(view)()
        onClick()
    }
    val interactionModifier =
        if (selectedForUninstall != null) {
            Modifier
                .heightIn(min = DesignTokens.MinTouchTarget)
                .clip(AppShapes.large)
                .background(tileColor)
                // Apps that can't be uninstalled (system apps) fade out of the selection.
                .alpha(if (uninstallSelectionEnabled) 1f else AppColors.DisabledContentAlpha)
                .toggleable(
                    value = selectedForUninstall,
                    enabled = uninstallSelectionEnabled,
                    role = Role.Checkbox,
                    onValueChange = { handleClick() },
                )
        } else {
            Modifier.combinedClickable(
                role = Role.Button,
                onClick = handleClick,
                onLongClickLabel = stringResource(R.string.desc_show_options),
                onLongClick =
                    if (app.isArchived || appActions == null || appState == null) null
                    else ({ showOptions = true }),
            )
        }
    val iconResult =
            rememberAppIcon(
                    packageName = app.packageName,
                    iconPackPackage = iconPackPackage,
                    userHandleId = app.userHandleId,
                    forceCircularMask = appIconShape == AppIconShape.CIRCLE,
            )
    Box(
            modifier =
                    Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
            contentAlignment = Alignment.TopCenter,
    ) {
        Column(
                modifier =
                        Modifier
                                .fillMaxWidth()
                                .then(interactionModifier)
                                .padding(
                                        horizontal = DesignTokens.SpacingXSmall,
                                        vertical = DesignTokens.SpacingXSmall,
                                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
        ) {
            Box(
                    modifier =
                            Modifier.size(iconSurfaceSize).then(
                                    if (appActions != null && !isSelecting) Modifier.appSwipeGestures(app) else Modifier,
                            ),
                    contentAlignment = Alignment.Center,
            ) {
                iconResult.bitmap?.let { icon ->
                    Image(
                            bitmap = icon,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize).scale(iconScale),
                    )
                } ?: Icon(
                        imageVector = MaterialSymbols.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(iconSize).scale(iconScale),
                )
                if (!isSelecting) {
                    AppNotificationBadge(count = notificationBadges.countFor(app), showCount = notificationBadges.showCount)
                } else if (uninstallSelectionEnabled) {
                    UninstallSelectionBadge(selected = isSelected)
                }
            }
            Text(
                    text = app.appName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) colors.onSecondaryContainer else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
            )
        }

        if (isSelecting || appActions == null || appState == null) return@Box
        AppItemDropdownMenu(
                expanded = showOptions,
                onDismiss = { showOptions = false },
                isPinned = appState.isPinned,
                showUninstall = appState.showUninstall,
                hasNickname = appState.hasNickname,
                hasTrigger = appState.hasTrigger,
                shortcuts = shortcuts,
                appInfo = app,
                iconPackPackage = iconPackPackage,
                appIconShape = appIconShape,
                onShortcutClick = appActions.onShortcutClick,
                onAppInfoClick = appActions.onAppInfoClick,
                onHideApp = appActions.onHideApp,
                onDisableShortcut = appActions.onDisableAppShortcut,
                onPinApp = appActions.onPinApp,
                onUnpinApp = appActions.onUnpinApp,
                onUninstallClick = appActions.onUninstallClick,
                onNicknameClick = appActions.onNicknameClick,
                onTriggerClick = appActions.onTriggerClick,
                onAddToHome = appActions.onAddToHome,
                onOpenInSplitScreen = appActions.onOpenInSplitScreen,
        )
    }
}

internal fun fillSuggestionGridApps(
        primaryApps: List<AppInfo>,
        fallbackApps: List<AppInfo>,
        minItems: Int,
): List<AppInfo> {
    if (primaryApps.size >= minItems) return primaryApps

    val seen = LinkedHashSet<String>(primaryApps.size + fallbackApps.size)
    val result = ArrayList<AppInfo>(minItems)
    primaryApps.forEach { app ->
        if (seen.add(app.launchCountKey())) {
            result.add(app)
        }
    }
    fallbackApps.forEach { app ->
        if (result.size >= minItems) return@forEach
        if (seen.add(app.launchCountKey())) {
            result.add(app)
        }
    }
    return result
}

internal fun replaceSuggestionAppsWithPinned(
        pinnedApps: List<AppInfo>,
        suggestedApps: List<AppInfo>,
        slotCount: Int,
): List<AppInfo> {
    val seen = LinkedHashSet<String>(slotCount)
    return (pinnedApps + suggestedApps)
            .asSequence()
            .filter { seen.add(it.launchCountKey()) }
            .take(slotCount)
            .toList()
}

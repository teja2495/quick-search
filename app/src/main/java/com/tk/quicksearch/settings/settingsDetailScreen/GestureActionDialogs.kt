package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.lazy.itemsIndexed
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerLeadingSize
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerIconBadge
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRowSpacing
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRow
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.getDisplayName
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction

@Composable
internal fun LauncherSwipeRightActionDialog(
    selectedAction: SwipeGestureAction,
    onSelect: (SwipeGestureAction) -> Unit,
    onDismiss: () -> Unit,
) {
    GestureActionDrawer(
        title = stringResource(R.string.settings_gesture_swipe_right),
        sections =
            listOf(
                GestureOptionSection(
                    listOf(
                        SwipeGestureAction.NONE,
                        SwipeGestureAction.WIDGETS_PANEL,
                        SwipeGestureAction.ALL_APPS,
                    ).map { action -> swipeOption(action, selectedAction, onSelect) },
                ),
            ),
        onDismiss = onDismiss,
    )
}

internal fun SwipeDirection.gestureIcon() =
    when (this) {
        SwipeDirection.RIGHT -> MaterialSymbols.ArrowForward
        SwipeDirection.LEFT -> MaterialSymbols.ArrowBack
        SwipeDirection.UP -> MaterialSymbols.ArrowUpward
        SwipeDirection.DOWN -> MaterialSymbols.ArrowDownward
    }

internal fun HomeGesture.icon() =
    when (this) {
        HomeGesture.SWIPE_UP -> MaterialSymbols.ArrowUpward
        HomeGesture.SWIPE_DOWN,
        HomeGesture.DOUBLE_TAP,
        -> MaterialSymbols.ArrowDownward
        HomeGesture.TWO_FINGER_SWIPE_UP -> MaterialSymbols.SwipeUp
        HomeGesture.TWO_FINGER_SWIPE_DOWN -> MaterialSymbols.SwipeDown
        HomeGesture.NOTCH_SINGLE_TAP,
        HomeGesture.NOTCH_DOUBLE_TAP,
        HomeGesture.NOTCH_LONG_PRESS,
        -> MaterialSymbols.Swipe
    }

@Composable
internal fun HomeVerticalGestureDialog(
    titleResId: Int,
    allowsNotificationPanel: Boolean,
    allowsQuickSettings: Boolean,
    allowsCloseQuickSearch: Boolean,
    allowsLockScreen: Boolean,
    allowsAllApps: Boolean,
    hasLockScreenAccessibilityPermission: Boolean,
    allowsAppSurfaces: Boolean = false,
    selectedAction: HomeSwipeGestureAction,
    selectedCustomActionJson: String?,
    selectedAliasTarget: String?,
    customActions: List<CustomWidgetButtonAction>,
    aliasItems: List<GestureAliasItem>,
    onSelectDefault: (HomeSwipeGestureAction) -> Unit,
    onSelectLockScreen: () -> Unit,
    onPickCustom: () -> Unit,
    onPickSearchEngine: () -> Unit,
    onPickTool: () -> Unit,
    onSelectCustom: (CustomWidgetButtonAction) -> Unit,
    onSelectAlias: (HomeSwipeGestureAction, String) -> Unit,
    onDeleteCustom: (String) -> Unit,
    onDeleteAlias: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    @Composable
    fun option(action: HomeSwipeGestureAction) = homeOption(action, selectedAction, onSelectDefault)

    val actionOptions =
        buildList {
            add(option(HomeSwipeGestureAction.NONE))
            if (allowsAppSurfaces) {
                add(option(HomeSwipeGestureAction.OPEN_QUICK_SEARCH))
                add(option(HomeSwipeGestureAction.PINNED_GLANCE))
            }
            if (allowsAllApps) add(option(HomeSwipeGestureAction.ALL_APPS))
            if (allowsAppSurfaces) add(option(HomeSwipeGestureAction.WIDGETS_PANEL))
            if (allowsCloseQuickSearch) add(option(HomeSwipeGestureAction.CLOSE_QUICK_SEARCH))
            if (allowsNotificationPanel) add(option(HomeSwipeGestureAction.NOTIFICATION_PANEL))
            if (allowsQuickSettings) add(option(HomeSwipeGestureAction.QUICK_SETTINGS))
            if (allowsLockScreen) {
                add(
                    GestureOption(
                        key = HomeSwipeGestureAction.LOCK_SCREEN.name,
                        label = stringResource(R.string.settings_gesture_lock_screen),
                        subtitle =
                            if (hasLockScreenAccessibilityPermission) {
                                null
                            } else {
                                stringResource(R.string.settings_gesture_lock_screen_requires_accessibility)
                            },
                        selected = selectedAction == HomeSwipeGestureAction.LOCK_SCREEN,
                        onClick = onSelectLockScreen,
                    ),
                )
            }
            if (allowsAppSurfaces) add(option(HomeSwipeGestureAction.PLAY_PAUSE_MEDIA))
        }
    GestureActionDrawer(
        title = stringResource(titleResId),
        sections =
            listOf(
                GestureOptionSection(actionOptions),
                shortcutsSection(
                    customActions = customActions,
                    aliasItems = aliasItems,
                    isCustomSelected = { json ->
                        selectedAction == HomeSwipeGestureAction.CUSTOM && selectedCustomActionJson == json
                    },
                    isAliasSelected = { item -> selectedAction == item.homeAction && selectedAliasTarget == item.id },
                    onSelectCustom = onSelectCustom,
                    onSelectAlias = { item -> onSelectAlias(item.homeAction, item.id) },
                    onDeleteCustom = onDeleteCustom,
                    onDeleteAlias = onDeleteAlias,
                    onPickCustom = onPickCustom,
                    onPickSearchEngine = onPickSearchEngine,
                    onPickTool = onPickTool,
                ),
            ),
        onDismiss = onDismiss,
    )
}

@Composable
internal fun KeyboardGestureDialog(
    action: SwipeGestureAction,
    selectedDirection: SwipeDirection?,
    onSelect: (SwipeDirection?) -> Unit,
    onDismiss: () -> Unit,
) {
    @Composable
    fun option(direction: SwipeDirection?, labelResId: Int) =
        GestureOption(
            key = direction?.name ?: "none",
            label = stringResource(labelResId),
            selected = selectedDirection == direction,
            onClick = { onSelect(direction) },
        )

    GestureActionDrawer(
        title = stringResource(action.labelResId()),
        sections =
            listOf(
                GestureOptionSection(
                    listOf(
                        option(null, R.string.settings_gesture_none),
                        option(SwipeDirection.UP, R.string.settings_gesture_swipe_up),
                        option(SwipeDirection.DOWN, R.string.settings_gesture_swipe_down),
                    ),
                ),
            ),
        onDismiss = onDismiss,
    )
}

@Composable
internal fun GestureActionDialog(
    direction: SwipeDirection,
    allowsCloseQuickSearch: Boolean,
    selectedAction: SwipeGestureAction,
    selectedCustomActionJson: String?,
    selectedAliasTarget: String?,
    customActions: List<CustomWidgetButtonAction>,
    aliasItems: List<GestureAliasItem>,
    onSelectDefault: (SwipeGestureAction) -> Unit,
    onPickCustom: () -> Unit,
    onPickSearchEngine: () -> Unit,
    onPickTool: () -> Unit,
    onSelectCustom: (CustomWidgetButtonAction) -> Unit,
    onSelectAlias: (SwipeGestureAction, String) -> Unit,
    onDeleteCustom: (String) -> Unit,
    onDeleteAlias: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val actionOptions =
        buildList {
            add(swipeOption(SwipeGestureAction.NONE, selectedAction, onSelectDefault))
            add(swipeOption(direction.defaultAction, selectedAction, onSelectDefault))
            add(swipeOption(SwipeGestureAction.ALL_APPS, selectedAction, onSelectDefault))
            if (allowsCloseQuickSearch) {
                add(swipeOption(SwipeGestureAction.CLOSE_QUICK_SEARCH, selectedAction, onSelectDefault))
            }
        }.distinctBy { it.key }
    GestureActionDrawer(
        title = stringResource(direction.titleResId),
        sections =
            listOf(
                GestureOptionSection(actionOptions),
                shortcutsSection(
                    customActions = customActions,
                    aliasItems = aliasItems,
                    isCustomSelected = { json ->
                        selectedAction == SwipeGestureAction.CUSTOM && selectedCustomActionJson == json
                    },
                    isAliasSelected = { item -> selectedAction == item.swipeAction && selectedAliasTarget == item.id },
                    onSelectCustom = onSelectCustom,
                    onSelectAlias = { item -> onSelectAlias(item.swipeAction, item.id) },
                    onDeleteCustom = onDeleteCustom,
                    onDeleteAlias = onDeleteAlias,
                    onPickCustom = onPickCustom,
                    onPickSearchEngine = onPickSearchEngine,
                    onPickTool = onPickTool,
                ),
            ),
        onDismiss = onDismiss,
    )
}

@Composable
private fun swipeOption(
    action: SwipeGestureAction,
    selectedAction: SwipeGestureAction,
    onSelect: (SwipeGestureAction) -> Unit,
) = GestureOption(
    key = action.name,
    label = stringResource(action.labelResId()),
    selected = selectedAction == action,
    onClick = { onSelect(action) },
)

@Composable
private fun homeOption(
    action: HomeSwipeGestureAction,
    selectedAction: HomeSwipeGestureAction,
    onSelect: (HomeSwipeGestureAction) -> Unit,
) = GestureOption(
    key = action.name,
    label = stringResource(action.labelResId()),
    selected = selectedAction == action,
    onClick = { onSelect(action) },
)

/** Saved custom and alias shortcuts, then the pickers that add new ones. */
@Composable
private fun shortcutsSection(
    customActions: List<CustomWidgetButtonAction>,
    aliasItems: List<GestureAliasItem>,
    isCustomSelected: (String) -> Boolean,
    isAliasSelected: (GestureAliasItem) -> Boolean,
    onSelectCustom: (CustomWidgetButtonAction) -> Unit,
    onSelectAlias: (GestureAliasItem) -> Unit,
    onDeleteCustom: (String) -> Unit,
    onDeleteAlias: (String) -> Unit,
    onPickCustom: () -> Unit,
    onPickSearchEngine: () -> Unit,
    onPickTool: () -> Unit,
): GestureOptionSection {
    val saved =
        customActions.map { action ->
            val json = action.toJson()
            GestureOption(
                key = "custom_$json",
                label = action.displayLabel(),
                selected = isCustomSelected(json),
                onDelete = { onDeleteCustom(json) },
                onClick = { onSelectCustom(action) },
            )
        } +
            aliasItems.map { item ->
                GestureOption(
                    key = "alias_${item.id}",
                    label = item.label,
                    selected = isAliasSelected(item),
                    onDelete = { onDeleteAlias(item.id) },
                    onClick = { onSelectAlias(item) },
                )
            }
    val pickers =
        listOf(
            GestureOption(
                key = "picker_custom",
                label = stringResource(R.string.settings_gesture_custom),
                opensPicker = true,
                onClick = onPickCustom,
            ),
            GestureOption(
                key = "picker_search_engine",
                label = stringResource(R.string.settings_app_shortcuts_filter_search_engines),
                opensPicker = true,
                onClick = onPickSearchEngine,
            ),
            GestureOption(
                key = "picker_tool",
                label = stringResource(R.string.settings_tools_title),
                opensPicker = true,
                onClick = onPickTool,
            ),
        )
    return GestureOptionSection(saved + pickers)
}

@Composable
internal fun gestureDescription(action: SwipeGestureAction, customActionJson: String?, aliasTarget: String?, state: com.tk.quicksearch.search.core.SearchUiState): String =
    customActionJson?.let(CustomWidgetButtonAction::fromJson)?.displayLabel()
        ?.takeIf { action == SwipeGestureAction.CUSTOM }
        ?: aliasTarget?.takeIf { action == SwipeGestureAction.SEARCH_ENGINE || action == SwipeGestureAction.TOOL }?.let { aliasDisplayName(it, state) }
        ?: stringResource(action.labelResId())

@Composable
internal fun homeGestureDescription(action: HomeSwipeGestureAction, customActionJson: String?, aliasTarget: String?, state: com.tk.quicksearch.search.core.SearchUiState): String =
    customActionJson?.let(CustomWidgetButtonAction::fromJson)?.displayLabel()
        ?.takeIf { action == HomeSwipeGestureAction.CUSTOM }
        ?: aliasTarget?.takeIf { action == HomeSwipeGestureAction.SEARCH_ENGINE || action == HomeSwipeGestureAction.TOOL }?.let { aliasDisplayName(it, state) }
        ?: stringResource(action.labelResId())

internal fun allCustomActions(
    swipeCustomActions: Map<*, String?>,
    homeCustomActions: Map<*, String?>,
): List<CustomWidgetButtonAction> =
    (swipeCustomActions.values + homeCustomActions.values)
        .mapNotNull(CustomWidgetButtonAction::fromJson)
        .distinctBy { it.toJson() }

@Composable
internal fun aliasDisplayName(
    targetId: String,
    state: com.tk.quicksearch.search.core.SearchUiState,
): String =
    state.searchTargetsOrder.firstOrNull { it.getId() == targetId }?.getDisplayName()
        ?: gestureToolItems(state).firstOrNull { it.first == targetId }?.second
        ?: targetId

internal fun UserAppPreferences.actionFor(direction: SwipeDirection): SwipeGestureAction =
    when (direction) {
        SwipeDirection.RIGHT -> getSwipeRightAction()
        SwipeDirection.LEFT -> getSwipeLeftAction()
        SwipeDirection.UP -> getSwipeUpAction()
        SwipeDirection.DOWN -> getSwipeDownAction()
    }

internal fun UserAppPreferences.customActionFor(direction: SwipeDirection): String? =
    when (direction) {
        SwipeDirection.RIGHT -> getSwipeRightCustomAction()
        SwipeDirection.LEFT -> getSwipeLeftCustomAction()
        SwipeDirection.UP -> getSwipeUpCustomAction()
        SwipeDirection.DOWN -> getSwipeDownCustomAction()
    }

internal fun UserAppPreferences.setActionFor(direction: SwipeDirection, action: SwipeGestureAction) {
    when (direction) {
        SwipeDirection.RIGHT -> setSwipeRightAction(action)
        SwipeDirection.LEFT -> setSwipeLeftAction(action)
        SwipeDirection.UP -> setSwipeUpAction(action)
        SwipeDirection.DOWN -> setSwipeDownAction(action)
    }
}

internal fun UserAppPreferences.setCustomActionFor(direction: SwipeDirection, actionJson: String?) {
    when (direction) {
        SwipeDirection.RIGHT -> setSwipeRightCustomAction(actionJson)
        SwipeDirection.LEFT -> setSwipeLeftCustomAction(actionJson)
        SwipeDirection.UP -> setSwipeUpCustomAction(actionJson)
        SwipeDirection.DOWN -> setSwipeDownCustomAction(actionJson)
    }
}

internal fun UserAppPreferences.aliasTargetFor(direction: SwipeDirection): String? =
    when (direction) {
        SwipeDirection.RIGHT -> getSwipeRightAliasTarget()
        SwipeDirection.LEFT -> getSwipeLeftAliasTarget()
        SwipeDirection.UP -> getSwipeUpAliasTarget()
        SwipeDirection.DOWN -> getSwipeDownAliasTarget()
    }

internal fun UserAppPreferences.setAliasTargetFor(direction: SwipeDirection, targetId: String?) {
    when (direction) {
        SwipeDirection.RIGHT -> setSwipeRightAliasTarget(targetId)
        SwipeDirection.LEFT -> setSwipeLeftAliasTarget(targetId)
        SwipeDirection.UP -> setSwipeUpAliasTarget(targetId)
        SwipeDirection.DOWN -> setSwipeDownAliasTarget(targetId)
    }
}

internal fun UserAppPreferences.homeActionFor(
    gesture: HomeGesture,
    isDefaultLauncher: Boolean,
): HomeSwipeGestureAction =
    when (gesture) {
        HomeGesture.SWIPE_UP -> getHomeSwipeUpAction()
        HomeGesture.SWIPE_DOWN -> getHomeSwipeDownAction(isDefaultLauncher)
        HomeGesture.DOUBLE_TAP -> getHomeDoubleTapAction()
        HomeGesture.TWO_FINGER_SWIPE_UP -> getHomeTwoFingerSwipeUpAction()
        HomeGesture.TWO_FINGER_SWIPE_DOWN -> getHomeTwoFingerSwipeDownAction()
        HomeGesture.NOTCH_SINGLE_TAP -> getNotchSingleTapAction()
        HomeGesture.NOTCH_DOUBLE_TAP -> getNotchDoubleTapAction()
        HomeGesture.NOTCH_LONG_PRESS -> getNotchLongPressAction()
    }

internal fun UserAppPreferences.setHomeActionFor(gesture: HomeGesture, action: HomeSwipeGestureAction) {
    when (gesture) {
        HomeGesture.SWIPE_UP -> setHomeSwipeUpAction(action)
        HomeGesture.SWIPE_DOWN -> setHomeSwipeDownAction(action)
        HomeGesture.DOUBLE_TAP -> setHomeDoubleTapAction(action)
        HomeGesture.TWO_FINGER_SWIPE_UP -> setHomeTwoFingerSwipeUpAction(action)
        HomeGesture.TWO_FINGER_SWIPE_DOWN -> setHomeTwoFingerSwipeDownAction(action)
        HomeGesture.NOTCH_SINGLE_TAP -> setNotchSingleTapAction(action)
        HomeGesture.NOTCH_DOUBLE_TAP -> setNotchDoubleTapAction(action)
        HomeGesture.NOTCH_LONG_PRESS -> setNotchLongPressAction(action)
    }
}

internal fun UserAppPreferences.homeCustomActionFor(gesture: HomeGesture): String? =
    when (gesture) {
        HomeGesture.SWIPE_UP -> getHomeSwipeUpCustomAction()
        HomeGesture.SWIPE_DOWN -> getHomeSwipeDownCustomAction()
        HomeGesture.DOUBLE_TAP -> getHomeDoubleTapCustomAction()
        HomeGesture.TWO_FINGER_SWIPE_UP -> getHomeTwoFingerSwipeUpCustomAction()
        HomeGesture.TWO_FINGER_SWIPE_DOWN -> getHomeTwoFingerSwipeDownCustomAction()
        HomeGesture.NOTCH_SINGLE_TAP -> getNotchSingleTapCustomAction()
        HomeGesture.NOTCH_DOUBLE_TAP -> getNotchDoubleTapCustomAction()
        HomeGesture.NOTCH_LONG_PRESS -> getNotchLongPressCustomAction()
    }

internal fun UserAppPreferences.setHomeCustomActionFor(gesture: HomeGesture, actionJson: String?) {
    when (gesture) {
        HomeGesture.SWIPE_UP -> setHomeSwipeUpCustomAction(actionJson)
        HomeGesture.SWIPE_DOWN -> setHomeSwipeDownCustomAction(actionJson)
        HomeGesture.DOUBLE_TAP -> setHomeDoubleTapCustomAction(actionJson)
        HomeGesture.TWO_FINGER_SWIPE_UP -> setHomeTwoFingerSwipeUpCustomAction(actionJson)
        HomeGesture.TWO_FINGER_SWIPE_DOWN -> setHomeTwoFingerSwipeDownCustomAction(actionJson)
        HomeGesture.NOTCH_SINGLE_TAP -> setNotchSingleTapCustomAction(actionJson)
        HomeGesture.NOTCH_DOUBLE_TAP -> setNotchDoubleTapCustomAction(actionJson)
        HomeGesture.NOTCH_LONG_PRESS -> setNotchLongPressCustomAction(actionJson)
    }
}

internal fun UserAppPreferences.homeAliasTargetFor(gesture: HomeGesture): String? =
    when (gesture) {
        HomeGesture.SWIPE_UP -> getHomeSwipeUpAliasTarget()
        HomeGesture.SWIPE_DOWN -> getHomeSwipeDownAliasTarget()
        HomeGesture.DOUBLE_TAP -> getHomeDoubleTapAliasTarget()
        HomeGesture.TWO_FINGER_SWIPE_UP -> getHomeTwoFingerSwipeUpAliasTarget()
        HomeGesture.TWO_FINGER_SWIPE_DOWN -> getHomeTwoFingerSwipeDownAliasTarget()
        HomeGesture.NOTCH_SINGLE_TAP -> getNotchSingleTapAliasTarget()
        HomeGesture.NOTCH_DOUBLE_TAP -> getNotchDoubleTapAliasTarget()
        HomeGesture.NOTCH_LONG_PRESS -> getNotchLongPressAliasTarget()
    }

internal fun UserAppPreferences.setHomeAliasTargetFor(gesture: HomeGesture, targetId: String?) {
    when (gesture) {
        HomeGesture.SWIPE_UP -> setHomeSwipeUpAliasTarget(targetId)
        HomeGesture.SWIPE_DOWN -> setHomeSwipeDownAliasTarget(targetId)
        HomeGesture.DOUBLE_TAP -> setHomeDoubleTapAliasTarget(targetId)
        HomeGesture.TWO_FINGER_SWIPE_UP -> setHomeTwoFingerSwipeUpAliasTarget(targetId)
        HomeGesture.TWO_FINGER_SWIPE_DOWN -> setHomeTwoFingerSwipeDownAliasTarget(targetId)
        HomeGesture.NOTCH_SINGLE_TAP -> setNotchSingleTapAliasTarget(targetId)
        HomeGesture.NOTCH_DOUBLE_TAP -> setNotchDoubleTapAliasTarget(targetId)
        HomeGesture.NOTCH_LONG_PRESS -> setNotchLongPressAliasTarget(targetId)
    }
}

@Composable
internal fun GestureToolPickerDialog(
    state: com.tk.quicksearch.search.core.SearchUiState,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val tools = ToolSettingsRegistry.definitions.mapNotNull { definition ->
        definition.aliasFeatureId?.let { GestureToolItem(it, stringResource(definition.titleResId), definition.icon) }
    } +
        state.customTools.filterNot { it.id in state.disabledCustomToolIds }.map { GestureToolItem(it.id, it.name, MaterialSymbols.Build) } +
        state.taskerIntentTools.map { GestureToolItem(it.id, it.name, MaterialSymbols.Build) }
    AppPickerDrawer(
        title = stringResource(R.string.settings_tools_title),
        onDismiss = onDismiss,
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing)) {
            itemsIndexed(tools, key = { _, tool -> tool.id }) { _, tool ->
                AppPickerDrawerRow(
                    title = tool.label,
                    onClick = { onSelect(tool.id) },
                    leading = { AppPickerDrawerIconBadge(icon = tool.icon) },
                )
            }
        }
    }
}

internal data class GestureToolItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
internal fun GestureSearchEnginePickerDialog(
    targets: List<SearchTarget>,
    onDismiss: () -> Unit,
    onSelect: (SearchTarget) -> Unit,
) {
    AppPickerDrawer(
        title = stringResource(R.string.settings_app_shortcuts_filter_search_engines),
        onDismiss = onDismiss,
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing)) {
            itemsIndexed(targets, key = { _, target -> target.getId() }) { _, target ->
                AppPickerDrawerRow(
                    title = target.getDisplayName(),
                    onClick = { onSelect(target) },
                    leading = { SearchTargetIcon(target = target, iconSize = AppPickerDrawerLeadingSize) },
                )
            }
        }
    }
}

internal data class GestureAliasItem(
    val id: String,
    val label: String,
    val swipeAction: SwipeGestureAction,
    val homeAction: HomeSwipeGestureAction,
)

@Composable
internal fun allGestureAliasItems(
    state: com.tk.quicksearch.search.core.SearchUiState,
    swipeTargets: Map<SwipeDirection, String?>,
    homeTargets: Map<HomeGesture, String?>,
): List<GestureAliasItem> {
    val selectedIds = (swipeTargets.values + homeTargets.values).filterNotNull().toSet()
    val engineNames = state.searchTargetsOrder.associate { it.getId() to it.getDisplayName() }
    val toolNames = gestureToolItems(state).toMap()
    return selectedIds.mapNotNull { id ->
        engineNames[id]?.let { GestureAliasItem(id, it, SwipeGestureAction.SEARCH_ENGINE, HomeSwipeGestureAction.SEARCH_ENGINE) }
            ?: toolNames[id]?.let { GestureAliasItem(id, it, SwipeGestureAction.TOOL, HomeSwipeGestureAction.TOOL) }
    }
}

@Composable
internal fun gestureToolItems(state: com.tk.quicksearch.search.core.SearchUiState): List<Pair<String, String>> =
    ToolSettingsRegistry.definitions.mapNotNull { definition ->
        definition.aliasFeatureId?.let { it to stringResource(definition.titleResId) }
    } +
        state.customTools
            .filterNot { it.id in state.disabledCustomToolIds }
            .map { it.id to it.name } +
        state.taskerIntentTools.map { it.id to it.name }

internal fun SwipeGestureAction.labelResId(): Int =
    when (this) {
        SwipeGestureAction.CLOSE_QUICK_SEARCH -> R.string.settings_gesture_close_quick_search
        SwipeGestureAction.WIDGETS_PANEL -> R.string.settings_gesture_widget_panel
        SwipeGestureAction.SETTINGS -> R.string.settings_gesture_settings
        SwipeGestureAction.ALL_APPS -> R.string.settings_gesture_view_all_apps
        SwipeGestureAction.OPEN_KEYBOARD -> R.string.action_open_keyboard
        SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS -> R.string.settings_gesture_close_keyboard_notifications
        SwipeGestureAction.CUSTOM -> R.string.settings_gesture_custom
        SwipeGestureAction.SEARCH_ENGINE -> R.string.settings_app_shortcuts_filter_search_engines
        SwipeGestureAction.TOOL -> R.string.settings_tools_title
        SwipeGestureAction.NONE -> R.string.settings_gesture_none
    }

internal fun HomeSwipeGestureAction.labelResId(): Int =
    when (this) {
        HomeSwipeGestureAction.CLOSE_QUICK_SEARCH -> R.string.settings_gesture_close_quick_search
        HomeSwipeGestureAction.LOCK_SCREEN -> R.string.settings_gesture_lock_screen
        HomeSwipeGestureAction.NOTIFICATION_PANEL -> R.string.settings_gesture_notification_panel
        HomeSwipeGestureAction.QUICK_SETTINGS -> R.string.settings_gesture_quick_settings
        HomeSwipeGestureAction.ALL_APPS -> R.string.settings_gesture_view_all_apps
        HomeSwipeGestureAction.CUSTOM -> R.string.settings_gesture_custom
        HomeSwipeGestureAction.SEARCH_ENGINE -> R.string.settings_app_shortcuts_filter_search_engines
        HomeSwipeGestureAction.TOOL -> R.string.settings_tools_title
        HomeSwipeGestureAction.OPEN_QUICK_SEARCH -> R.string.settings_gesture_open_quick_search
        HomeSwipeGestureAction.PINNED_GLANCE -> R.string.settings_gesture_pinned_glance
        HomeSwipeGestureAction.WIDGETS_PANEL -> R.string.settings_gesture_widget_panel
        HomeSwipeGestureAction.PLAY_PAUSE_MEDIA -> R.string.settings_gesture_play_pause_media
        HomeSwipeGestureAction.NONE -> R.string.settings_gesture_none
    }

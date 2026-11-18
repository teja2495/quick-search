package com.tk.quicksearch.search.data.preferences

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction.*
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

private const val NOTCH_GESTURE_KEY_PREFIX = "notch_gesture_"

/** Each prefix is part of the persisted settings and backup format. */
enum class SwipeDirection(
    internal val preferencePrefix: String,
    @StringRes val titleResId: Int,
    val defaultAction: SwipeGestureAction,
    val icon: () -> ImageVector,
) {
    RIGHT("swipe_right", R.string.settings_gesture_swipe_right, SwipeGestureAction.WIDGETS_PANEL, { MaterialSymbols.ArrowForward }),
    LEFT("swipe_left", R.string.settings_gesture_swipe_left, SwipeGestureAction.SETTINGS, { MaterialSymbols.ArrowBack }),
    UP("swipe_up", R.string.settings_gesture_swipe_up, SwipeGestureAction.OPEN_KEYBOARD, { MaterialSymbols.ArrowUpward }),
    DOWN("swipe_down", R.string.settings_gesture_swipe_down, SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS, { MaterialSymbols.ArrowDownward }),
}

private val notchActions = listOf(OPEN_QUICK_SEARCH, PINNED_GLANCE, ALL_APPS, WIDGETS_PANEL, LOCK_SCREEN, PLAY_PAUSE_MEDIA)

enum class HomeGesture(
    internal val preferencePrefix: String,
    @StringRes val titleResId: Int,
    val icon: () -> ImageVector,
    private val actions: List<HomeSwipeGestureAction>,
    val defaultAction: HomeSwipeGestureAction = NONE,
    private val nonLauncherDefaultAction: HomeSwipeGestureAction = defaultAction,
    @StringRes val searchTitleResId: Int = titleResId,
) {
    SWIPE_UP(
        "home_swipe_up",
        R.string.settings_gesture_swipe_up,
        { MaterialSymbols.ArrowUpward },
        listOf(ALL_APPS, CLOSE_QUICK_SEARCH),
    ),
    SWIPE_DOWN(
        "home_swipe_down",
        R.string.settings_gesture_swipe_down,
        { MaterialSymbols.ArrowDownward },
        listOf(ALL_APPS, CLOSE_QUICK_SEARCH, NOTIFICATION_PANEL, QUICK_SETTINGS),
        defaultAction = NOTIFICATION_PANEL,
        nonLauncherDefaultAction = NONE,
    ),
    DOUBLE_TAP(
        "home_double_tap",
        R.string.settings_gesture_double_tap_home,
        { MaterialSymbols.ArrowDownward },
        listOf(CLOSE_QUICK_SEARCH, NOTIFICATION_PANEL, LOCK_SCREEN),
    ),
    TWO_FINGER_SWIPE_UP(
        "home_two_finger_swipe_up",
        R.string.settings_gesture_two_finger_swipe_up_home,
        { MaterialSymbols.SwipeUp },
        listOf(ALL_APPS),
    ),
    TWO_FINGER_SWIPE_DOWN(
        "home_two_finger_swipe_down",
        R.string.settings_gesture_two_finger_swipe_down_home,
        { MaterialSymbols.SwipeDown },
        listOf(NOTIFICATION_PANEL, QUICK_SETTINGS),
    ),
    NOTCH_SINGLE_TAP(
        "${NOTCH_GESTURE_KEY_PREFIX}single_tap",
        R.string.settings_gesture_single_tap,
        { MaterialSymbols.Swipe },
        notchActions,
        searchTitleResId = R.string.settings_notch_single_tap_search_title,
    ),
    NOTCH_DOUBLE_TAP(
        "${NOTCH_GESTURE_KEY_PREFIX}double_tap",
        R.string.settings_gesture_double_tap_home,
        { MaterialSymbols.Swipe },
        notchActions,
        searchTitleResId = R.string.settings_notch_double_tap_search_title,
    ),
    NOTCH_LONG_PRESS(
        "${NOTCH_GESTURE_KEY_PREFIX}long_press",
        R.string.settings_gesture_long_press,
        { MaterialSymbols.Swipe },
        notchActions,
        searchTitleResId = R.string.settings_notch_long_press_search_title,
    ),
    ;

    val isNotch: Boolean
        get() = isNotchGesturePreference(preferencePrefix)

    internal val searchKey: String
        get() = preferencePrefix.removePrefix("home_").replaceFirst(NOTCH_GESTURE_KEY_PREFIX, "notch_")

    internal fun defaultAction(isDefaultLauncher: Boolean): HomeSwipeGestureAction =
        if (isDefaultLauncher) defaultAction else nonLauncherDefaultAction

    internal fun availableActions(isDefaultLauncher: Boolean): List<HomeSwipeGestureAction> =
        listOf(NONE) + if (isDefaultLauncher) actions.filterNot { it == CLOSE_QUICK_SEARCH } else actions
}

internal val SwipeDirection.actionKey: String get() = "${preferencePrefix}_action"
internal val HomeGesture.actionKey: String get() = "${preferencePrefix}_action"
internal val SwipeDirection.customActionKey: String get() = "${preferencePrefix}_custom_action"
internal val HomeGesture.customActionKey: String get() = "${preferencePrefix}_custom_action"
internal val SwipeDirection.aliasTargetKey: String get() = "${preferencePrefix}_alias_target"
internal val HomeGesture.aliasTargetKey: String get() = "${preferencePrefix}_alias_target"

internal fun isNotchGesturePreference(key: String?): Boolean = key?.startsWith(NOTCH_GESTURE_KEY_PREFIX) == true

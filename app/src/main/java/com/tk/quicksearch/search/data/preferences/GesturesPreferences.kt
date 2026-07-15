package com.tk.quicksearch.search.data.preferences

import android.content.Context

enum class SwipeGestureAction {
    QUICK_NOTE,
    SETTINGS,
    OPEN_KEYBOARD,
    CLOSE_KEYBOARD_OR_NOTIFICATIONS,
    CUSTOM,
    NONE,
}

enum class HomeSwipeGestureAction {
    NOTIFICATION_PANEL,
    CUSTOM,
    NONE,
}

class GesturesPreferences(
    context: Context,
) : BasePreferences(context) {
    fun getSwipeRightAction(): SwipeGestureAction =
        getGestureAction(KEY_SWIPE_RIGHT_ACTION, SwipeGestureAction.QUICK_NOTE)

    fun setSwipeRightAction(action: SwipeGestureAction) =
        setGestureAction(KEY_SWIPE_RIGHT_ACTION, action)

    fun getSwipeRightCustomAction(): String? = prefs.getString(KEY_SWIPE_RIGHT_CUSTOM_ACTION, null)

    fun setSwipeRightCustomAction(actionJson: String?) = setCustomAction(KEY_SWIPE_RIGHT_CUSTOM_ACTION, actionJson)

    fun getSwipeLeftAction(): SwipeGestureAction =
        getGestureAction(KEY_SWIPE_LEFT_ACTION, SwipeGestureAction.SETTINGS)

    fun setSwipeLeftAction(action: SwipeGestureAction) =
        setGestureAction(KEY_SWIPE_LEFT_ACTION, action)

    fun getSwipeLeftCustomAction(): String? = prefs.getString(KEY_SWIPE_LEFT_CUSTOM_ACTION, null)

    fun setSwipeLeftCustomAction(actionJson: String?) = setCustomAction(KEY_SWIPE_LEFT_CUSTOM_ACTION, actionJson)

    fun getSwipeUpAction(): SwipeGestureAction =
        getGestureAction(KEY_SWIPE_UP_ACTION, SwipeGestureAction.OPEN_KEYBOARD)

    fun setSwipeUpAction(action: SwipeGestureAction) =
        setGestureAction(KEY_SWIPE_UP_ACTION, action)

    fun getSwipeUpCustomAction(): String? = prefs.getString(KEY_SWIPE_UP_CUSTOM_ACTION, null)

    fun setSwipeUpCustomAction(actionJson: String?) = setCustomAction(KEY_SWIPE_UP_CUSTOM_ACTION, actionJson)

    fun getSwipeDownAction(): SwipeGestureAction =
        getGestureAction(KEY_SWIPE_DOWN_ACTION, SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS)

    fun setSwipeDownAction(action: SwipeGestureAction) =
        setGestureAction(KEY_SWIPE_DOWN_ACTION, action)

    fun getSwipeDownCustomAction(): String? = prefs.getString(KEY_SWIPE_DOWN_CUSTOM_ACTION, null)

    fun setSwipeDownCustomAction(actionJson: String?) = setCustomAction(KEY_SWIPE_DOWN_CUSTOM_ACTION, actionJson)

    fun getHomeSwipeUpAction(): HomeSwipeGestureAction =
        getHomeGestureAction(
            key = KEY_HOME_SWIPE_UP_ACTION,
            legacyNotificationKey = KEY_HOME_SWIPE_UP_NOTIFICATION_ENABLED,
            default = HomeSwipeGestureAction.NONE,
        )

    fun setHomeSwipeUpAction(action: HomeSwipeGestureAction) =
        setHomeGestureAction(KEY_HOME_SWIPE_UP_ACTION, action)

    fun getHomeSwipeUpCustomAction(): String? = prefs.getString(KEY_HOME_SWIPE_UP_CUSTOM_ACTION, null)

    fun setHomeSwipeUpCustomAction(actionJson: String?) = setCustomAction(KEY_HOME_SWIPE_UP_CUSTOM_ACTION, actionJson)

    fun getHomeSwipeDownAction(): HomeSwipeGestureAction =
        getHomeGestureAction(
            key = KEY_HOME_SWIPE_DOWN_ACTION,
            legacyNotificationKey = KEY_HOME_SWIPE_DOWN_NOTIFICATION_ENABLED,
            default = HomeSwipeGestureAction.NOTIFICATION_PANEL,
        )

    fun setHomeSwipeDownAction(action: HomeSwipeGestureAction) =
        setHomeGestureAction(KEY_HOME_SWIPE_DOWN_ACTION, action)

    fun getHomeSwipeDownCustomAction(): String? = prefs.getString(KEY_HOME_SWIPE_DOWN_CUSTOM_ACTION, null)

    fun setHomeSwipeDownCustomAction(actionJson: String?) = setCustomAction(KEY_HOME_SWIPE_DOWN_CUSTOM_ACTION, actionJson)

    private fun getGestureAction(key: String, default: SwipeGestureAction): SwipeGestureAction =
        prefs.getString(key, default.name)
            ?.let { value -> SwipeGestureAction.entries.firstOrNull { it.name == value } }
            ?: default

    private fun setGestureAction(key: String, action: SwipeGestureAction) {
        prefs.edit().putString(key, action.name).apply()
    }

    private fun getHomeGestureAction(
        key: String,
        legacyNotificationKey: String,
        default: HomeSwipeGestureAction,
    ): HomeSwipeGestureAction {
        prefs.getString(key, null)
            ?.let { value -> HomeSwipeGestureAction.entries.firstOrNull { it.name == value } }
            ?.let { return it }
        return if (prefs.getBoolean(legacyNotificationKey, default == HomeSwipeGestureAction.NOTIFICATION_PANEL)) {
            HomeSwipeGestureAction.NOTIFICATION_PANEL
        } else {
            HomeSwipeGestureAction.NONE
        }
    }

    private fun setHomeGestureAction(key: String, action: HomeSwipeGestureAction) {
        prefs.edit().putString(key, action.name).apply()
    }

    private fun setCustomAction(key: String, actionJson: String?) {
        prefs.edit().apply {
            if (actionJson == null) remove(key) else putString(key, actionJson)
            apply()
        }
    }
}

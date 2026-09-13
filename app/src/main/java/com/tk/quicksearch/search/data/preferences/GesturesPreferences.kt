package com.tk.quicksearch.search.data.preferences

import android.content.Context

enum class SwipeGestureAction {
    CLOSE_QUICK_SEARCH,
    WIDGETS_PANEL,
    SETTINGS,
    OPEN_KEYBOARD,
    CLOSE_KEYBOARD_OR_NOTIFICATIONS,
    CUSTOM,
    SEARCH_ENGINE,
    TOOL,
    NONE,
}

enum class HomeSwipeGestureAction {
    CLOSE_QUICK_SEARCH,
    LOCK_SCREEN,
    NOTIFICATION_PANEL,
    CUSTOM,
    SEARCH_ENGINE,
    TOOL,
    NONE,
}

class GesturesPreferences(
    context: Context,
) : BasePreferences(context) {
    fun getSwipeRightAction(): SwipeGestureAction =
        getGestureAction(KEY_SWIPE_RIGHT_ACTION, SwipeGestureAction.WIDGETS_PANEL)

    fun setSwipeRightAction(action: SwipeGestureAction) =
        setGestureAction(KEY_SWIPE_RIGHT_ACTION, action)

    fun getSwipeRightCustomAction(): String? = prefs.getString(KEY_SWIPE_RIGHT_CUSTOM_ACTION, null)

    fun setSwipeRightCustomAction(actionJson: String?) = setCustomAction(KEY_SWIPE_RIGHT_CUSTOM_ACTION, actionJson)
    fun getSwipeRightAliasTarget(): String? = prefs.getString(KEY_SWIPE_RIGHT_ALIAS_TARGET, null)
    fun setSwipeRightAliasTarget(targetId: String?) = setCustomAction(KEY_SWIPE_RIGHT_ALIAS_TARGET, targetId)

    fun isLauncherSwipeRightEnabled(): Boolean =
        PreferenceUtils.getBooleanPref(prefs, KEY_LAUNCHER_SWIPE_RIGHT_ENABLED, true)

    fun setLauncherSwipeRightEnabled(enabled: Boolean) =
        PreferenceUtils.setBooleanPref(prefs, KEY_LAUNCHER_SWIPE_RIGHT_ENABLED, enabled)

    fun getSwipeLeftAction(): SwipeGestureAction =
        getGestureAction(KEY_SWIPE_LEFT_ACTION, SwipeGestureAction.SETTINGS)

    fun setSwipeLeftAction(action: SwipeGestureAction) =
        setGestureAction(KEY_SWIPE_LEFT_ACTION, action)

    fun getSwipeLeftCustomAction(): String? = prefs.getString(KEY_SWIPE_LEFT_CUSTOM_ACTION, null)

    fun setSwipeLeftCustomAction(actionJson: String?) = setCustomAction(KEY_SWIPE_LEFT_CUSTOM_ACTION, actionJson)
    fun getSwipeLeftAliasTarget(): String? = prefs.getString(KEY_SWIPE_LEFT_ALIAS_TARGET, null)
    fun setSwipeLeftAliasTarget(targetId: String?) = setCustomAction(KEY_SWIPE_LEFT_ALIAS_TARGET, targetId)

    fun getSwipeUpAction(): SwipeGestureAction =
        getGestureAction(KEY_SWIPE_UP_ACTION, SwipeGestureAction.OPEN_KEYBOARD)

    fun setSwipeUpAction(action: SwipeGestureAction) =
        setGestureAction(KEY_SWIPE_UP_ACTION, action)

    fun getSwipeUpCustomAction(): String? = prefs.getString(KEY_SWIPE_UP_CUSTOM_ACTION, null)

    fun setSwipeUpCustomAction(actionJson: String?) = setCustomAction(KEY_SWIPE_UP_CUSTOM_ACTION, actionJson)
    fun getSwipeUpAliasTarget(): String? = prefs.getString(KEY_SWIPE_UP_ALIAS_TARGET, null)
    fun setSwipeUpAliasTarget(targetId: String?) = setCustomAction(KEY_SWIPE_UP_ALIAS_TARGET, targetId)

    fun getSwipeDownAction(): SwipeGestureAction =
        getGestureAction(KEY_SWIPE_DOWN_ACTION, SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS)

    fun setSwipeDownAction(action: SwipeGestureAction) =
        setGestureAction(KEY_SWIPE_DOWN_ACTION, action)

    fun getSwipeDownCustomAction(): String? = prefs.getString(KEY_SWIPE_DOWN_CUSTOM_ACTION, null)

    fun setSwipeDownCustomAction(actionJson: String?) = setCustomAction(KEY_SWIPE_DOWN_CUSTOM_ACTION, actionJson)
    fun getSwipeDownAliasTarget(): String? = prefs.getString(KEY_SWIPE_DOWN_ALIAS_TARGET, null)
    fun setSwipeDownAliasTarget(targetId: String?) = setCustomAction(KEY_SWIPE_DOWN_ALIAS_TARGET, targetId)

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
    fun getHomeSwipeUpAliasTarget(): String? = prefs.getString(KEY_HOME_SWIPE_UP_ALIAS_TARGET, null)
    fun setHomeSwipeUpAliasTarget(targetId: String?) = setCustomAction(KEY_HOME_SWIPE_UP_ALIAS_TARGET, targetId)

    fun getHomeSwipeDownAction(isDefaultLauncher: Boolean = true): HomeSwipeGestureAction =
        getHomeGestureAction(
            key = KEY_HOME_SWIPE_DOWN_ACTION,
            legacyNotificationKey = KEY_HOME_SWIPE_DOWN_NOTIFICATION_ENABLED,
            default =
                if (isDefaultLauncher) {
                    HomeSwipeGestureAction.NOTIFICATION_PANEL
                } else {
                    HomeSwipeGestureAction.NONE
                },
        )

    fun setHomeSwipeDownAction(action: HomeSwipeGestureAction) =
        setHomeGestureAction(KEY_HOME_SWIPE_DOWN_ACTION, action)

    fun getHomeSwipeDownCustomAction(): String? = prefs.getString(KEY_HOME_SWIPE_DOWN_CUSTOM_ACTION, null)

    fun setHomeSwipeDownCustomAction(actionJson: String?) = setCustomAction(KEY_HOME_SWIPE_DOWN_CUSTOM_ACTION, actionJson)
    fun getHomeSwipeDownAliasTarget(): String? = prefs.getString(KEY_HOME_SWIPE_DOWN_ALIAS_TARGET, null)
    fun setHomeSwipeDownAliasTarget(targetId: String?) = setCustomAction(KEY_HOME_SWIPE_DOWN_ALIAS_TARGET, targetId)

    fun getHomeDoubleTapAction(): HomeSwipeGestureAction =
        getHomeGestureAction(KEY_HOME_DOUBLE_TAP_ACTION, HomeSwipeGestureAction.NONE)

    fun setHomeDoubleTapAction(action: HomeSwipeGestureAction) =
        setHomeGestureAction(KEY_HOME_DOUBLE_TAP_ACTION, action)

    fun getHomeDoubleTapCustomAction(): String? = prefs.getString(KEY_HOME_DOUBLE_TAP_CUSTOM_ACTION, null)

    fun setHomeDoubleTapCustomAction(actionJson: String?) = setCustomAction(KEY_HOME_DOUBLE_TAP_CUSTOM_ACTION, actionJson)
    fun getHomeDoubleTapAliasTarget(): String? = prefs.getString(KEY_HOME_DOUBLE_TAP_ALIAS_TARGET, null)
    fun setHomeDoubleTapAliasTarget(targetId: String?) = setCustomAction(KEY_HOME_DOUBLE_TAP_ALIAS_TARGET, targetId)

    private fun getGestureAction(key: String, default: SwipeGestureAction): SwipeGestureAction {
        val stored = prefs.getString(key, default.name) ?: return default
        if (stored == LEGACY_QUICK_NOTE_ACTION) return SwipeGestureAction.WIDGETS_PANEL
        return SwipeGestureAction.entries.firstOrNull { it.name == stored } ?: default
    }

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

    private fun getHomeGestureAction(key: String, default: HomeSwipeGestureAction): HomeSwipeGestureAction =
        prefs.getString(key, default.name)
            ?.let { value -> HomeSwipeGestureAction.entries.firstOrNull { it.name == value } }
            ?: default

    private fun setHomeGestureAction(key: String, action: HomeSwipeGestureAction) {
        prefs.edit().putString(key, action.name).apply()
    }

    private fun setCustomAction(key: String, actionJson: String?) {
        prefs.edit().apply {
            if (actionJson == null) remove(key) else putString(key, actionJson)
            apply()
        }
    }

    private companion object {
        const val LEGACY_QUICK_NOTE_ACTION = "QUICK_NOTE"
    }
}

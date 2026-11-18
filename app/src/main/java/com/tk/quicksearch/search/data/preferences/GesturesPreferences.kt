package com.tk.quicksearch.search.data.preferences

import android.content.Context
import android.content.SharedPreferences

enum class SwipeGestureAction {
    CLOSE_QUICK_SEARCH,
    WIDGETS_PANEL,
    SETTINGS,
    ALL_APPS,
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
    QUICK_SETTINGS,
    ALL_APPS,
    CUSTOM,
    SEARCH_ENGINE,
    TOOL,
    OPEN_QUICK_SEARCH,
    PINNED_GLANCE,
    WIDGETS_PANEL,
    PLAY_PAUSE_MEDIA,
    NONE,
}

class GesturesPreferences(
    context: Context,
) : BasePreferences(context) {
    fun actionFor(direction: SwipeDirection): SwipeGestureAction =
        prefs.getString(direction.actionKey, null)
            ?.let { stored -> SwipeGestureAction.entries.firstOrNull { it.name == stored } }
            ?: direction.defaultAction

    fun actionFor(gesture: HomeGesture, isDefaultLauncher: Boolean = true): HomeSwipeGestureAction =
        prefs.getString(gesture.actionKey, null)
            ?.let { stored -> HomeSwipeGestureAction.entries.firstOrNull { it.name == stored } }
            ?: gesture.defaultAction(isDefaultLauncher)

    fun setActionFor(direction: SwipeDirection, action: SwipeGestureAction) =
        setAction(direction.actionKey, action.name)

    fun setActionFor(gesture: HomeGesture, action: HomeSwipeGestureAction) =
        setAction(gesture.actionKey, action.name)

    fun customActionFor(direction: SwipeDirection): String? = prefs.getString(direction.customActionKey, null)

    fun customActionFor(gesture: HomeGesture): String? = prefs.getString(gesture.customActionKey, null)

    fun setCustomActionFor(direction: SwipeDirection, actionJson: String?) =
        setOptionalValue(direction.customActionKey, actionJson)

    fun setCustomActionFor(gesture: HomeGesture, actionJson: String?) =
        setOptionalValue(gesture.customActionKey, actionJson)

    fun aliasTargetFor(direction: SwipeDirection): String? = prefs.getString(direction.aliasTargetKey, null)

    fun aliasTargetFor(gesture: HomeGesture): String? = prefs.getString(gesture.aliasTargetKey, null)

    fun setAliasTargetFor(direction: SwipeDirection, targetId: String?) =
        setOptionalValue(direction.aliasTargetKey, targetId)

    fun setAliasTargetFor(gesture: HomeGesture, targetId: String?) =
        setOptionalValue(gesture.aliasTargetKey, targetId)

    fun isLauncherSwipeRightEnabled(): Boolean =
        PreferenceUtils.getBooleanPref(prefs, KEY_LAUNCHER_SWIPE_RIGHT_ENABLED, true)

    fun setLauncherSwipeRightEnabled(enabled: Boolean) =
        PreferenceUtils.setBooleanPref(prefs, KEY_LAUNCHER_SWIPE_RIGHT_ENABLED, enabled)

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(listener)

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(listener)

    private fun setAction(key: String, actionName: String) {
        prefs.edit().putString(key, actionName).apply()
    }

    private fun setOptionalValue(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
            apply()
        }
    }

    companion object {
        fun isNotchGestureKey(key: String?): Boolean = isNotchGesturePreference(key)

        private val routeGestureKeys =
            SwipeDirection.entries.flatMap { listOf(it.actionKey, it.customActionKey, it.aliasTargetKey) } +
                HomeGesture.entries.filterNot(HomeGesture::isNotch)
                    .flatMap { listOf(it.actionKey, it.customActionKey, it.aliasTargetKey) }

        /**
         * True for keys the search route's gestures read, and for a null key (the file was cleared).
         */
        fun isRouteGestureKey(key: String?): Boolean =
            key == null || key == BasePreferences.KEY_LAUNCHER_SWIPE_RIGHT_ENABLED || key in routeGestureKeys
    }
}

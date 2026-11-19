package com.tk.quicksearch.search.data.preferences

import com.tk.quicksearch.settings.gestureSettings.GestureSettingTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class GestureDefinitionsTest {
    @Test
    fun gestureKeysAndLauncherDefaultsRemainCompatible() {
        assertEquals("swipe_right_action", SwipeDirection.RIGHT.actionKey)
        assertEquals("home_two_finger_swipe_up_custom_action", HomeGesture.TWO_FINGER_SWIPE_UP.customActionKey)
        assertEquals("notch_gesture_long_press_alias_target", HomeGesture.NOTCH_LONG_PRESS.aliasTargetKey)
        assertEquals(HomeSwipeGestureAction.NOTIFICATION_PANEL, HomeGesture.SWIPE_DOWN.defaultAction(true))
        assertEquals(HomeSwipeGestureAction.NONE, HomeGesture.SWIPE_DOWN.defaultAction(false))
    }

    @Test
    fun notchRowsAppendOnlyOnSupportedDevices() {
        val homeIds = GestureSettingTarget.searchableRows(false).map { it.searchId }
        val allIds = GestureSettingTarget.searchableRows(true).map { it.searchId }
        assertEquals(homeIds.size + 3, allIds.size)
        assertEquals(allIds.size, allIds.toSet().size)
        assertEquals("app_settings_gesture_notch_long_press", allIds.last())
    }
}

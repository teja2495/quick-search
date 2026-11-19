package com.tk.quicksearch.search.startup

import com.tk.quicksearch.search.appSettings.AppSettingsRouteAction
import com.tk.quicksearch.search.appSettings.AppSettingsTarget
import com.tk.quicksearch.search.data.preferences.HomeGesture
import com.tk.quicksearch.settings.gestureSettings.GestureSettingTarget
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import org.junit.Assert.assertEquals
import org.junit.Test

class StartupSettingsTargetCodecTest {
    @Test
    fun restoresLegacyPagesActionsAndGestureTargets() {
        assertEquals(
            AppSettingsTarget.Page(SettingsDetailType.BOOKMARKS),
            readStartupSettingsTarget("BOOKMARKS_LIST", "app_settings_bookmarks_list"),
        )
        assertEquals(
            AppSettingsRouteAction.AI_MODEL,
            readStartupSettingsTarget("AI_MODEL", "app_settings_ai_model"),
        )
        val gesture = GestureSettingTarget.Home(HomeGesture.NOTCH_LONG_PRESS)
        val target = AppSettingsTarget.Gesture(gesture)
        assertEquals(target, readStartupSettingsTarget("GESTURE_NOTCH_LONG_PRESS", gesture.searchId))
        assertEquals(target, readStartupSettingsTarget(target.snapshotValue(), gesture.searchId))
    }
}

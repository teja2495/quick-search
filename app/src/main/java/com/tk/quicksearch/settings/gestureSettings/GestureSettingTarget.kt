package com.tk.quicksearch.settings.gestureSettings

import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.HomeGesture
import com.tk.quicksearch.search.data.preferences.SwipeDirection
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

/** A configurable gesture shown in Settings and in the searchable settings catalog. */
sealed interface GestureSettingTarget {
    val titleResId: Int
    val searchTitleResId: Int get() = titleResId
    val searchId: String
    val icon: ImageVector

    data class Swipe(val direction: SwipeDirection) : GestureSettingTarget {
        override val titleResId: Int get() = direction.titleResId
        override val searchId: String get() = "app_settings_gesture_${direction.preferencePrefix}"
        override val icon: ImageVector get() = direction.icon()
    }

    data class Home(val gesture: HomeGesture) : GestureSettingTarget {
        override val titleResId: Int get() = gesture.titleResId
        override val searchTitleResId: Int get() = gesture.searchTitleResId
        override val searchId: String get() = "app_settings_gesture_${gesture.searchKey}"
        override val icon: ImageVector get() = gesture.icon()
    }

    enum class Keyboard(
        override val titleResId: Int,
        override val searchId: String,
        val action: SwipeGestureAction,
        private val iconProvider: () -> ImageVector,
    ) : GestureSettingTarget {
        OPEN(
            R.string.action_open_keyboard,
            "app_settings_gesture_open_keyboard",
            SwipeGestureAction.OPEN_KEYBOARD,
            { MaterialSymbols.Keyboard },
        ),
        CLOSE(
            R.string.settings_gesture_close_keyboard,
            "app_settings_gesture_close_keyboard",
            SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS,
            { MaterialSymbols.KeyboardHide },
        );

        override val icon: ImageVector get() = iconProvider()
    }

    companion object {
        private val homeTargets = HomeGesture.entries.filterNot(HomeGesture::isNotch).map(::Home)
        internal val notchTargets = HomeGesture.entries.filter(HomeGesture::isNotch).map(::Home)
        internal val homeRows = listOf(Swipe(SwipeDirection.RIGHT), Swipe(SwipeDirection.LEFT)) + homeTargets

        internal fun searchableRows(notchSupported: Boolean): List<GestureSettingTarget> =
            listOf(Swipe(SwipeDirection.LEFT), Swipe(SwipeDirection.RIGHT)) +
                homeTargets + Keyboard.entries + if (notchSupported) notchTargets else emptyList()
    }
}

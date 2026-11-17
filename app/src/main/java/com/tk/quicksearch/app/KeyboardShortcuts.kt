package com.tk.quicksearch.app

import android.content.Context
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import com.tk.quicksearch.R

/**
 * Hardware-keyboard shortcuts listed in the system shortcuts helper (Meta + /). Keep this in step
 * with the keys actually handled: Escape in the activities, Ctrl + comma in [MainActivity], and
 * Enter and the arrow keys in the search field.
 */
internal fun quickSearchKeyboardShortcuts(
    context: Context,
    includeOpenSettings: Boolean,
): KeyboardShortcutGroup {
    val shortcuts =
        buildList {
            add(shortcut(context, R.string.keyboard_shortcut_open_top_result, KeyEvent.KEYCODE_ENTER))
            add(shortcut(context, R.string.keyboard_shortcut_previous_result, KeyEvent.KEYCODE_DPAD_UP))
            add(shortcut(context, R.string.keyboard_shortcut_next_result, KeyEvent.KEYCODE_DPAD_DOWN))
            if (includeOpenSettings) {
                add(
                    shortcut(
                        context,
                        R.string.action_open_settings,
                        KeyEvent.KEYCODE_COMMA,
                        KeyEvent.META_CTRL_ON,
                    ),
                )
            }
            add(shortcut(context, R.string.common_close, KeyEvent.KEYCODE_ESCAPE))
        }
    return KeyboardShortcutGroup(context.getString(R.string.app_name), shortcuts)
}

/** True for Ctrl + comma (no other modifiers), the conventional "open settings" chord. */
internal fun KeyEvent.isOpenSettingsShortcut(): Boolean =
    keyCode == KeyEvent.KEYCODE_COMMA &&
        action == KeyEvent.ACTION_DOWN &&
        repeatCount == 0 &&
        hasModifiers(KeyEvent.META_CTRL_ON)

private fun shortcut(
    context: Context,
    labelRes: Int,
    keyCode: Int,
    modifiers: Int = 0,
): KeyboardShortcutInfo = KeyboardShortcutInfo(context.getString(labelRes), keyCode, modifiers)

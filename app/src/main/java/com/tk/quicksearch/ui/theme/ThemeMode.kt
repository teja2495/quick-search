package com.tk.quicksearch.ui.theme

/**
 * Represents the theme mode preference for the app.
 */
enum class ThemeMode(val value: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromString(value: String?): ThemeMode {
            return when (value) {
                LIGHT.value -> LIGHT
                DARK.value -> DARK
                else -> SYSTEM
            }
        }
    }
}


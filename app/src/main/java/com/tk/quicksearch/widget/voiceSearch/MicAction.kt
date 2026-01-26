package com.tk.quicksearch.widget.voiceSearch

/**
 * Defines the action to perform when the mic icon is tapped.
 * Used by both the main app and widget for voice search functionality.
 */
enum class MicAction(val value: String) {
    DEFAULT_VOICE_SEARCH("default_voice_search"),
    DIGITAL_ASSISTANT("digital_assistant"),
    OFF("off")
}
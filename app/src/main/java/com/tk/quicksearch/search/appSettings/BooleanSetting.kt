package com.tk.quicksearch.search.appSettings

import androidx.annotation.StringRes
import com.tk.quicksearch.R
import com.tk.quicksearch.tools.inlineSearch.SearchToolType

/** A persisted on/off setting whose search and Settings rows share the same value. */
data class BooleanSetting(
    val preferenceKey: String,
    val defaultValue: Boolean,
    val searchId: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int? = null,
    val keywords: List<String> = emptyList(),
) {
    init {
        require(preferenceKey.isNotBlank())
        require(searchId.isNotBlank())
    }

    fun valueIn(values: Map<String, Boolean>): Boolean = values[preferenceKey] ?: defaultValue
}

enum class BooleanSettings(val setting: BooleanSetting) {
    FILE_PREVIEWS(
        BooleanSetting(
            preferenceKey = "file_previews_enabled",
            defaultValue = true,
            searchId = "app_toggle_file_previews",
            titleRes = R.string.settings_file_previews_toggle,
            descriptionRes = R.string.settings_file_previews_description,
            keywords = listOf("files", "images", "pdf", "open"),
        ),
    ),
}

internal val registeredBooleanSettings: List<BooleanSetting>
    get() = BooleanSettings.entries.map { it.setting } + SearchToolType.entries.map { it.setting }

/** Reuses the startup preference batch. Unset keys are resolved by the definition's default. */
internal fun readBooleanSettings(preferences: Map<String, *>): Map<String, Boolean> =
    buildMap {
        for (setting in registeredBooleanSettings) {
            val stored = preferences[setting.preferenceKey] as? Boolean ?: continue
            put(setting.preferenceKey, stored)
        }
    }

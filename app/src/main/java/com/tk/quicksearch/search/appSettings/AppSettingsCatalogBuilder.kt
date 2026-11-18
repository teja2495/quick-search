package com.tk.quicksearch.search.appSettings

import android.content.Context

/** Collects searchable app-setting rows, resolving their string resources. */
internal class AppSettingsCatalogBuilder(
    private val context: Context,
    private val rows: MutableList<AppSettingResult> = mutableListOf(),
) : MutableList<AppSettingResult> by rows {
    fun addNavigation(
        id: String,
        titleRes: Int,
        descriptionRes: Int? = null,
        destination: AppSettingsDestination,
        keywords: List<String> = emptyList(),
    ) {
        add(
            AppSettingResult(
                id = id,
                title = context.getString(titleRes),
                description = descriptionRes?.let(context::getString),
                keywords = keywords,
                action = AppSettingResultAction.NAVIGATE,
                destination = destination,
            ),
        )
    }

    fun addToggle(
        id: String,
        titleRes: Int,
        descriptionRes: Int? = null,
        toggleKey: AppSettingsToggleKey,
        destination: AppSettingsDestination? = null,
        keywords: List<String> = emptyList(),
    ) {
        add(
            AppSettingResult(
                id = id,
                title = context.getString(titleRes),
                description = descriptionRes?.let(context::getString),
                keywords = keywords,
                action = AppSettingResultAction.TOGGLE,
                destination = destination,
                toggleKey = toggleKey,
            ),
        )
    }

    fun addBooleanSetting(
        setting: BooleanSetting,
        destination: AppSettingsDestination? = null,
    ) {
        add(
            AppSettingResult(
                id = setting.searchId,
                title = context.getString(setting.titleRes),
                description = setting.descriptionRes?.let(context::getString),
                keywords = setting.keywords,
                action = AppSettingResultAction.TOGGLE,
                destination = destination,
                booleanSetting = setting,
            ),
        )
    }

    fun build(): List<AppSettingResult> = rows.toList()
}

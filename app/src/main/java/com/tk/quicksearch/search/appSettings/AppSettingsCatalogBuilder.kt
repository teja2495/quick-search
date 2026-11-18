package com.tk.quicksearch.search.appSettings

import android.content.Context
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType

/** Collects searchable app-setting rows, resolving their string resources. */
internal class AppSettingsCatalogBuilder(
    private val context: Context,
    private val rows: MutableList<AppSettingResult> = mutableListOf(),
) : MutableList<AppSettingResult> by rows {
    fun addNavigation(
        id: String,
        titleRes: Int,
        descriptionRes: Int? = null,
        destination: AppSettingsTarget,
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

    fun addPageShortcut(page: SettingsDetailType, extraKeywords: List<String> = emptyList()) {
        val shortcut = checkNotNull(page.searchShortcut)
        addNavigation(
            id = shortcut.id,
            titleRes = shortcut.titleRes ?: page.titleResId(),
            descriptionRes = shortcut.descriptionRes,
            destination = AppSettingsTarget.Page(page),
            keywords = shortcut.keywords + extraKeywords,
        )
    }

    // Existing pages keep their original catalog order. New definitions can append automatically.
    fun addRemainingPageShortcuts() {
        val existingPages = rows.mapNotNull { (it.destination as? AppSettingsTarget.Page)?.detailType }.toSet()
        SettingsDetailType.entries.forEach { page ->
            if (page.searchShortcut != null && page !in existingPages) addPageShortcut(page)
        }
    }

    fun addToggle(
        id: String,
        titleRes: Int,
        descriptionRes: Int? = null,
        toggleKey: AppSettingsToggleKey,
        destination: AppSettingsTarget? = null,
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
        destination: AppSettingsTarget? = null,
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

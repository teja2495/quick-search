package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.R

/**
 * Enum to represent different types of settings detail screens.
 */
enum class SettingsDetailType {
    SEARCH_ENGINES,
    EXCLUDED_ITEMS,
    SEARCH_RESULTS,
    APP_MANAGEMENT,
    APP_SHORTCUTS,
    DEVICE_SETTINGS,
    CALENDAR_EVENTS,
    NOTES,
    APPEARANCE,
    CALLS_TEXTS,
    FILES,
    LAUNCH_OPTIONS,
    MORE_OPTIONS,
    PERMISSIONS,
    TOOLS,
    GEMINI_API_CONFIG,
    FEATURES_LIST,
    OPEN_SOURCE_LICENSES,
    UNIT_CONVERTER_INFO,
    DATE_CALCULATOR_INFO,
}

internal data class SettingsDestinationSpec(
    val titleResId: Int,
    val level: Int,
    val fallbackBackDestination: SettingsDetailType? = null,
    val preferSourceBackDestination: Boolean = false,
)

internal object SettingsDestinationRegistry {
    private val specs: Map<SettingsDetailType, SettingsDestinationSpec> =
        mapOf(
            SettingsDetailType.SEARCH_ENGINES to
                SettingsDestinationSpec(
                    titleResId = R.string.settings_app_shortcuts_filter_search_engines,
                    level = 1,
                ),
            SettingsDetailType.EXCLUDED_ITEMS to
                SettingsDestinationSpec(
                    titleResId = R.string.settings_excluded_items_title,
                    level = 2,
                    fallbackBackDestination = SettingsDetailType.SEARCH_RESULTS,
                ),
            SettingsDetailType.SEARCH_RESULTS to
                SettingsDestinationSpec(
                    titleResId = R.string.settings_search_results_title,
                    level = 1,
                ),
            SettingsDetailType.APP_MANAGEMENT to
                SettingsDestinationSpec(
                    titleResId = R.string.settings_manage_apps_title,
                    level = 2,
                    fallbackBackDestination = SettingsDetailType.SEARCH_RESULTS,
                ),
            SettingsDetailType.APP_SHORTCUTS to
                SettingsDestinationSpec(
                    titleResId = R.string.section_app_shortcuts,
                    level = 2,
                    fallbackBackDestination = SettingsDetailType.SEARCH_RESULTS,
                ),
            SettingsDetailType.DEVICE_SETTINGS to
                SettingsDestinationSpec(
                    titleResId = R.string.section_settings,
                    level = 2,
                    fallbackBackDestination = SettingsDetailType.SEARCH_RESULTS,
                ),
            SettingsDetailType.CALENDAR_EVENTS to
                SettingsDestinationSpec(
                    titleResId = R.string.section_calendar,
                    level = 2,
                    fallbackBackDestination = SettingsDetailType.SEARCH_RESULTS,
                ),
            SettingsDetailType.NOTES to
                SettingsDestinationSpec(
                    titleResId = R.string.section_notes,
                    level = 2,
                    fallbackBackDestination = SettingsDetailType.SEARCH_RESULTS,
                ),
            SettingsDetailType.APPEARANCE to
                SettingsDestinationSpec(
                    titleResId = R.string.settings_appearance_title,
                    level = 1,
                ),
            SettingsDetailType.CALLS_TEXTS to
                SettingsDestinationSpec(
                    titleResId = R.string.settings_calls_texts_title,
                    level = 2,
                    fallbackBackDestination = SettingsDetailType.SEARCH_RESULTS,
                ),
            SettingsDetailType.FILES to
                SettingsDestinationSpec(
                    titleResId = R.string.settings_file_types_title,
                    level = 2,
                    fallbackBackDestination = SettingsDetailType.SEARCH_RESULTS,
                ),
            SettingsDetailType.LAUNCH_OPTIONS to
                SettingsDestinationSpec(
                    titleResId = R.string.settings_launch_options_title,
                    level = 1,
                ),
            SettingsDetailType.MORE_OPTIONS to
                SettingsDestinationSpec(
                    titleResId = R.string.settings_more_options_title,
                    level = 1,
                ),
            SettingsDetailType.PERMISSIONS to
                SettingsDestinationSpec(
                    titleResId = R.string.settings_permissions_title,
                    level = 1,
                ),
            SettingsDetailType.TOOLS to
                SettingsDestinationSpec(
                    titleResId = R.string.settings_tools_title,
                    level = 2,
                ),
            SettingsDetailType.GEMINI_API_CONFIG to
                SettingsDestinationSpec(
                    titleResId = R.string.settings_backup_export_option_gemini_title,
                    level = 3,
                    preferSourceBackDestination = true,
                ),
            SettingsDetailType.FEATURES_LIST to
                SettingsDestinationSpec(
                    titleResId = R.string.settings_all_quick_search_features,
                    level = 1,
                ),
            SettingsDetailType.OPEN_SOURCE_LICENSES to
                SettingsDestinationSpec(
                    titleResId = R.string.settings_open_source_licenses_title,
                    level = 1,
                ),
            SettingsDetailType.UNIT_CONVERTER_INFO to
                SettingsDestinationSpec(
                    titleResId = R.string.unit_converter_info_title,
                    level = 3,
                    fallbackBackDestination = SettingsDetailType.TOOLS,
                ),
            SettingsDetailType.DATE_CALCULATOR_INFO to
                SettingsDestinationSpec(
                    titleResId = R.string.date_calculator_info_title,
                    level = 3,
                    fallbackBackDestination = SettingsDetailType.TOOLS,
                ),
        )

    fun titleResId(detailType: SettingsDetailType): Int = specFor(detailType).titleResId

    fun level(detailType: SettingsDetailType): Int = specFor(detailType).level

    fun isLevel2OrDeeper(detailType: SettingsDetailType): Boolean = level(detailType) >= 2

    fun resolveBackDestination(
        detailType: SettingsDetailType,
        sourceDetailType: SettingsDetailType?,
    ): SettingsDetailType? {
        val spec = specFor(detailType)
        if (spec.level < 2) return null
        if (spec.preferSourceBackDestination && sourceDetailType != null) {
            return sourceDetailType
        }
        return spec.fallbackBackDestination
    }

    fun coveredDestinationCount(): Int = specs.size

    private fun specFor(detailType: SettingsDetailType): SettingsDestinationSpec =
        specs[detailType] ?: error("Missing SettingsDestinationSpec for $detailType")
}

internal fun SettingsDetailType.titleResId(): Int = SettingsDestinationRegistry.titleResId(this)

internal fun SettingsDetailType.isLevel2(): Boolean =
    SettingsDestinationRegistry.isLevel2OrDeeper(this)

internal fun SettingsDetailType.level(): Int = SettingsDestinationRegistry.level(this)

internal fun SettingsDetailType.resolveBackDestination(
    sourceDetailType: SettingsDetailType?,
): SettingsDetailType? = SettingsDestinationRegistry.resolveBackDestination(this, sourceDetailType)

package com.tk.quicksearch.shared.featureFlags

import android.content.Context
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.data.preferences.BasePreferences
import com.tk.quicksearch.searchEngines.AliasHandler
import java.util.EnumMap

enum class FeatureFlag {
    CALENDAR_SEARCH,
    UNIT_CONVERTER,
    APP_SETTINGS_SEARCH,
    APP_THEME_SELECTION,
}

private data class FeatureFlagDefinition(
    val enabledByDefault: Boolean,
    val exportExcludedKeys: Set<String> = emptySet(),
    val exportExcludedPrefixes: Set<String> = emptySet(),
)

/**
 * Centralized feature-flag registry.
 *
 * To add a future flag:
 * 1) Add enum entry in [FeatureFlag]
 * 2) Add definition in [definitions]
 * 3) Wire the flag where behavior/UI should be gated
 */
object FeatureFlags {
    const val PREFERENCE_KEY_PREFIX = "feature_flag_"

    private val definitions: Map<FeatureFlag, FeatureFlagDefinition> =
        mapOf(
            FeatureFlag.CALENDAR_SEARCH to
                FeatureFlagDefinition(
                    enabledByDefault = false,
                    exportExcludedKeys =
                        setOf(
                            BasePreferences.KEY_PINNED_CALENDAR_EVENT_IDS,
                            BasePreferences.KEY_EXCLUDED_CALENDAR_EVENT_IDS,
                        ),
                    exportExcludedPrefixes =
                        setOf(
                            BasePreferences.KEY_NICKNAME_CALENDAR_EVENT_PREFIX,
                            BasePreferences.KEY_ALIAS_CODE_PREFIX +
                                AliasHandler.SEARCH_SECTION_CALENDAR_ALIAS_ID,
                            BasePreferences.KEY_ALIAS_ENABLED_PREFIX +
                                AliasHandler.SEARCH_SECTION_CALENDAR_ALIAS_ID,
                        ),
                ),
            FeatureFlag.UNIT_CONVERTER to
                FeatureFlagDefinition(
                    enabledByDefault = false,
                    exportExcludedKeys =
                        setOf(
                            BasePreferences.KEY_UNIT_CONVERTER_ENABLED,
                        ),
                    exportExcludedPrefixes =
                        setOf(
                            BasePreferences.KEY_ALIAS_CODE_PREFIX +
                                AliasHandler.UNIT_CONVERTER_ALIAS_FEATURE_ID,
                            BasePreferences.KEY_ALIAS_ENABLED_PREFIX +
                                AliasHandler.UNIT_CONVERTER_ALIAS_FEATURE_ID,
                        ),
                ),
            FeatureFlag.APP_SETTINGS_SEARCH to
                FeatureFlagDefinition(
                    enabledByDefault = false,
                ),
            FeatureFlag.APP_THEME_SELECTION to
                FeatureFlagDefinition(
                    enabledByDefault = false,
                ),
        )

    @Volatile
    private var initialized = false
    private val runtimeValues = EnumMap<FeatureFlag, Boolean>(FeatureFlag::class.java)

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        val prefs = context.getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE)
        FeatureFlag.entries.forEach { flag ->
            val defaultValue = definitions[flag]?.enabledByDefault == true
            runtimeValues[flag] = prefs.getBoolean(prefKey(flag), defaultValue)
        }
        initialized = true
    }

    @Synchronized
    fun setAll(context: Context, enabled: Boolean) {
        initialize(context)
        val prefs = context.getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        FeatureFlag.entries.forEach { flag ->
            runtimeValues[flag] = enabled
            editor.putBoolean(prefKey(flag), enabled)
        }
        editor.apply()
    }

    fun isEnabled(flag: FeatureFlag): Boolean {
        val runtimeValue = runtimeValues[flag]
        if (runtimeValue != null) return runtimeValue
        return definitions[flag]?.enabledByDefault == true
    }

    fun isCalendarSearchEnabled(): Boolean = isEnabled(FeatureFlag.CALENDAR_SEARCH)

    fun isUnitConverterEnabled(): Boolean = isEnabled(FeatureFlag.UNIT_CONVERTER)

    fun isAppSettingsSearchEnabled(): Boolean = isEnabled(FeatureFlag.APP_SETTINGS_SEARCH)

    fun isAppThemeSelectionEnabled(): Boolean = isEnabled(FeatureFlag.APP_THEME_SELECTION)

    fun isSearchSectionEnabled(section: SearchSection): Boolean =
        when (section) {
            SearchSection.CALENDAR -> isCalendarSearchEnabled()
            else -> true
        }

    fun isAnyEnabled(): Boolean = FeatureFlag.entries.any { flag -> isEnabled(flag) }

    fun shouldExcludePreferenceFromExport(key: String): Boolean {
        val disabledDefinitions =
            definitions
                .filterKeys { flag -> !isEnabled(flag) }
                .values

        return disabledDefinitions.any { definition ->
            key in definition.exportExcludedKeys ||
                definition.exportExcludedPrefixes.any { prefix -> key.startsWith(prefix) }
        }
    }

    private fun prefKey(flag: FeatureFlag): String = PREFERENCE_KEY_PREFIX + flag.name.lowercase()
}

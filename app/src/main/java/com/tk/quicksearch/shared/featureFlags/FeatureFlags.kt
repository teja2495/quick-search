package com.tk.quicksearch.shared.featureFlags

import android.content.Context
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.data.preferences.BasePreferences
import java.util.EnumMap

// This enum is intentionally empty — the feature flag infrastructure below should NOT be removed.
// Add entries here when new features need to be gated behind a flag.
enum class FeatureFlag

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

    private val definitions: Map<FeatureFlag, FeatureFlagDefinition> = emptyMap()

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

    fun isSearchSectionEnabled(@Suppress("UNUSED_PARAMETER") section: SearchSection): Boolean {
        return when (section) {
            else -> true
        }
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

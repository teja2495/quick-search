package com.tk.quicksearch.search.appSettings

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSectionRegistry
import com.tk.quicksearch.search.utils.SearchQueryContext

private val WHITESPACE_REGEX = "\\s+".toRegex()

internal class AppSettingDescriptionResolver(private val context: Context) {
    fun resolveSearchDescription(
        setting: AppSettingResult,
        queryContext: SearchQueryContext,
    ): String? {
        if (!setting.isNavigateAction || queryContext.tokens.isEmpty()) {
            return setting.description
        }
        if (setting.destination == AppSettingsDestination.APPEARANCE) {
            val appearanceDescription = getAppearanceSearchDescription(queryContext.tokens.toSet())
            if (appearanceDescription != null) {
                return appearanceDescription
            }
        }

        val matchedKeyword = findBestMatchingKeyword(setting.keywords, queryContext.tokens)
        return matchedKeyword?.let {
            context.getString(R.string.settings_search_dynamic_description_template, it)
        } ?: setting.description
    }

    private fun getAppearanceSearchDescription(queryTokens: Set<String>): String? {
        if (queryTokens.any { tokenMatchesAny(it, APPEARANCE_THEME_TOKENS) }) {
            return context.getString(R.string.settings_search_description_change_app_theme)
        }
        if (queryTokens.any { tokenMatchesAny(it, APPEARANCE_WALLPAPER_TOKENS) }) {
            return context.getString(R.string.settings_search_description_change_wallpaper)
        }
        if (queryTokens.any { tokenMatchesAny(it, APPEARANCE_FONT_TOKENS) }) {
            return context.getString(R.string.settings_search_description_change_font_size)
        }
        if (queryTokens.any { tokenMatchesAny(it, APPEARANCE_LAYOUT_TOKENS) }) {
            return context.getString(R.string.settings_search_description_change_layout)
        }
        return null
    }

    private fun findBestMatchingKeyword(
        keywords: List<String>,
        queryTokens: List<String>,
    ): String? {
        if (keywords.isEmpty() || queryTokens.isEmpty()) return null

        return keywords
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { keyword ->
                val normalizedKeywordTokens =
                    WHITESPACE_REGEX.split(keyword.lowercase()).filter { it.isNotBlank() }
                val score =
                    queryTokens.count { queryToken ->
                        normalizedKeywordTokens.any { keywordToken ->
                            tokenMatches(queryToken, keywordToken)
                        }
                    }
                keyword to score
            }.filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun tokenMatchesAny(
        queryToken: String,
        candidates: Set<String>,
    ): Boolean = candidates.any { candidate -> tokenMatches(queryToken, candidate) }

    private fun tokenMatches(
        queryToken: String,
        candidateToken: String,
    ): Boolean {
        val query = queryToken.trim().lowercase()
        val candidate = candidateToken.trim().lowercase()
        if (query.isEmpty() || candidate.isEmpty()) return false
        return query == candidate || query.startsWith(candidate) || candidate.startsWith(query)
    }

    private companion object {
        val APPEARANCE_THEME_TOKENS = setOf("themes", "dark", "light", "system", "background")
        val APPEARANCE_WALLPAPER_TOKENS =
            setOf("wallpaper", "blur", "transparency")
        val APPEARANCE_FONT_TOKENS = setOf("fonts", "size", "text")
        val APPEARANCE_LAYOUT_TOKENS =
            setOf("layout", "one handed", "bottom", "searchbar")
    }
}

internal fun validateAppSettingsCatalog(settings: List<AppSettingResult>) {
    check(settings.all { it.id.isNotBlank() }) { "Searchable app-setting IDs must not be blank" }

    val duplicateIds =
        settings
            .groupingBy { it.id }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
    check(duplicateIds.isEmpty()) {
        "Duplicate searchable app-setting IDs: ${duplicateIds.sorted().joinToString()}"
    }

    SearchSectionRegistry.orderedDefinitions.forEach { definition ->
        val registrationCount =
            settings.count { setting -> setting.toggleKey == definition.appSettingsToggleKey }
        check(registrationCount == 1) {
            "Expected exactly one searchable toggle for ${definition.section}, found $registrationCount"
        }
    }
}

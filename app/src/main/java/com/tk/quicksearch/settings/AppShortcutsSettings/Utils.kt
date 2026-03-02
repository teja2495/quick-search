package com.tk.quicksearch.settings.AppShortcutsSettings

import android.content.Context
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import java.util.Locale

fun shortcutMatchPriority(name: String, query: String, locale: Locale): ShortcutSearchMatchPriority? {
    val normalizedName = name.lowercase(locale)
    return when {
        normalizedName == query -> ShortcutSearchMatchPriority.EXACT
        normalizedName.startsWith(query) -> ShortcutSearchMatchPriority.STARTS_WITH
        normalizedName
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .any { it.isNotBlank() && it.startsWith(query) } ->
            ShortcutSearchMatchPriority.WORD_STARTS_WITH
        normalizedName.contains(query) -> ShortcutSearchMatchPriority.CONTAINS
        else -> null
    }
}

fun bestShortcutMatchPriority(
    group: AppShortcutGroup,
    query: String,
    locale: Locale,
): ShortcutSearchMatchPriority? {
    val allCandidates =
        buildList {
            add(group.appLabel)
            addAll(group.shortcuts.map(::shortcutDisplayName))
            addAll(group.sources.map { it.label })
            addAll(group.searchTargetSources.map { it.label })
        }
    return allCandidates.mapNotNull { shortcutMatchPriority(it, query, locale) }.minOrNull()
}

fun resolveAppLabel(
    context: Context,
    packageName: String,
    locale: Locale,
): String {
    return runCatching {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager
            .getApplicationLabel(appInfo)
            .toString()
            .takeIf { it.isNotBlank() }
            ?: packageName.substringAfterLast(".").replaceFirstChar { it.titlecase(locale) }
    }.getOrElse {
        packageName.substringAfterLast(".").replaceFirstChar { it.titlecase(locale) }
    }
}

fun fallbackAppLabel(
    packageName: String,
    locale: Locale,
): String = packageName.substringAfterLast(".").replaceFirstChar { it.titlecase(locale) }
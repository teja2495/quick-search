package com.tk.quicksearch.searchEngines

import com.tk.quicksearch.search.core.SearchTarget
import java.util.Locale

private const val SEARCH_TARGET_SHORTCUT_PACKAGE_PREFIX = "com.tk.quicksearch.searchtarget"

private fun getSearchTargetShortcutPackageName(target: SearchTarget): String =
    getSearchTargetShortcutPackageName(target.getId())

private fun getSearchTargetShortcutPackageName(targetId: String): String {
    val sanitizedId =
        targetId
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifBlank { "target" }
    return "$SEARCH_TARGET_SHORTCUT_PACKAGE_PREFIX.$sanitizedId"
}

fun isSearchTargetShortcutPackageName(packageName: String): Boolean =
    packageName.startsWith("$SEARCH_TARGET_SHORTCUT_PACKAGE_PREFIX.")

fun resolveSearchTargetShortcutPackageName(
    target: SearchTarget,
    existingPackages: Set<String> = emptySet(),
): String =
    when (target) {
        is SearchTarget.Browser -> target.app.packageName
        is SearchTarget.Custom -> getSearchTargetShortcutPackageName(target)
        is SearchTarget.Engine -> {
            val candidates = target.engine.getAppPackageCandidates()
            candidates.firstOrNull { it in existingPackages }
                ?: getSearchTargetShortcutPackageName(target)
        }
    }

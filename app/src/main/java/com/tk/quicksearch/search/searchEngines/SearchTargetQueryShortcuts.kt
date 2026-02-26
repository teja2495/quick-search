package com.tk.quicksearch.search.searchEngines

import com.tk.quicksearch.search.core.SearchTarget
import java.util.Locale

private const val SEARCH_TARGET_SHORTCUT_PACKAGE_PREFIX = "com.tk.quicksearch.searchtarget"

fun getSearchTargetShortcutPackageName(target: SearchTarget): String =
    getSearchTargetShortcutPackageName(target.getId())

fun getSearchTargetShortcutPackageName(targetId: String): String {
    val sanitizedId =
        targetId
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifBlank { "target" }
    return "$SEARCH_TARGET_SHORTCUT_PACKAGE_PREFIX.$sanitizedId"
}

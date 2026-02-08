package com.tk.quicksearch.search.searchEngines

import androidx.compose.runtime.Composable
import com.tk.quicksearch.search.core.SearchTarget

const val BROWSER_ID_PREFIX = "browser:"

fun SearchTarget.getId(): String =
    when (this) {
        is SearchTarget.Engine -> engine.name
        is SearchTarget.Browser -> "$BROWSER_ID_PREFIX${app.packageName}"
        is SearchTarget.Custom -> "$CUSTOM_ID_PREFIX${custom.id}"
    }

@Composable
fun SearchTarget.getDisplayName(): String =
    when (this) {
        is SearchTarget.Engine -> engine.getDisplayName()
        is SearchTarget.Browser -> app.label
        is SearchTarget.Custom -> custom.name
    }

fun SearchTarget.getContentDescription(): String =
    when (this) {
        is SearchTarget.Engine -> engine.getContentDescription()
        is SearchTarget.Browser -> app.label
        is SearchTarget.Custom -> custom.name
    }

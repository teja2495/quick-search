package com.tk.quicksearch.searchEngines

import androidx.compose.runtime.Composable
import com.tk.quicksearch.search.core.SearchTarget

const val BROWSER_ID_PREFIX = "browser:"

const val IN_APP_BROWSER_PACKAGE = "com.tk.quicksearch.inappbrowser"

fun isInAppBrowserPackage(packageName: String): Boolean = packageName == IN_APP_BROWSER_PACKAGE

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


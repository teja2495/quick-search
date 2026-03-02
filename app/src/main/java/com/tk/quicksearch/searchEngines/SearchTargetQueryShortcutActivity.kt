package com.tk.quicksearch.searchEngines

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.tk.quicksearch.app.MainActivity
import com.tk.quicksearch.search.core.IntentHelpers
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.overlay.OverlayModeController

class SearchTargetQueryShortcutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShortcutIntent(intent)
        finish()
    }

    private fun handleShortcutIntent(intent: Intent?) {
        val action = intent?.action
        if (action != ACTION_LAUNCH_SEARCH_TARGET_QUERY_SHORTCUT) {
            return
        }

        val query = intent.getStringExtra(EXTRA_QUERY)?.trim().orEmpty()
        val targetType = intent.getStringExtra(EXTRA_TARGET_TYPE).orEmpty()
        if (query.isBlank()) {
            return
        }

        val app = application as Application
        val userPreferences = UserAppPreferences(this)

        when (targetType) {
            TARGET_TYPE_ENGINE -> {
                val engineName = intent.getStringExtra(EXTRA_ENGINE_NAME)
                val engine = engineName?.let { runCatching { SearchEngine.valueOf(it) }.getOrNull() } ?: return

                if (engine == SearchEngine.DIRECT_SEARCH) {
                    startActivity(
                        Intent(this, MainActivity::class.java).apply {
                            setAction(MainActivity.ACTION_SEARCH_TARGET_SHORTCUT)
                            putExtra(MainActivity.EXTRA_SHORTCUT_QUERY, query)
                            putExtra(MainActivity.EXTRA_SHORTCUT_TARGET_ENGINE, engine.name)
                            putExtra(OverlayModeController.EXTRA_FORCE_NORMAL_LAUNCH, true)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        },
                    )
                    return
                }

                userPreferences.addRecentItem(RecentSearchEntry.Query(query))
                val amazonDomain =
                    if (engine == SearchEngine.AMAZON) {
                        userPreferences.getAmazonDomain()
                    } else {
                        null
                    }
                IntentHelpers.openSearchUrl(app, query, engine, amazonDomain, ::showToastMessage)
            }

            TARGET_TYPE_BROWSER -> {
                val browserPackage = intent.getStringExtra(EXTRA_BROWSER_PACKAGE).orEmpty()
                if (browserPackage.isBlank()) return
                userPreferences.addRecentItem(RecentSearchEntry.Query(query))
                if (isLikelyWebUrl(query)) {
                    IntentHelpers.openBrowserUrl(app, query, browserPackage, ::showToastMessage)
                } else {
                    IntentHelpers.openBrowserSearch(app, query, browserPackage, ::showToastMessage)
                }
            }

            TARGET_TYPE_CUSTOM -> {
                val urlTemplate = intent.getStringExtra(EXTRA_CUSTOM_URL_TEMPLATE).orEmpty()
                if (urlTemplate.isBlank()) return
                userPreferences.addRecentItem(RecentSearchEntry.Query(query))
                IntentHelpers.openCustomSearchUrl(app, query, urlTemplate, ::showToastMessage)
            }
        }
    }

    private fun showToastMessage(
        stringResId: Int,
        formatArg: String?,
    ) {
        val message =
            if (formatArg.isNullOrBlank()) {
                getString(stringResId)
            } else {
                getString(stringResId, formatArg)
            }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ACTION_LAUNCH_SEARCH_TARGET_QUERY_SHORTCUT =
            "com.tk.quicksearch.action.LAUNCH_SEARCH_TARGET_QUERY_SHORTCUT"

        const val EXTRA_QUERY = "com.tk.quicksearch.extra.SHORTCUT_QUERY"
        const val EXTRA_TARGET_TYPE = "com.tk.quicksearch.extra.SHORTCUT_TARGET_TYPE"
        const val EXTRA_ENGINE_NAME = "com.tk.quicksearch.extra.SHORTCUT_ENGINE_NAME"
        const val EXTRA_BROWSER_PACKAGE = "com.tk.quicksearch.extra.SHORTCUT_BROWSER_PACKAGE"
        const val EXTRA_CUSTOM_URL_TEMPLATE = "com.tk.quicksearch.extra.SHORTCUT_CUSTOM_URL_TEMPLATE"

        const val TARGET_TYPE_ENGINE = "engine"
        const val TARGET_TYPE_BROWSER = "browser"
        const val TARGET_TYPE_CUSTOM = "custom"

        fun createIntent(
            context: Context,
            targetType: String,
            query: String,
            engineName: String? = null,
            browserPackage: String? = null,
            customUrlTemplate: String? = null,
        ): Intent =
            Intent(context, SearchTargetQueryShortcutActivity::class.java).apply {
                action = ACTION_LAUNCH_SEARCH_TARGET_QUERY_SHORTCUT
                putExtra(EXTRA_TARGET_TYPE, targetType)
                putExtra(EXTRA_QUERY, query)
                putExtra(EXTRA_ENGINE_NAME, engineName)
                putExtra(EXTRA_BROWSER_PACKAGE, browserPackage)
                putExtra(EXTRA_CUSTOM_URL_TEMPLATE, customUrlTemplate)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }
}

package com.tk.quicksearch.search.noted

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.tk.quicksearch.BuildConfig

/**
 * Gate for the Noted integration. Noted is found by its search provider's authority, and the
 * integration turns on only when the app serving it is signed with the same key as Quick Search.
 * Everyone else never sees Noted results, a setting, or a prompt.
 *
 * It also needs [READ_PERMISSION], a signature permission that Android grants only when Noted was
 * already installed when Quick Search was installed or updated. Until then everything stays hidden.
 *
 * Standard builds only: F-Droid builds set `BuildConfig.NOTED_INTEGRATION` to false and leave out the
 * permission and receiver, which live in `src/standard/AndroidManifest.xml`.
 *
 * Mirrors `NotedSearchContract` in the Noted app; keep the two in sync.
 */
object NotedIntegration {
    const val READ_PERMISSION = "com.tk.noted.permission.READ_CONTENT"
    const val AUTHORITY = "com.tk.noted.search"
    const val SEARCH_PATH = "search"
    const val TASKS_PATH = "tasks"

    /** `items?keys=<kind>:<id>,...` returns those items in order, leaving out gone or completed ones. */
    const val ITEMS_PATH = "items"
    const val KEYS_PARAM = "keys"
    const val QUERY_PARAM = "q"
    const val LIMIT_PARAM = "limit"
    const val RESULT_LIMIT = 20

    /** Noted's own cap; fetched in full so title ranking happens before [RESULT_LIMIT] trims. */
    const val FETCH_LIMIT = 50
    const val DEEP_LINK_SCHEME = "noted"

    const val COLUMN_KIND = "kind"
    const val COLUMN_ID = "id"
    const val COLUMN_TITLE = "title"
    const val COLUMN_SUBTITLE = "subtitle"
    const val COLUMN_LIST_TITLE = "list_title"
    const val COLUMN_DUE_AT = "due_at"
    const val COLUMN_DUE_TIME_SET = "due_time_set"
    const val COLUMN_UPDATED_AT = "updated_at"
    const val COLUMN_DUE_LOCAL_DATE = "due_local_date"
    const val COLUMN_DAYS_TRACK = "days_track"
    const val COLUMN_COMPLETED = "completed"

    @Volatile
    var isAvailable: Boolean = false
        private set

    /** The package serving Noted's provider, once it passed the signature check. */
    @Volatile
    var packageName: String? = null
        private set

    @Volatile
    private var checked = false

    /** Re-checks for a same-key Noted. Returns true when availability changed. */
    fun refresh(context: Context): Boolean {
        val notedPackage =
            if (BuildConfig.NOTED_INTEGRATION) sameKeyProviderPackage(context)?.takeIf { hasReadPermission(context) } else null
        val available = notedPackage != null
        val changed = checked && available != isAvailable
        packageName = notedPackage
        isAvailable = available
        checked = true
        return changed
    }

    /** Checks once per process; later calls are free. */
    fun ensureChecked(context: Context): Boolean {
        if (!checked) refresh(context)
        return isAvailable
    }

    private fun hasReadPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, READ_PERMISSION) == PackageManager.PERMISSION_GRANTED

    private fun sameKeyProviderPackage(context: Context): String? {
        val packageManager = context.packageManager
        val provider = packageManager.resolveContentProvider(AUTHORITY, 0) ?: return null
        return provider.packageName.takeIf {
            packageManager.checkSignatures(context.packageName, it) == PackageManager.SIGNATURE_MATCH
        }
    }
}

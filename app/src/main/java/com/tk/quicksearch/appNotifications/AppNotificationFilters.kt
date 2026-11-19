package com.tk.quicksearch.appNotifications

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

/** An app whose notifications show at a glance: all of them, or only those mentioning [keywords]. */
@Parcelize
data class AppNotificationApp(
    val packageName: String,
    val keywords: List<String> = emptyList(),
) : Parcelable

/**
 * What App notifications shows at a glance: notifications from [apps] (each narrowed by its own
 * keywords when it has any), plus notifications from any app that mention one of [keywords].
 * Keywords match whole words in the title and text, ignoring case.
 */
@Parcelize
data class AppNotificationFilters(
    val apps: List<AppNotificationApp> = emptyList(),
    val keywords: List<String> = emptyList(),
) : Parcelable {
    val isEmpty: Boolean get() = apps.isEmpty() && keywords.isEmpty()

    /** Whether anything from [packageName] could match, checked before reading its text. */
    fun couldMatch(packageName: String): Boolean = keywords.isNotEmpty() || apps.any { it.packageName == packageName }

    fun matches(
        packageName: String,
        text: String,
    ): Boolean {
        val app = apps.firstOrNull { it.packageName == packageName }
        if (app != null && (app.keywords.isEmpty() || app.keywords.any { containsWholeWords(text, it) })) return true
        return keywords.any { containsWholeWords(text, it) }
    }

    companion object {
        /**
         * Whether [keyword] appears in [text] ignoring case, not as part of a longer word: "delivered"
         * matches "was delivered." but not "undelivered". Scripts written without spaces between
         * words, such as Chinese, Japanese and Thai, have no word edges to check, so they match
         * anywhere and also end a neighboring word ("Amazonで" contains "amazon").
         */
        fun containsWholeWords(
            text: String,
            keyword: String,
        ): Boolean {
            val needle = keyword.trim().lowercase()
            if (needle.isEmpty()) return false
            val haystack = text.lowercase()
            var start = haystack.indexOf(needle)
            while (start >= 0) {
                val end = start + needle.length
                val boundedBefore = start == 0 || !haystack[start - 1].isWordChar() || !needle.first().isWordChar()
                val boundedAfter = end == haystack.length || !haystack[end].isWordChar() || !needle.last().isWordChar()
                if (boundedBefore && boundedAfter) return true
                start = haystack.indexOf(needle, start + 1)
            }
            return false
        }

        private val UNSPACED_SCRIPTS =
            setOf(
                Character.UnicodeScript.HAN,
                Character.UnicodeScript.HIRAGANA,
                Character.UnicodeScript.KATAKANA,
                Character.UnicodeScript.THAI,
                Character.UnicodeScript.LAO,
                Character.UnicodeScript.KHMER,
                Character.UnicodeScript.MYANMAR,
                Character.UnicodeScript.TIBETAN,
            )

        /** A letter or digit that is part of a space-separated word. */
        private fun Char.isWordChar(): Boolean =
            isLetterOrDigit() && Character.UnicodeScript.of(code) !in UNSPACED_SCRIPTS
    }
}

/** Stores [AppNotificationFilters] as JSON in preferences. */
internal object AppNotificationFiltersCodec {
    fun encode(filters: AppNotificationFilters): String =
        JSONObject().apply {
            put(
                "apps",
                JSONArray().apply {
                    filters.apps.forEach { app ->
                        put(JSONObject().put("package", app.packageName).put("keywords", JSONArray(app.keywords)))
                    }
                },
            )
            put("keywords", JSONArray(filters.keywords))
        }.toString()

    fun decode(json: String?): AppNotificationFilters {
        if (json.isNullOrBlank()) return AppNotificationFilters()
        val root = runCatching { JSONObject(json) }.getOrElse { return AppNotificationFilters() }
        val appsArray = root.optJSONArray("apps")
        val apps = (0 until (appsArray?.length() ?: 0)).mapNotNull { index ->
            val item = appsArray?.optJSONObject(index) ?: return@mapNotNull null
            val packageName = item.optString("package").takeIf(String::isNotBlank) ?: return@mapNotNull null
            AppNotificationApp(packageName, item.optJSONArray("keywords").keywords())
        }.distinctBy { it.packageName }
        return AppNotificationFilters(apps = apps, keywords = root.optJSONArray("keywords").keywords())
    }

    /** Trimmed, non-blank and without repeats in any case, so each keyword is a unique list key. */
    private fun JSONArray?.keywords(): List<String> =
        if (this == null) {
            emptyList()
        } else {
            (0 until length())
                .mapNotNull { optString(it).trim().takeIf(String::isNotEmpty) }
                .distinctBy { it.lowercase() }
        }
}

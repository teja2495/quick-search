package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.models.AppInfo
import java.util.Locale

object AppSearchInitials {
    fun initialsFor(app: AppInfo): List<String> {
        val initials = computeInitials(app.appName) ?: return emptyList()
        return listOf(initials)
    }

    private fun computeInitials(appName: String): String? {
        if (appName.isBlank()) return null
        val source = appName.trim()
        val builder = StringBuilder()

        for (index in source.indices) {
            val current = source[index]
            if (!current.isLetterOrDigit()) continue

            val previous = source.getOrNull(index - 1)
            val startsWord = previous == null || !previous.isLetterOrDigit()
            val startsCamelWord =
                previous != null &&
                    previous.isLetter() &&
                    previous.isLowerCase() &&
                    current.isLetter() &&
                    current.isUpperCase()

            if (startsWord || startsCamelWord) {
                builder.append(current.lowercaseChar())
            }
        }

        val initials = builder.toString().lowercase(Locale.getDefault())
        return initials.takeIf { it.length >= 2 }
    }
}

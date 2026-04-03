package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.AppInfo
import java.util.Calendar
import java.util.Locale

internal class AppSuggestionSelector(
    private val repository: AppsRepository,
    private val userPreferences: UserAppPreferences,
) {
    fun selectSuggestedApps(
        apps: List<AppInfo>,
        limit: Int,
        hasUsagePermission: Boolean,
    ): List<AppInfo> {
        if (apps.isEmpty() || limit <= 0) return emptyList()

        val (recentInstallStart, recentInstallEnd) = getRecentInstallWindow()
        val recentInstalls =
            repository.extractRecentlyInstalledApps(apps, recentInstallStart, recentInstallEnd)

        val suggestions =
            if (hasUsagePermission) {
                val recentlyOpened = repository.getRecentlyOpenedApps(apps)
                val topRecent = recentlyOpened.firstOrNull()
                val recentInstallsExcludingTop =
                    recentInstalls.filterNot { it.launchCountKey() == topRecent?.launchCountKey() }
                val excludedPackages =
                    recentInstallsExcludingTop
                        .asSequence()
                        .map { it.launchCountKey() }
                        .toSet()
                        .let { packages ->
                            topRecent?.launchCountKey()?.let { packages + it } ?: packages
                        }
                val remainingRecents =
                    recentlyOpened.filterNot { excludedPackages.contains(it.launchCountKey()) }

                buildList {
                    topRecent?.let { add(it) }
                    addAll(recentInstallsExcludingTop)
                    addAll(remainingRecents)
                }
            } else {
                val appByKey = apps.associateBy { it.launchCountKey() }
                val recentlyOpened =
                    userPreferences
                        .getRecentAppLaunches()
                        .asSequence()
                        .mapNotNull { appByKey[it] }
                        .toList()
                val topRecent = recentlyOpened.firstOrNull()
                val recentInstallsExcludingTop =
                    recentInstalls.filterNot { it.launchCountKey() == topRecent?.launchCountKey() }
                val excludedPackages =
                    recentInstallsExcludingTop
                        .asSequence()
                        .map { it.launchCountKey() }
                        .toSet()
                        .let { packages ->
                            topRecent?.launchCountKey()?.let { packages + it } ?: packages
                        }
                val remainingRecents =
                    recentlyOpened.drop(1).filterNot { excludedPackages.contains(it.launchCountKey()) }

                buildList {
                    topRecent?.let { add(it) }
                    addAll(recentInstallsExcludingTop)
                    addAll(remainingRecents)
                }
            }

        if (suggestions.size >= limit) {
            return suggestions.take(limit)
        }

        val remainingSpots = limit - suggestions.size
        val suggestionPackageNames = suggestions.map { it.launchCountKey() }.toSet()
        val additionalApps =
            apps
                .filterNot { suggestionPackageNames.contains(it.launchCountKey()) }
                .sortedWith(
                    compareByDescending<AppInfo> { it.launchCount }
                        .thenBy { it.appName.lowercase(Locale.getDefault()) },
                )
                .take(remainingSpots)

        return suggestions + additionalApps
    }

    private fun getRecentInstallWindow(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val startOfTomorrow = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        val startOfYesterday = calendar.timeInMillis
        return startOfYesterday to startOfTomorrow
    }
}

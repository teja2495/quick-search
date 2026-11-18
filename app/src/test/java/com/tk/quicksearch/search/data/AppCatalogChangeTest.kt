package com.tk.quicksearch.search.data

import android.content.Intent
import com.tk.quicksearch.search.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCatalogChangeTest {
    @Test
    fun workProfileRemovalOnlyMatchesThatProfile() {
        val change = AppCatalogChange("com.example.shared", userHandleId = 10, isRemoval = true)

        assertFalse(change.matches(app("com.example.shared"), currentUserHandleId = 0))
        assertTrue(change.matches(app("com.example.shared", userHandleId = 10), currentUserHandleId = 0))
    }

    @Test
    fun removedKeysAreFilteredWithoutDroppingOtherProfiles() {
        val personal = app("com.example.shared")
        val work = app("com.example.shared", userHandleId = 10)
        val other = app("com.example.other")

        assertEquals(
            listOf(personal, other),
            filterRemovedApps(
                apps = listOf(personal, work, other),
                removedAppKeys = setOf("com.example.shared:10"),
            ),
        )
    }

    @Test
    fun packageReplacementIsNotTreatedAsAnUninstall() {
        val change =
            AppCatalogChange.fromPackageEvent(
                action = Intent.ACTION_PACKAGE_REMOVED,
                packageName = "com.example.updated",
                userHandleId = 0,
                replacing = true,
            )

        assertFalse(change.isRemoval)
        assertFalse(change.requiresCatalogReconciliation)
    }

    @Test
    fun packageReplacementAddIsNotTreatedAsAnInstall() {
        val change =
            AppCatalogChange.fromPackageEvent(
                action = Intent.ACTION_PACKAGE_ADDED,
                packageName = "com.example.updated",
                userHandleId = 0,
                replacing = true,
            )

        assertFalse(change.isInstallation)
        assertFalse(change.requiresCatalogReconciliation)
    }

    @Test
    fun packageAddRequiresCatalogReconciliation() {
        val change =
            AppCatalogChange.fromPackageEvent(
                action = Intent.ACTION_PACKAGE_ADDED,
                packageName = "com.example.installed",
                userHandleId = 0,
                replacing = false,
            )

        assertTrue(change.isInstallation)
        assertTrue(change.requiresCatalogReconciliation)
    }

    @Test
    fun packageChangeRequiresCatalogReconciliation() {
        val change =
            AppCatalogChange.fromPackageEvent(
                action = Intent.ACTION_PACKAGE_CHANGED,
                packageName = "com.example.enabled",
                userHandleId = 0,
                replacing = false,
            )

        assertFalse(change.isRemoval)
        assertFalse(change.isInstallation)
        assertTrue(change.isAvailabilityChange)
        assertTrue(change.requiresCatalogReconciliation)
    }

    @Test
    fun liveRemovalPrunesOnlyTheMatchingCatalogEntry() {
        val personal = app("com.example.shared")
        val work = app("com.example.shared", userHandleId = 10)
        val change = AppCatalogChange("com.example.shared", userHandleId = 0, isRemoval = true)

        assertEquals(
            listOf(work),
            applyCatalogRemoval(
                apps = listOf(personal, work),
                change = change,
                currentUserHandleId = 0,
            ),
        )
    }

    @Test
    fun reconciliationKeepsKnownRemovedAppsOutOfLauncherResults() {
        val removed = app("com.example.removed")
        val installed = app("com.example.installed")

        assertEquals(
            listOf(installed),
            filterRemovedApps(
                apps = listOf(removed, installed),
                removedAppKeys = setOf(removed.launchCountKey()),
            ),
        )
    }

    private fun app(
        packageName: String,
        userHandleId: Int? = null,
    ) =
        AppInfo(
            appName = packageName,
            packageName = packageName,
            lastUsedTime = 0L,
            totalTimeInForeground = 0L,
            firstInstallTime = 0L,
            isSystemApp = false,
            userHandleId = userHandleId,
        )
}

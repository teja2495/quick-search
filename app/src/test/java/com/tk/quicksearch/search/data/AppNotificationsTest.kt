package com.tk.quicksearch.search.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppNotificationsTest {
    private val bankOnly = AppNotificationFilters(apps = listOf(AppNotificationApp("com.bank")))
    private val active = AppNotificationsConfig(enabled = true, filters = bankOnly)

    private fun notification(
        key: String,
        postTime: Long,
        packageName: String = "com.bank",
        text: String = "Rs 500 debited",
    ) = AppNotification(key, packageName, title = null, text = text, postTime = postTime, contentIntent = null)

    private fun keys(notifications: List<AppNotification>) = notifications.map { it.key }

    @Before
    fun setUp() = AppNotifications.clear()

    @After
    fun tearDown() = AppNotifications.clear()

    @Test
    fun emptyConfigMatchesNothing() {
        val posted = listOf(notification("a", 1))
        val enabledWithoutFilters = AppNotificationsConfig(enabled = true, filters = AppNotificationFilters())
        assertFalse(enabledWithoutFilters.isActive)
        assertTrue(AppNotifications.select(posted, emptySet(), enabledWithoutFilters).isEmpty())
    }

    @Test
    fun matchesWithHomeToggleOffForWidgets() {
        val posted = listOf(notification("a", 1))
        val off = active.copy(enabled = false)
        assertFalse(off.isActive)
        assertEquals(listOf("a"), keys(AppNotifications.select(posted, emptySet(), off)))
    }

    @Test
    fun onlyMatchesNewestFirst() {
        val posted =
            listOf(
                notification("old", 1),
                notification("other", 3, packageName = "com.other"),
                notification("new", 2),
            )
        assertEquals(listOf("new", "old"), keys(AppNotifications.select(posted, emptySet(), active)))
    }

    @Test
    fun leavesOutNotificationsOtherRowsShow() {
        val posted = listOf(notification("otp", 2), notification("plain", 1))
        assertEquals(listOf("plain"), keys(AppNotifications.select(posted, setOf("otp"), active)))
    }

    @Test
    fun dismissedStaysHiddenUntilPostedAgain() {
        val first = notification("a", 1)
        AppNotifications.dismiss(first)
        assertTrue(AppNotifications.select(listOf(first), emptySet(), active).isEmpty())
        // The app updating the same notification brings it back.
        val reposted = notification("a", 2)
        assertEquals(listOf("a"), keys(AppNotifications.select(listOf(reposted), emptySet(), active)))
    }
}

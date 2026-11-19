package com.tk.quicksearch.appNotifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNotificationFiltersTest {
    @Test
    fun keywordMatchesWholeWordsIgnoringCase() {
        assertTrue(AppNotificationFilters.containsWholeWords("Your order was DELIVERED.", "delivered"))
        assertFalse(AppNotificationFilters.containsWholeWords("Order undelivered", "delivered"))
        assertFalse(AppNotificationFilters.containsWholeWords("Deliveredness", "delivered"))
    }

    @Test
    fun phraseAndLaterOccurrenceMatch() {
        assertTrue(AppNotificationFilters.containsWholeWords("Undelivered, then out for delivery", "out for delivery"))
        assertTrue(AppNotificationFilters.containsWholeWords("undelivered, now delivered", "delivered"))
    }

    @Test
    fun appWithoutKeywordsShowsEverythingFromIt() {
        val filters = AppNotificationFilters(apps = listOf(AppNotificationApp("com.food")))
        assertTrue(filters.matches("com.food", "anything"))
        assertFalse(filters.matches("com.other", "anything"))
    }

    @Test
    fun appKeywordsNarrowOnlyThatApp() {
        val filters = AppNotificationFilters(apps = listOf(AppNotificationApp("com.bank", listOf("debited"))))
        assertTrue(filters.matches("com.bank", "Rs 500 debited"))
        assertFalse(filters.matches("com.bank", "Statement ready"))
        assertFalse(filters.matches("com.other", "Rs 500 debited"))
    }

    @Test
    fun anyAppKeywordsMatchEveryApp() {
        val filters = AppNotificationFilters(
            apps = listOf(AppNotificationApp("com.bank", listOf("debited"))),
            keywords = listOf("delivered"),
        )
        assertTrue(filters.matches("com.other", "Package delivered"))
        assertTrue(filters.matches("com.bank", "Card delivered"))
    }

    @Test
    fun emptyFiltersMatchNothing() {
        assertFalse(AppNotificationFilters().matches("com.food", "anything"))
    }

    @Test
    fun unspacedScriptsMatchInsideSentences() {
        assertTrue(AppNotificationFilters.containsWholeWords("ご注文の商品が配達されました", "配達"))
        assertTrue(AppNotificationFilters.containsWholeWords("您的包裹已送达", "送达"))
        assertTrue(AppNotificationFilters.containsWholeWords("พัสดุของคุณจัดส่งแล้ว", "จัดส่ง"))
        assertTrue(AppNotificationFilters.containsWholeWords("Amazonで注文しました", "amazon"))
        assertFalse(AppNotificationFilters.containsWholeWords("Amazonian注文", "amazon"))
    }

    @Test
    fun couldMatchOnlyAddedAppsWithoutAnyAppKeywords() {
        val appsOnly = AppNotificationFilters(apps = listOf(AppNotificationApp("com.bank")))
        assertTrue(appsOnly.couldMatch("com.bank"))
        assertFalse(appsOnly.couldMatch("com.other"))
        assertTrue(appsOnly.copy(keywords = listOf("delivered")).couldMatch("com.other"))
    }

    @Test
    fun encodeThenDecodeKeepsAppsKeywordsAndOrder() {
        val filters = AppNotificationFilters(
            apps = listOf(AppNotificationApp("com.bank", listOf("debited", "out for delivery")), AppNotificationApp("com.food")),
            keywords = listOf("delivered", "配達"),
        )
        assertEquals(filters, AppNotificationFiltersCodec.decode(AppNotificationFiltersCodec.encode(filters)))
    }

    @Test
    fun decodeFallsBackToEmptyOnMissingOrBrokenJson() {
        assertTrue(AppNotificationFiltersCodec.decode(null).isEmpty)
        assertTrue(AppNotificationFiltersCodec.decode("not json").isEmpty)
        val decoded = AppNotificationFiltersCodec.decode("""{"apps":[{"package":""},{"package":"com.a"},{"package":"com.a"}]}""")
        assertEquals(listOf("com.a"), decoded.apps.map { it.packageName })
    }

    @Test
    fun decodeDropsRepeatedAndBlankKeywords() {
        val decoded = AppNotificationFiltersCodec.decode(
            """{"apps":[{"package":"com.bank","keywords":["Add"," add ","debited"]}],"keywords":["OTP","otp",""]}""",
        )
        assertEquals(listOf("Add", "debited"), decoded.apps.single().keywords)
        assertEquals(listOf("OTP"), decoded.keywords)
    }
}

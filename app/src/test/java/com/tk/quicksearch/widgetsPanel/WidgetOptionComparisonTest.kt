package com.tk.quicksearch.widgetsPanel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetOptionComparisonTest {
    private val desired = mapOf<String, Any?>(
        "minWidth" to 320,
        "minHeight" to 160,
        "sizes" to listOf(320 to 160),
        "columnSpan" to 4,
        "orientation" to 1,
    )

    @Test
    fun unchangedSizeWithProviderExtrasDoesNotRequireAnotherUpdate() {
        val current = desired + ("providerSetting" to "keep")
        assertTrue(widgetOptionsMatch(current, desired.toMap()))
    }

    @Test
    fun changedDimensionsRequireAnUpdate() {
        assertFalse(widgetOptionsMatch(desired + ("minWidth" to 240), desired))
    }

    @Test
    fun clearedResponsiveSizesRequireAnUpdateEvenWhenBoundsMatch() {
        assertFalse(widgetOptionsMatch(desired + ("sizes" to emptyList<Pair<Int, Int>>()), desired))
    }

    @Test
    fun missingHostOptionRequiresAnUpdate() {
        assertFalse(widgetOptionsMatch(desired - "sizes", desired))
    }
}

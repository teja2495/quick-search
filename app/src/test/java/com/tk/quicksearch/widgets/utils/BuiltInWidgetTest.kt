package com.tk.quicksearch.widgets.utils

import com.tk.quicksearch.widgets.worldClockWidget.WorldClockWidgetReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BuiltInWidgetTest {
    @Test
    fun receiverAndSharedVariantLookupsCoverTheirDefinitions() {
        assertEquals(BuiltInWidget.entries.size, BuiltInWidget.entries.map { it.receiver.name }.toSet().size)
        for (variant in WidgetVariant.entries) {
            val definition = BuiltInWidget.forVariant(variant)
            assertEquals(variant, BuiltInWidget.forReceiver(definition.receiver.name)?.variant)
        }
        assertNotNull(BuiltInWidget.forReceiver(WorldClockWidgetReceiver::class.java.name)?.configure)
        assertEquals(
            listOf(BuiltInWidget.SEARCH, BuiltInWidget.CUSTOM_BUTTONS, BuiltInWidget.MEDIA_CONTROLS),
            BuiltInWidget.entries.filter { it.updatesWithPlayback }.take(3),
        )
    }
}

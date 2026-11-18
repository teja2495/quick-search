package com.tk.quicksearch.search.glance

import com.tk.quicksearch.search.data.preferences.GlanceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LowStorageStatusTest {
    @Test
    fun keepsTheStoredKeysAndThreshold() {
        assertEquals("home_show_low_storage", GlanceItem.LOW_STORAGE.status?.preferenceKey)
        assertEquals("low_storage", GlanceItem.LOW_STORAGE.key)
        assertTrue(isLowStorage(10, 100))
        assertFalse(isLowStorage(11, 100))
        assertFalse(isLowStorage(0, 0))
    }

    @Test
    fun dismissalTracksRecoveryAndClearsAtAOnePointDrop() {
        assertEquals(9f, nextLowStorageDismissal(8f, 9f, true))
        assertEquals(9f, nextLowStorageDismissal(9f, 8.1f, true))
        assertNull(nextLowStorageDismissal(9f, 8f, true))
        assertNull(nextLowStorageDismissal(9f, 11f, false))
    }
}

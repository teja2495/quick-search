package com.tk.quicksearch.shared.permissions

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimePermissionRowTest {
    @Test
    fun runtimeRowsKeepTheirPermissionsAndVisibility() {
        assertEquals(Manifest.permission.READ_CONTACTS, RuntimePermissionRow.CONTACTS.permission)
        assertEquals(Manifest.permission.CALL_PHONE, RuntimePermissionRow.CALLING.permission)
        assertEquals(Manifest.permission.READ_CALENDAR, RuntimePermissionRow.CALENDAR.permission)
        val permissions = RuntimePermissionRow.entries.map { it.permission }
        assertEquals(permissions.size, permissions.toSet().size)
        assertTrue(RuntimePermissionRow.CONTACTS.isVisible(false, false))
        assertFalse(RuntimePermissionRow.CALENDAR.isVisible(true, false))
        assertTrue(RuntimePermissionRow.CALENDAR.isVisible(false, true))
        assertFalse(RuntimePermissionRow.CALLING.isVisible(false, true))
    }
}

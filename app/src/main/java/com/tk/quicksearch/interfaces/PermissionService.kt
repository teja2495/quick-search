package com.tk.quicksearch.interfaces

/**
 * Service for handling permission-related operations
 */
interface PermissionService {
    fun hasContactPermission(): Boolean
    fun hasFilePermission(): Boolean
    fun hasCallPermission(): Boolean
    fun refreshOptionalPermissions(): Boolean
    fun refreshUsageAccess()
}
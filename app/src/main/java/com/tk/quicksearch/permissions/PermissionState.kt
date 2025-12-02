package com.tk.quicksearch.permissions

/**
 * Represents the state of a permission, including whether it's granted and enabled.
 */
data class PermissionState(
    val isGranted: Boolean = false,
    val isEnabled: Boolean = false,
    val wasDenied: Boolean = false
) {
    companion object {
        fun granted() = PermissionState(isGranted = true, isEnabled = true)
        fun denied() = PermissionState(wasDenied = true)
        fun initial() = PermissionState()
    }
}

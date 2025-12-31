package com.tk.quicksearch.permissions

/**
 * Represents the state of a permission in the UI.
 *
 * @property isGranted Whether the permission is actually granted by the system
 * @property isEnabled Whether the user wants this permission enabled (UI toggle state)
 * @property wasDenied Whether the permission was previously denied by the user
 */
data class PermissionState(
    val isGranted: Boolean = false,
    val isEnabled: Boolean = false,
    val wasDenied: Boolean = false
) {
    companion object {
        /** Creates a state for a granted permission that is enabled */
        fun granted() = PermissionState(isGranted = true, isEnabled = true)

        /** Creates a state for a permission that was denied */
        fun denied() = PermissionState(wasDenied = true)

        /** Creates the initial state for a permission (not granted, not enabled) */
        fun initial() = PermissionState()
    }
}

package com.tk.quicksearch.util

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * Helpers for determining whether this app is configured as the system assistant.
 */
fun Context.isDefaultDigitalAssistant(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true) {
            return true
        }
    }

    val assistantSetting =
        Settings.Secure.getString(contentResolver, "assistant")
    return assistantSetting?.startsWith(packageName) == true
}

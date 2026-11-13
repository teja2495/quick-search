package com.tk.quicksearch.shared.util

import android.app.role.RoleManager
import android.content.ComponentName
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

/**
 * The package of the app the user picked as their digital assistant, or null if none is set or
 * the setting can't be read. The voice interaction service is what Assistant settings change;
 * the older "assistant" setting is often left empty.
 */
fun Context.defaultAssistantPackage(): String? =
    listOf("voice_interaction_service", "assistant")
        .firstNotNullOfOrNull { key ->
            runCatching { Settings.Secure.getString(contentResolver, key) }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { ComponentName.unflattenFromString(it)?.packageName }
        }

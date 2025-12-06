package com.tk.quicksearch.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Represents a navigable Android Settings destination that can be surfaced in search.
 */
data class SettingShortcut(
    val id: String,
    val title: String,
    val description: String? = null,
    val keywords: List<String> = emptyList(),
    val action: String,
    val data: String? = null,
    val categories: List<String> = emptyList(),
    val extras: Map<String, Any> = emptyMap(),
    val minSdk: Int = Build.VERSION_CODES.BASE,
    val maxSdk: Int = Int.MAX_VALUE
) {

    /**
     * Checks whether this shortcut is supported on the current device SDK.
     */
    fun isSupported(): Boolean = Build.VERSION.SDK_INT in minSdk..maxSdk

    /**
     * Builds a launch intent for this settings destination.
     */
    fun toIntent(context: Context): Intent {
        val intent = Intent(action)

        data?.let { dataString ->
            val resolvedData = dataString.replace(PACKAGE_PLACEHOLDER, context.packageName)
            intent.data = Uri.parse(resolvedData)
        }

        categories.forEach { category -> intent.addCategory(category) }

        extras.forEach { (key, value) ->
            when (value) {
                is String -> intent.putExtra(key, value.replace(PACKAGE_PLACEHOLDER, context.packageName))
                is Boolean -> intent.putExtra(key, value)
                is Int -> intent.putExtra(key, value)
                is Long -> intent.putExtra(key, value)
                else -> intent.putExtra(key, value.toString())
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    private companion object {
        private const val PACKAGE_PLACEHOLDER = "{packageName}"
    }
}


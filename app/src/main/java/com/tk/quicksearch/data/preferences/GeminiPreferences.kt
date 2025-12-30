package com.tk.quicksearch.data.preferences

import android.content.Context

import android.util.Log

/**
 * Preferences for Gemini API-related settings such as API key and personal context.
 * Uses encrypted storage for sensitive data.
 */
class GeminiPreferences(context: Context) : BasePreferences(context) {

    // ============================================================================
    // Gemini API Preferences
    // ============================================================================

    fun getGeminiApiKey(): String? {
        val securePrefs = encryptedPrefs ?: run {
            Log.e("GeminiPreferences", "EncryptedSharedPreferences unavailable; Gemini API key not loaded")
            return null
        }

        // First try to get from encrypted storage
        val encryptedKey = securePrefs.getString(KEY_GEMINI_API_KEY, null)
        if (!encryptedKey.isNullOrBlank()) return encryptedKey

        // Migration: If not in encrypted storage, check plain text storage and migrate
        val plainTextKey = prefs.getString(KEY_GEMINI_API_KEY, null)
        if (!plainTextKey.isNullOrBlank()) {
            // Migrate to encrypted storage
            securePrefs.edit().putString(KEY_GEMINI_API_KEY, plainTextKey).apply()
            // Remove from plain text storage
            prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
            return plainTextKey
        }

        return null
    }

    fun setGeminiApiKey(key: String?) {
        val securePrefs = encryptedPrefs ?: run {
            Log.e("GeminiPreferences", "EncryptedSharedPreferences unavailable; Gemini API key not persisted")
            return
        }

        if (key.isNullOrBlank()) {
            // Remove from both encrypted and plain text (for migration safety)
            securePrefs.edit().remove(KEY_GEMINI_API_KEY).apply()
            prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
            return
        }

        val normalizedKey = key.trim()
        // Save to encrypted storage
        securePrefs.edit().putString(KEY_GEMINI_API_KEY, normalizedKey).apply()
        // Also remove from plain text storage if it exists (cleanup)
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
    }

    fun getPersonalContext(): String? {
        // Prefer encrypted storage when available
        val securePrefs = encryptedPrefs
        val encryptedValue = securePrefs?.getString(KEY_GEMINI_PERSONAL_CONTEXT, null)
        if (!encryptedValue.isNullOrBlank()) return encryptedValue

        return prefs.getString(KEY_GEMINI_PERSONAL_CONTEXT, null)
    }

    fun setPersonalContext(context: String?) {
        val trimmed = context?.trim()
        val securePrefs = encryptedPrefs

        if (trimmed.isNullOrEmpty()) {
            securePrefs?.edit()?.remove(KEY_GEMINI_PERSONAL_CONTEXT)?.apply()
            prefs.edit().remove(KEY_GEMINI_PERSONAL_CONTEXT).apply()
            return
        }

        if (securePrefs != null) {
            securePrefs.edit().putString(KEY_GEMINI_PERSONAL_CONTEXT, trimmed).apply()
            // Keep plain storage clean if we can encrypt
            prefs.edit().remove(KEY_GEMINI_PERSONAL_CONTEXT).apply()
        } else {
            prefs.edit().putString(KEY_GEMINI_PERSONAL_CONTEXT, trimmed).apply()
        }
    }
}

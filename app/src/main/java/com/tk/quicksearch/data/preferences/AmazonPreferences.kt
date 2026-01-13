package com.tk.quicksearch.data.preferences

import android.content.Context

import com.tk.quicksearch.search.searchEngines.isValidAmazonDomain

/**
 * Preferences for Amazon domain-related settings.
 */
class AmazonPreferences(context: Context) : BasePreferences(context) {

    // ============================================================================
    // Amazon Domain Preferences
    // ============================================================================

    fun getAmazonDomain(): String? {
        return prefs.getString(AmazonPreferences.KEY_AMAZON_DOMAIN, null)
    }

    fun setAmazonDomain(domain: String?) {
        if (domain.isNullOrBlank()) {
            prefs.edit().remove(AmazonPreferences.KEY_AMAZON_DOMAIN).apply()
        } else {
            // Normalize domain (remove protocol, www, trailing slashes)
            val normalizedDomain = domain.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .removeSuffix("/")

            // Validate domain before saving
            if (isValidAmazonDomain(normalizedDomain)) {
                prefs.edit().putString(AmazonPreferences.KEY_AMAZON_DOMAIN, normalizedDomain).apply()
            } else {
                // Invalid domain - don't save, just remove the existing one
                prefs.edit().remove(AmazonPreferences.KEY_AMAZON_DOMAIN).apply()
            }
        }
    }

    companion object {
        // Amazon domain preferences keys
        const val KEY_AMAZON_DOMAIN = "amazon_domain"
    }
}

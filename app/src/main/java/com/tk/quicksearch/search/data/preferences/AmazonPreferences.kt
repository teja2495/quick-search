package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.searchEngines.isValidAmazonDomain

/**
 * Preferences for Amazon domain-related settings.
 */
class AmazonPreferences(
    context: Context,
) : BasePreferences(context) {
    // ============================================================================
    // Amazon Domain Preferences
    // ============================================================================

    fun getAmazonDomain(): String? = prefs.getString(BasePreferences.KEY_AMAZON_DOMAIN, null)

    /** True once first-launch region detection has run, so it never overrides a later choice. */
    fun hasDetectedAmazonRegion(): Boolean =
        prefs.getBoolean(BasePreferences.KEY_AMAZON_DOMAIN_REGION_DETECTED, false)

    fun markAmazonRegionDetected() {
        prefs.edit().putBoolean(BasePreferences.KEY_AMAZON_DOMAIN_REGION_DETECTED, true).apply()
    }

    fun setAmazonDomain(domain: String?) {
        if (domain.isNullOrBlank()) {
            prefs.edit().remove(BasePreferences.KEY_AMAZON_DOMAIN).apply()
        } else {
            // Normalize domain (remove protocol, www, trailing slashes)
            val normalizedDomain =
                domain
                    .trim()
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("www.")
                    .removeSuffix("/")

            // Validate domain before saving
            if (isValidAmazonDomain(normalizedDomain)) {
                prefs.edit().putString(BasePreferences.KEY_AMAZON_DOMAIN, normalizedDomain).apply()
            } else {
                // Invalid domain - don't save, just remove the existing one
                prefs.edit().remove(BasePreferences.KEY_AMAZON_DOMAIN).apply()
            }
        }
    }
}

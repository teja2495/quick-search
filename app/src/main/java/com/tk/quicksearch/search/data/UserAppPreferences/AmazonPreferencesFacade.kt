package com.tk.quicksearch.search.data

import com.tk.quicksearch.search.data.preferences.AmazonPreferences

/**
 * Facade for Amazon preference operations
 */
class AmazonPreferencesFacade(
    private val amazonPreferences: AmazonPreferences
) {
    fun getAmazonDomain(): String? = amazonPreferences.getAmazonDomain()

    fun setAmazonDomain(domain: String?) = amazonPreferences.setAmazonDomain(domain)
}
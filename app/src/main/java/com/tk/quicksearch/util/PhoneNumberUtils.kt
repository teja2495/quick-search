package com.tk.quicksearch.util

/**
 * Utility functions for phone number normalization and duplicate detection.
 */
object PhoneNumberUtils {
    
    /**
     * Extracts all digits from a phone number string.
     */
    fun extractDigits(phoneNumber: String): String {
        return phoneNumber.filter { it.isDigit() }
    }
    
    /**
     * Checks if a phone number has a country code prefix (starts with + followed by digits).
     */
    fun hasCountryCode(phoneNumber: String): Boolean {
        val trimmed = phoneNumber.trim()
        return trimmed.startsWith("+") && trimmed.length > 1 && trimmed[1].isDigit()
    }
    
    /**
     * Gets the country code from a phone number if present.
     * Returns null if no country code is found.
     */
    fun getCountryCode(phoneNumber: String): String? {
        if (!hasCountryCode(phoneNumber)) return null
        
        val trimmed = phoneNumber.trim()
        val digits = extractDigits(trimmed)
        // Country codes are typically 1-3 digits
        // We'll extract the first 1-3 digits after the + sign
        val afterPlus = trimmed.substring(1)
        val countryCodeDigits = afterPlus.takeWhile { it.isDigit() }
        return if (countryCodeDigits.isNotEmpty()) countryCodeDigits else null
    }
    
    /**
     * Checks if two phone numbers represent the same number.
     * One may have a country code while the other doesn't.
     * 
     * Examples:
     * - "+11234567890" and "1234567890" -> true (US number)
     * - "+911234567890" and "1234567890" -> true (India number)
     * - "1234567890" and "1234567890" -> true (exact match)
     */
    fun isSameNumber(number1: String, number2: String): Boolean {
        val digits1 = extractDigits(number1)
        val digits2 = extractDigits(number2)
        
        // Exact match after extracting digits
        if (digits1 == digits2) return true
        
        // Check if one has country code and the other doesn't
        val hasCountryCode1 = hasCountryCode(number1)
        val hasCountryCode2 = hasCountryCode(number2)
        
        if (hasCountryCode1 && !hasCountryCode2) {
            // number1 has country code, number2 doesn't
            // Try removing 1-3 digits from the start of digits1 to match digits2
            // Country codes are typically 1-3 digits
            for (countryCodeLength in 1..3) {
                if (digits1.length > countryCodeLength) {
                    val number1WithoutCountryCode = digits1.substring(countryCodeLength)
                    if (number1WithoutCountryCode == digits2) {
                        return true
                    }
                }
            }
        } else if (!hasCountryCode1 && hasCountryCode2) {
            // number2 has country code, number1 doesn't
            // Try removing 1-3 digits from the start of digits2 to match digits1
            for (countryCodeLength in 1..3) {
                if (digits2.length > countryCodeLength) {
                    val number2WithoutCountryCode = digits2.substring(countryCodeLength)
                    if (number2WithoutCountryCode == digits1) {
                        return true
                    }
                }
            }
        }
        
        return false
    }
    
    /**
     * Finds a duplicate number in the list that matches the given number.
     * Returns the existing number if found, null otherwise.
     */
    fun findDuplicate(existingNumbers: List<String>, newNumber: String): String? {
        return existingNumbers.firstOrNull { isSameNumber(it, newNumber) }
    }
}


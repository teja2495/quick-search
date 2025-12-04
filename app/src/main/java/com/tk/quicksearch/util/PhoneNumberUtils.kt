package com.tk.quicksearch.util

/**
 * Utility functions for phone number normalization and duplicate detection.
 */
object PhoneNumberUtils {
    
    private const val MIN_COUNTRY_CODE_LENGTH = 1
    private const val MAX_COUNTRY_CODE_LENGTH = 3
    
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
        
        // If both have or both don't have country codes, they're not the same
        if (hasCountryCode1 == hasCountryCode2) return false
        
        // One has country code, the other doesn't - try removing country code
        val numberWithCountryCode = if (hasCountryCode1) digits1 else digits2
        val numberWithoutCountryCode = if (hasCountryCode1) digits2 else digits1
        
        return tryRemoveCountryCode(numberWithCountryCode, numberWithoutCountryCode)
    }
    
    /**
     * Tries to match a number with country code to a number without country code
     * by removing 1-3 digits from the start of the number with country code.
     */
    private fun tryRemoveCountryCode(
        numberWithCountryCode: String,
        numberWithoutCountryCode: String
    ): Boolean {
        for (countryCodeLength in MIN_COUNTRY_CODE_LENGTH..MAX_COUNTRY_CODE_LENGTH) {
            if (numberWithCountryCode.length > countryCodeLength) {
                val numberWithoutCode = numberWithCountryCode.substring(countryCodeLength)
                if (numberWithoutCode == numberWithoutCountryCode) {
                    return true
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
    
    /**
     * Validates if a phone number is valid (non-empty and contains digits).
     */
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        val digits = extractDigits(phoneNumber)
        return digits.isNotEmpty() && digits.length >= 7 // Minimum reasonable phone number length
    }
    
    /**
     * Cleans a phone number by removing non-digit characters except the leading +.
     * Returns null if the cleaned number is invalid.
     */
    fun cleanPhoneNumber(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null
        
        val trimmed = phoneNumber.trim()
        val hasPlus = trimmed.startsWith("+")
        
        // Extract digits
        val digits = extractDigits(trimmed)
        if (digits.isEmpty() || digits.length < 7) return null
        
        // Return with + prefix if original had it, otherwise return digits only
        return if (hasPlus) "+$digits" else digits
    }
}


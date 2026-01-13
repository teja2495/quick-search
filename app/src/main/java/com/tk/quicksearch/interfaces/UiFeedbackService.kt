package com.tk.quicksearch.interfaces

/**
 * Service for handling UI feedback (toasts, dialogs, etc.)
 * Abstracts UI notifications from business logic
 */
interface UiFeedbackService {
    fun showToast(messageResId: Int)
    fun showToast(message: String)
}
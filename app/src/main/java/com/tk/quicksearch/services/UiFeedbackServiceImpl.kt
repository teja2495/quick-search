package com.tk.quicksearch.services

import android.content.Context
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.interfaces.UiFeedbackService

/**
 * Implementation of UiFeedbackService using Android Toast
 */
class UiFeedbackServiceImpl(private val context: Context) : UiFeedbackService {

    override fun showToast(messageResId: Int) {
        Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show()
    }

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
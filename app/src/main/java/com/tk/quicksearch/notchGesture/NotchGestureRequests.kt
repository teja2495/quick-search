package com.tk.quicksearch.notchGesture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Something a notch tap asked the search screen to open once it is in front. */
sealed interface NotchGestureRequest {
    data object AllApps : NotchGestureRequest

    data class SearchEngine(val targetId: String) : NotchGestureRequest

    data class Tool(val featureId: String) : NotchGestureRequest
}

/**
 * Hands a notch request from the accessibility service to whichever search surface (Home or the
 * overlay) resumes next. Process-wide because the service and the activities share the process.
 */
object NotchGestureRequests {
    private val pendingRequest = MutableStateFlow<NotchGestureRequest?>(null)
    val pending: StateFlow<NotchGestureRequest?> = pendingRequest.asStateFlow()

    fun post(request: NotchGestureRequest) {
        pendingRequest.value = request
    }

    /** Returns the pending request and clears it, so only one surface acts on it. */
    fun consume(): NotchGestureRequest? = pendingRequest.getAndSet(null)
}

private fun <T> MutableStateFlow<T>.getAndSet(value: T): T {
    while (true) {
        val current = this.value
        if (compareAndSet(current, value)) return current
    }
}

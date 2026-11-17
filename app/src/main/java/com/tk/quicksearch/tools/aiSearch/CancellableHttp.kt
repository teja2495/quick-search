package com.tk.quicksearch.tools.aiSearch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.util.concurrent.CancellationException

/**
 * Blocking [HttpURLConnection] calls ignore coroutine cancellation, so a cancelled AI request would
 * keep its socket (and an IO thread) busy until the server answered or timed out. Connections
 * opened through [trackCancellation] inside [withCancellableHttp] are disconnected as soon as the
 * calling coroutine is cancelled, which makes the blocked read fail right away.
 */
private class HttpConnectionTracker {
    private var current: HttpURLConnection? = null
    private var cancelled = false

    fun track(connection: HttpURLConnection) {
        val alreadyCancelled =
            synchronized(this) {
                if (!cancelled) current = connection
                cancelled
            }
        if (alreadyCancelled) {
            connection.disconnect()
            throw CancellationException("Request cancelled")
        }
    }

    /** Stops tracking without disconnecting; the request finished on its own. */
    fun close() {
        synchronized(this) { current = null }
    }

    fun cancel() {
        val connection =
            synchronized(this) {
                cancelled = true
                current.also { current = null }
            }
        connection?.disconnect()
    }
}

private val activeTracker = ThreadLocal<HttpConnectionTracker?>()

/** Registers this connection so it is disconnected if the surrounding [withCancellableHttp] is cancelled. */
internal fun HttpURLConnection.trackCancellation(): HttpURLConnection {
    activeTracker.get()?.track(this)
    return this
}

/**
 * Runs [block] on [Dispatchers.IO] like `withContext(Dispatchers.IO)`, but disconnects any
 * connection registered with [trackCancellation] when the caller is cancelled. A failure caused
 * by that disconnect surfaces as cancellation, never as an error result.
 */
internal suspend fun <T> withCancellableHttp(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.IO) {
        val tracker = HttpConnectionTracker()
        coroutineScope {
            val watcher =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        awaitCancellation()
                    } finally {
                        tracker.cancel()
                    }
                }
            try {
                val result = withContext(activeTracker.asContextElement(tracker)) { block() }
                coroutineContext.ensureActive()
                result
            } finally {
                tracker.close()
                watcher.cancel()
            }
        }
    }

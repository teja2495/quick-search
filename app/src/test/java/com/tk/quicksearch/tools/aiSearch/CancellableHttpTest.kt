package com.tk.quicksearch.tools.aiSearch

import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CancellableHttpTest {
    @Test
    fun `cancellation disconnects a blocked request and propagates cancellation`() = runBlocking {
        val connection = BlockingConnection()
        val request = async(Dispatchers.Default) {
            withCancellableHttp {
                connection.trackCancellation()
                connection.connect()
                // Simulates a client converting the socket failure into Result.failure.
                Result.failure<String>(IllegalStateException("Disconnected"))
            }
        }
        try {
            assertTrue(connection.started.await(5, TimeUnit.SECONDS))
            request.cancel()
            assertTrue(connection.disconnected.await(5, TimeUnit.SECONDS))
            withTimeout(5_000) { request.join() }
            assertTrue(request.isCancelled)
        } finally {
            connection.disconnect()
            request.cancel()
        }
    }

    @Test
    fun `completed request returns normally without cancellation cleanup disconnecting it`() = runBlocking {
        val connection = BlockingConnection()
        val result = withCancellableHttp {
            connection.trackCancellation()
            "answer"
        }
        assertEquals("answer", result)
        assertFalse(connection.disconnected.await(10, TimeUnit.MILLISECONDS))
    }

    private class BlockingConnection : HttpURLConnection(URL("http://localhost/")) {
        val started = CountDownLatch(1)
        val disconnected = CountDownLatch(1)

        override fun connect() {
            started.countDown()
            check(disconnected.await(10, TimeUnit.SECONDS)) { "Cancellation did not release connection" }
        }

        override fun disconnect() {
            disconnected.countDown()
        }

        override fun usingProxy() = false
    }
}

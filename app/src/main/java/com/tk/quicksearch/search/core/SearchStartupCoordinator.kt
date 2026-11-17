package com.tk.quicksearch.search.core

import android.os.SystemClock
import android.os.Trace
import com.tk.quicksearch.search.apps.AppSearchPerformanceLogger
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext

internal class SearchStartupCoordinator(
    private val scope: CoroutineScope,
    private val hasStartedStartupPhases: AtomicBoolean,
    private val updateStartupPhase: (StartupPhase) -> Unit,
    private val shouldReserveKeyboardStartupWindow: () -> Boolean,
    private val loadCacheAndMinimalPrefsBlock: suspend () -> Unit,
    private val loadRemainingStartupPreferencesBlock: suspend () -> Unit,
    private val launchDeferredInitializationBlock: () -> Unit,
    /** Main thread, once the keyboard has settled (or right after phase 1 without one). */
    private val onKeyboardWindowEndBlock: () -> Unit = {},
) {
    private companion object {
        const val KeyboardStartupMaxWaitMs = 350L
        const val KeyboardStartupSettleMs = 32L
    }

    private val startupKeyboardVisible = CompletableDeferred<Unit>()

    fun notifyStartupKeyboardVisible() {
        startupKeyboardVisible.complete(Unit)
    }

    fun startStartupPhases() {
        if (!hasStartedStartupPhases.compareAndSet(false, true)) return

        scope.launch(Dispatchers.Main.immediate) {
            val startedAtMs = SystemClock.elapsedRealtime()
            AppSearchPerformanceLogger.log { "startupPinned phase1Start" }
            updateStartupPhase(StartupPhase.PHASE_1_CACHE_PREFS)
            Trace.beginSection("QS.Startup.Phase1.CachePrefs")
            try {
                withContext(Dispatchers.IO) { loadCacheAndMinimalPrefs() }
            } finally {
                Trace.endSection()
            }

            AppSearchPerformanceLogger.log {
                "startupPinned phase1Done atMs=${SystemClock.elapsedRealtime() - startedAtMs}"
            }
            kotlinx.coroutines.yield()
            if (shouldReserveKeyboardStartupWindow()) {
                val keyboardBecameVisible =
                    withTimeoutOrNull(KeyboardStartupMaxWaitMs) {
                        startupKeyboardVisible.await()
                        true
                    } ?: false
                if (keyboardBecameVisible) {
                    delay(KeyboardStartupSettleMs)
                }
            }

            AppSearchPerformanceLogger.log {
                "startupPinned keyboardWaitDone atMs=${SystemClock.elapsedRealtime() - startedAtMs}"
            }
            onKeyboardWindowEndBlock()
            updateStartupPhase(StartupPhase.PHASE_2_HEAVY_FEATURES)
            Trace.beginSection("QS.Startup.Phase2.HeavyInit")
            try {
                withContext(Dispatchers.IO) { loadRemainingStartupPreferences() }
            } finally {
                Trace.endSection()
            }

            AppSearchPerformanceLogger.log {
                "startupPinned phase2Done atMs=${SystemClock.elapsedRealtime() - startedAtMs}"
            }
            Trace.beginSection("QS.Startup.Phase3.DeferredInit")
            try {
                launchDeferredInitialization()
            } finally {
                Trace.endSection()
            }
        }
    }

    private suspend fun loadCacheAndMinimalPrefs() {
        loadCacheAndMinimalPrefsBlock()
    }

    private suspend fun loadRemainingStartupPreferences() {
        loadRemainingStartupPreferencesBlock()
    }

    private fun launchDeferredInitialization() {
        launchDeferredInitializationBlock()
    }
}

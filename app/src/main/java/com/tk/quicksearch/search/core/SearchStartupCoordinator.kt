package com.tk.quicksearch.search.core

import android.os.Trace
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SearchStartupCoordinator(
    private val scope: CoroutineScope,
    private val hasStartedStartupPhases: AtomicBoolean,
    private val instantStartupSurfaceEnabled: Boolean,
    private val updateStartupPhase: (StartupPhase) -> Unit,
    private val preloadBackgroundForInitialSearchSurface: suspend () -> Unit,
    private val loadCacheAndMinimalPrefsBlock: suspend () -> Unit,
    private val loadRemainingStartupPreferencesBlock: suspend () -> Unit,
    private val launchDeferredInitializationBlock: () -> Unit,
) {
    fun startStartupPhases() {
        if (!hasStartedStartupPhases.compareAndSet(false, true)) return

        scope.launch(Dispatchers.Main.immediate) {
            if (!instantStartupSurfaceEnabled) {
                withContext(Dispatchers.IO) { preloadBackgroundForInitialSearchSurface() }
            }

            updateStartupPhase(StartupPhase.PHASE_1_CACHE_PREFS)
            Trace.beginSection("QS.Startup.Phase1.CachePrefs")
            try {
                withContext(Dispatchers.IO) { loadCacheAndMinimalPrefs() }
            } finally {
                Trace.endSection()
            }

            kotlinx.coroutines.yield()

            updateStartupPhase(StartupPhase.PHASE_2_HEAVY_FEATURES)
            Trace.beginSection("QS.Startup.Phase2.HeavyInit")
            try {
                withContext(Dispatchers.IO) { loadRemainingStartupPreferences() }
            } finally {
                Trace.endSection()
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

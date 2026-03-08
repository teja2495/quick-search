package com.tk.quicksearch.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    // Startup engineering targets:
    // - Search bar shell visible in the first frame budget.
    // - Phase 1 (cache + lightweight prefs) starts immediately after first frame.
    // - No package/content scans on phase 0.
    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun coldStartup_tracksStartupPhases() {
        benchmarkRule.measureRepeated(
            packageName = "com.tk.quicksearch",
            metrics =
                listOf(
                    StartupTimingMetric(),
                    FrameTimingMetric(),
                    TraceSectionMetric("QS.Startup.FirstFrameGate"),
                    TraceSectionMetric("QS.Startup.Phase1.CachePrefs"),
                    TraceSectionMetric("QS.Startup.Phase2.HeavyInit"),
                    TraceSectionMetric("QS.Startup.Phase3.DeferredInit"),
                ),
            iterations = 8,
            startupMode = StartupMode.COLD,
            compilationMode = CompilationMode.Partial(),
        ) {
            pressHome()
            startActivityAndWait()
        }
    }
}

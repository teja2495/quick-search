package com.tk.quicksearch.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineProfileRule.collect(
            packageName = "com.tk.quicksearch",
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            device.executeShellCommand("input text settings")
            device.wait(Until.hasObject(By.textContains("Settings")), RESULT_TIMEOUT_MS)
            device.waitForIdle()
        }
    }

    private companion object {
        const val RESULT_TIMEOUT_MS = 5_000L
    }
}

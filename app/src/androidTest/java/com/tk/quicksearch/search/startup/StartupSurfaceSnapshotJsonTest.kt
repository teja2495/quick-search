package com.tk.quicksearch.search.startup

import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.OverlayGradientTheme
import com.tk.quicksearch.search.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StartupSurfaceSnapshotJsonTest {
    @Test
    fun `round trips snapshot payload`() {
        val source =
            StartupSurfaceSnapshot(
                createdAtMillis = 1234L,
                backgroundSource = BackgroundSource.CUSTOM_IMAGE,
                showWallpaperBackground = true,
                wallpaperBackgroundAlpha = 0.42f,
                wallpaperBlurRadius = 18f,
                overlayGradientTheme = OverlayGradientTheme.AURORA,
                overlayThemeIntensity = 0.6f,
                customImageUri = "content://image/test",
                startupBackgroundPreviewPath = "/tmp/preview.jpg",
                oneHandedMode = true,
                bottomSearchBarEnabled = true,
                fontScaleMultiplier = 1.02f,
                showAppLabels = false,
                appSuggestionsEnabled = true,
                suggestedApps =
                    listOf(
                        AppInfo(
                            appName = "Maps",
                            packageName = "com.maps",
                            lastUsedTime = 9L,
                            totalTimeInForeground = 4L,
                            launchCount = 3,
                            firstInstallTime = 1L,
                            isSystemApp = false,
                            userHandleId = 10,
                            componentName = "com.maps/.Main",
                        ),
                    ),
            )

        val decoded = StartupSurfaceSnapshotJson.fromJson(StartupSurfaceSnapshotJson.toJson(source))

        assertNotNull(decoded)
        assertEquals(source, decoded)
    }

    @Test
    fun `returns null for unsupported version`() {
        val raw = """{"version":99}"""
        assertNull(StartupSurfaceSnapshotJson.fromJson(raw))
    }

    @Test
    fun `handles corrupt payload`() {
        assertNull(StartupSurfaceSnapshotJson.fromJson("not-json"))
    }
}

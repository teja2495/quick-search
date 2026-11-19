package com.tk.quicksearch.search.startup

import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.BrowserApp
import com.tk.quicksearch.search.core.CustomSearchEngine
import com.tk.quicksearch.searchEngines.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StartupSurfaceSnapshotJsonTest {
    @Test
    fun roundTripsSnapshotPayload() {
        val source =
            StartupSurfaceSnapshot(
                createdAtMillis = 1234L,
                backgroundSource = BackgroundSource.CUSTOM_IMAGE,
                showWallpaperBackground = true,
                wallpaperBackgroundAlpha = 0.42f,
                wallpaperBlurRadius = 18f,
                appTheme = AppTheme.AURORA,
                overlayThemeIntensity = 0.6f,
                customImageUri = "content://image/test",
                startupBackgroundPreviewPath = "/tmp/preview.jpg",
                oneHandedMode = true,
                bottomSearchBarEnabled = true,
                topResultIndicatorEnabled = true,
                openKeyboardOnLaunch = true,
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
                searchTargetsOrder =
                    listOf(
                        SearchTarget.Engine(SearchEngine.GOOGLE),
                        SearchTarget.Browser(BrowserApp("com.browser", "Browser")),
                        SearchTarget.Custom(
                            CustomSearchEngine(
                                id = "docs",
                                name = "Docs",
                                urlTemplate = "https://example.com?q=%s",
                                faviconBase64 = "favicon",
                                browserPackage = "com.browser",
                            ),
                        ),
                    ),
                disabledSearchTargetIds = setOf(SearchEngine.GOOGLE.name),
                isSearchEngineCompactMode = true,
                searchEngineCompactRowCount = 2,
            )

        val decoded = StartupSurfaceSnapshotJson.fromJson(StartupSurfaceSnapshotJson.toJson(source))

        assertNotNull(decoded)
        val expectedTargets = source.searchTargetsOrder.map { target ->
            if (target is SearchTarget.Custom) {
                SearchTarget.Custom(target.custom.copy(faviconBase64 = null))
            } else {
                target
            }
        }
        assertEquals(source.copy(searchTargetsOrder = expectedTargets), decoded)
        org.junit.Assert.assertFalse(StartupSurfaceSnapshotJson.toJson(source).contains("favicon"))
    }

    @Test
    fun legacyInlineFaviconIsIgnoredWithoutLosingCustomEngine() {
        val raw = """{"version":1,"searchTargetsOrder":[
            {"type":"custom","id":"docs","name":"Docs","urlTemplate":"https://example.com?q=%s",
            "faviconBase64":"legacy-icon","browserPackage":"com.browser"}]}"""
        val decoded = requireNotNull(StartupSurfaceSnapshotJson.fromJson(raw))
        val custom = (decoded.searchTargetsOrder.single() as SearchTarget.Custom).custom
        assertEquals("docs", custom.id)
        assertEquals("com.browser", custom.browserPackage)
        assertNull(custom.faviconBase64)
    }

    @Test
    fun returnsNullForUnsupportedVersion() {
        val raw = """{"version":99}"""
        assertNull(StartupSurfaceSnapshotJson.fromJson(raw))
    }

    @Test
    fun handlesCorruptPayload() {
        assertNull(StartupSurfaceSnapshotJson.fromJson("not-json"))
    }

    @Test
    fun missingFieldsUseBackwardCompatibleDefaults() {
        val decoded = StartupSurfaceSnapshotJson.fromJson("""{"version":1,"createdAtMillis":42}""")

        assertNotNull(decoded)
        requireNotNull(decoded)
        assertEquals(42L, decoded.createdAtMillis)
        assertEquals(BackgroundSource.THEME, decoded.backgroundSource)
        assertEquals(AppTheme.MONOCHROME, decoded.appTheme)
        assertEquals(false, decoded.showWallpaperBackground)
        assertEquals(0.5f, decoded.wallpaperBackgroundAlpha)
        assertEquals(20f, decoded.wallpaperBlurRadius)
        assertEquals(false, decoded.topResultIndicatorEnabled)
        assertEquals(true, decoded.openKeyboardOnLaunch)
        assertEquals(1f, decoded.fontScaleMultiplier)
        assertEquals(true, decoded.showAppLabels)
        assertEquals(true, decoded.appSuggestionsEnabled)
        assertEquals(emptyList<AppInfo>(), decoded.suggestedApps)
        assertEquals(emptyList<SearchTarget>(), decoded.searchTargetsOrder)
        assertEquals(emptySet<String>(), decoded.disabledSearchTargetIds)
        assertEquals(false, decoded.isSearchEngineCompactMode)
        assertEquals(1, decoded.searchEngineCompactRowCount)
    }

    @Test
    fun unknownTargetsAreSkippedWithoutDroppingValidTargets() {
        val raw =
            """
            {
              "version": 1,
              "searchTargetsOrder": [
                {"type":"engine","engine":"GOOGLE"},
                {"type":"engine","engine":"REMOVED_ENGINE"},
                {"type":"future_target","id":"future"}
              ],
              "disabledSearchTargetIds": ["GOOGLE", "future"]
            }
            """.trimIndent()

        val decoded = StartupSurfaceSnapshotJson.fromJson(raw)

        assertNotNull(decoded)
        assertEquals(listOf(SearchTarget.Engine(SearchEngine.GOOGLE)), decoded?.searchTargetsOrder)
        assertEquals(setOf("GOOGLE", "future"), decoded?.disabledSearchTargetIds)
    }
}

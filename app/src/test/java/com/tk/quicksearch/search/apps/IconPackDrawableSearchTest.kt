package com.tk.quicksearch.search.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class IconPackDrawableSearchTest {
    @Test
    fun `filters by installed app label when drawable name does not match`() {
        val bundledNotes = IconPackDrawableInfo("blue_tiles", setOf("com.bundled.notes"))
        val unrelated = IconPackDrawableInfo("bundled_logo", setOf("com.example.unrelated"))

        val results =
            filterIconPackDrawables(
                iconDrawables = listOf(unrelated, bundledNotes),
                query = "bundled",
                installedAppLabels = mapOf("com.bundled.notes" to "Bundled Notes"),
            )

        assertEquals(listOf(bundledNotes, unrelated), results)
    }

    @Test
    fun `filters by target package when the mapped app is not installed`() {
        val mappedIcon = IconPackDrawableInfo("blue_tiles", setOf("com.bundled.notes"))

        val results =
            filterIconPackDrawables(
                iconDrawables = listOf(mappedIcon),
                query = "bundled",
                installedAppLabels = emptyMap(),
            )

        assertEquals(listOf(mappedIcon), results)
    }

    @Test
    fun `includes catalog-only icons while preserving app mappings`() {
        val results =
            mergeIconPackDrawables(
                packageMapping = mapOf("com.google.android.apps.maps" to "google_maps"),
                catalogDrawableNames = setOf("google_maps", "google_maps_go", "system_ac_maps"),
            )

        assertEquals(
            listOf(
                IconPackDrawableInfo("google_maps", setOf("com.google.android.apps.maps")),
                IconPackDrawableInfo("google_maps_go", emptySet()),
                IconPackDrawableInfo("system_ac_maps", emptySet()),
            ),
            results,
        )
    }

    @Test
    fun `search returns matching catalog-only icons`() {
        val iconDrawables =
            mergeIconPackDrawables(
                packageMapping = emptyMap(),
                catalogDrawableNames = setOf("google_maps", "google_maps_go", "google_photos"),
            )

        val results =
            filterIconPackDrawables(
                iconDrawables = iconDrawables,
                query = "maps",
                installedAppLabels = emptyMap(),
            )

        assertEquals(listOf("google_maps", "google_maps_go"), results.map { it.drawableName })
    }
}

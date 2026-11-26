package com.tk.quicksearch.search.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class AppGridOrderTest {
    @Test
    fun `one handed visual order reverses grid rows`() {
        val apps = (0 until 10).toList()

        val visualOrder = appsInVisualGridOrder(apps, columns = 5, oneHandedMode = true)

        assertEquals(listOf(5, 6, 7, 8, 9, 0, 1, 2, 3, 4), visualOrder)
    }

    @Test
    fun `persisted order restores one handed visual order with partial row`() {
        val apps = (0 until 12).toList()
        val visualOrder = appsInVisualGridOrder(apps, columns = 5, oneHandedMode = true)

        val persistedOrder =
                appsInPersistedGridOrder(visualOrder, columns = 5, oneHandedMode = true)

        assertEquals(apps, persistedOrder)
    }

    @Test
    fun `visual reorder converts back without changing dragged position`() {
        val apps = (0 until 12).toList()
        val reorderedVisualApps =
                appsInVisualGridOrder(apps, columns = 5, oneHandedMode = true)
                        .toMutableList()
                        .apply { add(2, removeAt(7)) }

        val persistedOrder =
                appsInPersistedGridOrder(reorderedVisualApps, columns = 5, oneHandedMode = true)

        assertEquals(
                reorderedVisualApps,
                appsInVisualGridOrder(persistedOrder, columns = 5, oneHandedMode = true),
        )
        assertEquals(0, reorderedVisualApps[2])
    }

    @Test
    fun `normal mode preserves the same order`() {
        val apps = (0 until 8).toList()

        assertEquals(apps, appsInVisualGridOrder(apps, columns = 5, oneHandedMode = false))
        assertEquals(apps, appsInPersistedGridOrder(apps, columns = 5, oneHandedMode = false))
    }

    @Test
    fun `grid rows are filled with gaps after trimming trailing gaps`() {
        val items = listOf("a", "gap", "b", "gap", "gap")

        val filled =
                itemsFilledToGridRows(
                        items,
                        columns = 4,
                        isGap = { it.startsWith("gap") },
                        gap = { "gap$it" },
                )

        assertEquals(listOf("a", "gap0", "b", "gap1"), filled)
    }

    @Test
    fun `pinned tiles in an excess row fill vacancies in existing rows`() {
        val items = (1..8).map(Int::toString) + listOf("gap0", "gap1", "gap2", "Teams", "Telegram")
        val filled =
                itemsFilledToGridRows(
                        items,
                        columns = 4,
                        isGap = { it.startsWith("gap") },
                        gap = { "gap$it" },
                )

        assertEquals(
                listOf("Telegram", "gap0", "gap1", "Teams", "5", "6", "7", "8", "1", "2", "3", "4"),
                appsInVisualGridOrder(filled, columns = 4, oneHandedMode = true),
        )
    }

    @Test
    fun `full rows and only gaps are not padded`() {
        val isGap: (String) -> Boolean = { it.startsWith("gap") }

        assertEquals(
                listOf("a", "b"),
                itemsFilledToGridRows(listOf("a", "b"), columns = 2, isGap = isGap, gap = { "gap$it" }),
        )
        assertEquals(
                emptyList<String>(),
                itemsFilledToGridRows(listOf("gap", "gap"), columns = 3, isGap = isGap, gap = { "gap$it" }),
        )
    }

    @Test
    fun `gaps after the last present item are dropped`() {
        val g0 = pinnedGridGapKey(0)
        val g1 = pinnedGridGapKey(1)

        assertEquals(emptyList<String>(), gapKeysBeforeLastItem(listOf("a", "f", g0, g1), setOf("a", "f", "d")))
        assertEquals(listOf(g0), gapKeysBeforeLastItem(listOf("a", g0, "b"), setOf("a", "b")))
        assertEquals(emptyList<String>(), gapKeysBeforeLastItem(listOf("a", g0, g1, "x"), setOf("a")))
    }

    @Test
    fun `added items fill gaps before being appended`() {
        val isGap: (String) -> Boolean = { it == "_" }

        assertEquals(
                listOf("a", "d", "b", "e", "c", "f"),
                itemsFillingGaps(listOf("a", "_", "b", "_", "c"), listOf("d", "e", "f"), isGap),
        )
        assertEquals(listOf("a", "d", "_"), itemsFillingGaps(listOf("a", "_", "_"), listOf("d"), isGap))
    }
}

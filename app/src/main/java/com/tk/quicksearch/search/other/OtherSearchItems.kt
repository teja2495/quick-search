package com.tk.quicksearch.search.other

import com.tk.quicksearch.search.core.ScreenTimeState

enum class OtherSearchItemId(
    val persistedId: String,
) {
    SCREEN_TIME("screen_time"),
    ;

    val pinnedItemKey: String
        get() = "$PINNED_ITEM_PREFIX$persistedId"

    private companion object {
        const val PINNED_ITEM_PREFIX = "other:"
    }
}

data class OtherSearchItemDefinition(
    val id: OtherSearchItemId,
    val searchTerms: Set<String>,
)

object OtherSearchItemRegistry {
    val definitions =
        listOf(
            OtherSearchItemDefinition(
                id = OtherSearchItemId.SCREEN_TIME,
                searchTerms = setOf("screen time", "screen usage", "phone usage"),
            ),
        )

    fun matches(itemId: OtherSearchItemId, query: String): Boolean {
        val normalized = query.trim().lowercase()
        if (normalized.length < 3) return false
        val definition = definitions.first { it.id == itemId }
        return definition.searchTerms.any { term ->
            term.contains(normalized) || normalized.contains(term)
        }
    }

    fun matchesScreenTime(query: String): Boolean = matches(OtherSearchItemId.SCREEN_TIME, query)

    fun isPinned(
        itemId: OtherSearchItemId,
        pinnedItemOrder: List<String>,
    ): Boolean = itemId.pinnedItemKey in pinnedItemOrder

    fun togglePin(
        itemId: OtherSearchItemId,
        pinnedItemOrder: List<String>,
    ): List<String> =
        if (isPinned(itemId, pinnedItemOrder)) {
            pinnedItemOrder.filterNot { it == itemId.pinnedItemKey }
        } else {
            pinnedItemOrder + itemId.pinnedItemKey
        }

    fun shouldLoad(
        itemId: OtherSearchItemId,
        query: String,
        pinnedItemOrder: List<String>,
    ): Boolean =
        matches(itemId, query) ||
            (query.isBlank() && isPinned(itemId, pinnedItemOrder))

    fun shouldRenderScreenTime(
        query: String,
        pinnedItemOrder: List<String>,
        state: ScreenTimeState,
    ): Boolean {
        if (state !is ScreenTimeState.Available) return false
        return if (query.isBlank()) {
            isPinned(OtherSearchItemId.SCREEN_TIME, pinnedItemOrder)
        } else {
            matchesScreenTime(query)
        }
    }

    fun hasVisibleResult(
        query: String,
        pinnedItemOrder: List<String>,
        screenTimeState: ScreenTimeState,
    ): Boolean = shouldRenderScreenTime(query, pinnedItemOrder, screenTimeState)

    fun visibleSearchItemIds(
        query: String,
        pinnedItemOrder: List<String>,
        screenTimeState: ScreenTimeState,
    ): List<OtherSearchItemId> =
        buildList {
            if (
                query.isNotBlank() &&
                    shouldRenderScreenTime(query, pinnedItemOrder, screenTimeState)
            ) {
                add(OtherSearchItemId.SCREEN_TIME)
            }
        }

    fun searchTerms(itemId: OtherSearchItemId): Set<String> =
        definitions.first { it.id == itemId }.searchTerms

    fun isOtherPinnedItemKey(key: String): Boolean =
        OtherSearchItemId.entries.any { it.pinnedItemKey == key }
}

package com.tk.quicksearch.search.folders

// Pinned app grid order rewrites for folder changes. Each takes the Pinned tab's displayed order.

/** The target's tile becomes the folder's, and the dragged tile goes away. */
internal fun orderAfterCreatingFolder(
    orderKeys: List<String>,
    targetKey: String,
    draggedKey: String,
    folderGridKey: String,
): List<String> =
    orderKeys
        .filterNot { it == draggedKey }
        .map { if (it == targetKey) folderGridKey else it }

/**
 * The removed member's tile goes right after the folder, or in its place when the folder was
 * deleted with it. [memberGridKey] is null when the member is no longer available.
 */
internal fun orderAfterRemovingMember(
    orderKeys: List<String>,
    folderGridKey: String,
    memberGridKey: String?,
    folderRemoved: Boolean,
): List<String> {
    val order =
        orderKeys
            .filterNot { it == memberGridKey }
            .flatMap { key ->
                when {
                    key != folderGridKey -> listOf(key)
                    folderRemoved -> listOfNotNull(memberGridKey)
                    else -> listOfNotNull(key, memberGridKey)
                }
            }
    return when {
        folderRemoved || folderGridKey in order -> order
        // The folder wasn't displayed; keep it, with the member after it.
        else -> order.filterNot { it == memberGridKey } + listOfNotNull(folderGridKey, memberGridKey)
    }
}

/** The members' tiles take the folder's place, in folder order. */
internal fun orderAfterDeletingFolder(
    orderKeys: List<String>,
    folderGridKey: String,
    restoredGridKeys: List<String>,
): List<String> {
    val order =
        orderKeys
            .filterNot { it in restoredGridKeys }
            .flatMap { key -> if (key == folderGridKey) restoredGridKeys else listOf(key) }
    return if (folderGridKey in orderKeys) order else order + restoredGridKeys
}

/**
 * [currentKeys] in the order of [orderedKeys], skipping keys not in [currentKeys], followed by the
 * current keys [orderedKeys] leaves out.
 */
internal fun reorderedFolderMembers(
    currentKeys: List<String>,
    orderedKeys: List<String>,
): List<String> {
    val current = currentKeys.toSet()
    val ordered = orderedKeys.filter { it in current }.distinct()
    return ordered + currentKeys.filterNot { it in ordered.toSet() }
}

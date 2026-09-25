package com.tk.quicksearch.search.folders

import com.tk.quicksearch.search.core.SearchFeatureState
import com.tk.quicksearch.search.core.SearchResultsState
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.StartupPhase
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.isShortcutDisabled
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.models.AppInfo
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val AvailabilityPruneDebounceMs = 500L

/**
 * Owns Pinned-tab folder changes. Folder members are moved out of the pinned grid, so every change
 * also pins or unpins the affected apps and shortcuts and rewrites the pinned app grid order.
 *
 * Operations take [orderKeys], the Pinned tab's full grid order as currently displayed, so the new
 * order is derived from what the user sees even when no order has been saved yet.
 */
class FolderManager(
    private val scope: CoroutineScope,
    private val userPreferences: UserAppPreferences,
    private val resultsStateProvider: () -> SearchResultsState,
    private val updateFeatureState: ((SearchFeatureState) -> SearchFeatureState) -> Unit,
    private val pinApp: (AppInfo) -> Unit,
    private val unpinApp: (AppInfo) -> Unit,
    private val pinShortcut: (StaticShortcut) -> Unit,
    private val unpinShortcut: (StaticShortcut) -> Unit,
    private val reorderPinnedAppGrid: (List<String>, List<AppInfo>, List<StaticShortcut>) -> Unit,
) {
    /** Creates a folder holding [targetKey] then [draggedKey], placed at the target's position. */
    fun createFolder(
        targetKey: String,
        draggedKey: String,
        orderKeys: List<String>,
    ) {
        if (targetKey == draggedKey) return
        val targetMember = gridKeyToMemberKey(targetKey) ?: return
        val draggedMember = gridKeyToMemberKey(draggedKey) ?: return
        val folder =
            AppFolder(
                id = UUID.randomUUID().toString(),
                memberKeys = listOf(targetMember, draggedMember),
            )
        val folders = userPreferences.getAppFolders().map { it.withoutMembers(folder.memberKeys) }
        saveFolders(folders + folder)
        unpinMember(targetMember)
        unpinMember(draggedMember)
        commitOrder(orderAfterCreatingFolder(orderKeys, targetKey, draggedKey, folder.gridKey))
    }

    /** Appends [draggedKey] to the end of the folder and removes its own grid tile. */
    fun addToFolder(
        folderId: String,
        draggedKey: String,
        orderKeys: List<String>,
    ) {
        val draggedMember = gridKeyToMemberKey(draggedKey) ?: return
        val folders = userPreferences.getAppFolders()
        if (folders.none { it.id == folderId }) return
        saveFolders(
            folders.map { folder ->
                if (folder.id == folderId) {
                    folder.copy(memberKeys = (folder.memberKeys - draggedMember) + draggedMember)
                } else {
                    folder.withoutMembers(listOf(draggedMember))
                }
            },
        )
        unpinMember(draggedMember)
        commitOrder(orderKeys.filterNot { it == draggedKey })
    }

    /**
     * Takes [memberKey] out of the folder. If fewer than two members would remain, the folder is
     * dissolved and its remaining member returns to the grid. The removed member also returns to
     * the grid when [repin], or leaves the Pinned tab otherwise.
     */
    fun removeFromFolder(
        folderId: String,
        memberKey: String,
        orderKeys: List<String>,
        repin: Boolean = true,
    ) {
        val folders = userPreferences.getAppFolders()
        val folder = folders.firstOrNull { it.id == folderId } ?: return
        if (memberKey !in folder.memberKeys) return
        val remainingMembers = folder.memberKeys - memberKey
        val restoredMembers =
            membersRestoredAfterRemoval(folder.memberKeys, memberKey, repin)
        val folderRemoved = restoredMembers != null
        saveFolders(
            if (folderRemoved) {
                folders.filterNot { it.id == folderId }
            } else {
                folders.map { if (it.id == folderId) it.copy(memberKeys = remainingMembers) else it }
            },
        )
        if (folderRemoved) {
            val restoredGridKeys =
                restoredMembers.orEmpty().filter(::pinMember).mapNotNull(::memberKeyToGridKey)
            commitOrder(orderAfterDeletingFolder(orderKeys, folder.gridKey, restoredGridKeys))
        } else {
            val repinned = repin && pinMember(memberKey)
            commitOrder(
                orderAfterRemovingMember(
                    orderKeys = orderKeys,
                    folderGridKey = folder.gridKey,
                    memberGridKey = memberKeyToGridKey(memberKey)?.takeIf { repinned },
                    folderRemoved = false,
                ),
            )
        }
    }

    /** Deletes the folder and returns its members to the pinned grid at the folder's position. */
    fun deleteFolder(
        folderId: String,
        orderKeys: List<String>,
    ) {
        val folders = userPreferences.getAppFolders()
        val folder = folders.firstOrNull { it.id == folderId } ?: return
        saveFolders(folders.filterNot { it.id == folderId })
        val restoredGridKeys =
            folder.memberKeys.filter(::pinMember).mapNotNull(::memberKeyToGridKey)
        commitOrder(orderAfterDeletingFolder(orderKeys, folder.gridKey, restoredGridKeys))
    }

    /**
     * Reorders the folder's members to [memberKeys]. Keys no longer in the folder are ignored, and
     * members missing from [memberKeys] (e.g. unavailable ones) keep their place at the end.
     */
    fun reorderFolderMembers(
        folderId: String,
        memberKeys: List<String>,
    ) {
        val folders = userPreferences.getAppFolders()
        val folder = folders.firstOrNull { it.id == folderId } ?: return
        val reordered = reorderedFolderMembers(folder.memberKeys, memberKeys)
        if (reordered == folder.memberKeys) return
        saveFolders(folders.map { if (it.id == folderId) it.copy(memberKeys = reordered) else it })
    }

    fun renameFolder(
        folderId: String,
        name: String,
    ) {
        val trimmedName = name.trim()
        val folders = userPreferences.getAppFolders()
        if (folders.none { it.id == folderId && it.name != trimmedName }) return
        saveFolders(folders.map { if (it.id == folderId) it.copy(name = trimmedName) else it })
    }

    /**
     * Prunes uninstalled apps and removed or disabled shortcuts from folders whenever the app or
     * shortcut catalog changes, dissolving folders left with fewer than two members.
     */
    @OptIn(FlowPreview::class)
    fun observeAvailability(uiState: StateFlow<SearchUiState>) {
        scope.launch(Dispatchers.Default) {
            uiState
                .filter { it.startupPhase == StartupPhase.COMPLETE && it.allApps.isNotEmpty() }
                .map { state ->
                    FolderAvailability(
                        folders = state.appFolders,
                        apps = state.allApps,
                        shortcuts = state.allAppShortcuts,
                        disabledShortcutIds = state.disabledAppShortcutIds,
                    )
                }
                .distinctUntilChanged()
                .debounce(AvailabilityPruneDebounceMs)
                .collect { availability ->
                    if (availability.folders.isEmpty()) return@collect
                    val appKeys = availability.apps.mapTo(HashSet()) { appFolderMemberKey(it) }
                    val shortcutKeys =
                        availability.shortcuts.mapTo(HashSet()) { appFolderMemberKey(it) }
                    val disabledShortcutKeys =
                        availability.shortcuts
                            .filter { isShortcutDisabled(it, availability.disabledShortcutIds) }
                            .mapTo(HashSet()) { appFolderMemberKey(it) }
                    withContext(Dispatchers.Main.immediate) {
                        pruneUnavailableMembers(appKeys, shortcutKeys, disabledShortcutKeys)
                    }
                }
        }
    }

    private fun pruneUnavailableMembers(
        appKeys: Set<String>,
        shortcutKeys: Set<String>,
        disabledShortcutKeys: Set<String>,
    ) {
        val folders = userPreferences.getAppFolders()
        val prunedFolders =
            folders.map { folder ->
                folder.copy(
                    memberKeys =
                        availableFolderMemberKeys(
                            folder = folder,
                            appKeys = appKeys,
                            shortcutKeys = shortcutKeys,
                            disabledShortcutKeys = disabledShortcutKeys,
                        ),
                )
            }
        if (
            prunedFolders == folders &&
                folders.all { it.memberKeys.size >= MIN_APP_FOLDER_MEMBER_COUNT }
        ) {
            return
        }
        val (keptFolders, dissolvedFolders) =
            prunedFolders.partition { it.memberKeys.size >= MIN_APP_FOLDER_MEMBER_COUNT }
        saveFolders(keptFolders)
        if (dissolvedFolders.isEmpty()) return
        val updatedOrder =
            dissolvedFolders.fold(userPreferences.getPinnedAppGridOrder()) { order, folder ->
                val restoredGridKeys =
                    folder.memberKeys.filter(::pinMember).mapNotNull(::memberKeyToGridKey)
                orderAfterDeletingFolder(order, folder.gridKey, restoredGridKeys)
            }
        commitOrder(updatedOrder)
    }

    private fun saveFolders(folders: List<AppFolder>) {
        updateFeatureState { it.copy(appFolders = folders) }
        userPreferences.setAppFolders(folders)
    }

    private fun unpinMember(memberKey: String) {
        findApp(memberKey)?.let(unpinApp)
        findShortcut(memberKey)?.let(unpinShortcut)
    }

    /** Pins the member back as its own grid tile. Returns false when it is no longer available. */
    private fun pinMember(memberKey: String): Boolean {
        findApp(memberKey)?.let {
            pinApp(it)
            return true
        }
        findShortcut(memberKey)?.let {
            pinShortcut(it)
            return true
        }
        return false
    }

    private fun findApp(memberKey: String): AppInfo? =
        if (isShortcutMemberKey(memberKey)) {
            null
        } else {
            resultsStateProvider().allApps.firstOrNull { appFolderMemberKey(it) == memberKey }
        }

    private fun findShortcut(memberKey: String): StaticShortcut? =
        if (isShortcutMemberKey(memberKey)) {
            resultsStateProvider().allAppShortcuts.firstOrNull { appFolderMemberKey(it) == memberKey }
        } else {
            null
        }

    /** Persists [order] and aligns the pinned app and shortcut orders with it. */
    private fun commitOrder(order: List<String>) {
        val distinctOrder = order.distinct()
        val results = resultsStateProvider()
        val appsByGridKey = results.allApps.associateBy { it.launchCountKey() }
        val shortcutsByGridKey = results.allAppShortcuts.associateBy { shortcutGridKey(it) }
        reorderPinnedAppGrid(
            distinctOrder,
            distinctOrder.mapNotNull { appsByGridKey[it] },
            distinctOrder.mapNotNull { shortcutsByGridKey[it] },
        )
    }

    private fun AppFolder.withoutMembers(keys: List<String>): AppFolder =
        if (memberKeys.none { it in keys }) this else copy(memberKeys = memberKeys - keys.toSet())

    private data class FolderAvailability(
        val folders: List<AppFolder>,
        val apps: List<AppInfo>,
        val shortcuts: List<StaticShortcut>,
        val disabledShortcutIds: Set<String>,
    )
}

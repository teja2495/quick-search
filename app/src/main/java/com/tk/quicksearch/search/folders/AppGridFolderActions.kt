package com.tk.quicksearch.search.folders

/**
 * Folder changes requested from the Pinned tab. `orderKeys` is the Pinned tab's full grid order as
 * displayed when the change was made.
 */
data class AppGridFolderActions(
    val onCreateFolder: (targetKey: String, draggedKey: String, orderKeys: List<String>) -> Unit,
    val onAddToFolder: (folderId: String, draggedKey: String, orderKeys: List<String>) -> Unit,
    val onRemoveFromFolder: (folderId: String, memberKey: String, orderKeys: List<String>) -> Unit,
    /** Takes a member out of the folder and the Pinned tab. */
    val onUnpinFromFolder: (folderId: String, memberKey: String, orderKeys: List<String>) -> Unit,
    val onReorderFolder: (folderId: String, memberKeys: List<String>) -> Unit,
    val onRenameFolder: (folderId: String, name: String) -> Unit,
    val onDeleteFolder: (folderId: String, orderKeys: List<String>) -> Unit,
)

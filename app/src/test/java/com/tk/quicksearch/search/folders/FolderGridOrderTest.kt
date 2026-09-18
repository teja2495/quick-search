package com.tk.quicksearch.search.folders

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderGridOrderTest {

    @Test
    fun creatingFolderTakesTargetPositionAndDropsDraggedTile() {
        assertEquals(
            listOf("a", "folder:f", "c"),
            orderAfterCreatingFolder(listOf("a", "b", "c", "d"), "b", "d", "folder:f"),
        )
    }

    @Test
    fun removedMemberGoesRightAfterFolder() {
        assertEquals(
            listOf("a", "folder:f", "m", "c"),
            orderAfterRemovingMember(listOf("a", "folder:f", "c"), "folder:f", "m", folderRemoved = false),
        )
    }

    @Test
    fun lastRemovedMemberTakesFolderPosition() {
        assertEquals(
            listOf("a", "m", "c"),
            orderAfterRemovingMember(listOf("a", "folder:f", "c"), "folder:f", "m", folderRemoved = true),
        )
    }

    @Test
    fun unavailableMemberIsNotRestored() {
        assertEquals(
            listOf("a", "c"),
            orderAfterRemovingMember(listOf("a", "folder:f", "c"), "folder:f", null, folderRemoved = true),
        )
    }

    @Test
    fun deletedFolderMembersTakeItsPositionInFolderOrder() {
        assertEquals(
            listOf("a", "x", "y", "c"),
            orderAfterDeletingFolder(listOf("a", "folder:f", "c"), "folder:f", listOf("x", "y")),
        )
    }

    @Test
    fun reorderedMembersIgnoreStaleKeysAndKeepUnlistedAtEnd() {
        assertEquals(
            listOf("c", "a", "b"),
            reorderedFolderMembers(listOf("a", "b", "c"), listOf("c", "gone", "a")),
        )
    }
}

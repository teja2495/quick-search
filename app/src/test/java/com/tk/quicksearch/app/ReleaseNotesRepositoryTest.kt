package com.tk.quicksearch.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesRepositoryTest {
    @Test
    fun isVersionBefore_excludesCurrentAndFutureVersions() {
        assertTrue(isVersionBefore("3.9.9", "4.0"))
        assertTrue(isVersionBefore("4.0.1", "4.1"))
        assertFalse(isVersionBefore("4.0", "4.0"))
        assertFalse(isVersionBefore("4.1", "4.0"))
        assertFalse(isVersionBefore("4.10", "4.9"))
    }

    @Test
    fun isMinorReleaseVersion_excludesPatchReleases() {
        assertTrue(isMinorReleaseVersion("5.1"))
        assertTrue(isMinorReleaseVersion("5"))
        assertFalse(isMinorReleaseVersion("5.1.1"))
        assertFalse(isMinorReleaseVersion("5.1-beta"))
    }
}

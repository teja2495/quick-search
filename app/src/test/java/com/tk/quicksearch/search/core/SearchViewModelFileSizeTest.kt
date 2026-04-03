package com.tk.quicksearch.search.core

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchViewModelFileSizeTest {

    @Test
    fun searchViewModelStaysWithinLineBudget() {
        val file = resolveSearchViewModelFile()

        assertTrue("SearchViewModel.kt must exist at ${file.path}", file.exists())

        val lineCount = file.bufferedReader().useLines { lines -> lines.count() }
        assertTrue(
            "SearchViewModel.kt is $lineCount lines; max allowed is 800",
            lineCount <= 800L,
        )
    }

    private fun resolveSearchViewModelFile(): File {
        val userDirPath = System.getProperty("user.dir").orEmpty()
        val userDir = File(userDirPath).absoluteFile.normalize()
        val relativePath = "src/main/java/com/tk/quicksearch/search/core/SearchViewModel.kt"
        val moduleCandidate = File(userDir, relativePath)
        if (moduleCandidate.exists()) return moduleCandidate

        val rootCandidate = File(userDir, "app/$relativePath")
        if (rootCandidate.exists()) return rootCandidate

        return rootCandidate
    }
}

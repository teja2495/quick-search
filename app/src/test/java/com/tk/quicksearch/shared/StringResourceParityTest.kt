package com.tk.quicksearch.shared

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Every translatable string in values/strings.xml must exist in each localized
 * values-* /strings.xml, with the same format arguments, and locales must not carry keys
 * that no longer exist in the base file.
 */
class StringResourceParityTest {
    private val resDir =
        listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }

    private val base = parse(File(resDir, "values/strings.xml"))

    private val localeFiles =
        resDir
            .listFiles { file -> file.isDirectory && file.name.startsWith("values-") }
            .orEmpty()
            .map { File(it, "strings.xml") }
            .filter { it.isFile }
            .sortedBy { it.parentFile.name }

    @Test
    fun `all expected locales are present`() {
        assertEquals(
            "Unexpected set of localized strings.xml files; update EXPECTED_LOCALE_COUNT if a locale was added or removed",
            EXPECTED_LOCALE_COUNT,
            localeFiles.size,
        )
    }

    @Test
    fun `every locale has every translatable key and no stale keys`() {
        val translatableKeys = base.filterValues { it.translatable }.keys
        val problems =
            localeFiles.mapNotNull { file ->
                val locale = parse(file).keys
                val missing = translatableKeys - locale
                val stale = locale - base.keys
                if (missing.isEmpty() && stale.isEmpty()) {
                    null
                } else {
                    buildString {
                        append(file.parentFile.name)
                        if (missing.isNotEmpty()) append("\n  missing: ").append(missing.sorted())
                        if (stale.isNotEmpty()) append("\n  not in values/strings.xml: ").append(stale.sorted())
                    }
                }
            }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `format arguments match the base string`() {
        val problems =
            localeFiles.flatMap { file ->
                parse(file).mapNotNull { (key, entry) ->
                    val expected = base[key]?.formatArgs ?: return@mapNotNull null
                    if (entry.formatArgs == expected) {
                        null
                    } else {
                        "${file.parentFile.name}/$key: expected $expected, found ${entry.formatArgs}"
                    }
                }
            }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private data class Entry(
        val translatable: Boolean,
        val formatArgs: Set<String>,
    )

    private fun parse(file: File): Map<String, Entry> {
        val root =
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
        val entries = mutableMapOf<String, Entry>()
        val children = root.childNodes
        for (i in 0 until children.length) {
            val element = children.item(i) as? Element ?: continue
            if (element.tagName !in RESOURCE_TAGS) continue
            // Plural quantities differ by language, so only plain strings are compared for args.
            val args = if (element.tagName == "string") formatArgs(element.textContent) else emptySet()
            entries["${element.tagName}/${element.getAttribute("name")}"] =
                Entry(translatable = element.getAttribute("translatable") != "false", formatArgs = args)
        }
        return entries
    }

    /** Normalizes `%s`, `%d` and `%1$s` style arguments to `position:conversion`. */
    private fun formatArgs(text: String): Set<String> =
        FORMAT_ARG
            .findAll(text)
            .filter { it.groupValues[2] != "%" }
            .mapIndexed { index, match ->
                val position = match.groupValues[1].ifEmpty { (index + 1).toString() }
                "$position:${match.groupValues[2].lowercase()}"
            }.toSet()

    private companion object {
        const val EXPECTED_LOCALE_COUNT = 16
        val RESOURCE_TAGS = setOf("string", "plurals", "string-array")
        val FORMAT_ARG = Regex("""%(?:(\d+)\$)?[-#+0,(]*\d*(?:\.\d+)?([a-zA-Z%])""")
    }
}

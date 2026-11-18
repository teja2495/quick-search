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
 * that no longer exist in the base file or are marked translatable="false" there. Plurals
 * must have every quantity their language needs. scripts/check_strings.py mirrors this.
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
                val untranslatable = locale intersect (base.keys - translatableKeys)
                if (missing.isEmpty() && stale.isEmpty() && untranslatable.isEmpty()) {
                    null
                } else {
                    buildString {
                        append(file.parentFile.name)
                        if (missing.isNotEmpty()) append("\n  missing: ").append(missing.sorted())
                        if (stale.isNotEmpty()) append("\n  not in values/strings.xml: ").append(stale.sorted())
                        if (untranslatable.isNotEmpty()) {
                            append("\n  translatable=\"false\" in values/strings.xml: ").append(untranslatable.sorted())
                        }
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
                    if (entry.pluralItems.isNotEmpty()) return@mapNotNull null
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

    @Test
    fun `plurals have every quantity their language needs and only base arguments`() {
        val problems = mutableListOf<String>()
        localeFiles
            .map { it.parentFile.name.removePrefix("values-") }
            .filter { it !in PLURAL_QUANTITIES }
            .forEach { problems += "values-$it: add its plural quantities to PLURAL_QUANTITIES" }
        val files = listOf(File(resDir, "values/strings.xml")) + localeFiles
        for (file in files) {
            val folder = file.parentFile.name
            val required = PLURAL_QUANTITIES[folder.removePrefix("values").removePrefix("-").ifEmpty { "en" }].orEmpty()
            for ((key, entry) in parse(file)) {
                if (entry.pluralItems.isEmpty()) continue
                val baseArgs = base[key]?.formatArgs ?: continue
                val missing = required - entry.pluralItems.keys
                if (missing.isNotEmpty()) problems += "$folder/$key: missing quantities ${missing.sorted()}"
                entry.pluralItems.forEach { (quantity, args) ->
                    if (!baseArgs.containsAll(args)) problems += "$folder/$key '$quantity': args ${args - baseArgs} not in base"
                }
                val other = entry.pluralItems["other"]
                if (other != null && other != baseArgs) problems += "$folder/$key 'other': expected $baseArgs, found $other"
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private data class Entry(
        val translatable: Boolean,
        val formatArgs: Set<String>,
        val pluralItems: Map<String, Set<String>> = emptyMap(),
    )

    private fun parse(file: File): Map<String, Entry> {
        val root =
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
        val entries = mutableMapOf<String, Entry>()
        val children = root.childNodes
        for (i in 0 until children.length) {
            val element = children.item(i) as? Element ?: continue
            if (element.tagName !in RESOURCE_TAGS) continue
            val pluralItems =
                if (element.tagName == "plurals") {
                    val items = element.getElementsByTagName("item")
                    (0 until items.length)
                        .map { items.item(it) as Element }
                        .associate { it.getAttribute("quantity") to formatArgs(it.textContent) }
                } else {
                    emptyMap()
                }
            // A plural's arguments are the union of its items'; string-arrays aren't compared.
            val args =
                when (element.tagName) {
                    "string" -> formatArgs(element.textContent)
                    "plurals" -> pluralItems.values.flatten().toSet()
                    else -> emptySet()
                }
            entries["${element.tagName}/${element.getAttribute("name")}"] =
                Entry(
                    translatable = element.getAttribute("translatable") != "false",
                    formatArgs = args,
                    pluralItems = pluralItems,
                )
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

        /** Quantities each locale's grammar needs for integer counts (CLDR plural rules). */
        val PLURAL_QUANTITIES =
            mapOf(
                "en" to setOf("one", "other"),
                "ar" to setOf("zero", "one", "two", "few", "many", "other"),
                "de" to setOf("one", "other"),
                "el" to setOf("one", "other"),
                "es" to setOf("one", "other"),
                "fr" to setOf("one", "other"),
                "hi" to setOf("one", "other"),
                "in" to setOf("other"),
                "it" to setOf("one", "other"),
                "ja" to setOf("other"),
                "nl" to setOf("one", "other"),
                "pl" to setOf("one", "few", "many", "other"),
                "pt-rBR" to setOf("one", "other"),
                "ru" to setOf("one", "few", "many", "other"),
                "te" to setOf("one", "other"),
                "tr" to setOf("one", "other"),
                "zh-rCN" to setOf("other"),
            )
        val FORMAT_ARG = Regex("""%(?:(\d+)\$)?[-#+0,(]*\d*(?:\.\d+)?([a-zA-Z%])""")
    }
}

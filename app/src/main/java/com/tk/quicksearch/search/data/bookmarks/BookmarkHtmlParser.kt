package com.tk.quicksearch.search.data.bookmarks

/** A website read from a bookmarks export, before it is saved. */
data class ImportedBookmark(
    val name: String,
    val url: String,
    /** Base64 image data from the export's ICON attribute, when it has one. */
    val iconBase64: String? = null,
)

/**
 * Reads the standard bookmarks HTML export (the Netscape bookmark format written by Chrome, Firefox,
 * Edge, Safari and others). Only http(s) links are kept. Folders are ignored, so every link lands in
 * one list, and a link that appears more than once (say, in two folders) is kept once.
 */
object BookmarkHtmlParser {
    private val anchorRegex =
        Regex("<a\\s([^>]*)>(.*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val attributeRegex =
        Regex("([a-zA-Z_:-]+)\\s*=\\s*(\"([^\"]*)\"|'([^']*)')")
    private val tagRegex = Regex("<[^>]*>")
    private val whitespaceRegex = Regex("\\s+")
    private val numericEntityRegex = Regex("&#(x[0-9a-fA-F]+|[0-9]+);")
    private val namedEntities =
        mapOf("&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"", "&apos;" to "'", "&nbsp;" to " ")

    fun parse(html: String): List<ImportedBookmark> =
        dedupe(parseAll(html))

    /** Keeps the first copy of each link, borrowing an icon from a later copy when it has none. */
    private fun dedupe(items: List<ImportedBookmark>): List<ImportedBookmark> {
        val byKey = LinkedHashMap<String, ImportedBookmark>()
        items.forEach { item ->
            val key = BookmarkUrls.dedupeKey(item.url)
            val kept = byKey[key]
            if (kept == null) {
                byKey[key] = item
            } else if (kept.iconBase64 == null && item.iconBase64 != null) {
                byKey[key] = kept.copy(iconBase64 = item.iconBase64)
            }
        }
        return byKey.values.toList()
    }

    private fun parseAll(html: String): List<ImportedBookmark> =
        anchorRegex.findAll(html).mapNotNull { match ->
            val attributes =
                attributeRegex.findAll(match.groupValues[1]).associate { attribute ->
                    attribute.groupValues[1].lowercase() to
                        (attribute.groups[3]?.value ?: attribute.groups[4]?.value.orEmpty())
                }
            val url = decodeEntities(attributes["href"].orEmpty()).trim()
            if (!BookmarkUrls.isWebUrl(url)) return@mapNotNull null
            val title =
                decodeEntities(match.groupValues[2].replace(tagRegex, ""))
                    .replace(whitespaceRegex, " ")
                    .trim()
            ImportedBookmark(
                name = title.ifBlank { BookmarkUrls.displayHost(url) },
                url = url,
                iconBase64 = attributes["icon"]?.let(::base64ImageData),
            )
        }.toList()

    internal fun decodeEntities(text: String): String {
        if (!text.contains('&')) return text
        var decoded =
            numericEntityRegex.replace(text) { match ->
                val value = match.groupValues[1]
                val codePoint =
                    if (value.startsWith("x") || value.startsWith("X")) {
                        value.substring(1).toIntOrNull(16)
                    } else {
                        value.toIntOrNull()
                    }
                codePoint?.takeIf { Character.isValidCodePoint(it) }
                    ?.let { String(Character.toChars(it)) }
                    ?: match.value
            }
        // &amp; last so "&amp;lt;" stays "&lt;".
        namedEntities.filterKeys { it != "&amp;" }.forEach { (entity, value) ->
            decoded = decoded.replace(entity, value)
        }
        return decoded.replace("&amp;", "&")
    }

    private fun base64ImageData(value: String): String? {
        if (!value.startsWith("data:image/", ignoreCase = true)) return null
        val commaIndex = value.indexOf(',')
        if (commaIndex < 0 || !value.substring(0, commaIndex).endsWith(";base64", ignoreCase = true)) {
            return null
        }
        return value.substring(commaIndex + 1).trim().takeIf { it.isNotEmpty() }
    }
}

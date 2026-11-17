package com.tk.quicksearch.tools.aiTools

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.tools.aiSearch.trackCancellation
import com.tk.quicksearch.tools.aiSearch.withCancellableHttp
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

// Free English dictionary (Wiktionary data); no API key needed.
private const val DICTIONARY_API_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/"
private const val MAX_SYNONYMS = 5
private const val MAX_PARTS_OF_SPEECH = 3
private const val MAX_DEFINITIONS_PER_PART = 2

class DictionaryHandler(
        private val context: Context,
) {
    suspend fun define(confirmed: ConfirmedDictionaryQuery): Result<DictionaryModelResult> =
            withCancellableHttp {
                runCatching {
                    val term = confirmed.term.trim()
                    var connection: HttpURLConnection? = null
                    try {
                        val encodedTerm = URLEncoder.encode(term.lowercase(Locale.ROOT), "UTF-8").replace("+", "%20")
                        connection = (URL(DICTIONARY_API_URL + encodedTerm).openConnection() as HttpURLConnection).trackCancellation().apply {
                            connectTimeout = 10_000
                            readTimeout = 10_000
                            requestMethod = "GET"
                            setRequestProperty("Accept", "application/json")
                        }
                        val responseCode = runCatching { connection.responseCode }.getOrElse { error(unavailableMessage()) }
                        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                            error(context.getString(R.string.dictionary_definition_not_found, term))
                        }
                        if (responseCode !in 200..299) error(unavailableMessage())
                        val body = runCatching { connection.inputStream.bufferedReader().use { it.readText() } }
                                .getOrElse { error(unavailableMessage()) }
                        parseDictionaryEntries(body) ?: error(unavailableMessage())
                    } finally {
                        connection?.disconnect()
                    }
                }
            }

    private fun unavailableMessage(): String = context.getString(R.string.dictionary_unavailable)
}

/**
 * Merges every entry for the word, grouping definitions by part of speech in API order.
 * Returns null when the body isn't a usable entry list (e.g. a Cloudflare challenge page).
 */
internal fun parseDictionaryEntries(body: String): DictionaryModelResult? = runCatching {
    val entries = JSONArray(body).objects()
    val word = entries.map { it.optString("word").trim() }.firstOrNull { it.isNotBlank() }
            ?: return@runCatching null

    val definitionsByPart = linkedMapOf<String, MutableList<DictionaryDefinition>>()
    val synonymsByPart = linkedMapOf<String, MutableList<String>>()
    for (entry in entries) {
        if (!entry.optString("word").trim().equals(word, ignoreCase = true)) continue
        val meanings = entry.optJSONArray("meanings")?.objects() ?: continue
        for (meaning in meanings) {
            val part = meaning.optString("partOfSpeech").trim()
            val definitions = meaning.optJSONArray("definitions")?.objects()
                    ?.filter { it.optString("definition").isNotBlank() }
                    ?: continue
            if (definitions.isEmpty()) continue
            definitionsByPart.getOrPut(part) { mutableListOf() } += definitions.map {
                DictionaryDefinition(
                        text = it.optString("definition").trim(),
                        example = it.optString("example").trim(),
                )
            }
            synonymsByPart.getOrPut(part) { mutableListOf() } +=
                    definitions.flatMap { it.stringList("synonyms") } + meaning.stringList("synonyms")
        }
    }
    if (definitionsByPart.isEmpty()) return@runCatching null

    DictionaryModelResult(
            word = word,
            senses = definitionsByPart.entries.take(MAX_PARTS_OF_SPEECH).map { (part, definitions) ->
                DictionarySense(
                        partOfSpeech = part,
                        definitions = definitions.distinctBy { it.text }.take(MAX_DEFINITIONS_PER_PART),
                        synonyms = synonymsByPart[part].orEmpty().distinct().take(MAX_SYNONYMS),
                )
            },
    )
}.getOrNull()

private fun JSONArray.objects(): List<JSONObject> = List(length()) { optJSONObject(it) }.filterNotNull()

private fun JSONObject.stringList(key: String): List<String> =
        optJSONArray(key)?.let { array -> List(array.length()) { array.optString(it).trim() } }
                ?.filter { it.isNotBlank() }
                .orEmpty()

data class DictionaryModelResult(
        val word: String,
        val senses: List<DictionarySense>,
)

data class DictionarySense(
        val partOfSpeech: String,
        val definitions: List<DictionaryDefinition>,
        val synonyms: List<String>,
)

data class DictionaryDefinition(
        val text: String,
        val example: String,
)

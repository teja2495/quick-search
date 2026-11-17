package com.tk.quicksearch.tools.aiSearch

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-wide memory of the last live model catalog per provider, also kept on disk so model
 * pickers show the previous list immediately while a fresh one loads in the background.
 */
object LlmModelCatalogCache {
    private const val PREFS_NAME = "llm_model_catalog_cache"
    const val STARTUP_MAX_AGE_MS = 6L * 60 * 60 * 1_000
    const val SETTINGS_MAX_AGE_MS = 10L * 60 * 1_000
    private const val TIMESTAMP_PREFIX = "fetched_at/"
    private const val KEY_ID = "id"
    private const val KEY_DISPLAY_NAME = "name"
    private const val KEY_SYSTEM_INSTRUCTIONS = "system"
    private const val KEY_GROUNDING = "grounding"

    private val catalogs = ConcurrentHashMap<AiSearchLlmProviderId, List<LlmTextModel>>()

    private val fetchedAt = ConcurrentHashMap<AiSearchLlmProviderId, Long>()
    private val refreshLocks = ConcurrentHashMap<AiSearchLlmProviderId, Mutex>()

    @Volatile private var restored = false

    fun get(providerId: AiSearchLlmProviderId): List<LlmTextModel>? = catalogs[providerId]

    fun snapshot(): Map<AiSearchLlmProviderId, List<LlmTextModel>> = catalogs.toMap()

    /** Loads catalogs saved by earlier sessions. Reads disk once per process; call off the main thread. */
    fun restore(context: Context) {
        if (restored) return
        synchronized(this) {
            if (restored) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.all.forEach { (key, value) ->
                if (key.startsWith(TIMESTAMP_PREFIX)) {
                    (value as? Long)?.let { fetchedAt[AiSearchLlmProviderId(key.removePrefix(TIMESTAMP_PREFIX))] = it }
                    return@forEach
                }
                val models = (value as? String)?.let(::decode).orEmpty()
                if (models.isNotEmpty()) catalogs.putIfAbsent(AiSearchLlmProviderId(key), models)
            }
            restored = true
        }
    }

    fun put(
        context: Context,
        providerId: AiSearchLlmProviderId,
        models: List<LlmTextModel>,
    ) {
        if (models.isEmpty()) return
        val now = System.currentTimeMillis()
        catalogs[providerId] = models
        fetchedAt[providerId] = now
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(providerId.storageValue, encode(models))
            .putLong(TIMESTAMP_PREFIX + providerId.storageValue, now)
            .apply()
    }

    /** Serializes refreshes across surfaces; failed requests retain the last successful catalog. */
    suspend fun fetchIfNeeded(
        context: Context,
        providerId: AiSearchLlmProviderId,
        maxAgeMs: Long,
        fetch: suspend () -> Result<List<LlmTextModel>>,
    ): Result<List<LlmTextModel>> {
        restore(context)
        return refreshLocks.getOrPut(providerId) { Mutex() }.withLock {
            val age = System.currentTimeMillis() - (fetchedAt[providerId] ?: 0L)
            val cached = catalogs[providerId]
            if (cached != null && age >= 0 && age < maxAgeMs) {
                Result.success(cached)
            } else {
                val result = fetch()
                val failure = result.exceptionOrNull()
                if (failure is CancellationException) throw failure
                result.onSuccess { put(context, providerId, it) }
            }
        }
    }

    private fun encode(models: List<LlmTextModel>): String =
        JSONArray(
            models.map { model ->
                JSONObject()
                    .put(KEY_ID, model.id)
                    .put(KEY_DISPLAY_NAME, model.displayName)
                    .put(KEY_SYSTEM_INSTRUCTIONS, model.supportsSystemInstructions)
                    .put(KEY_GROUNDING, model.supportsGrounding)
            },
        ).toString()

    private fun decode(raw: String): List<LlmTextModel> =
        runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optString(KEY_ID).takeIf { it.isNotBlank() } ?: return@mapNotNull null
                LlmTextModel(
                    id = id,
                    displayName = item.optString(KEY_DISPLAY_NAME).ifBlank { id },
                    supportsSystemInstructions = item.optBoolean(KEY_SYSTEM_INSTRUCTIONS, true),
                    supportsGrounding = item.optBoolean(KEY_GROUNDING, true),
                )
            }
        }.getOrDefault(emptyList())
}

package com.tk.quicksearch.search.data.preferences

import android.content.Context
import android.util.Log
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.CustomLlmProviderConfig
import com.tk.quicksearch.tools.aiSearch.OpenAiModelCatalog
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Preferences for user-defined OpenAI-compatible LLM providers. */
class CustomLlmProviderPreferences(
    context: Context,
) : BasePreferences(context) {
    fun getProviders(): List<CustomLlmProviderConfig> {
        val securePrefs =
            encryptedPrefs ?: run {
                Log.e(TAG, "EncryptedSharedPreferences unavailable; custom LLM providers not loaded")
                return emptyList()
            }
        val raw = securePrefs.getString(BasePreferences.KEY_CUSTOM_LLM_PROVIDERS, null).orEmpty()
        if (raw.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString(FIELD_ID).takeIf { it.isNotBlank() } ?: continue
                    val baseUrl = item.optString(FIELD_BASE_URL).takeIf { it.isNotBlank() } ?: continue
                    val apiKey = item.optString(FIELD_API_KEY).takeIf { it.isNotBlank() } ?: continue
                    val modelId = item.optString(FIELD_MODEL_ID).takeIf { it.isNotBlank() } ?: continue
                    val advancedPayload = item.optString(FIELD_ADVANCED_PAYLOAD).takeIf { it.isNotBlank() }
                    val advancedPayloadEnabled = item.optBoolean(FIELD_ADVANCED_PAYLOAD_ENABLED, false)
                    add(
                        CustomLlmProviderConfig(
                            id = id,
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            modelId = modelId,
                            advancedPayload = advancedPayload,
                            advancedPayloadEnabled = advancedPayloadEnabled,
                        ),
                    )
                }
            }
        }.getOrElse {
            Log.e(TAG, "Failed to parse custom LLM providers", it)
            emptyList()
        }
    }

    fun getProvider(providerId: AiSearchLlmProviderId): CustomLlmProviderConfig? =
        providerId.customId?.let { customId ->
            getProviders().firstOrNull { it.id == customId }
        }

    fun addProvider(
        baseUrl: String,
        apiKey: String,
    ): CustomLlmProviderConfig? {
        val securePrefs =
            encryptedPrefs ?: run {
                Log.e(TAG, "EncryptedSharedPreferences unavailable; custom LLM provider not persisted")
                return null
            }
        val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
        val normalizedApiKey = apiKey.trim()
        if (normalizedBaseUrl.isBlank() || normalizedApiKey.isBlank()) {
            return null
        }

        val provider =
            CustomLlmProviderConfig(
                id = UUID.randomUUID().toString(),
                baseUrl = normalizedBaseUrl,
                apiKey = normalizedApiKey,
                modelId = OpenAiModelCatalog.DEFAULT_MODEL_ID,
            )
        val updated = getProviders() + provider
        securePrefs.edit().putString(BasePreferences.KEY_CUSTOM_LLM_PROVIDERS, encode(updated)).apply()
        return provider
    }

    fun removeProvider(providerId: AiSearchLlmProviderId) {
        val securePrefs =
            encryptedPrefs ?: run {
                Log.e(TAG, "EncryptedSharedPreferences unavailable; custom LLM provider not removed")
                return
            }
        val customId = providerId.customId ?: return
        val updated = getProviders().filterNot { it.id == customId }
        if (updated.isEmpty()) {
            securePrefs.edit().remove(BasePreferences.KEY_CUSTOM_LLM_PROVIDERS).apply()
        } else {
            securePrefs.edit().putString(BasePreferences.KEY_CUSTOM_LLM_PROVIDERS, encode(updated)).apply()
        }
    }

    fun setProviderModel(
        providerId: AiSearchLlmProviderId,
        modelId: String?,
    ) {
        val securePrefs =
            encryptedPrefs ?: run {
                Log.e(TAG, "EncryptedSharedPreferences unavailable; custom LLM provider model not persisted")
                return
            }
        val customId = providerId.customId ?: return
        val normalizedModelId = modelId?.trim().takeUnless { it.isNullOrBlank() } ?: return
        val updated =
            getProviders().map { provider ->
                if (provider.id == customId) {
                    provider.copy(modelId = normalizedModelId)
                } else {
                    provider
                }
            }
        securePrefs.edit().putString(BasePreferences.KEY_CUSTOM_LLM_PROVIDERS, encode(updated)).apply()
    }

    fun setProviderAdvancedPayload(
        providerId: AiSearchLlmProviderId,
        payload: String?,
        enabled: Boolean,
    ) {
        val securePrefs =
            encryptedPrefs ?: run {
                Log.e(TAG, "EncryptedSharedPreferences unavailable; custom LLM provider payload not persisted")
                return
            }
        val customId = providerId.customId ?: return
        val normalizedPayload = payload?.trim().takeUnless { it.isNullOrBlank() }
        val updated =
            getProviders().map { provider ->
                if (provider.id == customId) {
                    provider.copy(
                        advancedPayload = normalizedPayload,
                        advancedPayloadEnabled = enabled && normalizedPayload != null,
                    )
                } else {
                    provider
                }
            }
        securePrefs.edit().putString(BasePreferences.KEY_CUSTOM_LLM_PROVIDERS, encode(updated)).apply()
    }

    private fun encode(providers: List<CustomLlmProviderConfig>): String {
        val array = JSONArray()
        providers.forEach { provider ->
            array.put(
                JSONObject()
                    .put(FIELD_ID, provider.id)
                    .put(FIELD_BASE_URL, provider.baseUrl)
                    .put(FIELD_API_KEY, provider.apiKey)
                    .put(FIELD_MODEL_ID, provider.modelId)
                    .put(FIELD_ADVANCED_PAYLOAD, provider.advancedPayload.orEmpty())
                    .put(FIELD_ADVANCED_PAYLOAD_ENABLED, provider.advancedPayloadEnabled),
            )
        }
        return array.toString()
    }

    private companion object {
        const val TAG = "CustomLlmProviderPrefs"
        const val FIELD_ID = "id"
        const val FIELD_BASE_URL = "baseUrl"
        const val FIELD_API_KEY = "apiKey"
        const val FIELD_MODEL_ID = "modelId"
        const val FIELD_ADVANCED_PAYLOAD = "advancedPayload"
        const val FIELD_ADVANCED_PAYLOAD_ENABLED = "advancedPayloadEnabled"
    }
}

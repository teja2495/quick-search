package com.tk.quicksearch.tools.aiSearch

import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Prompt and native-search flag for one request after the Tavily setting is applied. */
data class PreparedWebSearch(
    val prompt: String,
    val useNativeSearch: Boolean,
)

/** Whether the provider can run its own web search as part of an answer request. */
fun providerSupportsNativeSearch(providerId: AiSearchLlmProviderId): Boolean =
    AiSearchLlmProviderRegistry.definition(providerId)?.supportsNativeSearch ?: false

/** Whether this provider/model exposes a user-controllable thinking mode. */
fun supportsThinkingControl(
    providerId: AiSearchLlmProviderId,
    modelId: String,
): Boolean {
    val definition = AiSearchLlmProviderRegistry.definition(providerId) ?: return false
    return modelId.isNotBlank() && definition.supportsThinkingRequest && definition.supportsModelThinking(modelId)
}

/** Whether a request to this provider may carry the thinking flag at all. */
fun isThinkingRequestSupported(providerId: AiSearchLlmProviderId): Boolean =
    AiSearchLlmProviderRegistry.definition(providerId)?.supportsThinkingRequest ?: false

/** Whether web search can be requested for this provider/model combination. */
fun isWebSearchAvailable(
    providerId: AiSearchLlmProviderId,
    modelSupportsGrounding: Boolean,
    hasTavilyKey: Boolean,
): Boolean =
    hasTavilyKey ||
        (providerSupportsNativeSearch(providerId) && modelSupportsGrounding)

/** Whether [modelId] can ground answers itself, including when no live catalog entry is available. */
fun modelSupportsGrounding(
    modelId: String,
    models: List<LlmTextModel>,
    providerId: AiSearchLlmProviderId,
): Boolean =
    modelId.isNotBlank() &&
        (models.firstOrNull { it.id == modelId }?.supportsGrounding
            ?: AiSearchLlmProviderRegistry.definition(providerId)?.supportsGroundingWithoutCatalog?.invoke(modelId)
            ?: false)

/**
 * Applies the Tavily web search setting to one request. If Tavily fails, the prompt is sent
 * unchanged and the model's own web search is used when it is available and turned on.
 *
 * @param onTavilyFailure invoked when the Tavily call fails, so callers can surface a toast.
 */
suspend fun prepareWebSearch(
    userPreferences: UserAppPreferences,
    searchQuery: String,
    prompt: String,
    nativeSearchSupported: Boolean,
    nativeSearchRequested: Boolean,
    onTavilyFailure: () -> Unit = {},
): PreparedWebSearch {
    val (mode, tavilyApiKey) =
        withContext(Dispatchers.IO) {
            userPreferences.getTavilyWebSearchMode() to userPreferences.getTavilyApiKey()
        }
    val plan =
        resolveWebSearchPlan(
            mode = mode,
            hasTavilyKey = !tavilyApiKey.isNullOrBlank(),
            nativeSearchSupported = nativeSearchSupported,
            nativeSearchRequested = nativeSearchRequested,
        )
    if (!plan.useTavily || tavilyApiKey == null) {
        return PreparedWebSearch(prompt = prompt, useNativeSearch = plan.useNativeSearch)
    }
    return TavilyClient(tavilyApiKey)
        .search(searchQuery)
        .fold(
            onSuccess = { results ->
                PreparedWebSearch(
                    prompt = TavilyClient.buildPromptWithResults(prompt, results),
                    useNativeSearch = false,
                )
            },
            onFailure = { error ->
                if (error is CancellationException) throw error
                onTavilyFailure()
                PreparedWebSearch(
                    prompt = prompt,
                    useNativeSearch = nativeSearchSupported && nativeSearchRequested,
                )
            },
        )
}

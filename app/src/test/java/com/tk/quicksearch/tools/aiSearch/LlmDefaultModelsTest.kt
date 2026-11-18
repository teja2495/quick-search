package com.tk.quicksearch.tools.aiSearch

import org.junit.Assert.assertEquals
import org.junit.Test

class LlmDefaultModelsTest {
    private fun models(vararg ids: String) = ids.map { LlmTextModel(it, it) }

    @Test
    fun geminiPicksFlashLatest() {
        val catalog = models("gemini-2.5-flash", "gemini-3.8-flash", "gemini-flash-latest", "gemini-pro-latest")
        assertEquals("gemini-flash-latest", LlmDefaultModels.pick(AiSearchLlmProviderId.GEMINI, catalog))
    }

    @Test
    fun metaPicksLatestMuseSparkWithoutVariantSuffix() {
        val catalog =
            models(
                "muse-spark-1.1",
                "muse-spark-1.2",
                "muse-spark-1.2-contributor",
                "muse-spark-1.3",
                "muse-spark-1.3-contributor",
            )
        assertEquals("muse-spark-1.3", LlmDefaultModels.pick(AiSearchLlmProviderId.META, catalog))
    }

    @Test
    fun anthropicPicksLatestSonnet() {
        val catalog =
            models(
                "claude-fable-5-1",
                "claude-opus-5-5",
                "claude-sonnet-4-5-20250929",
                "claude-sonnet-4-6",
                "claude-sonnet-5",
                "claude-sonnet-5-5",
            )
        assertEquals("claude-sonnet-5-5", LlmDefaultModels.pick(AiSearchLlmProviderId.ANTHROPIC, catalog))
    }

    @Test
    fun openAiPicksLatestLuna() {
        val catalog = models("gpt-5.5", "gpt-5.6-luna", "gpt-5.6-sol", "gpt-6-astra", "gpt-6-luna", "gpt-6-sol")
        assertEquals("gpt-6-luna", LlmDefaultModels.pick(AiSearchLlmProviderId.OPENAI, catalog))
    }

    @Test
    fun missingFamilyFallsBackToFirstModelAndEmptyCatalogToBlank() {
        assertEquals("gpt-5", LlmDefaultModels.pick(AiSearchLlmProviderId.OPENAI, models("gpt-5", "gpt-5-mini")))
        assertEquals("", LlmDefaultModels.pick(AiSearchLlmProviderId.ANTHROPIC, emptyList()))
    }

    @Test
    fun validSelectionIsKeptAndMissingSelectionGetsDefault() {
        val catalog = models("muse-spark-1.2", "muse-spark-1.3")
        assertEquals(
            "muse-spark-1.2",
            resolveModelSelectionOrDefault(AiSearchLlmProviderId.META, "muse-spark-1.2", catalog),
        )
        assertEquals(
            "muse-spark-1.3",
            resolveModelSelectionOrDefault(AiSearchLlmProviderId.META, "retired", catalog),
        )
    }
}

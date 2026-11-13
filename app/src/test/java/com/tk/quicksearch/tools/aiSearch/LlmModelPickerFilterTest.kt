package com.tk.quicksearch.tools.aiSearch

import org.junit.Assert.assertEquals
import org.junit.Test

class LlmModelPickerFilterTest {
    private fun models(vararg ids: String) = ids.map { LlmTextModel(it, it) }

    private fun List<LlmTextModel>.ids() = map { it.id }.toSet()

    @Test
    fun geminiKeepsAliasesLatestGemmaAndNewestFlashProFlashLite() {
        val catalog =
            models(
                "gemini-2.5-flash",
                "gemini-2.5-flash-lite",
                "gemini-2.5-pro",
                "gemini-3-flash-preview",
                "gemini-3.1-flash-lite",
                "gemini-3.1-flash-lite-preview",
                "gemini-3.1-pro-preview",
                "gemini-3.1-pro-preview-customtools",
                "gemini-3.5-flash",
                "gemini-3.5-flash-lite",
                "gemini-3.8-flash",
                "gemini-3.8-live",
                "gemini-omni-flash-preview",
                "gemini-flash-latest",
                "gemini-flash-lite-latest",
                "gemini-pro-latest",
                "gemma-3-27b-it",
                "gemma-3n-e4b-it",
                "gemma-4-26b-a4b-it",
                "gemma-4-31b-it",
            )
        assertEquals(
            setOf(
                "gemini-flash-latest",
                "gemini-flash-lite-latest",
                "gemini-pro-latest",
                "gemma-4-26b-a4b-it",
                "gemma-4-31b-it",
                "gemini-3.8-flash",
                "gemini-3.5-flash-lite",
                "gemini-3.1-pro-preview",
            ),
            GeminiModelCatalog.pickerModels(catalog).ids(),
        )
    }

    @Test
    fun geminiKeepsLatestOfHydrogenCarbonArgon() {
        val catalog =
            models(
                "gemini-flash-latest",
                "gemini-3-hydrogen",
                "gemini-3.5-hydrogen-preview",
                "gemini-3.5-hydrogen",
                "gemini-3-carbon-preview",
                "gemini-2.5-argon",
                "gemini-3.1-argon",
                "gemini-3.1-neon",
            )
        assertEquals(
            setOf(
                "gemini-flash-latest",
                "gemini-3.5-hydrogen",
                "gemini-3-carbon-preview",
                "gemini-3.1-argon",
            ),
            GeminiModelCatalog.pickerModels(catalog).ids(),
        )
    }

    @Test
    fun openAiKeepsLatestOfLunaSolAstra() {
        val catalog =
            models("gpt-5", "gpt-5.5", "gpt-5.5-pro", "gpt-5.6-luna", "gpt-5.6-sol", "gpt-5.6-terra", "gpt-6-astra", "gpt-6-luna", "gpt-6-sol")
        assertEquals(
            setOf("gpt-6-luna", "gpt-6-sol", "gpt-6-astra"),
            OpenAiModelCatalog.pickerModels(catalog).ids(),
        )
    }

    @Test
    fun anthropicKeepsLatestOfEachFamily() {
        val catalog =
            models(
                "claude-fable-5",
                "claude-fable-5-1",
                "claude-haiku-4-5-20251001",
                "claude-opus-4-5-20251101",
                "claude-opus-4-8",
                "claude-opus-5",
                "claude-opus-5-5",
                "claude-sonnet-4-5-20250929",
                "claude-sonnet-5",
                "claude-sonnet-5-5",
            )
        assertEquals(
            setOf("claude-fable-5-1", "claude-haiku-4-5-20251001", "claude-opus-5-5", "claude-sonnet-5-5"),
            AnthropicModelCatalog.pickerModels(catalog).ids(),
        )
    }

    @Test
    fun metaKeepsLatestRegularAndContributor() {
        val catalog =
            models("muse-spark-1.1", "muse-spark-1.2", "muse-spark-1.2-contributor", "muse-spark-1.3", "muse-spark-1.3-contributor")
        assertEquals(
            setOf("muse-spark-1.3", "muse-spark-1.3-contributor"),
            MetaModelCatalog.pickerModels(catalog).ids(),
        )
    }

    @Test
    fun unrecognizedCatalogIsKeptUnfiltered() {
        val catalog = models("gpt-5", "gpt-5-mini")
        assertEquals(catalog, OpenAiModelCatalog.pickerModels(catalog))
    }
}

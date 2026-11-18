package com.tk.quicksearch.tools.aiSearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenCodeModelCatalogTest {
    @Test
    fun modelRefsRoundTripThroughTheirCatalog() {
        assertEquals(OpenCodeTier.GO to "kimi-k3", OpenCodeModelCatalog.parseModelRef("opencode-go/kimi-k3"))
        assertEquals(OpenCodeTier.ZEN to "big-pickle", OpenCodeModelCatalog.parseModelRef("opencode/big-pickle"))
        assertEquals(OpenCodeTier.ZEN to "glm-5.3", OpenCodeModelCatalog.parseModelRef("glm-5.3"))

        val model = OpenCodeModelCatalog.textModel(OpenCodeTier.GO, "kimi-k3")
        assertEquals("opencode-go/kimi-k3", model.id)
    }

    @Test
    fun routesModelsToTheirDocumentedEndpointFormat() {
        fun format(tier: OpenCodeTier, id: String) = OpenCodeModelCatalog.apiFormatFor(tier, id)

        assertEquals(OpenCodeApiFormat.MESSAGES, format(OpenCodeTier.ZEN, "claude-sonnet-5"))
        assertEquals(OpenCodeApiFormat.MESSAGES, format(OpenCodeTier.GO, "minimax-m3"))
        assertEquals(OpenCodeApiFormat.RESPONSES, format(OpenCodeTier.ZEN, "gpt-5.5"))
        assertEquals(OpenCodeApiFormat.RESPONSES, format(OpenCodeTier.GO, "grok-4.7"))
        assertEquals(OpenCodeApiFormat.RESPONSES, format(OpenCodeTier.GO, "muse-spark-1.3-contributor"))
        assertEquals(OpenCodeApiFormat.CHAT_COMPLETIONS, format(OpenCodeTier.ZEN, "grok-code"))
        assertEquals(OpenCodeApiFormat.CHAT_COMPLETIONS, format(OpenCodeTier.GO, "kimi-k3"))
        assertNull(format(OpenCodeTier.ZEN, "gemini-3-pro"))
        assertNull(format(OpenCodeTier.ZEN, "jev-1.13"))
    }

    @Test
    fun sameModelCanUseDifferentFormatsPerCatalog() {
        assertEquals(OpenCodeApiFormat.CHAT_COMPLETIONS, OpenCodeModelCatalog.apiFormatFor(OpenCodeTier.ZEN, "qwen3.8-max"))
        assertEquals(OpenCodeApiFormat.MESSAGES, OpenCodeModelCatalog.apiFormatFor(OpenCodeTier.GO, "qwen3.8-max"))
    }

    @Test
    fun detectsOpenCodeApiKeyShapeBeforeOpenAi() {
        val openCodeKey = "sk-" + "aB3".repeat(21) + "x"
        assertEquals(AiSearchLlmProviderId.OPENCODE, AiSearchLlmProviderId.detectFromApiKey(openCodeKey))
        assertEquals(AiSearchLlmProviderId.OPENAI, AiSearchLlmProviderId.detectFromApiKey("sk-proj-abc_def-123"))
    }
}

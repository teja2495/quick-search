package com.tk.quicksearch.tools.aiSearch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmFeatureAvailabilityTest {
    @Test
    fun thinkingControlIsShownOnlyForProvidersThatCanToggleIt() {
        assertTrue(supportsThinkingControl(AiSearchLlmProviderId.GEMINI, "gemini-flash"))
        assertTrue(supportsThinkingControl(AiSearchLlmProviderId.ANTHROPIC, "claude-sonnet"))
        assertTrue(supportsThinkingControl(AiSearchLlmProviderId.GROQ, "openai/gpt-oss-120b"))
        assertTrue(supportsThinkingControl(AiSearchLlmProviderId.GROQ, "qwen/qwen3-32b"))
        assertFalse(supportsThinkingControl(AiSearchLlmProviderId.GROQ, "llama-3.3-70b"))
        assertTrue(supportsThinkingControl(AiSearchLlmProviderId.META, "muse-spark"))
        assertFalse(supportsThinkingControl(AiSearchLlmProviderId.OPENAI, "gpt-5"))
        assertFalse(supportsThinkingControl(AiSearchLlmProviderId.custom("local"), "local"))
    }

    @Test
    fun webSearchRequiresNativeSupportOrTavily() {
        assertTrue(isWebSearchAvailable(AiSearchLlmProviderId.GEMINI, true, false))
        assertTrue(isWebSearchAvailable(AiSearchLlmProviderId.OPENAI, true, false))
        assertFalse(isWebSearchAvailable(AiSearchLlmProviderId.GEMINI, false, false))
        assertFalse(isWebSearchAvailable(AiSearchLlmProviderId.GROQ, false, false))
        assertTrue(isWebSearchAvailable(AiSearchLlmProviderId.GROQ, false, true))
        assertTrue(
            isWebSearchAvailable(
                AiSearchLlmProviderId.custom("local"),
                false,
                true,
            ),
        )
    }

    @Test
    fun missingCatalogEntryUsesProviderSpecificWebSearchCapability() {
        assertFalse(modelSupportsGrounding("gpt-5", emptyList(), AiSearchLlmProviderId.OPENAI))
        assertTrue(
            modelSupportsGrounding(
                "gpt-4o",
                emptyList(),
                AiSearchLlmProviderId.OPENAI,
            ),
        )
        assertFalse(modelSupportsGrounding("anything", emptyList(), AiSearchLlmProviderId.GROQ))
        assertFalse(
            modelSupportsGrounding(
                "gemma-3-27b-it",
                emptyList(),
                AiSearchLlmProviderId.GEMINI,
            ),
        )
    }
}

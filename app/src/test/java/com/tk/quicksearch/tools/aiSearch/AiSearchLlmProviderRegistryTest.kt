package com.tk.quicksearch.tools.aiSearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSearchLlmProviderRegistryTest {
    @Test
    fun eachBuiltInIdHasAnAdapterAndRoundTripsThroughStorage() {
        for (definition in AiSearchLlmProviderRegistry.definitions) {
            val id = definition.id
            assertEquals(id, AiSearchLlmProviderId.fromStorageValue(id.storageValue))
            val adapter = AiSearchLlmProviderRegistry.builtIn(id)
            assertEquals(id, adapter.id)
            assertTrue(adapter.defaultModelId.isNotBlank())
            assertTrue(adapter.fallbackTextModels.isNotEmpty())
        }
    }

    @Test
    fun existingProvidersRetainTheirIdsAndPreferenceNames() {
        val legacy = listOf(
            AiSearchLlmProviderId.GEMINI to "gemini",
            AiSearchLlmProviderId.OPENAI to "openai",
            AiSearchLlmProviderId.ANTHROPIC to "anthropic",
            AiSearchLlmProviderId.GROQ to "groq",
            AiSearchLlmProviderId.META to "meta",
            AiSearchLlmProviderId.OPENCODE to "opencode",
        )
        for ((id, storedId) in legacy) {
            val definition = requireNotNull(AiSearchLlmProviderRegistry.definition(id))
            assertEquals(storedId, id.storageValue)
            assertEquals("${storedId}_api_key", definition.apiKeyPreference)
            assertEquals("${storedId}_personal_context", definition.personalContextPreference)
            assertEquals("${storedId}_model", definition.modelPreference)
            assertEquals("${storedId}_grounding_enabled", definition.groundingPreference)
            assertEquals("${storedId}_thinking_enabled", definition.thinkingPreference)
        }
    }

    @Test
    fun keyDetectionChecksSpecificSignaturesBeforeOpenAi() {
        assertEquals(AiSearchLlmProviderId.ANTHROPIC, AiSearchLlmProviderId.detectFromApiKey(" sk-ant-test "))
        assertEquals(AiSearchLlmProviderId.OPENCODE, AiSearchLlmProviderId.detectFromApiKey("sk-" + "a".repeat(64)))
        assertEquals(AiSearchLlmProviderId.OPENAI, AiSearchLlmProviderId.detectFromApiKey("sk-test"))
        assertEquals(AiSearchLlmProviderId.GROQ, AiSearchLlmProviderId.detectFromApiKey("gsk_test"))
        assertEquals(AiSearchLlmProviderId.META, AiSearchLlmProviderId.detectFromApiKey("LLM|test"))
        assertEquals(AiSearchLlmProviderId.GEMINI, AiSearchLlmProviderId.detectFromApiKey("unknown-prefix"))
    }

    @Test
    fun nativeSearchAndThinkingReflectProviderAndModelCapabilities() {
        assertTrue(providerSupportsNativeSearch(AiSearchLlmProviderId.OPENAI))
        assertFalse(providerSupportsNativeSearch(AiSearchLlmProviderId.OPENCODE))
        assertTrue(supportsThinkingControl(AiSearchLlmProviderId.GROQ, "openai/gpt-oss-120b"))
        assertFalse(supportsThinkingControl(AiSearchLlmProviderId.GROQ, "llama-3.3-70b"))
        assertFalse(supportsThinkingControl(AiSearchLlmProviderId.OPENAI, "gpt-6-luna"))
        assertFalse(modelSupportsGrounding("gemma-4", emptyList(), AiSearchLlmProviderId.GEMINI))
    }
}

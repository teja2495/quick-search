package com.tk.quicksearch.tools.aiSearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetaModelCatalogTest {
    @Test
    fun identifiesOnlyMuseSparkTextModels() {
        assertTrue(MetaModelCatalog.isTextModel("muse-spark-1.2"))
        assertTrue(MetaModelCatalog.isTextModel("MUSE-SPARK-1.2-CONTRIBUTOR"))
        assertFalse(MetaModelCatalog.isTextModel("muse-image-1.0"))
        assertFalse(MetaModelCatalog.isTextModel("other-model"))
    }

    @Test
    fun detectsMetaApiKeyPrefix() {
        assertEquals(
            AiSearchLlmProviderId.META,
            AiSearchLlmProviderId.detectFromApiKey("LLM|607358788850350|example"),
        )
    }
}

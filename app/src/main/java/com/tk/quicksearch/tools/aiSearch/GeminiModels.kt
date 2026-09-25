package com.tk.quicksearch.tools.aiSearch

/** Shared Gemini model configuration defaults. */
object GeminiModelCatalog {
        const val DEFAULT_MODEL_ID = "gemini-flash-latest"
        const val DEFAULT_GROUNDING_ENABLED = true

        /**
         * Fallback list used when the model catalog cannot be fetched from the API. This list is
         * text-focused and excludes image/audio-only variants.
         */
        val FALLBACK_TEXT_MODELS: List<LlmTextModel> =
                listOf(
                                LlmTextModel(
                                        id = DEFAULT_MODEL_ID,
                                        displayName = "Gemini Flash Latest",
                                ),
                                LlmTextModel(
                                        id = "gemini-flash-lite-latest",
                                        displayName = "Gemini Flash Lite Latest",
                                ),
                                LlmTextModel(
                                        id = "gemini-pro-latest",
                                        displayName = "Gemini Pro Latest",
                                ),
                        )
                        .distinctBy { it.id }
                        .filter { isLikelyTextModel(it.id) }

        /** Heuristic filter for text-first Gemini models. */
        fun isLikelyTextModel(modelId: String): Boolean {
                val lowerId = modelId.lowercase()
                if (!lowerId.startsWith("gemini-") && !lowerId.startsWith("gemma-")) return false

                val nonTextMarkers =
                        listOf(
                                "image",
                                "tts",
                                "native-audio",
                                "realtime",
                                "embedding",
                                "aqa",
                                "-exp",
                                "-001",
                                "robotics",
                                "computer-use",
                        )
                return nonTextMarkers.none(lowerId::contains)
        }
}

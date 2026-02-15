package com.tk.quicksearch.search.directSearch

/** Gemini model option for UI selection and request routing. */
data class GeminiTextModel(
        val id: String,
        val displayName: String,
        val supportsSystemInstructions: Boolean = true,
        val supportsGrounding: Boolean = true,
)

/** Shared Gemini model configuration defaults. */
object GeminiModelCatalog {
        const val DEFAULT_MODEL_ID = "gemini-2.5-flash"
        const val DEFAULT_GROUNDING_ENABLED = true

        /**
         * Fallback list used when the model catalog cannot be fetched from the API. This list is
         * text-focused and excludes image/audio-only variants.
         */
        val FALLBACK_TEXT_MODELS: List<GeminiTextModel> =
                listOf(
                                GeminiTextModel(
                                        id = DEFAULT_MODEL_ID,
                                        displayName = "Gemini 2.5 Flash",
                                ),
                                GeminiTextModel(
                                        id = "gemini-2.5-pro",
                                        displayName = "Gemini 2.5 Pro",
                                ),
                                GeminiTextModel(
                                        id = "gemma-2-9b-it",
                                        displayName = "Gemma 2 9B",
                                        supportsSystemInstructions = false,
                                        supportsGrounding = false,
                                ),
                                GeminiTextModel(
                                        id = "gemma-2-27b-it",
                                        displayName = "Gemma 2 27B",
                                        supportsSystemInstructions = false,
                                        supportsGrounding = false,
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
                                "preview",
                        )
                return nonTextMarkers.none(lowerId::contains)
        }
}

package com.tk.quicksearch.tools.aiSearch

/** Backward-compatible alias while the UI/state still references Gemini naming. */
typealias GeminiTextModel = LlmTextModel

/** Shared Gemini model configuration defaults. */
object GeminiModelCatalog {
        const val DEFAULT_MODEL_ID = "gemini-flash-latest"
        const val DEFAULT_GROUNDING_ENABLED = true

        /**
         * Fallback list used when the model catalog cannot be fetched from the API. This list is
         * text-focused and excludes image/audio-only variants.
         */
        val FALLBACK_TEXT_MODELS: List<GeminiTextModel> =
                listOf(
                                GeminiTextModel(
                                        id = DEFAULT_MODEL_ID,
                                        displayName = "Gemini Flash Latest",
                                ),
                                GeminiTextModel(
                                        id = "gemini-flash-lite-latest",
                                        displayName = "Gemini Flash Lite Latest",
                                ),
                                GeminiTextModel(
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

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

        private val pickerAliasIds =
                setOf("gemini-flash-latest", "gemini-pro-latest", "gemini-flash-lite-latest")
        private val versionedFamilyRegex =
                Regex(
                        "^gemini-(\\d+(?:\\.\\d+)*)-(flash-lite|flash|pro|hydrogen|carbon|argon)(?:-preview)?$"
                )
        private val gemmaVersionRegex = Regex("^gemma-(\\d+(?:\\.\\d+)*)-")

        /**
         * Picker list: the `-latest` aliases, every model of the newest Gemma generation, and the
         * newest versioned Flash, Pro, Flash Lite, Hydrogen, Carbon, and Argon.
         */
        fun pickerModels(models: List<LlmTextModel>): List<LlmTextModel> {
                val latestGemmaIds =
                        LlmModelVersions.allOfLatestVersion(models) { id ->
                                gemmaVersionRegex.find(id)?.groupValues?.get(1)?.split('.')?.map {
                                        it.toIntOrNull() ?: return@allOfLatestVersion null
                                }
                        }
                return LlmModelVersions.filterLatest(
                        models,
                        alwaysKeep = { id -> id in pickerAliasIds || id in latestGemmaIds },
                ) { id ->
                        versionedFamilyRegex.find(id)?.groupValues?.let {
                                LlmModelVersions.familyVersion(it[2], it[1].split('.'))
                        }
                }
        }

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

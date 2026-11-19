package com.tk.quicksearch.tools.aiSearch

/** Shared Meta Model API configuration defaults. */
object MetaModelCatalog {
    const val DEFAULT_MODEL_ID = "muse-spark-1.2"
    const val DEFAULT_GROUNDING_ENABLED = true

    val FALLBACK_TEXT_MODELS: List<LlmTextModel> =
        listOf(
            LlmTextModel(id = "muse-spark-1.2", displayName = "Muse Spark 1.2"),
            LlmTextModel(id = "muse-spark-1.1", displayName = "Muse Spark 1.1"),
            LlmTextModel(
                id = "muse-spark-1.2-contributor",
                displayName = "Muse Spark 1.2 Contributor",
            ),
        )

    private val museSparkRegex = Regex("^muse-spark-(\\d+(?:\\.\\d+)*)(-contributor)?$")

    /** Picker list: the latest Muse Spark version, regular and contributor. */
    fun pickerModels(models: List<LlmTextModel>): List<LlmTextModel> =
        LlmModelVersions.filterLatest(models) { id ->
            museSparkRegex.find(id)?.groupValues?.let {
                LlmModelVersions.familyVersion(it[2].ifEmpty { "regular" }, it[1].split('.'))
            }
        }

    fun isTextModel(modelId: String): Boolean =
        modelId.trim().startsWith("muse-spark-", ignoreCase = true)

    fun displayNameFor(modelId: String): String =
        modelId
            .trim()
            .replace('-', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                when {
                    part.equals("muse", ignoreCase = true) -> "Muse"
                    part.equals("spark", ignoreCase = true) -> "Spark"
                    else -> part.replaceFirstChar { char -> char.uppercaseChar() }
                }
            }
}

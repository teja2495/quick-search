package com.tk.quicksearch.tools.aiSearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.DictionaryState
import com.tk.quicksearch.search.core.DictionaryStatus
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.util.Locale

@Composable
fun DictionaryResult(
        dictionaryState: DictionaryState,
        llmProviderId: AiSearchLlmProviderId = AiSearchLlmProviderId.GEMINI,
        showWallpaperBackground: Boolean = false,
        onGeminiModelInfoClick: () -> Unit = {},
) {
    if (dictionaryState.status == DictionaryStatus.Idle) return

    val showAttribution =
            dictionaryState.status == DictionaryStatus.Success &&
                    !dictionaryState.meaning.isNullOrBlank()

    val copyText =
            if (dictionaryState.status == DictionaryStatus.Success) {
                dictionaryState.copyText()
            } else {
                null
            }

    GeminiResultCard(
            showWallpaperBackground = showWallpaperBackground,
            showAttribution = showAttribution,
            usedModelId = dictionaryState.usedModelId,
            llmProviderId = llmProviderId,
            isAttributionClickable = true,
            onGeminiModelInfoClick = onGeminiModelInfoClick,
            copyText = copyText,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                when (dictionaryState.status) {
                    DictionaryStatus.Loading -> {
                        GeminiLoadingAnimation()
                    }
                    DictionaryStatus.Success -> {
                        val word = dictionaryState.word.orEmpty().capitalizeFirstCharacter()
                        val part = dictionaryState.partOfSpeech.orEmpty()
                        val meaning = dictionaryState.meaning.orEmpty()
                        val example = dictionaryState.example.orEmpty()
                        val synonyms =
                                if (dictionaryState.synonyms.isEmpty()) {
                                    ""
                                } else {
                                    dictionaryState.synonyms.joinToString(", ")
                                }
                        Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement =
                                        Arrangement.spacedBy(DesignTokens.SpacingXXSmall),
                        ) {
                            Text(
                                    text = if (part.isBlank()) word else "$word ($part)",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                            )
                            HorizontalDivider(
                                    modifier =
                                            Modifier.padding(vertical = DesignTokens.SpacingXXSmall),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            Text(
                                    text = meaning,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (example.isNotBlank()) {
                                Text(
                                        text =
                                                "\n${stringResource(R.string.dictionary_example_label, example)}\n",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (synonyms.isNotBlank()) {
                                Text(
                                        text =
                                                stringResource(
                                                        R.string.dictionary_synonyms_label,
                                                        synonyms,
                                                ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    DictionaryStatus.Error -> {
                        Text(
                                text = dictionaryState.errorMessage
                                        ?: stringResource(R.string.direct_search_error_generic),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                        )
                    }
                    DictionaryStatus.Idle -> {}
                }
            }
        }
    }
}

private fun String.capitalizeFirstCharacter(): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

private fun DictionaryState.copyText(): String {
    val word = word.orEmpty().capitalizeFirstCharacter()
    val part = partOfSpeech.orEmpty()
    val meaning = meaning.orEmpty()
    val example = example.orEmpty()
    val synonyms = synonyms.joinToString(", ")

    return buildString {
        append(word)
        if (part.isNotBlank()) append(" ($part)")
        append('\n')
        append(meaning)
        if (example.isNotBlank()) append("\n\nExample: $example\n")
        if (synonyms.isNotBlank()) append("\nSynonyms: $synonyms")
    }
}

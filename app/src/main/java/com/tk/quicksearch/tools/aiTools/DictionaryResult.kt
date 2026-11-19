package com.tk.quicksearch.tools.aiTools

import com.tk.quicksearch.shared.ui.theme.AppColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.DictionaryState
import com.tk.quicksearch.search.core.DictionaryStatus
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.aiSearch.InformationCardLoadingAnimation
import com.tk.quicksearch.tools.aiSearch.GeminiResultCard
import com.tk.quicksearch.tools.aiSearch.ToolAttribution
import java.util.Locale

@Composable
fun DictionaryResult(
        dictionaryState: DictionaryState,
        showWallpaperBackground: Boolean = false,
        onSearchOnGoogle: (String) -> Unit = {},
) {
    if (dictionaryState.status == DictionaryStatus.Idle) return

    val copyText =
            if (dictionaryState.status == DictionaryStatus.Success) {
                dictionaryState.copyText()
            } else {
                null
            }

    GeminiResultCard(
            showWallpaperBackground = showWallpaperBackground,
            showAttribution = false,
            usedModelId = null,
            copyText = copyText,
            toolAttribution =
                    ToolAttribution(MaterialSymbols.MenuBookOutlined, R.string.dictionary_toggle_title)
                            .takeIf { dictionaryState.status == DictionaryStatus.Success },
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                when (dictionaryState.status) {
                    DictionaryStatus.Loading -> {
                        InformationCardLoadingAnimation()
                    }
                    DictionaryStatus.Success -> {
                        DictionaryEntryContent(dictionaryState)
                    }
                    DictionaryStatus.Error -> {
                        Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
                        ) {
                            Text(
                                    text = dictionaryState.errorMessage
                                            ?: stringResource(R.string.direct_search_error_generic),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineBreak = LineBreak.Heading),
                                    color = MaterialTheme.colorScheme.error,
                            )
                            dictionaryState.activeQuery?.takeIf { it.isNotBlank() }?.let { query ->
                                SearchOnGoogleButton(
                                        text = stringResource(R.string.dictionary_search_on_google),
                                        onClick = { onSearchOnGoogle(query) },
                                )
                            }
                        }
                    }
                    DictionaryStatus.Idle -> {}
                }
            }
        }
    }
}

@Composable
private fun DictionaryEntryContent(dictionaryState: DictionaryState) {
    val word = dictionaryState.word.orEmpty().capitalizeFirstCharacter()
    Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
    ) {
        Text(
                text = word,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
        )
        HorizontalDivider(
                modifier = Modifier.padding(vertical = DesignTokens.SpacingXSmall),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppColors.ContentFaintAlpha),
        )
        Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium)) {
            dictionaryState.senses.forEach { sense -> DictionarySenseContent(sense) }
        }
    }
}

@Composable
private fun DictionarySenseContent(sense: DictionarySense) {
    val numbered = sense.definitions.size > 1
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall)) {
        if (sense.partOfSpeech.isNotBlank()) {
            Text(
                    text = sense.partOfSpeech,
                    style = MaterialTheme.typography.labelLarge.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.primary,
            )
        }
        sense.definitions.forEachIndexed { index, definition ->
            Text(
                    text = if (numbered) "${index + 1}. ${definition.text}" else definition.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
            )
            if (definition.example.isNotBlank()) {
                Text(
                        text = stringResource(R.string.dictionary_example_label, definition.example),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (sense.synonyms.isNotEmpty()) {
            Text(
                    text = stringResource(R.string.dictionary_synonyms_label, sense.synonyms.joinToString(", ")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun String.capitalizeFirstCharacter(): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

private fun DictionaryState.copyText(): String =
        buildString {
            append(word.orEmpty().capitalizeFirstCharacter())
            senses.forEach { sense ->
                append("\n\n")
                if (sense.partOfSpeech.isNotBlank()) append(sense.partOfSpeech).append('\n')
                sense.definitions.forEachIndexed { index, definition ->
                    if (index > 0) append('\n')
                    if (sense.definitions.size > 1) append("${index + 1}. ")
                    append(definition.text)
                    if (definition.example.isNotBlank()) append("\n   Example: ${definition.example}")
                }
                if (sense.synonyms.isNotEmpty()) append("\nSynonyms: ${sense.synonyms.joinToString(", ")}")
            }
        }

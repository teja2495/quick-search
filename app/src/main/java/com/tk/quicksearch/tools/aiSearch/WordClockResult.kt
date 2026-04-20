package com.tk.quicksearch.tools.aiSearch

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.WordClockState
import com.tk.quicksearch.search.core.WordClockStatus
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun WordClockResult(
        wordClockState: WordClockState,
        showWallpaperBackground: Boolean = false,
        onGeminiModelInfoClick: () -> Unit = {},
) {
    if (wordClockState.status == WordClockStatus.Idle) return

    val showAttribution =
            wordClockState.status == WordClockStatus.Success &&
                    !wordClockState.wordClockText.isNullOrBlank()

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    GeminiResultCard(
            showWallpaperBackground = showWallpaperBackground,
            showAttribution = showAttribution,
            usedModelId = wordClockState.usedModelId,
            isAttributionClickable = true,
            onGeminiModelInfoClick = onGeminiModelInfoClick,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                when (wordClockState.status) {
                    WordClockStatus.Loading -> {
                        GeminiLoadingAnimation()
                    }
                    WordClockStatus.Success -> {
                        val line1 = wordClockState.wordClockText.orEmpty()
                        val placeLabel = wordClockState.placeText?.trim().orEmpty()
                        val timeZoneLabel = wordClockState.timeZoneText?.trim().orEmpty()
                        Column(
                                modifier =
                                        Modifier.fillMaxWidth().pointerInput(line1) {
                                            detectTapGestures(
                                                    onLongPress = {
                                                        clipboardManager.setText(
                                                                AnnotatedString(line1)
                                                        )
                                                    },
                                            )
                                        },
                        ) {
                            Text(
                                    text = line1,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                            )
                            wordClockState.sourceTimeText?.takeIf { it.isNotBlank() }?.let { source ->
                                Text(
                                        text = source,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (placeLabel.isNotBlank() || timeZoneLabel.isNotBlank()) {
                                Column(
                                        modifier =
                                                Modifier.padding(top = DesignTokens.SpacingSmall)
                                                        .fillMaxWidth(),
                                        verticalArrangement =
                                                Arrangement.spacedBy(DesignTokens.SpacingXXSmall),
                                ) {
                                    placeLabel.takeIf { it.isNotBlank() }?.let { place ->
                                        Text(
                                                text = place,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    timeZoneLabel.takeIf { it.isNotBlank() }?.let { timezone ->
                                        Text(
                                                text = timezone,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    WordClockStatus.Error -> {
                        Text(
                                text = wordClockState.errorMessage
                                        ?: stringResource(R.string.direct_search_error_generic),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                        )
                    }
                    WordClockStatus.Idle -> {}
                }
            }
        }
    }
}

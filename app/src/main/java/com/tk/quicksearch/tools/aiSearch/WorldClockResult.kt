package com.tk.quicksearch.tools.aiSearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.WorldClockState
import com.tk.quicksearch.search.core.WorldClockStatus
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun WorldClockResult(
        worldClockState: WorldClockState,
        llmProviderId: AiSearchLlmProviderId = AiSearchLlmProviderId.GEMINI,
        showWallpaperBackground: Boolean = false,
        onGeminiModelInfoClick: () -> Unit = {},
) {
    if (worldClockState.status == WorldClockStatus.Idle) return

    val showAttribution =
            worldClockState.status == WorldClockStatus.Success &&
                    !worldClockState.worldClockText.isNullOrBlank()

    val copyText =
            if (worldClockState.status == WorldClockStatus.Success) {
                worldClockState.worldClockText
            } else {
                null
            }

    GeminiResultCard(
            showWallpaperBackground = showWallpaperBackground,
            showAttribution = showAttribution,
            usedModelId = worldClockState.usedModelId,
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
                when (worldClockState.status) {
                    WorldClockStatus.Loading -> {
                        GeminiLoadingAnimation()
                    }
                    WorldClockStatus.Success -> {
                        val line1 = worldClockState.worldClockText.orEmpty()
                        val placeLabel = worldClockState.placeText?.trim().orEmpty()
                        val timeZoneLabel = worldClockState.timeZoneText?.trim().orEmpty()
                        Column(
                                modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                    text = line1,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                            )
                            worldClockState.sourceTimeText?.takeIf { it.isNotBlank() }?.let { source ->
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
                    WorldClockStatus.Error -> {
                        Text(
                                text = worldClockState.errorMessage
                                        ?: stringResource(R.string.direct_search_error_generic),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                        )
                    }
                    WorldClockStatus.Idle -> {}
                }
            }
        }
    }
}

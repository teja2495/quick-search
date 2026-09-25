package com.tk.quicksearch.tools.aiTools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.WeatherState
import com.tk.quicksearch.search.core.WeatherStatus
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.GeminiLoadingAnimation
import com.tk.quicksearch.tools.aiSearch.GeminiResultCard

@Composable
fun WeatherResult(
    weatherState: WeatherState,
    llmProviderId: AiSearchLlmProviderId = AiSearchLlmProviderId.GEMINI,
    showWallpaperBackground: Boolean = false,
    onGeminiModelInfoClick: () -> Unit = {},
) {
    if (weatherState.status == WeatherStatus.Idle) return

    GeminiResultCard(
        showWallpaperBackground = showWallpaperBackground,
        showAttribution = weatherState.status == WeatherStatus.Success,
        usedModelId = weatherState.usedModelId,
        llmProviderId = llmProviderId,
        isAttributionClickable = true,
        onGeminiModelInfoClick = onGeminiModelInfoClick,
        copyText = weatherState.summary.takeIf { weatherState.status == WeatherStatus.Success },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            when (weatherState.status) {
                WeatherStatus.Loading -> GeminiLoadingAnimation()
                WeatherStatus.Success -> Text(
                    text = weatherState.summary.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                WeatherStatus.Error -> Text(
                    text = weatherState.errorMessage
                        ?: stringResource(R.string.direct_search_error_generic),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                WeatherStatus.Idle -> Unit
            }
        }
    }
}

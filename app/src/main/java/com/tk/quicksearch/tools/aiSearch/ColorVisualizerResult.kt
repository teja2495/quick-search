package com.tk.quicksearch.tools.aiSearch

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CalculatorState
import com.tk.quicksearch.search.searchScreen.shared.InformationCard
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ColorVisualizerResult(
    calculatorState: CalculatorState,
    showWallpaperBackground: Boolean,
) {
    val clipboardManager = LocalClipboardManager.current
    val color = calculatorState.colorArgb?.let(::Color)
    InformationCard(
        modifier =
            Modifier.fillMaxWidth().combinedClickable(
                onClick = {},
                onLongClick = {
                    calculatorState.result?.let { clipboardManager.setText(AnnotatedString(it)) }
                },
            ),
        showWallpaperBackground = showWallpaperBackground,
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth().padding(
                    start = DesignTokens.SpacingLarge,
                    top = DesignTokens.SpacingMedium,
                    end = DesignTokens.SpacingLarge,
                    bottom = DesignTokens.SpacingLarge,
                ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            if (color == null) {
                Text(
                    text = stringResource(R.string.color_visualizer_invalid_input),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.color_visualizer_toggle_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = calculatorState.result.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                androidx.compose.foundation.layout.Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(112.dp)
                            .background(color, DesignTokens.ShapeLarge),
                )
            }
        }
    }
}

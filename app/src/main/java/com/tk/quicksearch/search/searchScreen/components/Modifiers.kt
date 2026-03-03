package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.DesignTokens

internal fun Modifier.predictedSubmitHighlight(
    isPredicted: Boolean,
    shape: Shape = DesignTokens.CardShape,
): Modifier =
    composed {
        if (!isPredicted) {
            this
        } else {
            this
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = shape,
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    shape = shape,
                )
        }
    }

internal fun Modifier.predictedSubmitCardBorder(
    isPredicted: Boolean,
    shape: Shape = DesignTokens.CardShape,
): Modifier =
    composed {
        if (!isPredicted) {
            this
        } else {
            this.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                shape = shape,
            )
        }
    }
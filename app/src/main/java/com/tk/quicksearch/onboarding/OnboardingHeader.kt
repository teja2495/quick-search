package com.tk.quicksearch.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun OnboardingHeader(
    title: String,
    modifier: Modifier = Modifier,
    topPadding: Dp = DesignTokens.Spacing40,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = DesignTokens.SpacingSmall)
            .semantics { heading() },
    )
}

package com.tk.quicksearch.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun OnboardingHeader(
    title: String,
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = DesignTokens.Spacing40, bottom = DesignTokens.SpacingSmall),
    )
}

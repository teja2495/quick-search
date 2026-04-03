package com.tk.quicksearch.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.settings.searchEnginesScreen.SearchEngines
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import kotlinx.coroutines.delay

/**
 * Setup screen for configuring search engines during first launch.
 * Shows search engine settings with a continue button.
 */
@Composable
fun SearchEngineSetupScreen(
    onContinue: () -> Unit,
    viewModel: SearchViewModel,
    currentStep: Int,
    totalSteps: Int,
    continueButtonTextRes: Int = R.string.setup_action_next,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreenBackground(
        appTheme = AppTheme.MONOCHROME,
        overlayThemeIntensity = 0.5f,
        modifier = modifier,
    ) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = DesignTokens.OnboardingHorizontalPadding),
        horizontalAlignment = Alignment.Start,
    ) {
        OnboardingHeader(
            title = stringResource(R.string.settings_app_shortcuts_filter_search_engines),
            currentStep = currentStep,
            totalSteps = totalSteps,
        )

        Text(
            text = stringResource(R.string.setup_search_engines_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
        )

        Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))

        val allEngines = uiState.searchTargetsOrder

        val scrollState = rememberScrollState()
        val hasAnimatedScroll = remember { mutableStateOf(false) }

        var disabledEngines by remember {
            mutableStateOf<Set<String>>(uiState.disabledSearchTargetIds)
        }

        LaunchedEffect(uiState.disabledSearchTargetIds) {
            disabledEngines = uiState.disabledSearchTargetIds
        }

        // Auto-scroll animation to indicate list is scrollable
        LaunchedEffect(Unit) {
            if (!hasAnimatedScroll.value) {
                delay(800) // Wait a bit for the UI to settle
                // Check if user hasn't scrolled yet before triggering animation
                if (scrollState.value == 0) {
                    scrollState.animateScrollTo(150) // Scroll down a bit
                    delay(600) // Pause to let user see it's scrollable
                    // Only scroll back if user hasn't scrolled during the pause
                    if (scrollState.value == 150) {
                        scrollState.animateScrollTo(0) // Scroll back to top
                    }
                }
                hasAnimatedScroll.value = true
            }
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
            horizontalAlignment = Alignment.Start,
        ) {
            SearchEngines(
                searchEngineOrder = allEngines,
                disabledSearchEngines = disabledEngines,
                onToggleSearchEngine = { engine, enabled ->
                    val targetId = engine.getId()
                    disabledEngines =
                        if (enabled) {
                            disabledEngines.minus(targetId)
                        } else {
                            disabledEngines.plus(targetId)
                        }
                    viewModel.setSearchTargetEnabled(engine, enabled)
                },
                onReorderSearchEngines = { newOrder ->
                    viewModel.reorderSearchTargets(newOrder)
                },
                showTitle = false, // We have our own title
                showAddSearchEngineButton = false,
                geminiModel = uiState.geminiModel,
                geminiGroundingEnabled = uiState.geminiGroundingEnabled,
                availableGeminiModels = uiState.availableGeminiModels,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))
        }

        Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))

        Button(
            onClick = onContinue,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.OnboardingButtonOuterHorizontalPadding),
            shape = RoundedCornerShape(DesignTokens.OnboardingButtonCornerRadius),
            contentPadding =
                PaddingValues(
                    horizontal = DesignTokens.OnboardingButtonHorizontalPadding,
                    vertical = DesignTokens.OnboardingButtonVerticalPadding,
                ),
        ) {
            Text(
                text = stringResource(continueButtonTextRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))
    }
    } // end SettingsScreenBackground
}

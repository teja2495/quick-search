package com.tk.quicksearch.setup

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.searchEngines.SearchEnginesSection
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchViewModel

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
    shouldShowFinalSetup: Boolean = false,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        OnboardingHeader(
            title = stringResource(R.string.setup_search_engines_title),
            currentStep = currentStep,
            totalSteps = totalSteps
        )

        Text(
            text = stringResource(R.string.setup_search_engines_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        val baseDefaultEnabledEngines = setOf(
            SearchEngine.GOOGLE,
            SearchEngine.CHATGPT,
            SearchEngine.PERPLEXITY,
            SearchEngine.GROK,
            SearchEngine.GEMINI,
            SearchEngine.GOOGLE_MAPS,
            SearchEngine.GOOGLE_DRIVE,
            SearchEngine.GOOGLE_PHOTOS,
            SearchEngine.GOOGLE_PLAY,
            SearchEngine.REDDIT,
            SearchEngine.YOUTUBE,
            SearchEngine.SPOTIFY,
            SearchEngine.AMAZON,
            SearchEngine.DUCKDUCKGO
        )

        val defaultEnabledEngines = if (uiState.geminiApiKeyLast4.isNullOrBlank()) {
            baseDefaultEnabledEngines
        } else {
            baseDefaultEnabledEngines + SearchEngine.DIRECT_SEARCH
        }

        val baseEngineOrder = SearchEngine.values().toList()
        val allEngines = if (uiState.geminiApiKeyLast4.isNullOrBlank()) {
            baseEngineOrder.filterNot { it == SearchEngine.DIRECT_SEARCH }
        } else {
            // Move Direct Search to the top when API key is set up
            listOf(SearchEngine.DIRECT_SEARCH) + baseEngineOrder.filterNot { it == SearchEngine.DIRECT_SEARCH }
        }

        val scrollState = rememberScrollState()
        val hasAnimatedScroll = remember { mutableStateOf(false) }

        var disabledEngines by remember { mutableStateOf<Set<SearchEngine>>(uiState.disabledSearchEngines) }

        LaunchedEffect(uiState.disabledSearchEngines) {
            disabledEngines = uiState.disabledSearchEngines
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
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.Start
        ) {
            SearchEnginesSection(
                searchEngineOrder = allEngines,
                disabledSearchEngines = disabledEngines,
                onToggleSearchEngine = { engine, enabled ->
                    // Update local state for immediate UI feedback
                    disabledEngines = if (enabled) {
                        disabledEngines.minus(engine)
                    } else {
                        disabledEngines.plus(engine)
                    }
                    // Also update viewModel to persist the change
                    viewModel.setSearchEngineEnabled(engine, enabled)
                },
                onReorderSearchEngines = { newOrder ->
                    viewModel.reorderSearchEngines(newOrder)
                },
                showTitle = false, // We have our own title
                showRequestSearchEngine = false, // Hide request text in setup
                onSetGeminiApiKey = viewModel::setGeminiApiKey,
                geminiApiKeyLast4 = uiState.geminiApiKeyLast4,
                personalContext = uiState.personalContext,
                onSetPersonalContext = viewModel::setPersonalContext,
                directSearchAvailable = true,
                showDirectSearchAtTop = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(
                    if (shouldShowFinalSetup) {
                        R.string.setup_action_next
                    } else {
                        R.string.setup_action_start
                    }
                ),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

package com.tk.quicksearch.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.util.RenderMarkdownDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val FEATURES_ASSET_FILE_NAME = "FEATURES.md"

@Composable
internal fun FeaturesList(
    scrollState: ScrollState? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val markdown by
        produceState<String?>(initialValue = null, context) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                        context.assets.open(FEATURES_ASSET_FILE_NAME).bufferedReader().use { it.readText() }
                    }.getOrNull()
                }
        }

    if (markdown.isNullOrBlank()) {
        Text(
            text = stringResource(R.string.settings_features_load_failed),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier,
        )
        return
    }

    RenderMarkdownDocument(
        markdown = markdown.orEmpty(),
        scrollState = scrollState,
        modifier = modifier,
    )
}

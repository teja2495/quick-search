package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.TipBanner
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
internal fun InfoBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = DesignTokens.ShapeLarge,
    ) {
        Text(
            text = message,
            modifier =
                Modifier.padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingMedium,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
internal fun PersonalContextHintBanner(
    onOpenPersonalContext: () -> Unit,
    onOpenDirectSearchConfigure: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkText = stringResource(R.string.settings_direct_search_personal_context)
    val fullText = stringResource(R.string.direct_search_personal_context_tip, linkText)
    val linkTag = "personal_context"

    val annotatedText =
        buildAnnotatedString {
            append(fullText)
            val startIndex = fullText.indexOf(linkText)
            if (startIndex >= 0) {
                val endIndex = startIndex + linkText.length
                addStyle(
                    style =
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                        ),
                    start = startIndex,
                    end = endIndex,
                )
                addStringAnnotation(
                    tag = linkTag,
                    annotation = linkText,
                    start = startIndex,
                    end = endIndex,
                )
            }
        }

    TipBanner(
        annotatedText = annotatedText,
        onContentLongClick = onOpenDirectSearchConfigure,
        onTextClick = { offset ->
            val annotations =
                annotatedText.getStringAnnotations(
                    tag = linkTag,
                    start = offset,
                    end = offset,
                )
            if (annotations.isNotEmpty()) {
                onOpenPersonalContext()
            }
        },
        onDismiss = onDismiss,
        modifier = modifier,
    )
}
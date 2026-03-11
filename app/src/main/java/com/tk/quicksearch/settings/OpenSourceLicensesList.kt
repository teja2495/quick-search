package com.tk.quicksearch.settings

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class OpenSourceLicenseEntry(
    val title: String,
    val text: String,
)

@Composable
internal fun OpenSourceLicensesList(
    modifier: Modifier = Modifier,
    selectedEntry: OpenSourceLicenseEntry? = null,
    onSelectedEntryChange: (OpenSourceLicenseEntry?) -> Unit = {},
) {
    val context = LocalContext.current
    val entries by
        produceState<List<OpenSourceLicenseEntry>?>(initialValue = null, context) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching { OpenSourceLicenseParser.load(context.resources) }.getOrNull()
                }
        }

    if (entries == null) {
        Text(
            text = stringResource(R.string.settings_open_source_licenses_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    if (entries.isNullOrEmpty()) {
        Text(
            text = stringResource(R.string.settings_open_source_licenses_load_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
        return
    }

    BackHandler(enabled = selectedEntry != null) {
        onSelectedEntryChange(null)
    }

    if (selectedEntry == null) {
        Column(modifier = modifier) {
            entries!!.forEachIndexed { index, entry ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelectedEntryChange(entry) }
                            .padding(
                                horizontal = DesignTokens.SpacingLarge,
                                vertical = DesignTokens.SpacingMedium,
                            ),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_open_source_licenses_tap_to_view),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (index < entries!!.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        return
    }

    Column(
        modifier =
            modifier
                .padding(horizontal = DesignTokens.SpacingLarge),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        val annotatedLicenseText =
            buildLicenseAnnotatedText(
                text = selectedEntry!!.text,
                linkColor = MaterialTheme.colorScheme.primary,
            )
        Text(
            text = selectedEntry!!.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        ClickableText(
            text = annotatedLicenseText,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            onClick = { offset ->
                val annotation =
                    annotatedLicenseText.getStringAnnotations(
                        tag = URL_TAG,
                        start = offset,
                        end = offset,
                    ).firstOrNull() ?: return@ClickableText
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)),
                    )
                }
            },
        )
    }
}

private object OpenSourceLicenseParser {
    fun load(resources: Resources): List<OpenSourceLicenseEntry> {
        val metadataBytes =
            resources.openRawResource(R.raw.third_party_license_metadata).use { input ->
                input.readBytes()
            }
        val licenseBytes =
            resources.openRawResource(R.raw.third_party_licenses).use { input ->
                input.readBytes()
            }
        val metadata = metadataBytes.decodeToString()
        val entries = mutableListOf<OpenSourceLicenseEntry>()

        metadata
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val firstSpace = line.indexOf(' ')
                if (firstSpace <= 0 || firstSpace >= line.lastIndex) return@forEach
                val range = line.substring(0, firstSpace)
                val title = line.substring(firstSpace + 1).trim()
                val separator = range.indexOf(':')
                if (separator <= 0 || separator >= range.lastIndex) return@forEach

                val offset = range.substring(0, separator).toIntOrNull() ?: return@forEach
                val length = range.substring(separator + 1).toIntOrNull() ?: return@forEach
                val endExclusive = (offset + length).coerceAtMost(licenseBytes.size)
                if (offset < 0 || offset >= endExclusive) return@forEach

                val text = licenseBytes.copyOfRange(offset, endExclusive).decodeToString()
                entries += OpenSourceLicenseEntry(title = title, text = text.trim())
            }

        return entries.sortedBy { it.title.lowercase() }
    }
}

private const val URL_TAG = "url"
private val URL_REGEX = Regex("""https?://[^\s)]+""")

private fun buildLicenseAnnotatedText(
    text: String,
    linkColor: Color,
) =
    buildAnnotatedString {
        var cursor = 0
        URL_REGEX.findAll(text).forEach { match ->
            if (cursor < match.range.first) {
                append(text.substring(cursor, match.range.first))
            }
            val url = match.value
            pushStringAnnotation(tag = URL_TAG, annotation = url)
            withStyle(
                style =
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
            ) {
                append(url)
            }
            pop()
            cursor = match.range.last + 1
        }
        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }

package com.tk.quicksearch.shared.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.launch

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock

    data class Paragraph(val text: String) : MarkdownBlock

    data class UnorderedListItem(val text: String, val indentLevel: Int) : MarkdownBlock

    data class OrderedListItem(val index: Int, val text: String, val indentLevel: Int) : MarkdownBlock

    data object HorizontalRule : MarkdownBlock
}

@Composable
internal fun RenderMarkdownDocument(
    markdown: String,
    scrollState: ScrollState? = null,
    modifier: Modifier = Modifier,
) {
    val blocks = parseMarkdown(markdown)
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val sectionLinks = remember(blocks) { extractSectionLinks(blocks) }
    val sectionScrollTopOffsetPx = remember(density) { with(density) { SECTION_SCROLL_TOP_OFFSET_DP.roundToPx() } }
    var documentTopInWindow by remember { mutableFloatStateOf(0f) }
    val headingTopInWindow = remember(blocks) { mutableStateMapOf<Int, Float>() }
    val headingRequesters =
        remember(blocks) {
            blocks.mapIndexedNotNull { index, block ->
                if (block is MarkdownBlock.Heading) {
                    index to BringIntoViewRequester()
                } else {
                    null
                }
            }.toMap()
        }
    val versionBlockIndex =
        remember(blocks) {
            blocks.indexOfFirst { block ->
                block is MarkdownBlock.Paragraph &&
                    block.text.trimStart().startsWith(VERSION_MARKDOWN_PREFIX)
            }
        }
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary

    Column(
        modifier =
            modifier.onGloballyPositioned { coordinates ->
                documentTopInWindow = coordinates.positionInWindow().y
            },
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val style =
                        when (block.level) {
                            1 -> MaterialTheme.typography.headlineMedium
                            2 -> MaterialTheme.typography.headlineSmall
                            3 -> MaterialTheme.typography.titleLarge
                            else -> MaterialTheme.typography.titleMedium
                        }
                    Text(
                        text = parseInlineMarkdown(block.text, linkColor),
                        style = style,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier =
                            Modifier
                                .bringIntoViewRequester(
                                    headingRequesters.getValue(index),
                                ).onGloballyPositioned { coordinates ->
                                    headingTopInWindow[index] = coordinates.positionInWindow().y
                                }.padding(top = 4.dp),
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    val annotatedText = parseInlineMarkdown(block.text, linkColor)
                    ClickableText(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        onClick = { offset ->
                            annotatedText.getStringAnnotations(
                                tag = LINK_ANNOTATION_TAG,
                                start = offset,
                                end = offset,
                            ).firstOrNull()?.let { annotation ->
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)),
                                    )
                                }
                            }
                        },
                    )
                }

                is MarkdownBlock.UnorderedListItem -> {
                    MarkdownListItem(
                        marker = "\u2022",
                        text = block.text,
                        indentLevel = block.indentLevel,
                        linkColor = linkColor,
                        onLinkClick = { url ->
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                )
                            }
                        },
                    )
                }

                is MarkdownBlock.OrderedListItem -> {
                    MarkdownListItem(
                        marker = "${block.index}.",
                        text = block.text,
                        indentLevel = block.indentLevel,
                        linkColor = linkColor,
                        onLinkClick = { url ->
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                )
                            }
                        },
                    )
                }

                is MarkdownBlock.HorizontalRule -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = DesignTokens.SpacingMedium),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }

            if (index == versionBlockIndex && sectionLinks.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.settings_features_sections_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                ) {
                    sectionLinks.forEach { section ->
                        Text(
                            text = "\u2022 ${section.title}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier
                                    .padding(
                                        start = ((section.level - SECTION_HEADING_LEVEL) * 12)
                                            .coerceAtLeast(0)
                                            .dp,
                                    ).clickable {
                                        if (scrollState != null) {
                                            val headingTop = headingTopInWindow[section.blockIndex]
                                            if (headingTop != null) {
                                                val deltaFromTop = (headingTop - documentTopInWindow).toInt()
                                                val targetScroll =
                                                    (deltaFromTop - sectionScrollTopOffsetPx)
                                                        .coerceAtLeast(0)
                                                coroutineScope.launch {
                                                    scrollState.animateScrollTo(targetScroll)
                                                }
                                            } else {
                                                headingRequesters[section.blockIndex]?.let { requester ->
                                                    coroutineScope.launch { requester.bringIntoView() }
                                                }
                                            }
                                        } else {
                                            headingRequesters[section.blockIndex]?.let { requester ->
                                                coroutineScope.launch { requester.bringIntoView() }
                                            }
                                        }
                                    },
                        )
                    }
                }
            }
        }
    }
}

private data class SectionLink(
    val title: String,
    val blockIndex: Int,
    val level: Int,
)

private fun extractSectionLinks(blocks: List<MarkdownBlock>): List<SectionLink> {
    val versionBlockIndex =
        blocks.indexOfFirst { block ->
            block is MarkdownBlock.Paragraph &&
                block.text.trimStart().startsWith(VERSION_MARKDOWN_PREFIX)
        }

    if (versionBlockIndex == -1) return emptyList()

    return blocks.mapIndexedNotNull { index, block ->
        if (index <= versionBlockIndex || block !is MarkdownBlock.Heading) return@mapIndexedNotNull null
        if (block.level < SECTION_HEADING_LEVEL || block.level > MAX_SECTION_HEADING_LEVEL) {
            return@mapIndexedNotNull null
        }
        SectionLink(
            title = block.text.trim(),
            blockIndex = index,
            level = block.level,
        )
    }
}

private const val VERSION_MARKDOWN_PREFIX = "**Version**"
private const val LINK_ANNOTATION_TAG = "url"
private const val SECTION_HEADING_LEVEL = 3
private const val MAX_SECTION_HEADING_LEVEL = 4
private val SECTION_SCROLL_TOP_OFFSET_DP = 4.dp

@Composable
private fun MarkdownListItem(
    marker: String,
    text: String,
    indentLevel: Int,
    linkColor: Color,
    onLinkClick: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = (indentLevel * 16).dp),
    ) {
        Text(
            text = marker,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = DesignTokens.SpacingMedium),
        )
        val annotatedText = parseInlineMarkdown(text, linkColor)
        ClickableText(
            text = annotatedText,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.weight(1f),
            onClick = { offset ->
                annotatedText.getStringAnnotations(
                    tag = LINK_ANNOTATION_TAG,
                    start = offset,
                    end = offset,
                ).firstOrNull()?.let { annotation ->
                    onLinkClick(annotation.item)
                }
            },
        )
    }
}

private fun parseMarkdown(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraphBuilder = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphBuilder.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(paragraphBuilder.joinToString(" ").trim())
            paragraphBuilder.clear()
        }
    }

    markdown.lines().forEach { rawLine ->
        val line = rawLine.trimEnd()
        if (line.isBlank()) {
            flushParagraph()
            return@forEach
        }

        val trimmed = line.trimStart()
        if (isHorizontalRule(trimmed)) {
            flushParagraph()
            blocks += MarkdownBlock.HorizontalRule
            return@forEach
        }

        val headingMatch = Regex("^(#{1,6})\\s+(.+)$").matchEntire(trimmed)
        if (headingMatch != null) {
            flushParagraph()
            val level = headingMatch.groupValues[1].length
            val text = headingMatch.groupValues[2].trim()
            blocks += MarkdownBlock.Heading(level = level, text = text)
            return@forEach
        }

        val leadingSpaces = line.length - line.trimStart().length
        val indentLevel = (leadingSpaces / 2).coerceAtLeast(0)

        val unorderedListMatch = Regex("^[-*+]\\s+(.+)$").matchEntire(trimmed)
        if (unorderedListMatch != null) {
            flushParagraph()
            blocks += MarkdownBlock.UnorderedListItem(
                text = unorderedListMatch.groupValues[1].trim(),
                indentLevel = indentLevel,
            )
            return@forEach
        }

        val orderedListMatch = Regex("^(\\d+)\\.\\s+(.+)$").matchEntire(trimmed)
        if (orderedListMatch != null) {
            flushParagraph()
            blocks += MarkdownBlock.OrderedListItem(
                index = orderedListMatch.groupValues[1].toIntOrNull() ?: 1,
                text = orderedListMatch.groupValues[2].trim(),
                indentLevel = indentLevel,
            )
            return@forEach
        }

        paragraphBuilder += trimmed
    }

    flushParagraph()
    return blocks
}

private fun isHorizontalRule(line: String): Boolean {
    if (line.length < 3) return false
    val allowed = line.all { it == '-' || it == '*' || it == '_' || it == ' ' }
    if (!allowed) return false
    val withoutSpaces = line.filter { it != ' ' }
    return withoutSpaces.length >= 3 && withoutSpaces.all { it == withoutSpaces.first() }
}

private fun parseInlineMarkdown(text: String, linkColor: Color? = null): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            when {
                text.startsWith("[", index) -> {
                    val closeBracket = text.indexOf("]", index + 1)
                    if (closeBracket > index &&
                        text.startsWith("(", closeBracket + 1)
                    ) {
                        val closeParen = text.indexOf(")", closeBracket + 2)
                        if (closeParen > closeBracket + 1) {
                            val linkText = text.substring(index + 1, closeBracket)
                            val url = text.substring(closeBracket + 2, closeParen)
                            pushStringAnnotation(
                                tag = LINK_ANNOTATION_TAG,
                                annotation = url,
                            )
                            if (linkColor != null) {
                                pushStyle(
                                    SpanStyle(
                                        color = linkColor,
                                        textDecoration = TextDecoration.Underline,
                                    ),
                                )
                            }
                            append(parseInlineMarkdown(linkText, linkColor))
                            if (linkColor != null) pop()
                            pop()
                            index = closeParen + 1
                        } else {
                            append(text[index])
                            index += 1
                        }
                    } else {
                        append(text[index])
                        index += 1
                    }
                }

                text.startsWith("**", index) -> {
                    val end = text.indexOf("**", index + 2)
                    if (end > index + 1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(parseInlineMarkdown(text.substring(index + 2, end), linkColor))
                        pop()
                        index = end + 2
                    } else {
                        append(text[index])
                        index += 1
                    }
                }

                text.startsWith("*", index) -> {
                    val end = text.indexOf("*", index + 1)
                    if (end > index) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(parseInlineMarkdown(text.substring(index + 1, end), linkColor))
                        pop()
                        index = end + 1
                    } else {
                        append(text[index])
                        index += 1
                    }
                }

                text.startsWith("`", index) -> {
                    val end = text.indexOf("`", index + 1)
                    if (end > index) {
                        pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                        append(text.substring(index + 1, end))
                        pop()
                        index = end + 1
                    } else {
                        append(text[index])
                        index += 1
                    }
                }

                else -> {
                    append(text[index])
                    index += 1
                }
            }
        }
    }
}

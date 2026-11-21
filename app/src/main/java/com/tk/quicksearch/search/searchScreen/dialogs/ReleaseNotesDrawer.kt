package com.tk.quicksearch.search.searchScreen.dialogs

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import com.tk.quicksearch.shared.ui.components.AppBottomPopup
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import androidx.compose.material3.ripple
import com.tk.quicksearch.shared.ui.components.AppLoadingIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.app.PastReleaseNotes
import com.tk.quicksearch.app.ReleaseNotesRepository
import androidx.compose.ui.platform.LocalContext
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.AppButton
import com.tk.quicksearch.shared.ui.components.AppTextButton

@Composable
internal fun ReleaseNotesDrawer(
    versionName: String?,
    onAcknowledge: () -> Unit,
    onViewAllFeatures: () -> Unit,
) {
    val context = LocalContext.current
    val releaseNotesMarkdown = remember {
        context.assets.open("RELEASE_NOTES.md").bufferedReader().use { it.readText() }
    }
    val bulletPoints = remember(releaseNotesMarkdown) {
        parseReleaseNotesBulletPoints(releaseNotesMarkdown)
    }
    val title =
        if (versionName != null) {
            stringResource(R.string.release_notes_title, versionName)
        } else {
            stringResource(R.string.release_notes_title_no_version)
        }

    // Past releases are fetched lazily, the first time the history section is expanded.
    var isHistoryExpanded by remember(versionName) { mutableStateOf(false) }
    var pastReleaseNotes by remember(versionName) { mutableStateOf<List<PastReleaseNotes>>(emptyList()) }
    var isLoadingHistory by remember(versionName) { mutableStateOf(false) }
    var hasLoadedHistory by remember(versionName) { mutableStateOf(false) }
    var expandedPastVersion by remember(versionName) { mutableStateOf<String?>(null) }

    LaunchedEffect(isHistoryExpanded) {
        if (!isHistoryExpanded || hasLoadedHistory) return@LaunchedEffect
        isLoadingHistory = true
        pastReleaseNotes = ReleaseNotesRepository().getPastReleaseNotes(versionName)
        isLoadingHistory = false
        hasLoadedHistory = true
    }

    AppBottomPopup(
        onDismiss = onAcknowledge,
        contentCardColor = AppColors.DialogBackground,
        contentSpacing = 8.dp,
        title = {
            Text(text = title)
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            ReleaseNotesBulletList(bulletPoints = bulletPoints)

            HorizontalDivider(
                modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.TintLightAlpha),
            )

            ReleaseNotesExpanderRow(
                label = stringResource(R.string.release_notes_previous_versions),
                isExpanded = isHistoryExpanded,
                style = MaterialTheme.typography.bodyMedium,
                onClick = { isHistoryExpanded = !isHistoryExpanded },
            )

            AnimatedVisibility(visible = isHistoryExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    when {
                        isLoadingHistory ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.SpacingMedium),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                AppLoadingIndicator(
                                    modifier = Modifier.size(DesignTokens.IconSizeSmall),
                                )
                            }

                        pastReleaseNotes.isEmpty() ->
                            Text(
                                text = stringResource(R.string.release_notes_previous_versions_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = DesignTokens.SpacingMedium),
                            )

                        else ->
                            {
                                pastReleaseNotes.forEach { release ->
                                    val isExpanded = expandedPastVersion == release.versionName
                                    ReleaseNotesExpanderRow(
                                        label = "v${release.versionName}",
                                        isExpanded = isExpanded,
                                        style = MaterialTheme.typography.bodyMedium,
                                        bottomPadding = if (isExpanded) 0.dp else DesignTokens.SpacingMedium,
                                        onClick = {
                                            expandedPastVersion =
                                                if (isExpanded) null else release.versionName
                                        },
                                    )
                                    AnimatedVisibility(visible = isExpanded) {
                                        ReleaseNotesBulletList(
                                            bulletPoints = parseReleaseNotesBulletPoints(release.markdown),
                                            modifier = Modifier.padding(start = DesignTokens.SpacingSmall, top = DesignTokens.SpacingXSmall, bottom = DesignTokens.SpacingSmall),
                                        )
                                    }
                                }

                                ReleaseNotesNavigationRow(
                                    label = stringResource(R.string.release_notes_action_more),
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(releasesUrl)),
                                            )
                                        }
                                    },
                                )
                            }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.SpacingSmall),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTextButton(onClick = onViewAllFeatures) {
                    Text(text = stringResource(R.string.release_notes_action_all_features))
                }
                AppButton(onClick = onAcknowledge) {
                    Text(text = stringResource(R.string.release_notes_action_got_it))
                }
            }
        }
    }
}

private const val releasesUrl = "https://github.com/teja2495/quick-search/releases/"

@Composable
private fun ReleaseNotesBulletList(
    bulletPoints: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        bulletPoints.forEach { point ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = point,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ReleaseNotesExpanderRow(
    label: String,
    isExpanded: Boolean,
    style: TextStyle,
    bottomPadding: Dp = DesignTokens.SpacingMedium,
    onClick: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "releaseNotesChevronRotation",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DesignTokens.MinTouchTarget)
            .clip(AppShapes.small)
            .clickable(
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
            .padding(top = DesignTokens.SpacingMedium, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = style,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = MaterialSymbols.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSizeSmall).rotate(chevronRotation),
        )
    }
}

@Composable
private fun ReleaseNotesNavigationRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DesignTokens.MinTouchTarget)
            .clip(AppShapes.small)
            .clickable(
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
            .padding(vertical = DesignTokens.SpacingMedium),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = MaterialSymbols.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSizeSmall),
        )
    }
}

private fun parseReleaseNotesBulletPoints(markdown: String): List<String> {
    val bulletPattern = Regex("^\\s*[-*+]\\s+(.+)$")
    val numberedPattern = Regex("^\\s*\\d+\\.\\s+(.+)$")
    return markdown
        .lineSequence()
        .mapNotNull { rawLine ->
            val line = rawLine.trim()
            when {
                line.isBlank() -> null
                else -> {
                    bulletPattern.matchEntire(line)?.groupValues?.get(1)
                        ?: numberedPattern.matchEntire(line)?.groupValues?.get(1)
                        ?: line.takeIf { !it.startsWith("#") }
                }
            }
        }.map { it.trim() }
        .filter { it.isNotEmpty() }
        .toList()
}

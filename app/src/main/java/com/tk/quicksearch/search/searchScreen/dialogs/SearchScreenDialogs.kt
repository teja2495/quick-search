package com.tk.quicksearch.search.searchScreen.dialogs

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ChevronRight
import com.tk.quicksearch.shared.ui.components.AppBottomPopup
import com.tk.quicksearch.shared.ui.theme.AppColors
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import androidx.compose.ui.platform.LocalContext

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
        containerColor = AppColors.DialogBackground,
        contentCardColor = AppColors.DialogBackground,
        contentSpacing = 8.dp,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReleaseNotesBulletList(bulletPoints = bulletPoints)

            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
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
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            }

                        pastReleaseNotes.isEmpty() ->
                            Text(
                                text = stringResource(R.string.release_notes_previous_versions_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )

                        else ->
                            {
                                pastReleaseNotes.forEach { release ->
                                    val isExpanded = expandedPastVersion == release.versionName
                                    ReleaseNotesExpanderRow(
                                        label = "v${release.versionName}",
                                        isExpanded = isExpanded,
                                        style = MaterialTheme.typography.bodyMedium,
                                        bottomPadding = if (isExpanded) 0.dp else 10.dp,
                                        onClick = {
                                            expandedPastVersion =
                                                if (isExpanded) null else release.versionName
                                        },
                                    )
                                    AnimatedVisibility(visible = isExpanded) {
                                        ReleaseNotesBulletList(
                                            bulletPoints = parseReleaseNotesBulletPoints(release.markdown),
                                            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 8.dp),
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
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onViewAllFeatures) {
                    Text(text = stringResource(R.string.release_notes_action_all_features))
                }
                Button(onClick = onAcknowledge) {
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        bulletPoints.forEach { point ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
    bottomPadding: Dp = 10.dp,
    onClick: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "releaseNotesChevronRotation",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(top = 10.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = style,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp).rotate(chevronRotation),
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
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
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

sealed class NicknameDialogState {
    data class App(
        val app: AppInfo,
        val currentNickname: String?,
        val itemName: String,
    ) : NicknameDialogState()

    data class AppShortcut(
        val shortcut: StaticShortcut,
        val currentNickname: String?,
        val itemName: String,
    ) : NicknameDialogState()

    data class Contact(
        val contact: ContactInfo,
        val currentNickname: String?,
        val itemName: String,
    ) : NicknameDialogState()

    data class File(
        val file: DeviceFile,
        val currentNickname: String?,
        val itemName: String,
    ) : NicknameDialogState()

    data class Setting(
        val setting: DeviceSetting,
        val currentNickname: String?,
        val itemName: String,
    ) : NicknameDialogState()

    data class CalendarEvent(
        val event: CalendarEventInfo,
        val currentNickname: String?,
        val itemName: String,
    ) : NicknameDialogState()
}

sealed class TriggerDialogState {
    data class App(
        val app: AppInfo,
        val currentTrigger: com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        val itemName: String,
    ) : TriggerDialogState()

    data class AppShortcut(
        val shortcut: StaticShortcut,
        val currentTrigger: com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        val itemName: String,
    ) : TriggerDialogState()

    data class Contact(
        val contact: ContactInfo,
        val currentTrigger: com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        val itemName: String,
    ) : TriggerDialogState()

    data class ContactAction(
        val contact: ContactInfo,
        val action: ContactCardAction,
        val currentTrigger: com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        val itemName: String,
    ) : TriggerDialogState()

    data class File(
        val file: DeviceFile,
        val currentTrigger: com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        val itemName: String,
    ) : TriggerDialogState()

    data class Setting(
        val setting: DeviceSetting,
        val currentTrigger: com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        val itemName: String,
    ) : TriggerDialogState()

    data class Note(
        val note: NoteInfo,
        val currentTrigger: com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        val itemName: String,
    ) : TriggerDialogState()
}

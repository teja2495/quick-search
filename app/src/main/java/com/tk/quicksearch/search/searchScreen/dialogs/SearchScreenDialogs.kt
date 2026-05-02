package com.tk.quicksearch.search.searchScreen.dialogs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import androidx.compose.runtime.snapshotFlow

private const val RELEASE_NOTES_ASSET_FILE_NAME = "RELEASE_NOTES.md"

@Composable
internal fun ReleaseNotesDialog(
    versionName: String?,
    onAcknowledge: () -> Unit,
    onViewAllFeatures: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val title =
        if (versionName != null) {
            stringResource(R.string.release_notes_title, versionName)
        } else {
            stringResource(R.string.release_notes_title_no_version)
        }
    val scrollBarAlpha = remember { Animatable(1f) }
    LaunchedEffect(scrollState) {
        val scope = this
        var hideJob: Job? = null
        snapshotFlow { scrollState.value }.collect {
            hideJob?.cancel()
            scrollBarAlpha.snapTo(1f)
            hideJob = scope.launch {
                delay(1500)
                scrollBarAlpha.animateTo(0f, animationSpec = tween(300))
            }
        }
    }

    val bulletPoints by
        produceState<List<String>>(initialValue = emptyList(), context) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                        context.assets.open(RELEASE_NOTES_ASSET_FILE_NAME).bufferedReader().use { reader ->
                            parseReleaseNotesBulletPoints(reader.readText())
                        }
                    }.getOrDefault(emptyList())
                }
        }

    AppAlertDialog(
        onDismissRequest = onAcknowledge,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(end = 12.dp),
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

                if (scrollState.maxValue > 0) {
                    PersistentScrollIndicator(
                        scrollState = scrollState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .graphicsLayer { alpha = scrollBarAlpha.value },
                        thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onViewAllFeatures) {
                Text(text = stringResource(R.string.release_notes_action_all_features))
            }
        },
        confirmButton = {
            Button(onClick = onAcknowledge) {
                Text(text = stringResource(R.string.release_notes_action_got_it))
            }
        },
    )
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

@Composable
private fun PersistentScrollIndicator(
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier,
    thumbColor: Color,
    trackColor: Color,
) {
    Canvas(
        modifier =
            modifier
                .width(4.dp)
                .padding(vertical = 2.dp),
    ) {
        val trackWidth = size.width
        val cornerRadius = CornerRadius(x = trackWidth / 2f, y = trackWidth / 2f)

        drawRoundRect(
            color = trackColor,
            cornerRadius = cornerRadius,
        )

        if (scrollState.maxValue <= 0) return@Canvas

        val totalContentHeight = size.height + scrollState.maxValue
        val thumbHeight = (size.height * (size.height / totalContentHeight) * 0.85f).coerceAtLeast(20.dp.toPx())
        val maxThumbOffset = (size.height - thumbHeight).coerceAtLeast(0f)
        val progress = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
        val thumbTop = maxThumbOffset * progress

        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(x = 0f, y = thumbTop),
            size = Size(width = trackWidth, height = thumbHeight),
            cornerRadius = cornerRadius,
        )
    }
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

    data class CalendarEvent(
        val event: CalendarEventInfo,
        val currentTrigger: com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        val itemName: String,
    ) : TriggerDialogState()
}

package com.tk.quicksearch.search.searchScreen.dialogs

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.shared.util.InAppBrowserUtils

@Composable
internal fun ReleaseNotesDialog(
    versionName: String?,
    onAcknowledge: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val title =
        if (versionName != null) {
            stringResource(R.string.release_notes_title, versionName)
        } else {
            stringResource(R.string.release_notes_title_no_version)
        }
    val bulletPoints =
        stringArrayResource(R.array.release_notes_points)
            .filter { it.isNotBlank() }
    val context = LocalContext.current

    AlertDialog(
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

                    Spacer(modifier = Modifier.height(8.dp))

                    val annotatedLink =
                        buildAnnotatedString {
                            withLink(
                                LinkAnnotation.Clickable(
                                    tag = "features_link",
                                    linkInteractionListener = {
                                        val url = "https://github.com/teja2495/quick-search/blob/main/FEATURES.md"
                                        InAppBrowserUtils.openUrl(context, url)
                                    }
                                )
                            ) {
                                withStyle(
                                    style =
                                        SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = TextDecoration.Underline,
                                        ),
                                ) {
                                    append(stringResource(R.string.release_notes_view_all_features))
                                }
                            }
                        }

                    Text(text = annotatedLink)
                }

                if (scrollState.maxValue > 0) {
                    PersistentScrollIndicator(
                        scrollState = scrollState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text(text = stringResource(R.string.release_notes_action_got_it))
            }
        },
    )
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

        val thumbHeight = (size.height * 0.22f).coerceAtLeast(20.dp.toPx())
        val maxThumbOffset = (size.height - thumbHeight).coerceAtLeast(0f)
        val progress = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
        val thumbTop = maxThumbOffset * progress

        drawRoundRect(
            color = thumbColor,
            topLeft = androidx.compose.ui.geometry.Offset(x = 0f, y = thumbTop),
            size = androidx.compose.ui.geometry.Size(width = trackWidth, height = thumbHeight),
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
}

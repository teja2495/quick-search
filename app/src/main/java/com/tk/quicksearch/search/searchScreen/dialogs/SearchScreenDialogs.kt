package com.tk.quicksearch.search.searchScreen.dialogs

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.util.InAppBrowserUtils

@Composable
internal fun ReleaseNotesDialog(
    versionName: String?,
    onAcknowledge: () -> Unit,
) {
    val title =
        if (versionName != null) {
            stringResource(R.string.release_notes_title, versionName)
        } else {
            stringResource(R.string.release_notes_title_no_version)
        }
    val bulletPoints =
        stringResource(R.string.release_notes_points)
            .split("\n")
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
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
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

                // View all features link
                val annotatedLink =
                    buildAnnotatedString {
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

                androidx.compose.foundation.text.ClickableText(
                    text = annotatedLink,
                    onClick = {
                        val url = "https://github.com/teja2495/quick-search/blob/main/FEATURES.md"
                        InAppBrowserUtils.openUrl(context, url)
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text(text = stringResource(R.string.release_notes_action_got_it))
            }
        },
    )
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

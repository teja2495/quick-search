package com.tk.quicksearch.app

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.tk.quicksearch.R

@Composable
fun EnjoyingAppDialog(
    onYes: () -> Unit,
    onNo: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_enjoying_app_title))
        },
        confirmButton = {
            Button(onClick = onYes) {
                Text(text = stringResource(R.string.dialog_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onNo) {
                Text(text = stringResource(R.string.dialog_no))
            }
        }
    )
}

@Composable
fun SendFeedbackDialog(
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var feedbackText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_feedback_title))
        },
        text = {
            TextField(
                value = feedbackText,
                onValueChange = { feedbackText = it },
                placeholder = {
                    Text(text = stringResource(R.string.dialog_feedback_hint))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                minLines = 3,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        onSend(feedbackText.trim())
                        onDismiss()
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onSend(feedbackText.trim())
                    onDismiss()
                }
            ) {
                Text(text = stringResource(R.string.dialog_send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_close))
            }
        }
    )
}

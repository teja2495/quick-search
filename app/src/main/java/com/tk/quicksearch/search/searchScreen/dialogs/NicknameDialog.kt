package com.tk.quicksearch.search.searchScreen.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.tk.quicksearch.shared.ui.components.AppOutlinedTextField
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.R
import com.tk.quicksearch.search.utils.NicknameUtils
import kotlinx.coroutines.delay
import com.tk.quicksearch.shared.ui.components.AppTextButton

@Composable
fun NicknameDialog(
    currentNickname: String?,
    itemName: String,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialText = currentNickname ?: ""
    var nicknameText by
        remember(currentNickname) {
            mutableStateOf(
                TextFieldValue(
                    text = initialText,
                    selection = TextRange(initialText.length),
                ),
            )
        }
    val hasExistingNickname = !currentNickname.isNullOrBlank()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        // Give the dialog a frame to appear before requesting focus
        delay(50)
        focusRequester.requestFocus()
        keyboardController?.show()
        // Ensure cursor is at the end of text
        nicknameText = nicknameText.copy(selection = TextRange(nicknameText.text.length))
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =
                    stringResource(
                        if (hasExistingNickname) {
                            R.string.action_edit_nickname
                        } else {
                            R.string.common_nickname
                        },
                    ),
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.dialog_nickname_message, itemName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(DesignTokens.SpacingLarge))

                AppOutlinedTextField(
                    value = nicknameText,
                    onValueChange = { nicknameText = it },
                    label = stringResource(R.string.common_nickname),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    onClear = { nicknameText = TextFieldValue("") },
                )
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = { onSave(NicknameUtils.normalizeInput(nicknameText.text)) },
            ) { Text(text = stringResource(R.string.dialog_save)) }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

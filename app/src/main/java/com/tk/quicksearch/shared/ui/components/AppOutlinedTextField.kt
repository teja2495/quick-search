package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppShapes

/** Standard outlined form field with the app's dialog field colors. */
@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText,
        placeholder = placeholder,
        keyboardOptions = keyboardOptions,
        colors = dialogTextFieldColors(),
    )
}

/** Rounded single-line field for forms that manage cursor selection as part of the value. */
@Composable
fun AppOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onClear: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        shape = AppShapes.large,
        trailingIcon = if (onClear != null) {
            {
                if (value.text.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = MaterialSymbols.Close,
                            contentDescription = stringResource(R.string.desc_clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            null
        },
        colors = dialogTextFieldColors(),
        isError = isError,
    )
}

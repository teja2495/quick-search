package com.tk.quicksearch.settings.shared

import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun SettingsManagementSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    applyDefaultPadding: Boolean = true,
    applyImePadding: Boolean = true,
    fillMaxWidth: Boolean = true,
    onFocusChange: ((FocusState) -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    placeholder: String? = null,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            modifier
                .then(if (applyImePadding) Modifier.imePadding() else Modifier)
                .then(
                    if (applyDefaultPadding) {
                        Modifier.padding(start = DesignTokens.SpacingLarge, end = DesignTokens.SpacingLarge, bottom = DesignTokens.SpacingMedium)
                    } else {
                        Modifier
                    },
                )
                .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .let { m -> if (onFocusChange != null) m.onFocusChanged(onFocusChange) else m },
        shape = AppShapes.extraLarge,
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = MaterialSymbols.Search,
                contentDescription = stringResource(R.string.common_search),
                modifier = Modifier.offset(x = 2.dp),
            )
        },
        trailingIcon =
            if (query.isNotBlank()) {
                {
                    AppIconButton(
                        onClick = onClear,
                        modifier = Modifier.offset(x = (-2).dp),
                        tooltip = stringResource(R.string.common_close),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Close,
                            contentDescription = stringResource(R.string.common_close),
                        )
                    }
                }
            } else {
                null
            },
        placeholder = {
            Text(text = placeholder ?: stringResource(R.string.common_search))
        },
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = AppColors.getSettingsCardContainerColor(),
                unfocusedContainerColor = AppColors.getSettingsCardContainerColor(),
                disabledContainerColor = AppColors.getSettingsCardContainerColor(),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
    )
}

package com.tk.quicksearch.settings.shared

import com.tk.quicksearch.shared.ui.components.AppIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

@Composable
internal fun SettingsRemoveButton(
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    AppIconButton(onClick = onClick, tooltip = stringResource(R.string.action_remove)) {
        Icon(
            imageVector = MaterialSymbols.Close,
            contentDescription = stringResource(R.string.action_remove),
            tint = tint,
        )
    }
}

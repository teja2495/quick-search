package com.tk.quicksearch.widgets.customButtonsWidget

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.TonalButton
import com.tk.quicksearch.shared.ui.components.appPickerDrawerItemBackground
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** The action already assigned in a picker, with a pill Reset button that clears it. */
@Composable
internal fun CustomWidgetCurrentActionCard(
    action: CustomWidgetButtonAction,
    iconPackPackage: String?,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.extraLarge,
        color = appPickerDrawerItemBackground(selected = true),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = DesignTokens.SpacingLarge, vertical = DesignTokens.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
        ) {
            Text(
                text = stringResource(R.string.app_swipe_current_action),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            ) {
                CustomWidgetButtonIcon(
                    action = action,
                    iconSize = 24.dp,
                    iconPackPackage = iconPackPackage,
                )
                Text(
                    text = action.displayLabel(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TonalButton(
                    text = stringResource(R.string.dialog_reset),
                    onClick = onReset,
                    small = true,
                )
            }
        }
    }
}

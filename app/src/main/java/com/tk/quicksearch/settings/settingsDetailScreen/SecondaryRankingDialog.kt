package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.SecondaryRankingSignal
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
internal fun SecondaryRankingDialog(
    selectedSignal: SecondaryRankingSignal,
    onSignalSelected: (SecondaryRankingSignal) -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.secondary_ranking_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.secondary_ranking_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = DesignTokens.SpacingSmall),
                )
                SecondaryRankingOptionRow(
                    label = stringResource(R.string.secondary_ranking_recency),
                    selected = selectedSignal == SecondaryRankingSignal.RECENCY,
                    onClick = {
                        selectAndDismiss(
                            SecondaryRankingSignal.RECENCY,
                            onSignalSelected,
                            onDismiss,
                        )
                    },
                )
                SecondaryRankingOptionRow(
                    label = stringResource(R.string.secondary_ranking_most_opened),
                    selected = selectedSignal == SecondaryRankingSignal.MOST_OPENED,
                    onClick = {
                        selectAndDismiss(
                            SecondaryRankingSignal.MOST_OPENED,
                            onSignalSelected,
                            onDismiss,
                        )
                    },
                )
                SecondaryRankingOptionRow(
                    label = stringResource(R.string.settings_gesture_none),
                    selected = selectedSignal == SecondaryRankingSignal.NONE,
                    onClick = {
                        selectAndDismiss(
                            SecondaryRankingSignal.NONE,
                            onSignalSelected,
                            onDismiss,
                        )
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_done)) }
        },
    )
}

private fun selectAndDismiss(
    signal: SecondaryRankingSignal,
    onSignalSelected: (SecondaryRankingSignal) -> Unit,
    onDismiss: () -> Unit,
) {
    onSignalSelected(signal)
    onDismiss()
}

@Composable
private fun SecondaryRankingOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = DesignTokens.SpacingMedium),
        )
    }
}

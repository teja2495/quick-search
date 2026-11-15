package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.SecondaryRankingSignal
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerIconBadge
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRow
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRowSpacing
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

@Composable
internal fun SecondaryRankingDialog(
    selectedSignal: SecondaryRankingSignal,
    onSignalSelected: (SecondaryRankingSignal) -> Unit,
    onDismiss: () -> Unit,
) {
    AppPickerDrawer(
        title = stringResource(R.string.secondary_ranking_title),
        onDismiss = onDismiss,
        header = {
            Text(
                text = stringResource(R.string.secondary_ranking_dialog_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) { dismiss ->
        LazyColumn(verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing)) {
            items(listOf(SecondaryRankingSignal.RECENCY, SecondaryRankingSignal.MOST_OPENED, SecondaryRankingSignal.NONE)) { signal ->
                val label = when (signal) {
                    SecondaryRankingSignal.RECENCY -> R.string.secondary_ranking_recency
                    SecondaryRankingSignal.MOST_OPENED -> R.string.secondary_ranking_most_opened
                    SecondaryRankingSignal.NONE -> R.string.settings_gesture_none
                }
                val icon = when (signal) {
                    SecondaryRankingSignal.RECENCY -> MaterialSymbols.History
                    SecondaryRankingSignal.MOST_OPENED -> MaterialSymbols.Apps
                    SecondaryRankingSignal.NONE -> MaterialSymbols.Block
                }
                AppPickerDrawerRow(
                    title = stringResource(label),
                    selected = signal == selectedSignal,
                    leading = { AppPickerDrawerIconBadge(icon) },
                    onClick = {
                        onSignalSelected(signal)
                        dismiss()
                    },
                )
            }
        }
    }
}

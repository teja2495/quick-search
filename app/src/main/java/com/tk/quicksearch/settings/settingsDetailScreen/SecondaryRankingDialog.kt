package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.SecondaryRankingSignal
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerSelectedCheck
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.components.TonalIconBadge
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens

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
        val signals = SecondaryRankingSignal.entries
        LazyColumn(
            modifier = Modifier.fillMaxWidth().clip(AppShapes.extraLarge).selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
        ) {
            itemsIndexed(signals, key = { _, signal -> signal.name }) { index, signal ->
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
                val selected = signal == selectedSignal
                val interactionSource = remember { MutableInteractionSource() }
                SegmentedTile(
                    index = index,
                    count = signals.size,
                    interactionSource = interactionSource,
                ) {
                    SegmentedTileRow(
                        title = stringResource(label),
                        modifier = Modifier.selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = {
                                onSignalSelected(signal)
                                dismiss()
                            },
                        ),
                        leading = { TonalIconBadge(icon) },
                        trailing = {
                            if (selected) AppPickerDrawerSelectedCheck(contentDescription = null)
                        },
                    )
                }
            }
        }
    }
}

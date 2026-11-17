package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.LocalOverlayActionColor
import com.tk.quicksearch.shared.ui.components.AppButton
import com.tk.quicksearch.shared.ui.components.AppButtonTone
import com.tk.quicksearch.shared.ui.components.AppTonalButton
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

@Composable
internal fun RetryAiSearchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val overlayActionColor = LocalOverlayActionColor.current
    val label: @Composable RowScope.() -> Unit = {
        Text(text = stringResource(R.string.action_retry), style = MaterialTheme.typography.bodySmall)
    }
    // Over the overlay the action color is a solid fill; elsewhere the quiet secondary tonal.
    if (overlayActionColor != null) {
        AppButton(
            onClick = onClick,
            modifier = modifier,
            tone = AppButtonTone.Custom(overlayActionColor),
            icon = MaterialSymbols.Refresh,
            content = label,
        )
    } else {
        AppTonalButton(
            onClick = onClick,
            modifier = modifier,
            tone = AppButtonTone.Secondary,
            icon = MaterialSymbols.Refresh,
            content = label,
        )
    }
}

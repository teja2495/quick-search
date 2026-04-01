package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.searchEngines.shared.SearchTargetConstants
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm

@Composable
fun CurrencyConverterSearchCard(
    onClick: () -> Unit,
    showWallpaperBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    SearchResultCard(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(DesignTokens.SearchResultCardShape)
                .combinedClickable(
                    onClick = {
                        hapticConfirm(view)()
                        onClick()
                    },
                ),
        showWallpaperBackground = showWallpaperBackground,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = SearchTargetConstants.CARD_HORIZONTAL_PADDING,
                        vertical = SearchTargetConstants.CARD_VERTICAL_PADDING,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(SearchTargetConstants.DEFAULT_ICON_SIZE),
            )
            Spacer(modifier = Modifier.size(SearchTargetConstants.ICON_TEXT_SPACING))
            Text(
                text = stringResource(R.string.get_currency_value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

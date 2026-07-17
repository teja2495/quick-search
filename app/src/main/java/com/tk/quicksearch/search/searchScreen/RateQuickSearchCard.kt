package com.tk.quicksearch.search.searchScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.shared.ui.theme.DesignTokens

private const val QUICK_SEARCH_PACKAGE_NAME = "com.tk.quicksearch"
private val RateCardIconSize = 24.dp
private val RateCardDismissSize = 28.dp

@Composable
internal fun RateQuickSearchCard(
    showWallpaperBackground: Boolean,
    onClick: () -> Unit,
    onNotNowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QuickSearchPromptCard(
        title = stringResource(R.string.settings_feedback_rate_title),
        description = stringResource(R.string.settings_feedback_rate_desc),
        action = stringResource(R.string.action_rate),
        showWallpaperBackground = showWallpaperBackground,
        onClick = onClick,
        onNotNowClick = onNotNowClick,
        modifier = modifier,
    )
}

@Composable
internal fun QuickSearchPromptCard(
    title: String,
    description: String? = null,
    action: String,
    showWallpaperBackground: Boolean,
    onClick: () -> Unit,
    onNotNowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appIconResult =
        rememberAppIcon(
            packageName = QUICK_SEARCH_PACKAGE_NAME,
            iconPackPackage = null,
        )

    SearchResultCard(
        modifier = modifier.fillMaxWidth(),
        showWallpaperBackground = showWallpaperBackground,
    ) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                appIconResult.bitmap?.let { iconBitmap ->
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(RateCardIconSize)
                                .padding(start = DesignTokens.SpacingXSmall),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    onClick = onNotNowClick,
                    modifier =
                        Modifier
                            .align(Alignment.Top)
                            .size(RateCardDismissSize),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_not_now),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(
                    modifier =
                        Modifier.size(RateCardIconSize)
                            .padding(start = DesignTokens.SpacingXSmall),
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    FilledTonalButton(
                        onClick = onClick,
                        modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 4.dp,
                        ),
                        shape = DesignTokens.ShapeFull,
                    ) {
                        Text(text = action)
                    }
                }
                Spacer(modifier = Modifier.size(RateCardDismissSize))
            }
        }
    }
}

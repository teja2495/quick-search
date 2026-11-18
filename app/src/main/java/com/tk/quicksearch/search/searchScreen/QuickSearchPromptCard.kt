package com.tk.quicksearch.search.searchScreen

import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.components.AppButtonSize
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
import androidx.compose.material3.Icon
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
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.AppTonalButton

private const val QUICK_SEARCH_PACKAGE_NAME = "com.tk.quicksearch"
private val RateCardIconSize = 24.dp
private val RateCardDismissSize = 28.dp

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
                AppIconButton(
                    onClick = onNotNowClick,
                    tooltip = stringResource(R.string.action_not_now),
                    modifier = Modifier.size(RateCardDismissSize),
                    anchorModifier = Modifier.align(Alignment.Top),
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Close,
                        contentDescription = stringResource(R.string.action_not_now),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AppColors.ContentHighAlpha),
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
                    AppTonalButton(
                        onClick = onClick,
                        size = AppButtonSize.ExtraSmall,
                    ) {
                        Text(text = action)
                    }
                }
                Spacer(modifier = Modifier.size(RateCardDismissSize))
            }
        }
    }
}

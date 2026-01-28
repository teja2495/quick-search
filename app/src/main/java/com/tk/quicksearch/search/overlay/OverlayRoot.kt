package com.tk.quicksearch.search.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.searchScreen.SearchRoute
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.ui.theme.QuickSearchTheme

@Composable
fun OverlayRoot(
    viewModel: SearchViewModel,
    onCloseRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val maxOverlayHeight = (configuration.screenHeightDp * 0.7f).dp
    QuickSearchTheme {
        Box(modifier = modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier.align(Alignment.TopCenter)
                        .padding(
                            horizontal = DesignTokens.ContentHorizontalPadding,
                            vertical = DesignTokens.SpacingLarge
                        )
                        .fillMaxWidth()
                        .heightIn(max = maxOverlayHeight)
                        .background(MaterialTheme.colorScheme.background)
            ) {
                SearchRoute(
                    viewModel = viewModel,
                    isOverlayPresentation = true,
                    onSettingsClick = {
                        OverlayModeController.openMainActivity(context, openSettings = true)
                        onCloseRequested()
                    },
                    onSearchEngineLongPress = {
                        OverlayModeController.openMainActivity(context, openSettings = true)
                        onCloseRequested()
                    },
                    onCustomizeSearchEnginesClick = {
                        OverlayModeController.openMainActivity(context, openSettings = true)
                        onCloseRequested()
                    }
                )
            }

            FilledTonalButton(
                onClick = onCloseRequested,
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .padding(
                            bottom = DesignTokens.SpacingLarge,
                            start = DesignTokens.ContentHorizontalPadding,
                            end = DesignTokens.ContentHorizontalPadding
                        )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.desc_close)
                )
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
                Text(text = stringResource(R.string.dialog_close))
            }
        }
    }
}

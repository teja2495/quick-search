package com.tk.quicksearch.search.searchScreen.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchBarStyle
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.util.hapticStrong

/** Matches the single-line search field's resting height so the button lines up with the bar. */
internal val SearchBarSettingsButtonSize = 56.dp

/** Opens the system Settings app; used by long-press on the search bar's settings icons. */
internal fun openDeviceSettings(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/**
 * Settings button shown beside the search bar when the mic takes the bar's trailing slot. It
 * reuses the bar's fill, outline, corner radius and light-theme shadow so the two read as a pair.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SearchBarSettingsButton(
    searchBarStyle: SearchBarStyle,
    cornerRadius: Dp,
    showWallpaperBackground: Boolean,
    transparentBackground: Boolean,
    forceRestingOutline: Boolean,
    dismissKeyboardBeforeClick: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val isDarkTheme = LocalAppIsDarkTheme.current
    val lightWallpaperSearchBar = !isDarkTheme && showWallpaperBackground
    val accentColor = AppColors.Accent
    val shape = RoundedCornerShape(cornerRadius)
    val background =
        if (transparentBackground) {
            Color.Transparent
        } else {
            searchBarStyle.backgroundColor(AppColors.getSearchBarBackground(showWallpaperBackground))
        }
    val showOutline =
        searchBarStyle.borderWidthDp > 0f &&
            !(lightWallpaperSearchBar && !forceRestingOutline && !searchBarStyle.hasCustomBorder)

    Box(
        modifier =
            modifier
                .size(SearchBarSettingsButtonSize)
                .graphicsLayer {
                    if (!isDarkTheme && !transparentBackground) {
                        val elevation =
                            if (lightWallpaperSearchBar) {
                                DesignTokens.ElevationLevel5
                            } else {
                                DesignTokens.ElevationLevel4
                            }
                        shadowElevation = with(density) { elevation.toPx() }
                        this.shape = shape
                        ambientShadowColor = accentColor.copy(alpha = LightSearchBarShadowAmbientAlpha)
                        spotShadowColor = accentColor.copy(alpha = LightSearchBarShadowSpotAlpha)
                    }
                }.then(
                    if (showOutline) {
                        Modifier.border(searchBarStyle.borderWidth(), searchBarStyle.borderColor(accentColor), shape)
                    } else {
                        Modifier
                    },
                ).clip(shape)
                .background(background)
                .combinedClickable(
                    hapticFeedbackEnabled = false,
                    onLongClick = {
                        hapticStrong(view)()
                        openDeviceSettings(context)
                    },
                ) {
                    hapticStrong(view)()
                    if (dismissKeyboardBeforeClick) {
                        view.clearFocus()
                        keyboardController?.hide()
                    }
                    onClick()
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MaterialSymbols.Settings,
            contentDescription = stringResource(R.string.desc_open_settings),
            tint = AppColors.getSearchBarSecondaryIconTint(showWallpaperBackground),
        )
    }
}

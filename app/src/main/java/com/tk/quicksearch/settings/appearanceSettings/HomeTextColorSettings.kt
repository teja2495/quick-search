package com.tk.quicksearch.settings.appearanceSettings

import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.HomeTextColor
import com.tk.quicksearch.shared.ui.components.ChoiceOption
import com.tk.quicksearch.shared.ui.components.ConnectedChoiceGroup
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.util.WallpaperUtils

@Composable
fun HomeTextColorOptions(
        selectedColor: HomeTextColor?,
        onColorSelected: (HomeTextColor) -> Unit,
        backgroundSource: BackgroundSource,
        customImageUri: String?,
        compactLayoutEnabled: Boolean = false,
        modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val defaultColor = if (LocalAppIsDarkTheme.current) HomeTextColor.WHITE else HomeTextColor.BLACK
    val wallpaperDefaultColor by produceState(
            initialValue = defaultColor,
            backgroundSource,
            customImageUri,
    ) {
        value =
                WallpaperUtils.getBackgroundAppearance(context, backgroundSource, customImageUri)
                        ?.isDark
                        ?.let { isDark -> if (isDark) HomeTextColor.WHITE else HomeTextColor.BLACK }
                        ?: defaultColor
    }
    val effectiveSelectedColor = selectedColor ?: wallpaperDefaultColor
    Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.TextColumnSpacing)) {
                Text(
                        text = stringResource(homeTextColorTitleRes(compactLayoutEnabled)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = DesignTokens.SpacingXSmall),
                )
                if (!compactLayoutEnabled) {
                    Text(
                            text = stringResource(R.string.settings_home_text_color_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = DesignTokens.SpacingXSmall),
                    )
                }
            }
            ConnectedChoiceGroup(
                    options =
                            listOf(
                                    ChoiceOption(
                                            value = HomeTextColor.WHITE,
                                            label = stringResource(R.string.widget_text_icon_color_white),
                                            icon = MaterialSymbols.LightMode,
                                    ),
                                    ChoiceOption(
                                            value = HomeTextColor.BLACK,
                                            label = stringResource(R.string.widget_text_icon_color_black),
                                            icon = MaterialSymbols.DarkMode,
                                    ),
                            ),
                    selected = effectiveSelectedColor,
                    onSelect = { color -> if (color != effectiveSelectedColor) onColorSelected(color) },
            )
    }
}

/** Compact layout applies the Home text colour to search results too, so its title says so. */
internal fun homeTextColorTitleRes(compactLayoutEnabled: Boolean): Int =
        if (compactLayoutEnabled) {
            R.string.settings_home_text_color_title_compact
        } else {
            R.string.settings_home_text_color_title
        }

package com.tk.quicksearch.settings.settingsScreen

import com.tk.quicksearch.shared.ui.theme.AppColors
import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRowShape
import com.tk.quicksearch.shared.ui.components.appPickerDrawerItemBackground
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.AppLanguageOption

/** Language picker with adaptive radio tiles, System default first. */
@Composable
internal fun AppLanguagePickerDialog(
    selectedLanguageTag: String?,
    languageOptions: List<AppLanguageOption>,
    onDismiss: () -> Unit,
    onLanguageSelected: (String?) -> Unit,
) {
    val tiles =
        remember(languageOptions) {
            val deviceLanguage = deviceLanguageLabel()
            languageOptions.map { option ->
                if (option.languageTag == null) option.copy(supportingLabel = deviceLanguage) else option
            }
        }

    AppPickerDrawer(
        title = stringResource(R.string.settings_app_language_picker_title),
        onDismiss = onDismiss,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = DesignTokens.Spacing48 * 4),
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(LanguageTileSpacing),
            verticalArrangement = Arrangement.spacedBy(LanguageTileSpacing),
        ) {
            items(tiles, key = { it.languageTag.orEmpty() }) { option ->
                LanguageTile(
                    option = option,
                    selected = option.languageTag == selectedLanguageTag,
                    onClick = { onLanguageSelected(option.languageTag) },
                )
            }
        }
    }
}

@Composable
private fun LanguageTile(
    option: AppLanguageOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = languageTileHeight())
                .clip(AppPickerDrawerRowShape)
                .background(
                    appPickerDrawerItemBackground(
                        selected = selected,
                        unselected = MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.TintFaintAlpha),
                    ),
                ).selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(DesignTokens.SpacingMedium),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
        ) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            option.supportingLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        RadioButton(selected = selected, onClick = null)
    }
}

/**
 * One height for every tile, so a one-line tile (English) or a tall script (Arabic, Hindi, Japanese)
 * doesn't make its tile differ from the rest. Built from the text line heights so it follows the
 * font scale.
 */
@Composable
private fun languageTileHeight(): Dp {
    val typography = MaterialTheme.typography
    val textHeight =
        with(LocalDensity.current) {
            (typography.titleMedium.lineHeight.value + typography.bodyMedium.lineHeight.value + TALL_SCRIPT_HEADROOM_SP)
                .sp
                .toDp()
        }
    return textHeight + DesignTokens.SpacingXSmall + DesignTokens.SpacingMedium * 2
}

private fun deviceLanguageLabel(): String? {
    val locale = Resources.getSystem().configuration.locales.get(0) ?: return null
    return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }.ifBlank { null }
}

private val LanguageTileSpacing = DesignTokens.SpacingSmall
private const val TALL_SCRIPT_HEADROOM_SP = 14f

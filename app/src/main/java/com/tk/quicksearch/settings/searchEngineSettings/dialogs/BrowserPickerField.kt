package com.tk.quicksearch.settings.searchEngineSettings.dialogs

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.BrowserApp
import com.tk.quicksearch.searchEngines.IN_APP_BROWSER_PACKAGE
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRow
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRowSpacing
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Browser picker shown inside Add/Edit custom search engine dialogs: an outlined field showing
 * the chosen browser that opens an [AppPickerDrawer] list, like the bookmark editor's field.
 *
 * [selectedPackage] == null means "Default browser". Custom search engines select the
 * in-app browser by default.
 */
@Composable
fun BrowserPickerField(
    availableBrowsers: List<BrowserApp>,
    selectedPackage: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inAppBrowser =
        BrowserApp(
            packageName = IN_APP_BROWSER_PACKAGE,
            label = stringResource(R.string.browser_in_app_name),
        )
    val browserOptions =
        if (availableBrowsers.any { it.packageName == IN_APP_BROWSER_PACKAGE }) {
            availableBrowsers
        } else {
            availableBrowsers + inAppBrowser
        }

    var showPicker by remember { mutableStateOf(false) }
    val fieldLabel = stringResource(R.string.settings_search_engine_browser_label)
    val defaultLabel = stringResource(R.string.settings_search_engine_browser_default)
    val selectedLabel = browserOptions.firstOrNull { it.packageName == selectedPackage }?.label ?: defaultLabel

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(OutlinedTextFieldDefaults.shape)
                .border(
                    DesignTokens.BorderWidth,
                    MaterialTheme.colorScheme.outline,
                    OutlinedTextFieldDefaults.shape,
                )
                .clickable(role = Role.DropdownList, onClick = { showPicker = true })
                .padding(horizontal = DesignTokens.SpacingLarge, vertical = DesignTokens.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        Icon(
            imageVector = MaterialSymbols.Language,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSizeSmall),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fieldLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Icon(
            imageVector = MaterialSymbols.UnfoldMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSizeSmall),
        )
    }

    if (showPicker) {
        AppPickerDrawer(
            title = fieldLabel,
            onDismiss = { showPicker = false },
        ) { dismiss ->
            val options = listOf<Pair<String?, String>>(null to defaultLabel) +
                browserOptions.map { it.packageName to it.label }
            LazyColumn(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing),
            ) {
                items(options, key = { it.first ?: "" }) { (packageName, label) ->
                    AppPickerDrawerRow(
                        title = label,
                        selected = packageName == selectedPackage,
                        onClick = {
                            onSelect(packageName)
                            dismiss()
                        },
                    )
                }
            }
        }
    }
}

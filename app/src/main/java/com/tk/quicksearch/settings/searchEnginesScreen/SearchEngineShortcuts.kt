package com.tk.quicksearch.settings.searchEnginesScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import com.tk.quicksearch.R
import com.tk.quicksearch.util.hapticToggle
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.settings.searchEnginesScreen.EditShortcutDialog
import com.tk.quicksearch.settings.searchEnginesScreen.SearchEngineDivider
import com.tk.quicksearch.settings.searchEnginesScreen.getSearchEngineIconColorFilter
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.searchEngines.getDisplayName
import com.tk.quicksearch.search.searchEngines.getDrawableResId
import com.tk.quicksearch.ui.theme.DesignTokens

/**
 * Feature for managing search engine shortcuts.
 */
@Composable
fun SearchEngineShortcuts(
    shortcutCodes: Map<SearchEngine, String>,
    setShortcutCode: (SearchEngine, String) -> Unit,
    shortcutEnabled: Map<SearchEngine, Boolean>,
    setShortcutEnabled: (SearchEngine, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.settings_shortcuts_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = DesignTokens.SectionTopPadding, bottom = DesignTokens.SectionTitleBottomPadding)
    )
    Text(
        text = stringResource(R.string.settings_shortcuts_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = DesignTokens.SectionDescriptionBottomPadding)
    )
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            SearchEngine.values().forEachIndexed { index, engine ->
                ShortcutRow(
                    engine = engine,
                    shortcutCode = shortcutCodes[engine] ?: "",
                    isEnabled = shortcutEnabled[engine] ?: true,
                    onCodeChange = { code -> setShortcutCode(engine, code) },
                    onToggle = { enabled -> setShortcutEnabled(engine, enabled) },
                    existingShortcuts = shortcutCodes.mapKeys { it.key.name }
                )
                if (index != SearchEngine.values().lastIndex) {
                    SearchEngineDivider()
                }
            }
        }
    }
}

/**
 * Display component for shortcut code with edit dialog.
 */
@Composable
internal fun ShortcutCodeDisplay(
    shortcutCode: String,
    isEnabled: Boolean,
    onCodeChange: ((String) -> Unit)?,
    onToggle: ((Boolean) -> Unit)?,
    engineName: String = "",
    existingShortcuts: Map<String, String> = emptyMap(),
    currentShortcutId: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog && onCodeChange != null) {
        EditShortcutDialog(
            engineName = engineName,
            currentCode = shortcutCode,
            isEnabled = isEnabled,
            existingShortcuts = existingShortcuts,
            currentShortcutId = currentShortcutId,
            onSave = { code -> onCodeChange(code) },
            onToggle = onToggle,
            onDismiss = { showDialog = false }
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isEnabled) {
            Text(
                text = stringResource(R.string.settings_shortcut_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = shortcutCode,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showDialog = true }
            )
        } else {
            Text(
                text = stringResource(R.string.settings_add_shortcut),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showDialog = true }
            )
        }
    }
}

/**
 * Row component for displaying and editing a shortcut.
 */
@Composable
private fun ShortcutRow(
    engine: SearchEngine,
    shortcutCode: String,
    isEnabled: Boolean,
    onCodeChange: (String) -> Unit,
    onToggle: (Boolean) -> Unit,
    existingShortcuts: Map<String, String> = emptyMap()
) {
    var showDialog by remember { mutableStateOf(false) }
    val view = LocalView.current

    val engineName = engine.getDisplayName()
    val drawableId = engine.getDrawableResId()

    if (showDialog) {
        EditShortcutDialog(
            engineName = engineName,
            currentCode = shortcutCode,
            isEnabled = isEnabled,
            existingShortcuts = existingShortcuts,
            currentShortcutId = engine.name,
            onSave = { code -> onCodeChange(code) },
            onToggle = { enabled -> onToggle(enabled) },
            onDismiss = { showDialog = false }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = DesignTokens.CardHorizontalPadding,
                vertical = DesignTokens.CardVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = engineName,
            modifier = Modifier.size(24.dp),
            contentScale = ContentScale.Fit,
            colorFilter = getSearchEngineIconColorFilter(engine)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = engineName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isEnabled) {
                    Text(
                        text = shortcutCode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showDialog = true }
                    )
                } else {
                    Text(
                        text = stringResource(R.string.settings_add_shortcut),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showDialog = true }
                    )
                }
            }
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = { enabled ->
                hapticToggle(view)()
                onToggle(enabled)
            }
        )
    }
}

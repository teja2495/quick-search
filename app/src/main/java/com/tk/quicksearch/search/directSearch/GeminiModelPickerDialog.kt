package com.tk.quicksearch.search.directSearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R

@Composable
fun GeminiModelPickerDialog(
        selectedModelId: String,
        models: List<GeminiTextModel>,
        groundingEnabled: Boolean,
        onGroundingChange: (Boolean) -> Unit,
        onModelSelected: (String) -> Unit,
        onDismiss: () -> Unit,
) {
        val listState = rememberLazyListState()
        val selectedModel = models.firstOrNull { it.id == selectedModelId }
        val supportsGrounding = selectedModel?.supportsGrounding != false

        LaunchedEffect(Unit) {
                val index = models.indexOfFirst { it.id == selectedModelId }
                if (index >= 0) {
                        listState.scrollToItem(index)
                }
        }

        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(text = stringResource(R.string.dialog_gemini_model_picker_title)) },
                text = {
                        Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                                LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                        items(models) { model ->
                                                val isSelected = model.id == selectedModelId
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .clip(MaterialTheme.shapes.medium)
                                                                        .clickable {
                                                                                onModelSelected(model.id)
                                                                        }
                                                                        .padding(
                                                                                horizontal = 8.dp,
                                                                                vertical = 8.dp
                                                                        ),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                        RadioButton(
                                                                selected = isSelected,
                                                                onClick = { onModelSelected(model.id) },
                                                                modifier = Modifier.size(16.dp).padding(end = 8.dp)
                                                        )
                                                        Text(
                                                                text = model.displayName,
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.weight(1f)
                                                        )
                                                }
                                        }
                                }
                                HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                )
                                if (supportsGrounding) {
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .clickable {
                                                                        onGroundingChange(!groundingEnabled)
                                                                },
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                                Checkbox(
                                                        checked = groundingEnabled,
                                                        onCheckedChange = onGroundingChange,
                                                )
                                                Text(
                                                        text =
                                                                stringResource(
                                                                        R.string
                                                                                .settings_direct_search_grounding_label
                                                                ),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                )
                                        }
                                } else {
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Rounded.Info,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                        text = stringResource(R.string.dialog_gemini_model_no_grounding_info),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.weight(1f)
                                                )
                                        }
                                }
                        }
                },
                confirmButton = {
                        TextButton(onClick = onDismiss) {
                                Text(text = stringResource(R.string.dialog_close))
                        }
                }
        )
}

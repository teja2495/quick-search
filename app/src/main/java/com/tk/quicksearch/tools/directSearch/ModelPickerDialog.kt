package com.tk.quicksearch.tools.directSearch

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.tk.quicksearch.shared.ui.components.AppAlertDialog

@Composable
fun ModelPickerDialog(
    selectedModelId: String,
    models: List<GeminiTextModel>,
    groundingEnabled: Boolean,
    onGroundingChange: (Boolean) -> Unit,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    showGroundingToggle: Boolean = true,
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

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_gemini_model_picker_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(models) { model ->
                        val isSelected = model.id == selectedModelId
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        onModelSelected(model.id)
                                        onDismiss()
                                    }
                                    .padding(
                                        horizontal = 8.dp,
                                        vertical = 8.dp,
                                    ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onModelSelected(model.id)
                                    onDismiss()
                                },
                                modifier = Modifier.size(16.dp).padding(end = 8.dp),
                            )
                            Text(
                                text = model.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                if (showGroundingToggle && supportsGrounding) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Row(
                        modifier =
                            Modifier.fillMaxWidth().clickable {
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
                                    R.string.settings_direct_search_grounding_label,
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.desc_close))
            }
        },
    )
}

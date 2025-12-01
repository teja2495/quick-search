package com.tk.quicksearch.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile

@Composable
fun ExcludedItemsSection(
    hiddenApps: List<AppInfo>,
    excludedContacts: List<ContactInfo>,
    excludedFiles: List<DeviceFile>,
    onRemoveExcludedApp: (AppInfo) -> Unit,
    onRemoveExcludedContact: (ContactInfo) -> Unit,
    onRemoveExcludedFile: (DeviceFile) -> Unit,
    onClearAll: () -> Unit
) {
    // Combine all items into one list
    val allItems = remember(hiddenApps, excludedContacts, excludedFiles) {
        (hiddenApps.map { ExcludedItem.App(it) } +
         excludedContacts.map { ExcludedItem.Contact(it) } +
         excludedFiles.map { ExcludedItem.File(it) })
            .sortedBy { it.displayName.lowercase() }
    }
    
    if (allItems.isEmpty()) {
        return
    }

    var isExpanded by remember { mutableStateOf(false) }
    var showClearAllConfirmation by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.settings_excluded_items_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(onClick = { showClearAllConfirmation = true }) {
            Text(
                text = stringResource(R.string.settings_action_clear_all),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Show items based on expanded state
            val itemsToShow = if (isExpanded) allItems else allItems.take(3)
            itemsToShow.forEachIndexed { index, item ->
                ExcludedItemRow(
                    item = item,
                    isExpanded = isExpanded,
                    onRemove = {
                        when (item) {
                            is ExcludedItem.App -> onRemoveExcludedApp(item.appInfo)
                            is ExcludedItem.Contact -> onRemoveExcludedContact(item.contactInfo)
                            is ExcludedItem.File -> onRemoveExcludedFile(item.deviceFile)
                        }
                    }
                )
                if (index != itemsToShow.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            // "More" / "Show Less" button if there are more than 3 items
            if (allItems.size > 3) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (isExpanded) {
                            stringResource(R.string.settings_excluded_items_show_less)
                        } else {
                            stringResource(R.string.settings_excluded_items_more)
                        },
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // Clear All confirmation dialog
    if (showClearAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmation = false },
            title = {
                Text(text = stringResource(R.string.settings_excluded_items_clear_all))
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_excluded_items_clear_all_confirmation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(onClick = {
                    onClearAll()
                    showClearAllConfirmation = false
                }) {
                    Text(stringResource(R.string.settings_action_clear_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmation = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun ExcludedItemRow(
    item: ExcludedItem,
    isExpanded: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on item type
            when (item) {
                is ExcludedItem.Contact -> {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                is ExcludedItem.File -> {
                    Icon(
                        imageVector = Icons.Rounded.InsertDriveFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                is ExcludedItem.App -> {
                    AppIcon(appInfo = item.appInfo)
                }
            }
            Column(
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
                )
                Text(
                    text = item.typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.settings_action_remove),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@Composable
private fun AppIcon(appInfo: AppInfo, size: androidx.compose.ui.unit.Dp = 24.dp) {
    val context = LocalContext.current
    val packageName = appInfo.packageName
    
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = packageName) {
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap()
                    .asImageBitmap()
            }.getOrNull()
        }
        value = bitmap
    }
    
    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap!!,
            contentDescription = null,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit
        )
    } else {
        // Fallback placeholder if icon can't be loaded
        Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size)
        )
    }
}

private sealed class ExcludedItem {
    abstract val displayName: String
    abstract val typeLabel: String

    data class App(val appInfo: AppInfo) : ExcludedItem() {
        override val displayName: String = appInfo.appName
        override val typeLabel: String = "App"
    }

    data class Contact(val contactInfo: ContactInfo) : ExcludedItem() {
        override val displayName: String = contactInfo.displayName
        override val typeLabel: String = "Contact"
    }

    data class File(val deviceFile: DeviceFile) : ExcludedItem() {
        override val displayName: String = deviceFile.displayName
        override val typeLabel: String = "File"
    }
}

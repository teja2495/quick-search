package com.tk.quicksearch.settings.settingsDetailScreen

import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.data.AppShortcutRepository.AppShortcutCache
import com.tk.quicksearch.search.data.preferences.NicknamePreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsRepository
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NicknameItemsScreen(
    modifier: Modifier = Modifier,
    onAfterRemove: () -> Unit = {},
) {
    val context = LocalContext.current
    var refreshSignal by remember { mutableIntStateOf(0) }
    var allItems by remember { mutableStateOf<List<NicknameItem>>(emptyList()) }

    LaunchedEffect(refreshSignal) {
        val items = withContext(Dispatchers.IO) {
            val prefs = NicknamePreferences(context)
            val pm = context.packageManager
            val settingsById = try {
                DeviceSettingsRepository(context).loadShortcuts().associateBy { it.id }
            } catch (e: Exception) {
                emptyMap()
            }
            val shortcutsById = try {
                val cache = AppShortcutCache(context)
                val all = (cache.loadCachedShortcuts() ?: emptyList()) +
                    (cache.loadCustomShortcuts() ?: emptyList())
                all.associateBy { "${it.packageName}:${it.id}" }
            } catch (e: Exception) {
                emptyMap()
            }
            // Fallback lookup by bare shortcut id for legacy entries stored without packageName prefix
            val shortcutsByBareId = shortcutsById.mapKeys { (key, _) -> key.substringAfter(':') }

            buildList {
                prefs.getAllAppNicknames().forEach { (packageName, nickname) ->
                    val appName = try {
                        pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
                    } catch (e: Exception) {
                        packageName
                    }
                    add(NicknameItem.App(packageName = packageName, nickname = nickname, itemName = appName))
                }
                prefs.getAllAppShortcutNicknames().forEach { (shortcutId, nickname) ->
                    val shortcut = shortcutsById[shortcutId] ?: shortcutsByBareId[shortcutId]
                    val shortcutName = shortcut?.shortLabel?.takeIf { it.isNotBlank() }
                        ?: shortcut?.longLabel?.takeIf { it.isNotBlank() }
                        ?: shortcut?.appLabel?.takeIf { it.isNotBlank() }
                        ?: try { pm.getApplicationInfo(shortcutId.substringBefore(':'), 0).loadLabel(pm).toString() } catch (e: Exception) { shortcutId.substringBefore(':') }
                    add(NicknameItem.AppShortcut(shortcutId = shortcutId, nickname = nickname, itemName = shortcutName, iconBase64 = shortcut?.iconBase64))
                }
                prefs.getAllContactNicknames().forEach { (contactId, nickname) ->
                    val contactName = try {
                        context.contentResolver.query(
                            ContactsContract.Contacts.CONTENT_URI,
                            arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                            "${ContactsContract.Contacts._ID} = ?",
                            arrayOf(contactId.toString()),
                            null,
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        }
                    } catch (e: Exception) {
                        null
                    } ?: contactId.toString()
                    add(NicknameItem.Contact(contactId = contactId, nickname = nickname, itemName = contactName))
                }
                prefs.getAllFileNicknames().forEach { (uri, nickname) ->
                    val fileName = try {
                        context.contentResolver.query(
                            android.net.Uri.parse(uri),
                            arrayOf(OpenableColumns.DISPLAY_NAME),
                            null, null, null,
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        }
                    } catch (e: Exception) {
                        null
                    } ?: android.net.Uri.parse(uri).lastPathSegment ?: uri
                    add(NicknameItem.File(uri = uri, nickname = nickname, itemName = fileName))
                }
                prefs.getAllSettingNicknames().forEach { (settingId, nickname) ->
                    val settingName = settingsById[settingId]?.title ?: settingId
                    add(NicknameItem.Setting(settingId = settingId, nickname = nickname, itemName = settingName))
                }
                prefs.getAllCalendarEventNicknames().forEach { (eventId, nickname) ->
                    val eventTitle = try {
                        context.contentResolver.query(
                            CalendarContract.Events.CONTENT_URI,
                            arrayOf(CalendarContract.Events.TITLE),
                            "${CalendarContract.Events._ID} = ?",
                            arrayOf(eventId.toString()),
                            null,
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        }
                    } catch (e: Exception) {
                        null
                    } ?: eventId.toString()
                    add(NicknameItem.CalendarEvent(eventId = eventId, nickname = nickname, itemName = eventTitle))
                }
            }.let { list ->
                // Deduplicate: remove legacy bare-ID entries when a full packageName:id entry exists
                val shortcutItems = list.filterIsInstance<NicknameItem.AppShortcut>()
                val fullKeyBareIds = shortcutItems
                    .filter { ':' in it.shortcutId }
                    .map { it.shortcutId.substringAfter(':') }
                    .toSet()
                val legacyToRemove = shortcutItems
                    .filter { ':' !in it.shortcutId && it.shortcutId in fullKeyBareIds }
                    .toSet()
                if (legacyToRemove.isEmpty()) list else list.filter { it !in legacyToRemove }
            }.sortedBy { it.nickname.lowercase() }
        }
        allItems = items
    }

    if (allItems.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_nicknames_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxWidth()
                .padding(DesignTokens.ContentHorizontalPadding),
        )
        return
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_nicknames_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = DesignTokens.SectionDescriptionBottomPadding),
        )

        SettingsCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                allItems.forEachIndexed { index, item ->
                    NicknameItemRow(
                        item = item,
                        onRemove = {
                            val prefs = NicknamePreferences(context)
                            when (item) {
                                is NicknameItem.App -> prefs.setAppNickname(item.packageName, null)
                                is NicknameItem.AppShortcut -> prefs.setAppShortcutNickname(item.shortcutId, null)
                                is NicknameItem.Contact -> prefs.setContactNickname(item.contactId, null)
                                is NicknameItem.File -> prefs.setFileNickname(item.uri, null)
                                is NicknameItem.Setting -> prefs.setSettingNickname(item.settingId, null)
                                is NicknameItem.CalendarEvent -> prefs.setCalendarEventNickname(item.eventId, null)
                            }
                            refreshSignal++
                            onAfterRemove()
                        },
                    )
                    if (index < allItems.lastIndex) {
                        HorizontalDivider(color = AppColors.SettingsDivider)
                    }
                }
            }
        }
    }
}

@Composable
private fun NicknameItemRow(
    item: NicknameItem,
    onRemove: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NicknameItemIcon(item = item)

            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = item.nickname,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Visible,
                )
                if (item.itemName.isNotBlank()) {
                    Text(
                        text = item.itemName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.action_remove),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun NicknameItemIcon(item: NicknameItem) {
    when (item) {
        is NicknameItem.App -> {
            val iconResult = rememberAppIcon(packageName = item.packageName)
            if (iconResult.bitmap != null) {
                Image(
                    bitmap = iconResult.bitmap!!,
                    contentDescription = null,
                    modifier = Modifier.size(DesignTokens.IconSize),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DesignTokens.IconSize),
                )
            }
        }
        is NicknameItem.AppShortcut -> {
            val decodedIcon = remember(item.iconBase64) {
                item.iconBase64?.let { base64 ->
                    runCatching {
                        val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }.getOrNull()
                }
            }
            if (decodedIcon != null) {
                Image(
                    bitmap = decodedIcon,
                    contentDescription = null,
                    modifier = Modifier.size(DesignTokens.IconSize),
                    contentScale = ContentScale.Fit,
                )
            } else {
                val packageName = item.shortcutId.substringBefore(':')
                val iconResult = rememberAppIcon(packageName = packageName)
                if (iconResult.bitmap != null) {
                    Image(
                        bitmap = iconResult.bitmap!!,
                        contentDescription = null,
                        modifier = Modifier.size(DesignTokens.IconSize),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DesignTokens.IconSize),
                    )
                }
            }
        }
        is NicknameItem.Contact -> Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
        is NicknameItem.File -> Icon(
            imageVector = Icons.AutoMirrored.Rounded.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
        is NicknameItem.Setting -> Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
        is NicknameItem.CalendarEvent -> Icon(
            imageVector = Icons.Rounded.Event,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
    }
}

private sealed class NicknameItem {
    abstract val nickname: String
    abstract val itemName: String

    data class App(
        val packageName: String,
        override val nickname: String,
        override val itemName: String,
    ) : NicknameItem()

    data class AppShortcut(
        val shortcutId: String,
        override val nickname: String,
        override val itemName: String,
        val iconBase64: String?,
    ) : NicknameItem()

    data class Contact(
        val contactId: Long,
        override val nickname: String,
        override val itemName: String,
    ) : NicknameItem()

    data class File(
        val uri: String,
        override val nickname: String,
        override val itemName: String,
    ) : NicknameItem()

    data class Setting(
        val settingId: String,
        override val nickname: String,
        override val itemName: String,
    ) : NicknameItem()

    data class CalendarEvent(
        val eventId: Long,
        override val nickname: String,
        override val itemName: String,
    ) : NicknameItem()
}

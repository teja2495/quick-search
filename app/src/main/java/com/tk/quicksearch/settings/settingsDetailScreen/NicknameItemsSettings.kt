package com.tk.quicksearch.settings.settingsDetailScreen

import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.settings.shared.SettingsRemoveButton
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.data.appShortcutRepository.AppShortcutCache
import com.tk.quicksearch.search.data.preferences.NicknamePreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsRepository
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NicknameItemsScreen(
    modifier: Modifier = Modifier,
    onAfterRemove: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var allItems by remember { mutableStateOf<List<NicknameItem>>(emptyList()) }

    LaunchedEffect(Unit) {
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
            buildList {
                prefs.getAllAppNicknames().forEach { (appKey, nickname) ->
                    val packageName = appKey.substringBefore(':')
                    val userHandleId = appKey.substringAfter(':', "").toIntOrNull()
                    val appName = try {
                        pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
                    } catch (e: Exception) {
                        packageName
                    }
                    add(
                        NicknameItem.App(
                            appKey = appKey,
                            packageName = packageName,
                            userHandleId = userHandleId,
                            nickname = nickname,
                            itemName = appName,
                        ),
                    )
                }
                prefs.getAllAppShortcutNicknames().forEach { (shortcutId, nickname) ->
                    val shortcut = shortcutsById[shortcutId]
                    val shortcutName = shortcut?.shortLabel?.takeIf { it.isNotBlank() }
                        ?: shortcut?.longLabel?.takeIf { it.isNotBlank() }
                        ?: shortcut?.appLabel?.takeIf { it.isNotBlank() }
                        ?: try { pm.getApplicationInfo(shortcutId.substringBefore(':'), 0).loadLabel(pm).toString() } catch (e: Exception) { shortcutId.substringBefore(':') }
                    add(NicknameItem.AppShortcut(shortcutId = shortcutId, nickname = nickname, itemName = shortcutName, iconBase64 = shortcut?.iconBase64))
                }
                val contactNicknames = prefs.getAllContactNicknames()
                val contactNames =
                    queryNamesById(
                        context = context,
                        uri = ContactsContract.Contacts.CONTENT_URI,
                        idColumn = ContactsContract.Contacts._ID,
                        nameColumn = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                        ids = contactNicknames.keys,
                    )
                contactNicknames.forEach { (contactId, nickname) ->
                    val contactName = contactNames[contactId] ?: contactId.toString()
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
                val eventNicknames = prefs.getAllCalendarEventNicknames()
                val eventTitles =
                    queryNamesById(
                        context = context,
                        uri = CalendarContract.Events.CONTENT_URI,
                        idColumn = CalendarContract.Events._ID,
                        nameColumn = CalendarContract.Events.TITLE,
                        ids = eventNicknames.keys,
                    )
                eventNicknames.forEach { (eventId, nickname) ->
                    val eventTitle = eventTitles[eventId] ?: eventId.toString()
                    add(NicknameItem.CalendarEvent(eventId = eventId, nickname = nickname, itemName = eventTitle))
                }
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

        SegmentedGroup {
            allItems.forEachIndexed { index, item ->
                SegmentedTile(index = index, count = allItems.size) {
                    NicknameItemRow(
                        item = item,
                        onRemove = {
                            // Drop the row right away; the other rows are unchanged, so there is no
                            // need to query contacts, files, and events again.
                            allItems = allItems.filterNot { it === item }
                            // NonCancellable: leaving the screen right after tapping must not skip
                            // the write or the cache reload in onAfterRemove.
                            scope.launch(NonCancellable) {
                                withContext(Dispatchers.IO) {
                                    val prefs = NicknamePreferences(context)
                                    when (item) {
                                        is NicknameItem.App -> prefs.setAppNickname(item.appKey, null)
                                        is NicknameItem.AppShortcut -> prefs.setAppShortcutNickname(item.shortcutId, null)
                                        is NicknameItem.Contact -> prefs.setContactNickname(item.contactId, null)
                                        is NicknameItem.File -> prefs.setFileNickname(item.uri, null)
                                        is NicknameItem.Setting -> prefs.setSettingNickname(item.settingId, null)
                                        is NicknameItem.CalendarEvent -> prefs.setCalendarEventNickname(item.eventId, null)
                                    }
                                }
                                onAfterRemove()
                            }
                        },
                    )
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
    SegmentedTileRow(
        title = item.nickname,
        description = item.itemName.takeIf { it.isNotBlank() },
        leading = {
            Box(
                modifier = Modifier.size(NicknameLeadingSlotSize),
                contentAlignment = Alignment.Center,
            ) {
                NicknameItemIcon(item = item)
            }
        },
        trailing = {
            SettingsRemoveButton(onClick = onRemove, tint = AppColors.Accent)
        },
    )
}

private val NicknameLeadingSlotSize = 40.dp

@Composable
private fun NicknameItemIcon(item: NicknameItem) {
    when (item) {
        is NicknameItem.App -> {
            val iconResult = rememberAppIcon(packageName = item.packageName, userHandleId = item.userHandleId)
            if (iconResult.bitmap != null) {
                Image(
                    bitmap = iconResult.bitmap,
                    contentDescription = null,
                    modifier = Modifier.size(DesignTokens.IconSize),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Icon(
                    imageVector = MaterialSymbols.Apps,
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
                        bitmap = iconResult.bitmap,
                        contentDescription = null,
                        modifier = Modifier.size(DesignTokens.IconSize),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Icon(
                        imageVector = MaterialSymbols.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DesignTokens.IconSize),
                    )
                }
            }
        }
        is NicknameItem.Contact -> Icon(
            imageVector = MaterialSymbols.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
        is NicknameItem.File -> Icon(
            imageVector = MaterialSymbols.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
        is NicknameItem.Setting -> Icon(
            imageVector = MaterialSymbols.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
        is NicknameItem.CalendarEvent -> Icon(
            imageVector = MaterialSymbols.Event,
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
        val appKey: String,
        val packageName: String,
        val userHandleId: Int?,
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

private const val NAME_QUERY_BATCH_SIZE = 500

/**
 * Looks up display names for [ids] with one `IN (...)` query per batch instead of one query per
 * row. Ids that fail to load are left out, so callers fall back to the id as before.
 */
private fun queryNamesById(
    context: android.content.Context,
    uri: android.net.Uri,
    idColumn: String,
    nameColumn: String,
    ids: Collection<Long>,
): Map<Long, String> {
    if (ids.isEmpty()) return emptyMap()
    val names = HashMap<Long, String>(ids.size)
    ids.chunked(NAME_QUERY_BATCH_SIZE).forEach { batch ->
        try {
            context.contentResolver.query(
                uri,
                arrayOf(idColumn, nameColumn),
                "$idColumn IN (${batch.joinToString(",") { "?" }})",
                batch.map(Long::toString).toTypedArray(),
                null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1) ?: continue
                    names[cursor.getLong(0)] = name
                }
            }
        } catch (e: Exception) {
            // Missing permission or provider failure: those rows show their ids, as before.
        }
    }
    return names
}

package com.tk.quicksearch.settings.settingsDetailScreen

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
import androidx.compose.material.icons.rounded.Description
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
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.data.preferences.TriggerPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsRepository
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TriggerItemsScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var refreshSignal by remember { mutableIntStateOf(0) }
    var allItems by remember { mutableStateOf<List<TriggerItem>>(emptyList()) }

    LaunchedEffect(refreshSignal) {
        val items = withContext(Dispatchers.IO) {
            val prefs = TriggerPreferences(context)
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
            val notesById = try {
                NotesRepository(context).getAllNotes().associateBy { it.noteId }
            } catch (e: Exception) {
                emptyMap()
            }

            buildList {
                prefs.getAllAppTriggers().forEach { (packageName, trigger) ->
                    val appName = try {
                        pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
                    } catch (e: Exception) {
                        packageName
                    }
                    add(TriggerItem.App(packageName = packageName, word = trigger.word, triggerAfterSpace = trigger.triggerAfterSpace, itemName = appName))
                }
                prefs.getAllAppShortcutTriggers().forEach { (shortcutId, trigger) ->
                    val shortcut = shortcutsById[shortcutId] ?: shortcutsByBareId[shortcutId]
                    val shortcutName = shortcut?.shortLabel?.takeIf { it.isNotBlank() }
                        ?: shortcut?.longLabel?.takeIf { it.isNotBlank() }
                        ?: shortcut?.appLabel?.takeIf { it.isNotBlank() }
                        ?: try { pm.getApplicationInfo(shortcutId.substringBefore(':'), 0).loadLabel(pm).toString() } catch (e: Exception) { shortcutId.substringBefore(':') }
                    add(TriggerItem.AppShortcut(shortcutId = shortcutId, word = trigger.word, triggerAfterSpace = trigger.triggerAfterSpace, itemName = shortcutName, iconBase64 = shortcut?.iconBase64))
                }
                prefs.getAllContactTriggers().forEach { (contactId, trigger) ->
                    val contactName = loadContactName(context, contactId)
                    add(TriggerItem.Contact(contactId = contactId, word = trigger.word, triggerAfterSpace = trigger.triggerAfterSpace, itemName = contactName))
                }
                prefs.getAllContactActionTriggers().forEach { (key, trigger) ->
                    val contactName = loadContactName(context, key.contactId)
                    val actionName = contactActionLabel(context, key.action)
                    add(
                        TriggerItem.ContactAction(
                            contactId = key.contactId,
                            action = key.action,
                            word = trigger.word,
                            triggerAfterSpace = trigger.triggerAfterSpace,
                            itemName = "$contactName - $actionName",
                        ),
                    )
                }
                prefs.getAllFileTriggers().forEach { (uri, trigger) ->
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
                    add(TriggerItem.File(uri = uri, word = trigger.word, triggerAfterSpace = trigger.triggerAfterSpace, itemName = fileName))
                }
                prefs.getAllSettingTriggers().forEach { (settingId, trigger) ->
                    val settingName = settingsById[settingId]?.title ?: settingId
                    add(TriggerItem.Setting(settingId = settingId, word = trigger.word, triggerAfterSpace = trigger.triggerAfterSpace, itemName = settingName))
                }
                prefs.getAllNoteTriggers().forEach { (noteId, trigger) ->
                    val noteTitle = notesById[noteId]?.title?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.notes_untitled)
                    add(TriggerItem.Note(noteId = noteId, word = trigger.word, triggerAfterSpace = trigger.triggerAfterSpace, itemName = noteTitle))
                }
            }.let { list ->
                // Deduplicate: remove legacy bare-ID entries when a full packageName:id entry exists
                val shortcutItems = list.filterIsInstance<TriggerItem.AppShortcut>()
                val fullKeyBareIds = shortcutItems
                    .filter { ':' in it.shortcutId }
                    .map { it.shortcutId.substringAfter(':') }
                    .toSet()
                val legacyToRemove = shortcutItems
                    .filter { ':' !in it.shortcutId && it.shortcutId in fullKeyBareIds }
                    .toSet()
                if (legacyToRemove.isEmpty()) list else list.filter { it !in legacyToRemove }
            }.sortedBy { it.word.lowercase() }
        }
        allItems = items
    }

    if (allItems.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_triggers_empty),
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
            text = stringResource(R.string.settings_triggers_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = DesignTokens.SectionDescriptionBottomPadding),
        )

        SettingsCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                allItems.forEachIndexed { index, item ->
                    TriggerItemRow(
                        item = item,
                        onRemove = {
                            val prefs = TriggerPreferences(context)
                            when (item) {
                                is TriggerItem.App -> prefs.setAppTrigger(item.packageName, null)
                                is TriggerItem.AppShortcut -> prefs.setAppShortcutTrigger(item.shortcutId, null)
                                is TriggerItem.Contact -> prefs.setContactTrigger(item.contactId, null)
                                is TriggerItem.ContactAction -> prefs.setContactActionTrigger(item.contactId, item.action, null)
                                is TriggerItem.File -> prefs.setFileTrigger(item.uri, null)
                                is TriggerItem.Setting -> prefs.setSettingTrigger(item.settingId, null)
                                is TriggerItem.Note -> prefs.setNoteTrigger(item.noteId, null)
                            }
                            refreshSignal++
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
private fun TriggerItemRow(
    item: TriggerItem,
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
            TriggerItemIcon(item = item)

            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = item.word,
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
private fun TriggerItemIcon(item: TriggerItem) {
    when (item) {
        is TriggerItem.App -> {
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
        is TriggerItem.AppShortcut -> {
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
        is TriggerItem.Contact -> Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
        is TriggerItem.ContactAction -> Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
        is TriggerItem.File -> Icon(
            imageVector = Icons.AutoMirrored.Rounded.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
        is TriggerItem.Setting -> Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
        is TriggerItem.Note -> Icon(
            imageVector = Icons.Rounded.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
    }
}

private sealed class TriggerItem {
    abstract val word: String
    abstract val triggerAfterSpace: Boolean
    abstract val itemName: String

    data class App(
        val packageName: String,
        override val word: String,
        override val triggerAfterSpace: Boolean,
        override val itemName: String,
    ) : TriggerItem()

    data class AppShortcut(
        val shortcutId: String,
        override val word: String,
        override val triggerAfterSpace: Boolean,
        override val itemName: String,
        val iconBase64: String?,
    ) : TriggerItem()

    data class Contact(
        val contactId: Long,
        override val word: String,
        override val triggerAfterSpace: Boolean,
        override val itemName: String,
    ) : TriggerItem()

    data class ContactAction(
        val contactId: Long,
        val action: ContactCardAction,
        override val word: String,
        override val triggerAfterSpace: Boolean,
        override val itemName: String,
    ) : TriggerItem()

    data class File(
        val uri: String,
        override val word: String,
        override val triggerAfterSpace: Boolean,
        override val itemName: String,
    ) : TriggerItem()

    data class Setting(
        val settingId: String,
        override val word: String,
        override val triggerAfterSpace: Boolean,
        override val itemName: String,
    ) : TriggerItem()

    data class Note(
        val noteId: Long,
        override val word: String,
        override val triggerAfterSpace: Boolean,
        override val itemName: String,
    ) : TriggerItem()
}

private fun loadContactName(
    context: android.content.Context,
    contactId: Long,
): String =
    try {
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

private fun contactActionLabel(
    context: android.content.Context,
    action: ContactCardAction,
): String =
    when (action) {
        is ContactCardAction.Phone -> context.getString(R.string.contact_method_call_label)
        is ContactCardAction.Sms -> context.getString(R.string.contact_method_message_label)
        is ContactCardAction.WhatsAppCall -> context.getString(R.string.contact_method_whatsapp_voice_call_label)
        is ContactCardAction.WhatsAppMessage -> context.getString(R.string.contact_method_whatsapp_message_label)
        is ContactCardAction.WhatsAppVideoCall -> context.getString(R.string.contact_method_whatsapp_video_call_label)
        is ContactCardAction.TelegramMessage -> context.getString(R.string.contact_method_telegram_message_label)
        is ContactCardAction.TelegramCall -> context.getString(R.string.contact_method_telegram_voice_call_label)
        is ContactCardAction.TelegramVideoCall -> context.getString(R.string.contact_method_telegram_video_call_label)
        is ContactCardAction.SignalMessage -> context.getString(R.string.contact_method_signal_message_label)
        is ContactCardAction.SignalCall -> context.getString(R.string.contact_method_signal_voice_call_label)
        is ContactCardAction.SignalVideoCall -> context.getString(R.string.contact_method_signal_video_call_label)
        is ContactCardAction.GoogleMeet -> context.getString(R.string.contact_method_google_meet_label)
        is ContactCardAction.Email -> context.getString(R.string.contact_method_email_label)
        is ContactCardAction.VideoCall -> context.getString(R.string.contacts_action_button_video_call)
        is ContactCardAction.CustomApp -> action.displayLabel
        is ContactCardAction.ViewInContactsApp -> context.getString(R.string.contacts_action_button_contacts)
    }

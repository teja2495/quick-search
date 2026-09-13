package com.tk.quicksearch.pinnedNotifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.WidgetActionActivity
import com.tk.quicksearch.widgets.customButtonsWidget.rememberWidgetButtonIcon
import org.json.JSONArray

object PinnedNotifications {
    private const val PreferencesName = "pinned_notification_items"
    private const val ActionsKey = "actions_json"
    private const val SeparateActionsKey = "separate_actions_json"
    private const val ChannelId = "pinned_shortcuts"
    private const val NotificationId = 7_300
    private const val ItemsPerRow = 4

    enum class PinMode {
        WITH_OTHER_ITEMS,
        SEPARATELY,
    }

    fun pin(context: Context, action: CustomWidgetButtonAction, mode: PinMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            context.startActivity(PinnedNotificationPermissionActivity.createIntent(context, action, mode))
            return
        }
        savePin(context, action, mode)
        show(context)
    }

    fun isPinned(context: Context, action: CustomWidgetButtonAction): Boolean =
        actions(context).any { it.matches(action) } || separateActions(context).any { it.matches(action) }

    fun pinnedItems(context: Context): List<CustomWidgetButtonAction> = actions(context)

    fun updatePinnedNote(context: Context, note: NoteInfo) {
        val updatedTitle = note.title.ifBlank { context.getString(R.string.notes_untitled) }
        fun update(actions: List<CustomWidgetButtonAction>): Pair<List<CustomWidgetButtonAction>, Boolean> {
            var changed = false
            val updatedActions =
                actions.map { action ->
                    if (action is CustomWidgetButtonAction.Note && action.noteId == note.noteId) {
                        changed = changed ||
                            action.title != updatedTitle || action.markdownContent != note.markdownContent
                        action.copy(
                            title = updatedTitle,
                            markdownContent = note.markdownContent,
                        )
                    } else {
                        action
                    }
                }
            return updatedActions to changed
        }

        val (updatedCombined, combinedChanged) = update(actions(context))
        val (updatedSeparate, separateChanged) = update(separateActions(context))
        if (!combinedChanged && !separateChanged) return
        if (combinedChanged) saveActions(context, updatedCombined)
        if (separateChanged) saveSeparateActions(context, updatedSeparate)
        show(context)
    }

    fun remove(context: Context, action: CustomWidgetButtonAction) {
        saveActions(context, actions(context).filterNot { it.matches(action) })
        val separateItem = separateActions(context).firstOrNull { it.matches(action) }
        saveSeparateActions(context, separateActions(context).filterNot { it.matches(action) })
        separateItem?.let {
            NotificationManagerCompat.from(context).cancel(separateNotificationId(it))
        }
        show(context)
    }

    fun reorder(context: Context, actions: List<CustomWidgetButtonAction>) {
        saveActions(context, actions)
        show(context)
    }

    fun toggle(context: Context, action: CustomWidgetButtonAction) {
        if (isPinned(context, action)) {
            remove(context, action)
        } else {
            context.startActivity(PinnedNotificationChoiceActivity.createIntent(context, action))
        }
    }

    fun completePermissionRequest(context: Context, action: CustomWidgetButtonAction, mode: PinMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            savePin(context, action, mode)
            show(context)
        }
    }

    fun show(context: Context) {
        val actions = actions(context)
        if (actions.isEmpty()) {
            NotificationManagerCompat.from(context).cancel(NotificationId)
        } else {
            createChannel(context)
            val compact = buildRemoteViews(context, actions.take(ItemsPerRow), showLabels = false)
            val expanded = buildRemoteViews(context, actions, showLabels = true)
            val notification = NotificationCompat.Builder(context, ChannelId)
                .setSmallIcon(R.drawable.ic_pin)
                .setContentTitle(context.getString(R.string.pinned_notifications_title))
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setDeleteIntent(restorePendingIntent(context, NotificationId))
                .setCustomContentView(compact)
                .setCustomBigContentView(expanded)
                .build()
            NotificationManagerCompat.from(context).notify(NotificationId, notification)
        }
        createChannel(context)
        separateActions(context).forEach { action -> showSeparate(context, action) }
    }

    private fun showSeparate(context: Context, action: CustomWidgetButtonAction) {
        val notificationId = separateNotificationId(action)
        val iconSize = (context.resources.displayMetrics.density * 48).toInt()
        val iconPackPackage = UserAppPreferences(context).uiPreferences.getSelectedIconPackPackage()
        val icon = rememberWidgetButtonIcon(
            context = context,
            action = action,
            iconSizePx = iconSize,
            textIconColor = Color.White,
            iconPackPackage = iconPackPackage,
        )
        val largeIcon = notificationIconBitmap(context, action, icon.bitmap, icon.drawableResId, iconSize)
            ?: icon.drawableResId?.let { drawableResId ->
                ContextCompat.getDrawable(context, drawableResId)?.toBitmap(width = iconSize, height = iconSize)
            }
        val launchIntent = WidgetActionActivity.createIntent(context, action).apply {
            data = android.net.Uri.parse("quicksearch://separate-pinned-notification/${action.stableKey().hashCode()}")
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val unpinIntent = Intent(context, PinnedNotificationUnpinReceiver::class.java)
            .putExtra(PinnedNotificationUnpinReceiver.ExtraAction, action.toJson())
            .setData(android.net.Uri.parse("quicksearch://unpin/${action.stableKey().hashCode()}"))
        val unpinPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            unpinIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_pin)
            .setContentTitle(action.displayLabel())
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setDeleteIntent(restorePendingIntent(context, notificationId))
        if (action is CustomWidgetButtonAction.Note && action.markdownContent.isNotBlank()) {
            val copyIntent = Intent(context, PinnedNotificationCopyContentReceiver::class.java)
                .putExtra(PinnedNotificationCopyContentReceiver.ExtraContent, action.markdownContent)
                .setData(android.net.Uri.parse("quicksearch://copy-content/${action.stableKey().hashCode()}"))
            val copyPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                R.drawable.ic_copy_content,
                context.getString(R.string.action_copy_content),
                copyPendingIntent,
            )
        }
        builder.addAction(R.drawable.ic_unpin, context.getString(R.string.action_unpin_app), unpinPendingIntent)
        largeIcon?.let(builder::setLargeIcon)
        if (action is CustomWidgetButtonAction.Note && action.markdownContent.isNotBlank()) {
            val noteContent = action.markdownContent.normalizedNotificationText()
            builder
                .setContentText(noteContent.compactNotePreview())
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(noteContent.take(MaxExpandedNoteCharacters)),
                )
        }
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private fun buildRemoteViews(
        context: Context,
        actions: List<CustomWidgetButtonAction>,
        showLabels: Boolean,
    ): RemoteViews {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return RemoteViews(context.packageName, R.layout.notification_pinned_items)
        }
        return RemoteViews(context.packageName, R.layout.notification_pinned_items).apply {
            actions.chunked(ItemsPerRow).forEachIndexed { rowIndex, rowActions ->
                val row = RemoteViews(context.packageName, R.layout.notification_pinned_row)
                rowActions.forEachIndexed { columnIndex, action ->
                    row.addView(
                        R.id.pinned_notification_row,
                        itemView(context, action, rowIndex * ItemsPerRow + columnIndex, showLabels),
                    )
                }
                addView(R.id.pinned_notification_rows, row)
            }
        }
    }

    private fun itemView(
        context: Context,
        action: CustomWidgetButtonAction,
        index: Int,
        showLabels: Boolean,
    ): RemoteViews {
        val iconSize = (context.resources.displayMetrics.density * 32).toInt()
        val iconPackPackage =
            UserAppPreferences(context).uiPreferences.getSelectedIconPackPackage()
        val icon =
            rememberWidgetButtonIcon(
                context = context,
                action = action,
                iconSizePx = iconSize,
                textIconColor = Color.White,
                iconPackPackage = iconPackPackage,
            )
        return RemoteViews(context.packageName, R.layout.notification_pinned_item).apply {
            notificationIconBitmap(context, action, icon.bitmap, icon.drawableResId, iconSize)?.let {
                setImageViewBitmap(R.id.pinned_notification_icon, it)
            }
                ?: icon.drawableResId?.let { setImageViewResource(R.id.pinned_notification_icon, it) }
            setTextViewText(R.id.pinned_notification_label, action.displayLabel())
            setViewVisibility(
                R.id.pinned_notification_label,
                if (showLabels) android.view.View.VISIBLE else android.view.View.GONE,
            )
            val launchIntent = WidgetActionActivity.createIntent(context, action).apply {
                data = android.net.Uri.parse("quicksearch://pinned-notification/$index/${action.toJson().hashCode()}")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                index,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            setOnClickPendingIntent(R.id.pinned_notification_icon, pendingIntent)
            setOnClickPendingIntent(R.id.pinned_notification_label, pendingIntent)
        }
    }

    private fun notificationIconBitmap(
        context: Context,
        action: CustomWidgetButtonAction,
        bitmap: android.graphics.Bitmap?,
        drawableResId: Int?,
        iconSize: Int,
    ): android.graphics.Bitmap? {
        if (bitmap != null) return bitmap
        val isDarkMode =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        if (action !is CustomWidgetButtonAction.Note || !isDarkMode || drawableResId == null) return null
        return ContextCompat.getDrawable(context, drawableResId)
            ?.mutate()
            ?.apply { setTint(0xFFDCDCDC.toInt()) }
            ?.toBitmap(width = iconSize, height = iconSize)
    }

    private fun actions(context: Context): List<CustomWidgetButtonAction> =
        readActions(context, ActionsKey)

    private fun separateActions(context: Context): List<CustomWidgetButtonAction> =
        readActions(context, SeparateActionsKey)

    private fun readActions(context: Context, key: String): List<CustomWidgetButtonAction> =
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .getString(key, null)
            ?.let { raw -> runCatching { JSONArray(raw) }.getOrNull() }
            ?.let { array -> List(array.length()) { index -> CustomWidgetButtonAction.fromJson(array.optString(index)) } }
            ?.filterNotNull()
            .orEmpty()

    private fun saveActions(context: Context, actions: List<CustomWidgetButtonAction>) {
        save(context, ActionsKey, actions)
    }

    private fun saveSeparateActions(context: Context, actions: List<CustomWidgetButtonAction>) {
        save(context, SeparateActionsKey, actions)
    }

    private fun save(context: Context, key: String, actions: List<CustomWidgetButtonAction>) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(key, JSONArray(actions.map { it.toJson() }).toString())
            .apply()
    }

    private fun savePin(context: Context, action: CustomWidgetButtonAction, mode: PinMode) {
        val existingCombined = actions(context).filterNot { it.matches(action) }
        val existingSeparate = separateActions(context).filterNot { it.matches(action) }
        when (mode) {
            PinMode.WITH_OTHER_ITEMS -> {
                saveActions(context, existingCombined + action)
                saveSeparateActions(context, existingSeparate)
                NotificationManagerCompat.from(context).cancel(separateNotificationId(action))
            }
            PinMode.SEPARATELY -> {
                saveActions(context, existingCombined)
                saveSeparateActions(context, existingSeparate + action)
            }
        }
    }

    private fun restorePendingIntent(context: Context, notificationId: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            notificationId,
            Intent(context, PinnedNotificationRestoreReceiver::class.java)
                .setData(android.net.Uri.parse("quicksearch://restore/$notificationId")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun separateNotificationId(action: CustomWidgetButtonAction): Int =
        0x40000000 or (action.stableKey().hashCode() and 0x3fffffff)

    private fun CustomWidgetButtonAction.matches(other: CustomWidgetButtonAction): Boolean =
        stableKey() == other.stableKey()

    private fun CustomWidgetButtonAction.stableKey(): String =
        when (this) {
            is CustomWidgetButtonAction.App -> "app:$packageName:${userHandleId ?: -1}"
            is CustomWidgetButtonAction.Contact -> "contact:$contactId:${serializedAction.orEmpty()}"
            is CustomWidgetButtonAction.File -> "file:$uri"
            is CustomWidgetButtonAction.Setting -> "setting:$id"
            is CustomWidgetButtonAction.AppShortcut -> "shortcut:$packageName:$id"
            is CustomWidgetButtonAction.Note -> "note:$noteId"
        }

    private fun String.normalizedNotificationText(): String =
        replace("\r\n", "\n").replace('\r', '\n').trim()

    private fun String.compactNotePreview(): String =
        lineSequence()
            .take(CompactNotePreviewLines)
            .joinToString("\n")
            .take(CompactNotePreviewCharacters)

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ChannelId,
                context.getString(R.string.pinned_notifications_title),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.pinned_notifications_channel_description) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private const val CompactNotePreviewLines = 3
    private const val CompactNotePreviewCharacters = 240
    private const val MaxExpandedNoteCharacters = 5_000
}

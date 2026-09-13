package com.tk.quicksearch.pinnedNotifications

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.tk.quicksearch.R

class PinnedNotificationCopyContentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val content = intent.getStringExtra(ExtraContent).orEmpty()
        if (content.isBlank()) return
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.action_copy_content), content))
    }

    companion object {
        const val ExtraContent = "pinned_notification_content"
    }
}

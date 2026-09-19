package com.tk.quicksearch.search.searchScreen

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.tk.quicksearch.search.notes.PendingSnippetUndo
import com.tk.quicksearch.search.notes.SnippetExpander
import com.tk.quicksearch.search.notes.SnippetKeywordCache
import com.tk.quicksearch.search.notes.SnippetReplacement

/**
 * Backs two features: the Home double-tap lock gesture, and snippet keyword expansion in editable
 * fields across the system.
 */
class LockScreenAccessibilityService : AccessibilityService() {
    /** Text this service wrote itself; used to ignore the echo events our own writes produce. */
    private var selfWrittenText: String? = null
    private var pendingUndo: PendingSnippetUndo? = null

    override fun onServiceConnected() {
        instance = this
        SnippetKeywordCache.load(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> handleTextChanged(event)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            -> clearUndoWindow()
            else -> Unit
        }
    }

    private fun handleTextChanged(event: AccessibilityEvent) {
        if (event.packageName == packageName) return
        val node = event.source ?: return
        if (!node.isEditable || node.isPassword) {
            clearUndoWindow()
            return
        }

        val text = node.text?.toString().orEmpty()
        // Our own ACTION_SET_TEXT/ACTION_SET_SELECTION echo back as text changes.
        if (text == selfWrittenText) return
        selfWrittenText = null

        val cursor = node.textSelectionEnd
        if (cursor !in 0..text.length) {
            clearUndoWindow()
            return
        }

        pendingUndo?.let { pending ->
            // The undo handle survives exactly one edit: deleting the space we appended.
            clearUndoWindow()
            val undo = SnippetExpander.computeUndo(text, cursor, pending)
            if (undo != null) {
                applyReplacement(node, undo)
                return
            }
        }

        val (replacement, undo) =
            SnippetExpander.computeExpansion(text, cursor, SnippetKeywordCache.current()) ?: return
        if (applyReplacement(node, replacement)) {
            pendingUndo = undo
        }
    }

    /** Writes [replacement] into [node], returning false when the field rejects programmatic edits. */
    private fun applyReplacement(
        node: AccessibilityNodeInfo,
        replacement: SnippetReplacement,
    ): Boolean {
        val setTextArgs =
            Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    replacement.text,
                )
            }
        // WebViews and some custom editors refuse ACTION_SET_TEXT; leave their text untouched.
        if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setTextArgs)) {
            selfWrittenText = null
            return false
        }
        selfWrittenText = replacement.text

        val selectionArgs =
            Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, replacement.cursor)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, replacement.cursor)
            }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
        return true
    }

    private fun clearUndoWindow() {
        pendingUndo = null
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) {
            instance = null
            SnippetKeywordCache.clear()
        }
        super.onDestroy()
    }

    private fun lockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }

    companion object {
        @Volatile
        private var instance: LockScreenAccessibilityService? = null

        fun lockScreen() {
            instance?.lockScreen()
        }

        fun isEnabled(context: Context): Boolean {
            val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
                ?: return false
            return accessibilityManager
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { service ->
                    service.resolveInfo.serviceInfo.packageName == context.packageName &&
                        service.resolveInfo.serviceInfo.name == LockScreenAccessibilityService::class.java.name
                }
        }
    }
}

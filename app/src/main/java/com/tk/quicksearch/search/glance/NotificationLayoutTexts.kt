package com.tk.quicksearch.search.glance

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView

/** A visible, non-blank text view of an inflated notification layout, with its id's entry name when it has one. */
internal class LayoutText(
    val idName: String?,
    val text: String,
)

/**
 * Inflates a notification's custom layout and reads its visible, non-blank text views in layout
 * order, or null when it can't be inflated. Runs on the listener's main thread, as view inflation needs.
 */
internal fun RemoteViews.readLayoutTexts(context: Context): List<LayoutText>? =
    runCatching {
        val textViews = mutableListOf<TextView>()
        apply(context, FrameLayout(context)).collectTextViews(textViews)
        textViews.mapNotNull { view ->
            view.text?.toString()?.trim()?.takeIf(String::isNotEmpty)?.let { LayoutText(view.entryName(), it) }
        }
    }.getOrNull()

private fun View.entryName(): String? =
    if (id == View.NO_ID) null else runCatching { resources.getResourceEntryName(id) }.getOrNull()

private fun View.collectTextViews(into: MutableList<TextView>) {
    if (visibility != View.VISIBLE) return
    if (this is TextView) into += this
    if (this is ViewGroup) {
        for (index in 0 until childCount) getChildAt(index).collectTextViews(into)
    }
}

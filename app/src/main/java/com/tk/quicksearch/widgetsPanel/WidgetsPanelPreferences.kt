package com.tk.quicksearch.widgetsPanel

import android.content.ComponentName
import android.content.Context
import com.tk.quicksearch.search.data.preferences.BasePreferences
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class PanelWidgetInfo(
    val appWidgetId: Int,
    val providerPackage: String,
    val providerClassName: String,
    val column: Int? = null,
    val row: Int? = null,
    val columnSpan: Int? = null,
    val rowSpan: Int? = null,
)

internal const val QUICK_NOTE_PANEL_WIDGET_ID = -2

internal fun PanelWidgetInfo.isQuickNoteWidget(): Boolean =
    appWidgetId == QUICK_NOTE_PANEL_WIDGET_ID

class WidgetsPanelPreferences(
    context: Context,
) : BasePreferences(context) {
    fun getWidgets(): List<PanelWidgetInfo> {
        val stored = prefs.getString(KEY_WIDGETS_PANEL_ITEMS, null).orEmpty()
        if (stored.isBlank()) return emptyList()
        return try {
            val array = JSONArray(stored)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val appWidgetId = item.optInt(FIELD_APP_WIDGET_ID, -1)
                    val providerPackage = item.optString(FIELD_PROVIDER_PACKAGE)
                    val providerClassName = item.optString(FIELD_PROVIDER_CLASS)
                    if (
                        appWidgetId != -1 &&
                        providerPackage.isNotBlank() &&
                        providerClassName.isNotBlank()
                    ) {
                        add(
                            PanelWidgetInfo(
                                appWidgetId = appWidgetId,
                                providerPackage = providerPackage,
                                providerClassName = providerClassName,
                                column = item.optGridInt(FIELD_COLUMN),
                                row = item.optGridInt(FIELD_ROW),
                                columnSpan = item.optGridInt(FIELD_COLUMN_SPAN),
                                rowSpan = item.optGridInt(FIELD_ROW_SPAN),
                            ),
                        )
                    }
                }
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    fun setWidgets(widgets: List<PanelWidgetInfo>) {
        val array = JSONArray()
        widgets.forEach { widget ->
            array.put(
                JSONObject()
                    .put(FIELD_APP_WIDGET_ID, widget.appWidgetId)
                    .put(FIELD_PROVIDER_PACKAGE, widget.providerPackage)
                    .put(FIELD_PROVIDER_CLASS, widget.providerClassName)
                    .apply {
                        widget.column?.let { put(FIELD_COLUMN, it) }
                        widget.row?.let { put(FIELD_ROW, it) }
                        widget.columnSpan?.let { put(FIELD_COLUMN_SPAN, it) }
                        widget.rowSpan?.let { put(FIELD_ROW_SPAN, it) }
                    },
            )
        }
        prefs.edit().putString(KEY_WIDGETS_PANEL_ITEMS, array.toString()).apply()
    }

    fun getQuickNoteWidget(): PanelWidgetInfo =
        PanelWidgetInfo(
            appWidgetId = QUICK_NOTE_PANEL_WIDGET_ID,
            providerPackage = "",
            providerClassName = "",
            column = prefs.getInt(KEY_QUICK_NOTE_COLUMN, 0).coerceAtLeast(0),
            row = prefs.getInt(KEY_QUICK_NOTE_ROW, 0).coerceAtLeast(0),
            columnSpan = WIDGET_PANEL_GRID_COLUMNS,
            rowSpan = WIDGET_PANEL_DEFAULT_ROW_SPAN,
        )

    fun setQuickNoteWidget(widget: PanelWidgetInfo) {
        require(widget.isQuickNoteWidget())
        prefs.edit()
            .putInt(KEY_QUICK_NOTE_COLUMN, widget.column ?: 0)
            .putInt(KEY_QUICK_NOTE_ROW, widget.row ?: 0)
            .apply()
    }

    fun addWidget(
        appWidgetId: Int,
        provider: ComponentName,
        columnSpan: Int,
        rowSpan: Int,
    ): List<PanelWidgetInfo> {
        val next =
            getWidgets() +
                PanelWidgetInfo(
                    appWidgetId = appWidgetId,
                    providerPackage = provider.packageName,
                    providerClassName = provider.className,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan,
                )
        setWidgets(next)
        return next
    }

    fun removeWidget(appWidgetId: Int): List<PanelWidgetInfo> {
        val next = getWidgets().filterNot { it.appWidgetId == appWidgetId }
        setWidgets(next)
        return next
    }

    private companion object {
        const val KEY_WIDGETS_PANEL_ITEMS = "widgets_panel_items"
        const val FIELD_APP_WIDGET_ID = "appWidgetId"
        const val FIELD_PROVIDER_PACKAGE = "providerPackage"
        const val FIELD_PROVIDER_CLASS = "providerClassName"
        const val FIELD_COLUMN = "column"
        const val FIELD_ROW = "row"
        const val FIELD_COLUMN_SPAN = "columnSpan"
        const val FIELD_ROW_SPAN = "rowSpan"
        const val KEY_QUICK_NOTE_COLUMN = "quick_note_widget_column"
        const val KEY_QUICK_NOTE_ROW = "quick_note_widget_row"
    }
}

private fun JSONObject.optGridInt(field: String): Int? =
    if (has(field)) {
        optInt(field).takeIf { it >= 0 }
    } else {
        null
    }

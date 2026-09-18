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
    /** Home placement; null when the widget is only in the widgets panel. */
    val home: HomeWidgetPlacement? = null,
)

/**
 * Where a widget pinned to Home renders. [anchor] is the name of the Home layout item the widget
 * sits before in logical Home order ([HOME_WIDGET_ANCHOR_END] for after every item), so the widget
 * keeps its place while Home sections load in or hide. [order] breaks ties between widgets that
 * share an anchor. Spans use the Home width split into [WIDGET_PANEL_GRID_COLUMNS] columns and are
 * independent of the panel grid spans.
 */
data class HomeWidgetPlacement(
    val anchor: String,
    val order: Int,
    val column: Int,
    val columnSpan: Int,
    val rowSpan: Int,
)

internal const val HOME_WIDGET_ANCHOR_END = "END"

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
                                home = item.optJSONObject(FIELD_HOME)?.toHomePlacement(),
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
                        widget.home?.let { put(FIELD_HOME, it.toJson()) }
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

    /** Replaces the Home placements of the given widgets, leaving their panel placement alone. */
    fun setHomePlacements(placements: Map<Int, HomeWidgetPlacement?>): List<PanelWidgetInfo> {
        val next =
            getWidgets().map { widget ->
                if (widget.appWidgetId in placements) {
                    widget.copy(home = placements[widget.appWidgetId])
                } else {
                    widget
                }
            }
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
        const val FIELD_HOME = "home"
        const val KEY_QUICK_NOTE_COLUMN = "quick_note_widget_column"
        const val KEY_QUICK_NOTE_ROW = "quick_note_widget_row"
    }
}

private const val FIELD_HOME_ANCHOR = "anchor"
private const val FIELD_HOME_ORDER = "order"
private const val FIELD_HOME_COLUMN = "column"
private const val FIELD_HOME_COLUMN_SPAN = "columnSpan"
private const val FIELD_HOME_ROW_SPAN = "rowSpan"

private fun HomeWidgetPlacement.toJson(): JSONObject =
    JSONObject()
        .put(FIELD_HOME_ANCHOR, anchor)
        .put(FIELD_HOME_ORDER, order)
        .put(FIELD_HOME_COLUMN, column)
        .put(FIELD_HOME_COLUMN_SPAN, columnSpan)
        .put(FIELD_HOME_ROW_SPAN, rowSpan)

private fun JSONObject.toHomePlacement(): HomeWidgetPlacement? {
    val anchor = optString(FIELD_HOME_ANCHOR).takeIf { it.isNotBlank() } ?: return null
    val columnSpan = optInt(FIELD_HOME_COLUMN_SPAN, WIDGET_PANEL_GRID_COLUMNS)
        .coerceIn(1, WIDGET_PANEL_GRID_COLUMNS)
    return HomeWidgetPlacement(
        anchor = anchor,
        order = optInt(FIELD_HOME_ORDER, 0),
        column = optInt(FIELD_HOME_COLUMN, 0).coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan),
        columnSpan = columnSpan,
        rowSpan = optInt(FIELD_HOME_ROW_SPAN, WIDGET_PANEL_DEFAULT_ROW_SPAN)
            .coerceIn(1, WIDGET_PANEL_MAX_ROW_SPAN),
    )
}

private fun JSONObject.optGridInt(field: String): Int? =
    if (has(field)) {
        optInt(field).takeIf { it >= 0 }
    } else {
        null
    }

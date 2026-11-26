package com.tk.quicksearch.widgetsPanel

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
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
    /** True for a widgets panel record; false for a Home record. */
    val isInPanel: Boolean = true,
    /**
     * Home placement, set only on Home records. The panel and Home keep separate records (and
     * separate widget instances), so a widget on one is independent of any on the other.
     */
    val home: HomeWidgetPlacement? = null,
    /**
     * Further pages of a widget stack, after this widget, which is the first page and owns the
     * stack's placement. Empty for a regular widget.
     */
    val stack: List<StackedPanelWidget> = emptyList(),
)

/** True for a Home record, false for a widgets panel record. */
internal val PanelWidgetInfo.isOnHome: Boolean
    get() = home != null

/** The record on Home ([onHome]) or in the panel whose first page is [ownerId]. */
internal fun PanelWidgetInfo.isOwner(
    ownerId: Int,
    onHome: Boolean,
): Boolean = appWidgetId == ownerId && isOnHome == onHome

/**
 * Splits records from before the panel and Home kept separate records: a widget that was in the
 * panel and pinned to Home becomes a panel record (with its stack) and a Home record showing its
 * first page, as Home did then. Both keep using the one widget id they shared.
 */
internal fun List<PanelWidgetInfo>.splitSharedRecords(): List<PanelWidgetInfo> =
    flatMap { widget ->
        if (widget.isInPanel && widget.home != null) {
            listOf(widget.copy(home = null), widget.copy(isInPanel = false, stack = emptyList()))
        } else {
            listOf(widget)
        }
    }

/** Whether any record still shows [appWidgetId], so its widget id must not be released. */
/**
 * Turns the Home record [ownerId] into a panel record that keeps its size; the grid places it in
 * the first free spot. Pages the panel already shows are dropped so no widget id appears there
 * twice, and if that leaves nothing the Home record is simply removed.
 */
internal fun List<PanelWidgetInfo>.withHomeWidgetMovedToPanel(ownerId: Int): List<PanelWidgetInfo> {
    val source = firstOrNull { it.isOwner(ownerId, onHome = true) } ?: return this
    val home = source.home ?: return this
    val shownInPanel = filter { it.isInPanel }.flatMap { it.pageIds }.toSet()
    val stack = source.stack.filterNot { it.appWidgetId in shownInPanel }
    val moved =
        when {
            source.appWidgetId !in shownInPanel -> source.copy(stack = stack)
            stack.isNotEmpty() ->
                source.copy(
                    appWidgetId = stack.first().appWidgetId,
                    providerPackage = stack.first().providerPackage,
                    providerClassName = stack.first().providerClassName,
                    stack = stack.drop(1),
                )
            else -> null
        }?.copy(
            isInPanel = true,
            home = null,
            column = null,
            row = null,
            columnSpan = home.columnSpan,
            rowSpan = home.rowSpan,
        )
    return mapNotNull { widget -> if (widget === source) moved else widget }
}

internal fun List<PanelWidgetInfo>.referencesWidgetId(appWidgetId: Int): Boolean =
    any { appWidgetId in it.pageIds }

/** See [WidgetsPanelPreferences.removeStackPage]. */
internal fun List<PanelWidgetInfo>.withoutStackPage(
    ownerId: Int,
    pageId: Int,
    onHome: Boolean = false,
): List<PanelWidgetInfo> =
    flatMap { widget ->
        when {
            !widget.isOwner(ownerId, onHome) -> listOf(widget)
            pageId != ownerId ->
                listOf(widget.copy(stack = widget.stack.filterNot { it.appWidgetId == pageId }))
            widget.stack.isEmpty() -> emptyList()
            else -> {
                // The next page takes over the stack's record, keeping its place.
                val promoted = widget.stack.first()
                listOf(
                    widget.copy(
                        appWidgetId = promoted.appWidgetId,
                        providerPackage = promoted.providerPackage,
                        providerClassName = promoted.providerClassName,
                        stack = widget.stack.drop(1),
                    ),
                )
            }
        }
    }

/**
 * Puts the pages of the stack owned by [ownerId] in [orderedIds] order. The new first page takes
 * over the stack's record, keeping its placement. Left unchanged when [orderedIds] isn't a reordering of the stack's pages.
 */
internal fun List<PanelWidgetInfo>.withStackOrder(
    ownerId: Int,
    orderedIds: List<Int>,
    onHome: Boolean = false,
): List<PanelWidgetInfo> =
    map { widget ->
        if (!widget.isOwner(ownerId, onHome)) return@map widget
        val pages =
            (listOf(StackedPanelWidget(widget.appWidgetId, widget.providerPackage, widget.providerClassName)) + widget.stack)
                .associateBy { it.appWidgetId }
        if (orderedIds.size != pages.size || orderedIds.toSet() != pages.keys) return@map widget
        val first = pages.getValue(orderedIds.first())
        widget.copy(
            appWidgetId = first.appWidgetId,
            providerPackage = first.providerPackage,
            providerClassName = first.providerClassName,
            stack = orderedIds.drop(1).map(pages::getValue),
        )
    }

/**
 * Drops widgets whose ids are no longer bound, as [WidgetsPanelPreferences.removeStackPage] would:
 * a stack keeps its place while any of its pages is still bound.
 */
internal fun List<PanelWidgetInfo>.dropUnboundPages(unboundIds: Set<Int>): List<PanelWidgetInfo> =
    mapNotNull { widget ->
        val pages = widget.stack.filterNot { it.appWidgetId in unboundIds }
        when {
            widget.appWidgetId !in unboundIds -> widget.copy(stack = pages)
            pages.isEmpty() -> null
            else ->
                widget.copy(
                    appWidgetId = pages.first().appWidgetId,
                    providerPackage = pages.first().providerPackage,
                    providerClassName = pages.first().providerClassName,
                    stack = pages.drop(1),
                )
        }
    }

/** A widget shown as a later page of a widget stack in the panel. */
data class StackedPanelWidget(
    val appWidgetId: Int,
    val providerPackage: String,
    val providerClassName: String,
)

/** Every hosted widget id in this panel item: the first page, then the stacked pages. */
internal val PanelWidgetInfo.pageIds: List<Int>
    get() = listOf(appWidgetId) + stack.map { it.appWidgetId }

/** The panel item that shows [appWidgetId], as its first page or as a stacked page. */
internal fun List<PanelWidgetInfo>.stackOwnerOf(appWidgetId: Int): PanelWidgetInfo? =
    firstOrNull { it.appWidgetId == appWidgetId } ?:
        firstOrNull { item -> item.stack.any { it.appWidgetId == appWidgetId } }

/**
 * Where a widget pinned to Home renders. [anchor] is the name of the Home layout item the widget
 * sits before in logical Home order ([HOME_WIDGET_ANCHOR_END] for after every item), so the widget
 * keeps its place while Home sections load in or hide. [order] breaks ties between widgets that
 * share an anchor. Spans use the Home width split into [WIDGET_PANEL_GRID_COLUMNS] columns and are
 * independent of the panel grid spans. [gapSteps] is empty space the user dragged out before the
 * widget in logical order, in [HOME_WIDGET_GAP_STEPS_MAX]-capped steps, so it sits above the widget
 * normally and below it in one-handed mode. [offsetSteps], when set, takes over from [gapSteps]: it
 * is the widget's distance from Home's anchored end in gap steps, so the widget holds its spot and
 * Home content growing toward it fills the empty space before pushing it.
 */
data class HomeWidgetPlacement(
    val anchor: String,
    val order: Int,
    val column: Int,
    val columnSpan: Int,
    val rowSpan: Int,
    val gapSteps: Int = 0,
    val offsetSteps: Int? = null,
)

/** Largest empty space a Home widget can hold, in gap steps. */
internal const val HOME_WIDGET_GAP_STEPS_MAX = 60

/** Farthest a Home widget can sit from Home's anchored end, in gap steps. */
internal const val HOME_WIDGET_OFFSET_STEPS_MAX = 600

internal const val HOME_WIDGET_ANCHOR_END = "END"

internal const val QUICK_NOTE_PANEL_WIDGET_ID = -2

/** Placeholder provider stored for the Quick Note, which is not a hosted app widget. */
internal const val QUICK_NOTE_PROVIDER = "quick_note"

internal fun PanelWidgetInfo.isQuickNoteWidget(): Boolean =
    appWidgetId == QUICK_NOTE_PANEL_WIDGET_ID

internal fun isQuickNotePage(appWidgetId: Int): Boolean = appWidgetId == QUICK_NOTE_PANEL_WIDGET_ID

/** True when the Quick Note is in the panel, on its own or as a page of a stack. */
internal fun List<PanelWidgetInfo>.containsQuickNote(): Boolean =
    any { widget -> widget.isInPanel && widget.pageIds.any(::isQuickNotePage) }

internal fun newQuickNotePanelWidget(): PanelWidgetInfo =
    PanelWidgetInfo(
        appWidgetId = QUICK_NOTE_PANEL_WIDGET_ID,
        providerPackage = QUICK_NOTE_PROVIDER,
        providerClassName = QUICK_NOTE_PROVIDER,
        columnSpan = WIDGET_PANEL_GRID_COLUMNS,
        rowSpan = WIDGET_PANEL_DEFAULT_ROW_SPAN,
    )

class WidgetsPanelPreferences(
    context: Context,
) : BasePreferences(context) {
    fun isLayoutLocked(): Boolean = getBooleanPref(KEY_LAYOUT_LOCKED, false)

    fun setLayoutLocked(locked: Boolean) = setBooleanPref(KEY_LAYOUT_LOCKED, locked)

    fun getWidgets(): List<PanelWidgetInfo> {
        val widgets = readWidgets()
        if (prefs.getBoolean(KEY_QUICK_NOTE_IN_WIDGETS, false)) return widgets
        return migrateQuickNote(widgets)
    }

    /**
     * The Quick Note used to keep its own placement keys beside the widget list. It now lives in
     * the list like any widget, so it can own a stack or be a page of one.
     */
    private fun migrateQuickNote(widgets: List<PanelWidgetInfo>): List<PanelWidgetInfo> {
        val next =
            if (prefs.getBoolean(BasePreferences.KEY_QUICK_NOTE_ENABLED, true) && !widgets.containsQuickNote()) {
                listOf(
                    newQuickNotePanelWidget().copy(
                        column = prefs.getInt(KEY_QUICK_NOTE_COLUMN, 0).coerceAtLeast(0),
                        row = prefs.getInt(KEY_QUICK_NOTE_ROW, 0).coerceAtLeast(0),
                        rowSpan =
                            prefs.getInt(KEY_QUICK_NOTE_ROW_SPAN, WIDGET_PANEL_DEFAULT_ROW_SPAN)
                                .coerceIn(1, WIDGET_PANEL_MAX_ROW_SPAN),
                    ),
                ) + widgets
            } else {
                widgets
            }
        writeWidgets(next, prefs.edit().putBoolean(KEY_QUICK_NOTE_IN_WIDGETS, true))
        return next
    }

    private fun readWidgets(): List<PanelWidgetInfo> {
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
                                isInPanel = item.optBoolean(FIELD_IS_IN_PANEL, true),
                                home = item.optJSONObject(FIELD_HOME)?.toHomePlacement(),
                                stack = item.optJSONArray(FIELD_STACK)?.toStackedWidgets().orEmpty(),
                            ),
                        )
                    }
                }
            }.splitSharedRecords()
        } catch (_: JSONException) {
            emptyList()
        }
    }

    fun setWidgets(widgets: List<PanelWidgetInfo>) {
        writeWidgets(widgets, prefs.edit().putBoolean(KEY_QUICK_NOTE_IN_WIDGETS, true))
    }

    private fun writeWidgets(
        widgets: List<PanelWidgetInfo>,
        editor: SharedPreferences.Editor,
    ) {
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
                        if (!widget.isInPanel) put(FIELD_IS_IN_PANEL, false)
                        widget.home?.let { put(FIELD_HOME, it.toJson()) }
                        if (widget.stack.isNotEmpty()) put(FIELD_STACK, widget.stack.toJson())
                    },
            )
        }
        editor.putString(KEY_WIDGETS_PANEL_ITEMS, array.toString()).apply()
    }

    /** Adds the Quick Note to the panel on its own. */
    fun addQuickNote(): List<PanelWidgetInfo> {
        val current = getWidgets()
        if (current.containsQuickNote()) return current
        val next = current + newQuickNotePanelWidget()
        setWidgets(next)
        return next
    }

    /** Adds the Quick Note as the last page of the stack owned by [ownerId]. */
    fun addQuickNoteToStack(ownerId: Int): List<PanelWidgetInfo> {
        val current = getWidgets()
        if (current.containsQuickNote()) return current
        val page = StackedPanelWidget(QUICK_NOTE_PANEL_WIDGET_ID, QUICK_NOTE_PROVIDER, QUICK_NOTE_PROVIDER)
        val next =
            current.map { widget ->
                if (widget.isOwner(ownerId, onHome = false)) widget.copy(stack = widget.stack + page) else widget
            }
        setWidgets(next)
        return next
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

    /** Adds a widget straight to Home, without a place in the widgets panel. */
    fun addHomeWidget(
        appWidgetId: Int,
        provider: ComponentName,
        columnSpan: Int,
        rowSpan: Int,
    ): List<PanelWidgetInfo> {
        val current = getWidgets()
        val widget =
            PanelWidgetInfo(
                appWidgetId = appWidgetId,
                providerPackage = provider.packageName,
                providerClassName = provider.className,
                columnSpan = columnSpan,
                rowSpan = rowSpan,
                isInPanel = false,
            )
        val next = current + widget.copy(home = defaultHomePlacement(widget, current))
        setWidgets(next)
        return next
    }

    /** Adds [appWidgetId] as the last page of the stack owned by [ownerId], on Home or in the panel. */
    fun addToStack(
        ownerId: Int,
        appWidgetId: Int,
        provider: ComponentName,
        onHome: Boolean = false,
    ): List<PanelWidgetInfo> {
        val page = StackedPanelWidget(appWidgetId, provider.packageName, provider.className)
        val next =
            getWidgets().map { widget ->
                if (widget.isOwner(ownerId, onHome)) widget.copy(stack = widget.stack + page) else widget
            }
        setWidgets(next)
        return next
    }

    /**
     * Removes one page of a stack on Home or in the panel. Removing the first page promotes the
     * next one to own the stack's placement, so the stack keeps its place; a stack left with one page becomes a
     * regular widget. Ids no longer shown anywhere are for the caller to release.
     */
    fun reorderStack(
        ownerId: Int,
        orderedIds: List<Int>,
        onHome: Boolean = false,
    ): List<PanelWidgetInfo> {
        val next = getWidgets().withStackOrder(ownerId, orderedIds, onHome)
        setWidgets(next)
        return next
    }

    fun removeStackPage(
        ownerId: Int,
        pageId: Int,
        onHome: Boolean = false,
    ): List<PanelWidgetInfo> {
        val next = getWidgets().withoutStackPage(ownerId, pageId, onHome)
        setWidgets(next)
        return next
    }

    /**
     * Replaces the placements of the given Home widgets; a null placement removes the widget from
     * Home. Returns the widget ids no record shows any more, so the caller can release them.
     */
    fun setHomePlacements(placements: Map<Int, HomeWidgetPlacement?>): List<Int> {
        val updated =
            getWidgets().map { widget ->
                if (widget.isOnHome && widget.appWidgetId in placements) {
                    widget.copy(home = placements[widget.appWidgetId])
                } else {
                    widget
                }
            }
        val (removed, next) = updated.partition { !it.isInPanel && it.home == null }
        setWidgets(next)
        return removed.flatMap { it.pageIds }.filterNot { isBuiltInPanelPage(it) || next.referencesWidgetId(it) }
    }

    /** Moves a Home widget (with its stack) to the panel. Returns the updated widget list. */
    fun moveHomeWidgetToPanel(ownerId: Int): List<PanelWidgetInfo> {
        val next = getWidgets().withHomeWidgetMovedToPanel(ownerId)
        setWidgets(next)
        return next
    }

    private companion object {
        const val KEY_LAYOUT_LOCKED = "widgets_panel_layout_locked"
        const val KEY_WIDGETS_PANEL_ITEMS = "widgets_panel_items"
        const val FIELD_IS_IN_PANEL = "isInPanel"
        const val FIELD_APP_WIDGET_ID = "appWidgetId"
        const val FIELD_PROVIDER_PACKAGE = "providerPackage"
        const val FIELD_PROVIDER_CLASS = "providerClassName"
        const val FIELD_COLUMN = "column"
        const val FIELD_ROW = "row"
        const val FIELD_COLUMN_SPAN = "columnSpan"
        const val FIELD_ROW_SPAN = "rowSpan"
        const val FIELD_HOME = "home"
        const val FIELD_STACK = "stack"
        const val KEY_QUICK_NOTE_COLUMN = "quick_note_widget_column"
        const val KEY_QUICK_NOTE_ROW = "quick_note_widget_row"
        const val KEY_QUICK_NOTE_ROW_SPAN = "quick_note_widget_row_span"
        const val KEY_QUICK_NOTE_IN_WIDGETS = "widgets_panel_quick_note_in_items"
    }
}

private const val FIELD_HOME_ANCHOR = "anchor"
private const val FIELD_HOME_ORDER = "order"
private const val FIELD_HOME_COLUMN = "column"
private const val FIELD_HOME_COLUMN_SPAN = "columnSpan"
private const val FIELD_HOME_ROW_SPAN = "rowSpan"
private const val FIELD_HOME_GAP_STEPS = "gapSteps"
private const val FIELD_HOME_OFFSET_STEPS = "offsetSteps"
private const val FIELD_STACK_APP_WIDGET_ID = "appWidgetId"
private const val FIELD_STACK_PROVIDER_PACKAGE = "providerPackage"
private const val FIELD_STACK_PROVIDER_CLASS = "providerClassName"

private fun HomeWidgetPlacement.toJson(): JSONObject =
    JSONObject()
        .put(FIELD_HOME_ANCHOR, anchor)
        .put(FIELD_HOME_ORDER, order)
        .put(FIELD_HOME_COLUMN, column)
        .put(FIELD_HOME_COLUMN_SPAN, columnSpan)
        .put(FIELD_HOME_ROW_SPAN, rowSpan)
        .put(FIELD_HOME_GAP_STEPS, gapSteps)
        .apply { offsetSteps?.let { put(FIELD_HOME_OFFSET_STEPS, it) } }

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
        gapSteps = optInt(FIELD_HOME_GAP_STEPS, 0).coerceIn(0, HOME_WIDGET_GAP_STEPS_MAX),
        offsetSteps =
            if (has(FIELD_HOME_OFFSET_STEPS)) {
                optInt(FIELD_HOME_OFFSET_STEPS, 0).coerceIn(0, HOME_WIDGET_OFFSET_STEPS_MAX)
            } else {
                null
            },
    )
}

private fun List<StackedPanelWidget>.toJson(): JSONArray =
    JSONArray().also { array ->
        forEach { page ->
            array.put(
                JSONObject()
                    .put(FIELD_STACK_APP_WIDGET_ID, page.appWidgetId)
                    .put(FIELD_STACK_PROVIDER_PACKAGE, page.providerPackage)
                    .put(FIELD_STACK_PROVIDER_CLASS, page.providerClassName),
            )
        }
    }

private fun JSONArray.toStackedWidgets(): List<StackedPanelWidget> =
    buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val appWidgetId = item.optInt(FIELD_STACK_APP_WIDGET_ID, -1)
            val providerPackage = item.optString(FIELD_STACK_PROVIDER_PACKAGE)
            val providerClassName = item.optString(FIELD_STACK_PROVIDER_CLASS)
            if (appWidgetId != -1 && providerPackage.isNotBlank() && providerClassName.isNotBlank()) {
                add(StackedPanelWidget(appWidgetId, providerPackage, providerClassName))
            }
        }
    }

private fun JSONObject.optGridInt(field: String): Int? =
    if (has(field)) {
        optInt(field).takeIf { it >= 0 }
    } else {
        null
    }

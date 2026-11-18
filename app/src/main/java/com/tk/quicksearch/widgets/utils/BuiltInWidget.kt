package com.tk.quicksearch.widgets.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidget
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetReceiver
import com.tk.quicksearch.widgets.customButtonsWidget.CustomButtonsWidgetReceiver
import com.tk.quicksearch.widgets.mediaControlsWidget.MediaControlsWidget
import com.tk.quicksearch.widgets.mediaControlsWidget.MediaControlsWidgetReceiver
import com.tk.quicksearch.widgets.countdownWidget.CountdownWidget
import com.tk.quicksearch.widgets.countdownWidget.CountdownWidgetReceiver
import com.tk.quicksearch.widgets.noteWidget.NoteWidget
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetReceiver
import com.tk.quicksearch.widgets.searchWidget.SearchWidget
import com.tk.quicksearch.widgets.searchWidget.SearchWidgetReceiver
import com.tk.quicksearch.widgets.worldClockWidget.WorldClockWidget
import com.tk.quicksearch.widgets.worldClockWidget.WorldClockWidgetReceiver
import com.tk.quicksearch.widgets.countdownWidget.CountdownWidgetConfigContent
import com.tk.quicksearch.widgets.worldClockWidget.WorldClockWidgetConfigContent

/** Configuration and redraw behavior for each manifest-registered widget receiver. */
internal enum class BuiltInWidget(
    val receiver: Class<out GlanceAppWidgetReceiver>,
    val createWidget: () -> GlanceAppWidget,
    val defaultSize: () -> DpSize,
    val variant: WidgetVariant? = null,
    val updatesWithPlayback: Boolean = false,
    val configure: (@Composable (Int, () -> Unit, () -> Unit) -> Unit)? = null,
    private val redraw: (suspend (Context, GlanceAppWidget, Int, GlanceId) -> Unit)? = null,
) {
    SEARCH(
        SearchWidgetReceiver::class.java,
        { SearchWidget(WidgetVariant.STANDARD) },
        { SearchWidget.DEFAULT_SIZE },
        variant = WidgetVariant.STANDARD,
        updatesWithPlayback = true,
    ),
    CUSTOM_BUTTONS(
        CustomButtonsWidgetReceiver::class.java,
        { SearchWidget(WidgetVariant.CUSTOM_BUTTONS_ONLY) },
        { SearchWidget.DEFAULT_SIZE },
        variant = WidgetVariant.CUSTOM_BUTTONS_ONLY,
        updatesWithPlayback = true,
    ),
    MEDIA_CONTROLS(
        MediaControlsWidgetReceiver::class.java,
        ::MediaControlsWidget,
        { MediaControlsWidget.DEFAULT_SIZE },
        variant = WidgetVariant.MEDIA_CONTROLS,
        updatesWithPlayback = true,
    ),
    NOTE(
        NoteWidgetReceiver::class.java,
        ::NoteWidget,
        { NoteWidget.DEFAULT_SIZE },
        variant = WidgetVariant.NOTE,
    ),
    COUNTDOWN(
        CountdownWidgetReceiver::class.java,
        ::CountdownWidget,
        { CountdownWidget.DEFAULT_SIZE },
        configure = { id, save, cancel -> CountdownWidgetConfigContent(id, save, cancel) },
    ),
    WORLD_CLOCK(
        WorldClockWidgetReceiver::class.java,
        ::WorldClockWidget,
        { WorldClockWidget.DEFAULT_SIZE },
        configure = { id, save, cancel -> WorldClockWidgetConfigContent(id, save, cancel) },
    ),
    AT_A_GLANCE(
        AtAGlanceWidgetReceiver::class.java,
        ::AtAGlanceWidget,
        { AtAGlanceWidget.DEFAULT_SIZE },
        variant = WidgetVariant.AT_A_GLANCE,
        redraw = { context, widget, id, glanceId -> (widget as AtAGlanceWidget).drawNow(context, id, glanceId) },
    ),
    ;

    suspend fun drawPlaced(context: Context, widget: GlanceAppWidget, id: Int, glanceId: GlanceId) {
        val draw = redraw
        if (draw != null) draw(context, widget, id, glanceId)
        else widget.drawNow(context, id, glanceId, defaultSize())
    }

    companion object {
        private val byReceiver = entries.associateBy { it.receiver.name }
        private val byVariant = entries.mapNotNull { widget -> widget.variant?.let { it to widget } }.toMap()

        fun forReceiver(className: String?): BuiltInWidget? = byReceiver[className]

        fun forVariant(variant: WidgetVariant): BuiltInWidget = byVariant.getValue(variant)
    }
}

package com.tk.quicksearch.widgets.customButtonsWidget

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.tk.quicksearch.media.MediaCommand
import com.tk.quicksearch.media.MediaControls

/** A set of fixed actions, shared by the widget, gesture picker, and notification shortcuts. */
internal data class WidgetCommandDefinition(
    val parameterKey: String,
    val choices: List<WidgetCommandChoice>,
)

internal data class WidgetCommandChoice(
    val id: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val keywords: List<String>,
    val currentIcon: ((Context) -> Int)? = null,
    val execute: (Context) -> Unit,
)

internal val mediaWidgetCommands = WidgetCommandDefinition(
    parameterKey = "mediaCommand",
    choices = MediaCommand.entries.map { command ->
        WidgetCommandChoice(
            id = command.value,
            labelRes = command.labelRes,
            iconRes = command.iconRes,
            keywords = command.searchKeywords.toList(),
            currentIcon = if (command == MediaCommand.PLAY_PAUSE) ::playPauseIconRes else null,
            execute = { MediaControls.dispatch(it, command) },
        )
    },
)

/** Dispatches fixed actions without opening an activity over the launcher. */
class WidgetCommandActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val type = CustomWidgetButtonType.entries.firstOrNull { it.value == parameters[TYPE] } ?: return
        val commandId = parameters[COMMAND_ID] ?: return
        type.command?.choices?.firstOrNull { it.id == commandId }?.execute?.invoke(context)
    }

    companion object {
        val TYPE = ActionParameters.Key<String>("widget_command_type")
        val COMMAND_ID = ActionParameters.Key<String>("widget_command_id")
    }
}

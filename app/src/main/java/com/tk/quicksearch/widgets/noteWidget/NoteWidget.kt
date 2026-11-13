package com.tk.quicksearch.widgets.noteWidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider as DayNightColorProvider
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.notes.NotesRoomStore
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.WidgetActionActivity
import com.tk.quicksearch.widgets.searchWidget.SearchWidgetConfigureActivity
import com.tk.quicksearch.widgets.utils.WidgetBitmapUtils
import com.tk.quicksearch.widgets.utils.WidgetLayoutUtils
import com.tk.quicksearch.widgets.utils.WidgetVariant
import com.tk.quicksearch.widgets.utils.enforceVariantConstraints
import com.tk.quicksearch.widgets.utils.toWidgetPreferences
import kotlin.math.roundToInt

/** Default size (3 columns by 2 rows) used until the host reports the placed widget's size. */
private const val DEFAULT_WIDTH_DP = 180f
private const val DEFAULT_HEIGHT_DP = 110f

class NoteWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    companion object {
        /** Drawn at until the host reports the placed widget's size. */
        val DEFAULT_SIZE = DpSize(DEFAULT_WIDTH_DP.dp, DEFAULT_HEIGHT_DP.dp)
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent { WidgetBody(appWidgetId) }
    }

    @Composable
    private fun WidgetBody(appWidgetId: Int) {
        // Reading the state keeps this composition subscribed to refreshNoteWidgets' revision
        // bump, so the note below is re-read whenever notes change.
        val prefs = currentState<Preferences>()
        val context = LocalContext.current
        val storedConfig = prefs.toWidgetPreferences(context).enforceVariantConstraints(WidgetVariant.NOTE)
        // Until a note is saved, show the look the configure screen starts from.
        val config = if (storedConfig.noteId == null) storedConfig.withNoteWidgetDefaults() else storedConfig
        val note = config.noteId?.let { NotesRoomStore(context).getById(it) }

        val size = LocalSize.current
        val widthDp = WidgetLayoutUtils.resolveOr(size.width, DEFAULT_WIDTH_DP.dp)
        val heightDp = WidgetLayoutUtils.resolveOr(size.height, DEFAULT_HEIGHT_DP.dp)
        val resources = context.resources
        val density = resources.displayMetrics.density
        val isDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        // Identical unless the background follows the system theme.
        val dayColors = NoteWidgetColors.forWidget(config, isSystemDark = false)
        val nightColors = NoteWidgetColors.forWidget(config, isSystemDark = true)

        val backgroundModifier =
            when {
                config.backgroundAlpha <= 0f -> GlanceModifier
                // Day/night colors let the launcher switch the card with the system theme.
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    GlanceModifier
                        .background(DayNightColorProvider(day = dayColors.background, night = nightColors.background))
                        .cornerRadius(config.borderRadiusDp.dp)
                // Corner radii need Android 12+ in Glance, so older versions draw the card as a bitmap.
                else ->
                    GlanceModifier.background(
                        ImageProvider(
                            WidgetBitmapUtils.createWidgetBitmap(
                                widthPx = (widthDp.value * density).roundToInt().coerceAtLeast(1),
                                heightPx = (heightDp.value * density).roundToInt().coerceAtLeast(1),
                                backgroundColor = if (isDark) nightColors.background else dayColors.background,
                                borderColor = null,
                                borderWidthPx = 0,
                                cornerRadiusPx = config.borderRadiusDp * density,
                            ),
                        ),
                    )
            }

        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .then(backgroundModifier)
                    .padding(NoteWidgetDimens.CONTENT_PADDING)
                    .clickable(
                        onClick = clickAction(context, note, appWidgetId),
                        rippleOverride = android.R.color.transparent,
                    ),
        ) {
            NoteContent(
                context = context,
                note = note,
                hasSelectedNote = config.noteId != null,
                heightDp = heightDp.value,
                fontScale = resources.configuration.fontScale,
                dayColors = dayColors,
                nightColors = nightColors,
            )
        }
    }

    @Composable
    private fun NoteContent(
        context: Context,
        note: NoteInfo?,
        hasSelectedNote: Boolean,
        heightDp: Float,
        fontScale: Float,
        dayColors: NoteWidgetColors,
        nightColors: NoteWidgetColors,
    ) {
        val placeholderStyle =
            TextStyle(
                color = DayNightColorProvider(day = dayColors.placeholder, night = nightColors.placeholder),
                fontSize = NoteWidgetDimens.BODY_FONT_SIZE,
            )
        if (note == null) {
            Text(
                text =
                    context.getString(
                        if (hasSelectedNote) R.string.widget_note_deleted else R.string.widget_note_picker_title,
                    ),
                style = placeholderStyle,
            )
            return
        }

        val title = note.title.takeIf { it.isNotBlank() }
        if (title != null) {
            Text(
                text = title,
                style =
                    TextStyle(
                        color = DayNightColorProvider(day = dayColors.title, night = nightColors.title),
                        fontSize = NoteWidgetDimens.TITLE_FONT_SIZE,
                        fontWeight = FontWeight.Medium,
                    ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(NoteWidgetDimens.TITLE_BODY_SPACING))
        }
        val maxLines = NoteWidgetDimens.bodyMaxLines(heightDp, hasTitle = title != null, fontScale = fontScale)
        val body = NoteWidgetDimens.bodyText(note.markdownContent, maxLines)
        if (body.isBlank()) {
            Text(text = context.getString(R.string.notes_empty_note_subtext), style = placeholderStyle, maxLines = 1)
        } else {
            Text(
                text = body,
                style =
                    TextStyle(
                        color = DayNightColorProvider(day = dayColors.body, night = nightColors.body),
                        fontSize = NoteWidgetDimens.BODY_FONT_SIZE,
                    ),
                maxLines = maxLines,
            )
        }
    }

    /** Opens the note, or the note picker when no note is set or it was deleted. */
    private fun clickAction(
        context: Context,
        note: NoteInfo?,
        appWidgetId: Int,
    ): Action =
        if (note != null) {
            actionStartActivity(
                WidgetActionActivity.createIntent(
                    context,
                    CustomWidgetButtonAction.Note(noteId = note.noteId, title = note.title),
                ),
            )
        } else {
            actionStartActivity(
                Intent(context, SearchWidgetConfigureActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    // A task of its own, so finishing it returns to the home screen, not Quick Search.
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                },
            )
        }
}

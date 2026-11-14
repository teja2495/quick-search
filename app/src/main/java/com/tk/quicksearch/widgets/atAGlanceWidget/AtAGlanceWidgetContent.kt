package com.tk.quicksearch.widgets.atAGlanceWidget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import com.tk.quicksearch.search.apps.appIconCacheVersion
import com.tk.quicksearch.search.data.preferences.AppGlanceItemSettings
import com.tk.quicksearch.search.data.preferences.WidgetGlanceItemSettings
import com.tk.quicksearch.search.data.preferences.newWidgetGlanceItems
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.widgets.mediaControlsWidget.MediaControlsSnapshot
import com.tk.quicksearch.widgets.noteWidget.withNoteWidgetDefaults
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.WidgetVariant
import com.tk.quicksearch.widgets.utils.enforceVariantConstraints
import com.tk.quicksearch.widgets.utils.hasStoredWidgetConfig
import com.tk.quicksearch.widgets.utils.toWidgetPreferences
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * Everything an At a Glance widget draws apart from its size, read once per draw. The widget is
 * redrawn on every change it watches, and most (another app's notification, a download's progress)
 * leave its rows as they were, so a draw is skipped when this is the same as the last one.
 */
internal class AtAGlanceWidgetContent(
    private val prefs: Preferences,
    val config: WidgetPreferences,
    val snapshot: AtAGlanceWidgetSnapshot,
    val nowPlaying: MediaControlsSnapshot?,
    /** The settings the draw also reads: night mode, font scale, density, locale, today's date and app icons. */
    private val environment: List<Any?>,
) {
    fun showsSameAs(other: AtAGlanceWidgetContent): Boolean =
        prefs == other.prefs &&
            environment == other.environment &&
            snapshot.rows == other.snapshot.rows &&
            nowPlaying.showsSameAs(other.nowPlaying)

    companion object {
        /** The content each placed widget last composed, by widget; forgotten when the process ends. */
        private val lastComposed = ConcurrentHashMap<GlanceId, AtAGlanceWidgetContent>()

        /** Reads what [prefs]' widget shows now. Blocking; call off the main thread. */
        fun load(
            context: Context,
            prefs: Preferences,
        ): AtAGlanceWidgetContent {
            val storedConfig = prefs.toWidgetPreferences(context).enforceVariantConstraints(WidgetVariant.AT_A_GLANCE)
            val appItems = AppGlanceItemSettings(context)
            // Until the configure screen saves, show the look and items it starts from.
            val config =
                if (prefs.hasStoredWidgetConfig()) {
                    storedConfig
                } else {
                    storedConfig.withNoteWidgetDefaults().copy(glanceItems = newWidgetGlanceItems(appItems))
                }
            val items = WidgetGlanceItemSettings(config.glanceItems, appItems)
            val snapshot = loadAtAGlanceWidgetSnapshot(context, items, config.glanceAppNotificationFilters)
            val nowPlaying = loadAtAGlanceWidgetNowPlaying(context, items)
            val configuration = context.resources.configuration
            val environment =
                listOf(
                    configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK,
                    configuration.fontScale,
                    configuration.densityDpi,
                    configuration.locales.toLanguageTags(),
                    DateFormat.is24HourFormat(context),
                    LocalDate.now(),
                    appIconCacheVersion.value,
                    UserAppPreferences(context).uiPreferences.getSelectedIconPackPackage(),
                )
            return AtAGlanceWidgetContent(prefs, config, snapshot, nowPlaying, environment)
        }

        fun composed(
            glanceId: GlanceId,
            content: AtAGlanceWidgetContent,
        ) {
            lastComposed[glanceId] = content
        }

        /** Makes the next check for [glanceId] draw, as its last composition may not have reached the launcher. */
        fun forget(glanceId: GlanceId) {
            lastComposed.remove(glanceId)
        }

        /**
         * Whether [glanceId] already shows what it would draw from [prefs] now. When it does, the
         * refresh its rows asked for is scheduled as a draw would have.
         */
        fun isShowingCurrent(
            context: Context,
            glanceId: GlanceId,
            prefs: Preferences,
        ): Boolean {
            val last = lastComposed[glanceId] ?: return false
            val current = load(context, prefs)
            if (!current.showsSameAs(last)) return false
            AtAGlanceWidgetReceiver.scheduleRefresh(context, current.snapshot.nextChangeMillis)
            return true
        }
    }
}

/** Equal apart from the bitmaps, which are read anew each time and so are compared by their pixels. */
private fun MediaControlsSnapshot?.showsSameAs(other: MediaControlsSnapshot?): Boolean {
    if (this == null || other == null) return this == other
    return withoutBitmaps() == other.withoutBitmaps() &&
        albumArt.hasSamePixels(other.albumArt) &&
        appSeekBackIcon.hasSamePixels(other.appSeekBackIcon) &&
        appSeekForwardIcon.hasSamePixels(other.appSeekForwardIcon)
}

private fun MediaControlsSnapshot.withoutBitmaps(): MediaControlsSnapshot =
    copy(albumArt = null, appSeekBackIcon = null, appSeekForwardIcon = null)

private fun Bitmap?.hasSamePixels(other: Bitmap?): Boolean {
    if (this === other) return true
    if (this == null || other == null || isRecycled || other.isRecycled) return false
    return runCatching { sameAs(other) }.getOrDefault(false)
}

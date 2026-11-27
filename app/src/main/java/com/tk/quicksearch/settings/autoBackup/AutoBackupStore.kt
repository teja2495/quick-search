package com.tk.quicksearch.settings.autoBackup

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.settingsScreen.SettingsBackupManager.ExportItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AutoBackupFrequency(
    val days: Int,
    @StringRes val labelRes: Int,
) {
    DAILY(1, R.string.calendar_repeats_daily),
    EVERY_3_DAYS(3, R.string.auto_backup_frequency_every_3_days),
    WEEKLY(7, R.string.calendar_repeats_weekly),
}

/** How many auto-backup files to keep in the folder; older ones are deleted after each backup. */
val AutoBackupKeptCounts = listOf(1, 3, 5)
const val DEFAULT_KEPT_BACKUPS = 3

/** A scheduled auto-backup. There is none until the user picks a frequency and a folder. */
data class AutoBackupState(
    val frequency: AutoBackupFrequency,
    val folderUri: Uri,
    val folderName: String,
    val selectedItems: Set<ExportItem>,
    val keptBackups: Int,
    val lastBackupMillis: Long?,
    val lastBackupFailed: Boolean,
)

/**
 * The auto-backup schedule, in its own preference file so backups never carry it: the folder
 * grant belongs to this device and install.
 */
object AutoBackupStore {
    const val PREFS_NAME = "auto_backup"

    private const val KEY_FREQUENCY = "frequency"
    private const val KEY_FOLDER_URI = "folder_uri"
    private const val KEY_FOLDER_NAME = "folder_name"
    private const val KEY_SELECTED_ITEMS = "selected_items"
    private const val KEY_KEPT_BACKUPS = "kept_backups"
    private const val KEY_LAST_BACKUP_MILLIS = "last_backup_millis"
    private const val KEY_LAST_BACKUP_FAILED = "last_backup_failed"

    private val mutableState = MutableStateFlow<AutoBackupState?>(null)

    /** The last state read or written in this process; call [refresh] to load it from disk. */
    val state: StateFlow<AutoBackupState?> = mutableState.asStateFlow()

    /** Reads the schedule from disk and publishes it. Call off the main thread. */
    fun refresh(context: Context): AutoBackupState? = read(context).also { mutableState.value = it }

    /** Call off the main thread. */
    fun read(context: Context): AutoBackupState? {
        val prefs = prefs(context)
        val frequency =
            prefs.getString(KEY_FREQUENCY, null)
                ?.let { name -> AutoBackupFrequency.entries.firstOrNull { it.name == name } }
                ?: return null
        val folderUri = prefs.getString(KEY_FOLDER_URI, null)?.let(Uri::parse) ?: return null
        val selectedNames = prefs.getStringSet(KEY_SELECTED_ITEMS, null)
        return AutoBackupState(
            frequency = frequency,
            folderUri = folderUri,
            folderName = prefs.getString(KEY_FOLDER_NAME, null).orEmpty(),
            selectedItems =
                selectedNames
                    ?.mapNotNullTo(mutableSetOf()) { name -> ExportItem.entries.firstOrNull { it.name == name } }
                    ?: ExportItem.defaultSelection,
            keptBackups = prefs.getInt(KEY_KEPT_BACKUPS, DEFAULT_KEPT_BACKUPS).takeIf { it in AutoBackupKeptCounts } ?: DEFAULT_KEPT_BACKUPS,
            lastBackupMillis = prefs.getLong(KEY_LAST_BACKUP_MILLIS, 0L).takeIf { it > 0L },
            lastBackupFailed = prefs.getBoolean(KEY_LAST_BACKUP_FAILED, false),
        )
    }

    /** Starts a schedule, or replaces one; a new folder starts without a last backup. */
    fun save(
        context: Context,
        frequency: AutoBackupFrequency,
        folderUri: Uri,
        folderName: String,
        selectedItems: Set<ExportItem>,
        keptBackups: Int,
    ) {
        val previous = mutableState.value
        val sameFolder = previous?.folderUri == folderUri
        val next =
            AutoBackupState(
                frequency = frequency,
                folderUri = folderUri,
                folderName = folderName,
                selectedItems = selectedItems,
                keptBackups = keptBackups,
                lastBackupMillis = previous?.lastBackupMillis?.takeIf { sameFolder },
                lastBackupFailed = sameFolder && previous.lastBackupFailed,
            )
        prefs(context).edit {
            putString(KEY_FREQUENCY, frequency.name)
            putString(KEY_FOLDER_URI, folderUri.toString())
            putString(KEY_FOLDER_NAME, folderName)
            putStringSet(KEY_SELECTED_ITEMS, selectedItems.mapTo(mutableSetOf()) { it.name })
            putInt(KEY_KEPT_BACKUPS, keptBackups)
            if (next.lastBackupMillis == null) remove(KEY_LAST_BACKUP_MILLIS)
            putBoolean(KEY_LAST_BACKUP_FAILED, next.lastBackupFailed)
        }
        mutableState.value = next
    }

    fun updateSchedule(context: Context, frequency: AutoBackupFrequency, keptBackups: Int) {
        val current = mutableState.value ?: return
        prefs(context).edit {
            putString(KEY_FREQUENCY, frequency.name)
            putInt(KEY_KEPT_BACKUPS, keptBackups)
        }
        mutableState.value = current.copy(frequency = frequency, keptBackups = keptBackups)
    }

    fun updateSelection(context: Context, selectedItems: Set<ExportItem>) {
        val current = mutableState.value ?: return
        if (current.selectedItems == selectedItems) return
        prefs(context).edit { putStringSet(KEY_SELECTED_ITEMS, selectedItems.mapTo(mutableSetOf()) { it.name }) }
        mutableState.value = current.copy(selectedItems = selectedItems)
    }

    /** Records a backup run. A failure keeps the last successful time. */
    fun recordResult(context: Context, success: Boolean, nowMillis: Long) {
        prefs(context).edit {
            if (success) putLong(KEY_LAST_BACKUP_MILLIS, nowMillis)
            putBoolean(KEY_LAST_BACKUP_FAILED, !success)
        }
        refresh(context)
    }

    fun clear(context: Context) {
        prefs(context).edit { clear() }
        mutableState.value = null
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

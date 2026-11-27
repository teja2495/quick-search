package com.tk.quicksearch.settings.autoBackup

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.tk.quicksearch.settings.settingsScreen.SettingsBackupManager
import com.tk.quicksearch.settings.settingsScreen.SettingsBackupManager.ExportItem
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Writes a backup file to the user's chosen folder on a repeating JobScheduler job. The job is
 * persisted, so it survives reboots and app updates without a boot receiver.
 */
object AutoBackupScheduler {
    private const val PERIODIC_JOB_ID = 6_000_000
    private const val RUN_NOW_JOB_ID = 6_000_001
    // Named like a manual export, with "-auto" before the extension.
    private const val FILE_PREFIX = "quick-search-settings-"
    private const val FILE_SUFFIX = "-auto.quicksearch"
    private const val MIME_TYPE = "application/octet-stream"

    // Schedule changes finish even if the drawer that started them closes.
    private val changeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Runs a schedule change from the UI in a scope that outlives the drawer that started it. */
    fun launchChange(block: suspend AutoBackupScheduler.() -> Unit) {
        changeScope.launch { block() }
    }

    /** Takes the folder grant, saves the schedule, and runs the first backup right away. */
    suspend fun enable(
        context: Context,
        frequency: AutoBackupFrequency,
        folderUri: Uri,
        selectedItems: Set<ExportItem>,
        keptBackups: Int,
    ) = withContext(Dispatchers.IO + NonCancellable) {
        val resolver = context.contentResolver
        resolver.takePersistableUriPermission(folderUri, FOLDER_PERMISSIONS)
        val previous = AutoBackupStore.refresh(context)
        if (previous != null && previous.folderUri != folderUri) releaseFolder(context, previous.folderUri)
        AutoBackupStore.save(context, frequency, folderUri, folderDisplayName(context, folderUri), selectedItems, keptBackups)
        schedulePeriodic(context, frequency)
        runNow(context)
    }

    suspend fun changeSchedule(
        context: Context,
        frequency: AutoBackupFrequency,
        keptBackups: Int,
    ) = withContext(Dispatchers.IO + NonCancellable) {
        val current = AutoBackupStore.state.value ?: return@withContext
        if (current.frequency == frequency && current.keptBackups == keptBackups) return@withContext
        AutoBackupStore.updateSchedule(context, frequency, keptBackups)
        if (current.frequency != frequency) schedulePeriodic(context, frequency)
        // Fewer kept backups takes effect now, not at the next run.
        if (keptBackups < current.keptBackups) runCatching { deleteOldBackups(context, current.folderUri, keptBackups) }
    }

    suspend fun disable(context: Context) = withContext(Dispatchers.IO + NonCancellable) {
        context.getSystemService(JobScheduler::class.java)?.let { jobs ->
            jobs.cancel(PERIODIC_JOB_ID)
            jobs.cancel(RUN_NOW_JOB_ID)
        }
        (AutoBackupStore.state.value ?: AutoBackupStore.read(context))?.let { releaseFolder(context, it.folderUri) }
        AutoBackupStore.clear(context)
    }

    private fun schedulePeriodic(context: Context, frequency: AutoBackupFrequency) {
        val job =
            JobInfo.Builder(PERIODIC_JOB_ID, ComponentName(context, AutoBackupJobService::class.java))
                .setPeriodic(TimeUnit.DAYS.toMillis(frequency.days.toLong()))
                .setRequiresBatteryNotLow(true)
                .setPersisted(true)
                .build()
        context.getSystemService(JobScheduler::class.java)?.schedule(job)
    }

    private fun runNow(context: Context) {
        val job =
            JobInfo.Builder(RUN_NOW_JOB_ID, ComponentName(context, AutoBackupJobService::class.java))
                .setMinimumLatency(1L)
                .build()
        context.getSystemService(JobScheduler::class.java)?.schedule(job)
    }

    /** Writes one backup and records the result. Call off the main thread. */
    internal fun run(context: Context) {
        val state = AutoBackupStore.read(context) ?: return
        val success = runCatching { writeBackup(context, state) }.isSuccess
        AutoBackupStore.recordResult(context, success, System.currentTimeMillis())
    }

    private fun writeBackup(context: Context, state: AutoBackupState) {
        val resolver = context.contentResolver
        val folder = folderDocumentUri(state.folderUri)
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file =
            DocumentsContract.createDocument(resolver, folder, MIME_TYPE, "$FILE_PREFIX$timestamp$FILE_SUFFIX")
                ?: throw IOException("Unable to create the auto-backup file")
        try {
            SettingsBackupManager.exportToUri(
                context,
                file,
                SettingsBackupManager.ExportOptions(ExportItem.requiredItems + state.selectedItems),
            )
        } catch (error: Throwable) {
            runCatching { DocumentsContract.deleteDocument(resolver, file) }
            throw error
        }
        runCatching { deleteOldBackups(context, state.folderUri, state.keptBackups) }
    }

    /** Deletes all but the newest [keep] auto-backups; manual exports are never touched. */
    private fun deleteOldBackups(context: Context, treeUri: Uri, keep: Int) {
        val resolver = context.contentResolver
        val children =
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        val backups = mutableListOf<Pair<String, String>>()
        resolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1) ?: continue
                if (name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX)) {
                    backups += cursor.getString(0) to name
                }
            }
        }
        // Timestamps in the names sort oldest first.
        backups.sortedByDescending { it.second }.drop(keep).forEach { (documentId, _) ->
            DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId))
        }
    }

    private fun folderDocumentUri(treeUri: Uri): Uri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))

    private fun folderDisplayName(context: Context, treeUri: Uri): String =
        runCatching {
            context.contentResolver.query(
                folderDocumentUri(treeUri),
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()
            ?: treeUri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/').orEmpty()

    private fun releaseFolder(context: Context, folderUri: Uri) {
        runCatching { context.contentResolver.releasePersistableUriPermission(folderUri, FOLDER_PERMISSIONS) }
    }

    private const val FOLDER_PERMISSIONS =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
}

class AutoBackupJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            AutoBackupScheduler.run(applicationContext)
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

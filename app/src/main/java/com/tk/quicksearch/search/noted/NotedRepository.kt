package com.tk.quicksearch.search.noted

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.tk.quicksearch.search.models.NotedItem
import com.tk.quicksearch.search.utils.SearchRankingUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Searches the Noted app through its permission-protected content provider. */
class NotedRepository(
    context: Context,
) {
    private val appContext = context.applicationContext

    suspend fun search(query: String): List<NotedItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val uri =
            providerUri(NotedIntegration.SEARCH_PATH)
                .appendQueryParameter(NotedIntegration.QUERY_PARAM, trimmed)
                .appendQueryParameter(NotedIntegration.LIMIT_PARAM, NotedIntegration.FETCH_LIMIT.toString())
                .build()
        // Noted orders by last edit; put better title matches first, keeping that order within a tier.
        return queryItems(uri, "search")
            .sortedBy { SearchRankingUtils.calculateMatchPriority(it.title, trimmed) }
            .take(NotedIntegration.RESULT_LIMIT)
    }

    /** Noted's open tasks that have a due date, soonest first. */
    suspend fun dueTasks(): List<NotedItem> = withContext(Dispatchers.IO) { dueTasksBlocking() }

    /** [dueTasks] for callers already off the main thread, such as the At a Glance widget. */
    fun dueTasksBlocking(): List<NotedItem> {
        if (!NotedIntegration.ensureChecked(appContext)) return emptyList()
        return guarded("due tasks", emptyList()) {
            appContext.contentResolver
                .query(providerUri(NotedIntegration.TASKS_PATH).build(), null, null, null, null)
                ?.use(::readItems)
                .orEmpty()
        }
    }

    /** The open items with [NotedItem.key]s [keys], in that order, e.g. those pinned on Home. Blocking. */
    fun itemsBlocking(keys: List<String>): List<NotedItem> {
        if (keys.isEmpty() || !NotedIntegration.ensureChecked(appContext)) return emptyList()
        val uri =
            providerUri(NotedIntegration.ITEMS_PATH)
                .appendQueryParameter(NotedIntegration.KEYS_PARAM, keys.joinToString(","))
                .build()
        return guarded("items", emptyList()) {
            appContext.contentResolver.query(uri, null, null, null, null)?.use(::readItems).orEmpty()
        }
    }

    /** Completes or reopens Noted task [taskId]; Noted updates its own reminders. Blocking. */
    fun setTaskCompletedBlocking(taskId: String, completed: Boolean): Boolean {
        if (!NotedIntegration.ensureChecked(appContext)) return false
        val values = ContentValues().apply { put(NotedIntegration.COLUMN_COMPLETED, if (completed) 1 else 0) }
        val uri = providerUri(NotedIntegration.TASKS_PATH).appendPath(taskId).build()
        return guarded("task update", false) { appContext.contentResolver.update(uri, values, null, null) > 0 }
    }

    /**
     * Adds a task to Noted's To Do list. A date-only task passes local midnight of its day.
     * Returns false when Noted is unavailable or refused it, so the caller can save it elsewhere.
     */
    suspend fun createTask(title: String, dueAtMillis: Long, isDueTimeSet: Boolean): Boolean {
        if (!NotedIntegration.ensureChecked(appContext)) return false
        val values =
            ContentValues().apply {
                put(NotedIntegration.COLUMN_TITLE, title)
                put(NotedIntegration.COLUMN_DUE_AT, dueAtMillis)
                put(NotedIntegration.COLUMN_DUE_TIME_SET, if (isDueTimeSet) 1 else 0)
            }
        return withContext(Dispatchers.IO) {
            guarded("task insert", false) {
                appContext.contentResolver.insert(providerUri(NotedIntegration.TASKS_PATH).build(), values) != null
            }
        }
    }

    /**
     * [createTask] in a process-wide scope, so it finishes even if the screen that asked closes.
     * [onResult] runs on the main thread with whether Noted took the task.
     */
    fun createTaskInBackground(
        title: String,
        dueAtMillis: Long,
        isDueTimeSet: Boolean,
        onResult: (added: Boolean) -> Unit,
    ) {
        backgroundScope.launch {
            val added = createTask(title, dueAtMillis, isDueTimeSet)
            onResult(added)
        }
    }

    private suspend fun queryItems(uri: Uri, label: String): List<NotedItem> {
        if (!NotedIntegration.ensureChecked(appContext)) return emptyList()
        return withContext(Dispatchers.IO) {
            guarded(label, emptyList()) {
                appContext.contentResolver.query(uri, null, null, null, null)?.use(::readItems).orEmpty()
            }
        }
    }

    private inline fun <T> guarded(label: String, fallback: T, block: () -> T): T =
        try {
            block()
        } catch (e: SecurityException) {
            // Permission revoked or Noted replaced by a differently signed build: hide Noted features.
            NotedIntegration.refresh(appContext)
            Log.w(TAG, "Noted $label denied", e)
            fallback
        } catch (e: RuntimeException) {
            Log.w(TAG, "Noted $label failed", e)
            fallback
        }

    private fun providerUri(path: String): Uri.Builder =
        Uri.Builder().scheme("content").authority(NotedIntegration.AUTHORITY).appendPath(path)

    private fun readItems(cursor: Cursor): List<NotedItem> {
        val kindIndex = cursor.getColumnIndex(NotedIntegration.COLUMN_KIND)
        val idIndex = cursor.getColumnIndex(NotedIntegration.COLUMN_ID)
        val titleIndex = cursor.getColumnIndex(NotedIntegration.COLUMN_TITLE)
        if (kindIndex < 0 || idIndex < 0 || titleIndex < 0) return emptyList()
        val subtitleIndex = cursor.getColumnIndex(NotedIntegration.COLUMN_SUBTITLE)
        val listTitleIndex = cursor.getColumnIndex(NotedIntegration.COLUMN_LIST_TITLE)
        val dueAtIndex = cursor.getColumnIndex(NotedIntegration.COLUMN_DUE_AT)
        val dueTimeSetIndex = cursor.getColumnIndex(NotedIntegration.COLUMN_DUE_TIME_SET)
        val updatedAtIndex = cursor.getColumnIndex(NotedIntegration.COLUMN_UPDATED_AT)
        val dueLocalDateIndex = cursor.getColumnIndex(NotedIntegration.COLUMN_DUE_LOCAL_DATE)
        val daysTrackIndex = cursor.getColumnIndex(NotedIntegration.COLUMN_DAYS_TRACK)

        val items = ArrayList<NotedItem>(cursor.count)
        while (cursor.moveToNext()) {
            val kind = NotedItem.Kind.fromRaw(cursor.getString(kindIndex)) ?: continue
            val id = cursor.getString(idIndex)?.takeIf { it.isNotBlank() } ?: continue
            items +=
                NotedItem(
                    kind = kind,
                    id = id,
                    title = cursor.getString(titleIndex).orEmpty(),
                    subtitle = cursor.stringOrNull(subtitleIndex),
                    listTitle = cursor.stringOrNull(listTitleIndex),
                    dueAtMillis = cursor.longOrNull(dueAtIndex),
                    isDueTimeSet = cursor.longOrNull(dueTimeSetIndex) == 1L,
                    updatedAtMillis = cursor.longOrNull(updatedAtIndex) ?: 0L,
                    dueLocalDate = cursor.stringOrNull(dueLocalDateIndex),
                    isDaysTrack = cursor.longOrNull(daysTrackIndex) == 1L,
                )
        }
        return items
    }

    private fun Cursor.stringOrNull(index: Int): String? =
        if (index < 0 || isNull(index)) null else getString(index)?.takeIf { it.isNotBlank() }

    private fun Cursor.longOrNull(index: Int): Long? = if (index < 0 || isNull(index)) null else getLong(index)

    private companion object {
        const val TAG = "NotedRepository"
        val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}

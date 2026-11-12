package com.tk.quicksearch.search.models

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A note, list, or open task from the user's Noted app (`com.tk.noted`). */
data class NotedItem(
    val kind: Kind,
    val id: String,
    val title: String,
    val subtitle: String?,
    val listTitle: String?,
    val dueAtMillis: Long?,
    val isDueTimeSet: Boolean,
    val updatedAtMillis: Long,
    /** Task due day as `yyyy-MM-dd`; date-only due dates are stored by day, not instant. */
    val dueLocalDate: String? = null,
    /** In a Noted Days Track list, which counts days since or until a date instead of falling due. */
    val isDaysTrack: Boolean = false,
) {
    enum class Kind(val raw: String) {
        NOTE("note"),
        LIST("list"),
        TASK("task"),
        ;

        companion object {
            fun fromRaw(raw: String?): Kind? = entries.firstOrNull { it.raw == raw }
        }
    }

    val key: String get() = "${kind.raw}:$id"

    /** The task's due day in the device zone, or null when it has no due date. */
    fun dueDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate? =
        dueLocalDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: dueAtMillis?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }

    /**
     * Matches reminders: a task without a time is only overdue once its day has passed. Days Track
     * items are never overdue; their past dates read as "3 days ago".
     */
    fun isOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (isDaysTrack) return false
        val zoneId = ZoneId.systemDefault()
        val dueAt = dueAtMillis
        return if (isDueTimeSet && dueAt != null) {
            dueAt < nowMillis
        } else {
            val day = dueDate(zoneId) ?: return false
            day.isBefore(Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate())
        }
    }
}

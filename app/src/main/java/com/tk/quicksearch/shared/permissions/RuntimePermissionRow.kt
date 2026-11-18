package com.tk.quicksearch.shared.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.tk.quicksearch.search.utils.PermissionUtils
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

/** The ordinary runtime permissions shown in the shared Settings and onboarding card. */
internal enum class RuntimePermissionRow(
    val permission: String,
    val position: Position,
    val title: (Context, PermissionCardTexts) -> String,
    val description: (Context, PermissionCardTexts) -> String,
    val icon: () -> ImageVector,
    val refreshRevocations: Boolean = false,
    val isVisible: (showCalling: Boolean, showCalendar: Boolean) -> Boolean = { _, _ -> true },
    private val check: ((Context) -> Boolean)? = null,
) {
    CONTACTS(
        Manifest.permission.READ_CONTACTS,
        Position.AFTER_USAGE,
        { _, texts -> texts.contactsTitle },
        { _, texts -> texts.contactsDescription },
        { MaterialSymbols.Contacts },
        check = PermissionUtils::hasContactsPermission,
    ),
    CALLING(
        Manifest.permission.CALL_PHONE,
        Position.AFTER_ACCESSIBILITY,
        { _, texts -> texts.callingTitle },
        { _, texts -> texts.callingDescription },
        { MaterialSymbols.Call },
        refreshRevocations = true,
        isVisible = { showCalling, _ -> showCalling },
    ),
    CALENDAR(
        Manifest.permission.READ_CALENDAR,
        Position.AFTER_FILES,
        { _, texts -> texts.calendarTitle },
        { _, texts -> texts.calendarDescription },
        { MaterialSymbols.CalendarMonth },
        isVisible = { _, showCalendar -> showCalendar },
    ),
    ;

    enum class Position { AFTER_USAGE, AFTER_FILES, AFTER_ACCESSIBILITY }

    fun isGranted(context: Context): Boolean =
        check?.invoke(context) ?: (
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
}

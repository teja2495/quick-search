package com.tk.quicksearch.settings.bookmarksSettings

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.bookmarks.BookmarkRepository
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuDivider
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuItem
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Actions picked from the Bookmarks page's header menu, handled by [BookmarksSettingsSection]. */
internal enum class BookmarksPageAction { ImportHtml, DeleteAll }

/** Carries header-menu picks to the page body, which owns the file picker and the confirmation. */
internal object BookmarksPageActions {
    private val _actions = MutableSharedFlow<BookmarksPageAction>(extraBufferCapacity = 1)
    val actions: SharedFlow<BookmarksPageAction> = _actions.asSharedFlow()

    fun send(action: BookmarksPageAction) {
        _actions.tryEmit(action)
    }
}

/** The three-dot button in the Bookmarks page header. Delete All is offered only when there are bookmarks. */
@Composable
fun BookmarksHeaderMenu() {
    val context = LocalContext.current
    val repository = remember(context) { BookmarkRepository(context.applicationContext) }
    val bookmarks by repository.bookmarks.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = MaterialSymbols.MoreVert,
                contentDescription = stringResource(R.string.settings_more_options_title),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        AppDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AppDropdownMenuItem(
                text = stringResource(R.string.bookmarks_menu_import_html),
                icon = MaterialSymbols.Upload,
                onClick = {
                    expanded = false
                    BookmarksPageActions.send(BookmarksPageAction.ImportHtml)
                },
            )
            if (bookmarks.isNotEmpty()) {
                AppDropdownMenuDivider()
                AppDropdownMenuItem(
                    text = stringResource(R.string.bookmarks_menu_delete_all),
                    icon = MaterialSymbols.DeleteOutline,
                    destructive = true,
                    onClick = {
                        expanded = false
                        BookmarksPageActions.send(BookmarksPageAction.DeleteAll)
                    },
                )
            }
        }
    }
}

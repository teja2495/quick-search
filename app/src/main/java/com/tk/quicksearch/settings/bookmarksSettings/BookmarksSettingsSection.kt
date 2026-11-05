package com.tk.quicksearch.settings.bookmarksSettings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.bookmarks.BookmarkFavicon
import com.tk.quicksearch.search.core.BrowserApp
import com.tk.quicksearch.search.data.bookmarks.BookmarkHtmlParser
import com.tk.quicksearch.search.data.bookmarks.BookmarkRepository
import com.tk.quicksearch.search.data.bookmarks.BookmarkUrls
import com.tk.quicksearch.search.models.BookmarkInfo
import com.tk.quicksearch.settings.appShortcutsSettings.shortcutMatchPriority
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.AppUndoSnackbarPopup
import com.tk.quicksearch.shared.ui.components.rememberUndoSnackbarController
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val EXPORT_FILE_NAME = "bookmarks.html"

/** Largest bookmarks file read into memory; browser exports with thousands of links stay well under it. */
private const val MAX_IMPORT_FILE_BYTES = 16L * 1024 * 1024

/**
 * The Bookmarks page: every saved website, an import from a browser's HTML export, and an
 * editor opened by tapping a row or by [addRequest] changing (the page's "Add bookmark" button).
 */
@Composable
fun BookmarksSettingsSection(
    searchQuery: String,
    availableBrowsers: List<BrowserApp>,
    addRequest: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember(context) { BookmarkRepository(context.applicationContext) }
    val bookmarks by repository.bookmarks.collectAsState()
    val faviconFetchesInProgress by repository.faviconFetchesInProgress.collectAsState()
    val scope = rememberCoroutineScope()
    var editorTarget by remember { mutableStateOf<BookmarkEditorTarget?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var showDefaultBrowserPicker by remember { mutableStateOf(false) }
    val storedDefaultBrowser by repository.defaultBrowserPackage.collectAsState()
    val undoSnackbar = rememberUndoSnackbarController()
    val deletedMessage = stringResource(R.string.bookmarks_deleted)
    // An uninstalled default falls back to the in-app browser.
    val defaultBrowser = resolveBookmarkBrowser(storedDefaultBrowser, availableBrowsers)

    LaunchedEffect(repository) {
        withContext(Dispatchers.IO) {
            repository.getAll()
            repository.loadDefaultBrowserPackage()
        }
    }
    LaunchedEffect(addRequest) { if (addRequest > 0) editorTarget = BookmarkEditorTarget.New }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            isImporting = true
            scope.launch {
                val message =
                    withContext(Dispatchers.IO) { importBookmarksFile(context, repository, uri, defaultBrowser) }
                isImporting = false
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val message = withContext(Dispatchers.IO) { exportBookmarksFile(context, repository, uri) }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

    val launchImport = { importLauncher.launch(arrayOf("text/html", "application/xhtml+xml")) }
    LaunchedEffect(Unit) {
        BookmarksPageActions.actions.collect { action ->
            when (action) {
                BookmarksPageAction.ImportHtml -> launchImport()
                BookmarksPageAction.ExportHtml -> exportLauncher.launch(EXPORT_FILE_NAME)
                BookmarksPageAction.DefaultBrowser -> showDefaultBrowserPicker = true
                BookmarksPageAction.DeleteAll -> showDeleteAllConfirm = true
            }
        }
    }

    val locale = Locale.getDefault()
    val normalizedQuery = remember(searchQuery, locale) { searchQuery.trim().lowercase(locale) }
    val displayed =
        remember(bookmarks, normalizedQuery, locale) {
            val sorted = bookmarks.sortedBy { it.name.lowercase(locale) }
            if (normalizedQuery.isBlank()) {
                sorted
            } else {
                sorted.filter { bookmark ->
                    shortcutMatchPriority(name = bookmark.name, query = normalizedQuery, locale = locale) != null ||
                        bookmark.url.lowercase(locale).contains(normalizedQuery)
                }
            }
        }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Column(modifier = modifier) {
        // With nothing saved, importing is the main way in, so it gets a card; otherwise it lives in
        // the header's menu.
        if (bookmarks.isEmpty()) {
            SettingsCard(modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.SectionTopPadding)) {
                BookmarkImportRow(isImporting = isImporting, onClick = launchImport)
            }
        }
        SettingsCard(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
            if (displayed.isEmpty()) {
                Text(
                    text =
                        stringResource(
                            if (bookmarks.isEmpty()) R.string.bookmarks_empty else R.string.bookmarks_no_matches,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = screenHeight)) {
                    itemsIndexed(items = displayed, key = { _, bookmark -> bookmark.bookmarkId }) { index, bookmark ->
                        BookmarkManagementRow(
                            bookmark = bookmark,
                            onClick = { editorTarget = BookmarkEditorTarget.Existing(bookmark) },
                            onDelete = {
                                scope.launch {
                                    val deleted =
                                        withContext(Dispatchers.IO) { repository.deleteForUndo(bookmark.bookmarkId) }
                                            ?: return@launch
                                    undoSnackbar.show(deletedMessage, MaterialSymbols.Delete) {
                                        scope.launch(Dispatchers.IO) { repository.restore(deleted) }
                                    }
                                }
                            },
                        )
                        if (index < displayed.lastIndex) HorizontalDivider(color = AppColors.SettingsDivider)
                    }
                }
            }
        }
    }

    AppUndoSnackbarPopup(hostState = undoSnackbar.hostState)

    if (showDeleteAllConfirm) {
        AppAlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text(stringResource(R.string.bookmarks_delete_all_title)) },
            text = { Text(stringResource(R.string.bookmarks_delete_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllConfirm = false
                        scope.launch(Dispatchers.IO) { repository.deleteAll() }
                    },
                ) {
                    Text(text = stringResource(R.string.dialog_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirm = false }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    if (showDefaultBrowserPicker) {
        BookmarkDefaultBrowserDrawer(
            availableBrowsers = availableBrowsers,
            selectedPackage = defaultBrowser,
            onSelect = { packageName ->
                scope.launch(Dispatchers.IO) { repository.setDefaultBrowserPackage(packageName) }
            },
            onDismiss = { showDefaultBrowserPicker = false },
        )
    }

    editorTarget?.let { target ->
        val editedId = (target as? BookmarkEditorTarget.Existing)?.bookmark?.bookmarkId
        BookmarkEditorSheet(
            // The live copy, so a favicon that finishes downloading shows up while the editor is open.
            bookmark =
                (target as? BookmarkEditorTarget.Existing)?.bookmark?.let { opened ->
                    bookmarks.firstOrNull { it.bookmarkId == opened.bookmarkId } ?: opened
                },
            isIconLoading = editedId != null && editedId in faviconFetchesInProgress,
            availableBrowsers = availableBrowsers,
            defaultBrowserPackage = defaultBrowser,
            onDismiss = { editorTarget = null },
            onSave = { name, url, browser, customIconBase64 ->
                val existing = (target as? BookmarkEditorTarget.Existing)?.bookmark
                editorTarget = null
                scope.launch(Dispatchers.IO) {
                    if (existing == null) {
                        repository.add(name, url, browser, customIconBase64)
                    } else {
                        repository.update(existing.bookmarkId, name, url, browser, customIconBase64)
                    }
                }
            },
        )
    }
}

private sealed interface BookmarkEditorTarget {
    data object New : BookmarkEditorTarget

    data class Existing(val bookmark: BookmarkInfo) : BookmarkEditorTarget
}

@Composable
private fun BookmarkImportRow(
    isImporting: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(enabled = !isImporting, onClick = onClick)
                .padding(horizontal = DesignTokens.CardHorizontalPadding, vertical = DesignTokens.CardVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        Box(modifier = Modifier.size(DesignTokens.IconSize), contentAlignment = Alignment.Center) {
            if (isImporting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = MaterialSymbols.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DesignTokens.IconSize),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.bookmarks_import_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.bookmarks_import_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BookmarkManagementRow(
    bookmark: BookmarkInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = DesignTokens.CardHorizontalPadding, end = DesignTokens.SpacingXSmall)
                .padding(vertical = DesignTokens.CardVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        BookmarkFavicon(bookmark = bookmark, size = DesignTokens.IconSize)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bookmark.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = BookmarkUrls.displayHost(bookmark.url),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = MaterialSymbols.DeleteOutline,
                contentDescription = stringResource(R.string.dialog_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Reads and imports a bookmarks HTML file, returning the message to show. Runs off the main thread. */
private fun importBookmarksFile(
    context: Context,
    repository: BookmarkRepository,
    uri: Uri,
    browserPackageName: String,
): String {
    val html =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readNBytesCompat(MAX_IMPORT_FILE_BYTES)
                bytes?.toString(Charsets.UTF_8)
            }
        }.getOrNull() ?: return context.getString(R.string.bookmarks_import_failed)

    val items = BookmarkHtmlParser.parse(html)
    if (items.isEmpty()) return context.getString(R.string.bookmarks_import_none_found)
    val result = repository.import(items, browserPackageName)
    return if (result.skippedDuplicates > 0) {
        context.getString(R.string.bookmarks_import_result_with_skipped, result.added, result.skippedDuplicates)
    } else {
        context.getString(R.string.bookmarks_import_result, result.added)
    }
}

/** Writes every bookmark to [uri] as a bookmarks HTML file, returning the message to show. Runs off the main thread. */
private fun exportBookmarksFile(
    context: Context,
    repository: BookmarkRepository,
    uri: Uri,
): String {
    val bookmarks = repository.getAll()
    val written =
        runCatching {
            val html = repository.exportHtml()
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(html.toByteArray(Charsets.UTF_8)) }
        }.getOrNull() != null
    return if (written) {
        context.getString(R.string.bookmarks_export_result, bookmarks.size)
    } else {
        context.getString(R.string.bookmarks_export_failed)
    }
}

/** Reads the whole stream, or returns null when it is larger than [limit] bytes. */
private fun java.io.InputStream.readNBytesCompat(limit: Long): ByteArray? {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > limit) return null
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

package com.tk.quicksearch.settings.bookmarksSettings

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.bookmarks.BookmarkFavicon
import com.tk.quicksearch.search.core.BrowserApp
import com.tk.quicksearch.search.data.bookmarks.BookmarkUrls
import com.tk.quicksearch.search.models.BookmarkInfo
import com.tk.quicksearch.searchEngines.loadCustomIconAsBase64
import com.tk.quicksearch.shared.ui.components.AppBottomSheet
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CUSTOM_ICON_MAX_SIZE_PX = 256

/** Space an OutlinedTextField keeps above its border for the floating label. */
private val OutlinedTextFieldLabelInset = 8.dp

private enum class BookmarkEditorPage { Form, Browsers }

/**
 * Adds a bookmark when [bookmark] is null, otherwise edits it, in one bottom sheet. Choosing the
 * browser swaps the sheet to a list page instead of stacking another surface on top. A blank name
 * falls back to the site's host when saved. Tapping the icon picks a custom image; [onSave] gets
 * it as base64, or null to keep the current icon. While [isIconLoading] the site's favicon is
 * still downloading and would overwrite a picked image, so picking waits for it.
 */
@Composable
internal fun BookmarkEditorSheet(
    bookmark: BookmarkInfo?,
    isIconLoading: Boolean,
    availableBrowsers: List<BrowserApp>,
    defaultBrowserPackage: String,
    onDismiss: () -> Unit,
    onSave: (name: String, url: String, browserPackageName: String, customIconBase64: String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(bookmark?.name.orEmpty()) }
    var url by rememberSaveable { mutableStateOf(bookmark?.url.orEmpty()) }
    var browserPackageName by rememberSaveable {
        mutableStateOf(
            if (bookmark == null) {
                defaultBrowserPackage
            } else {
                resolveBookmarkBrowser(bookmark.browserPackageName, availableBrowsers)
            },
        )
    }
    var customIconBase64 by rememberSaveable { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableStateOf(BookmarkEditorPage.Form) }
    // Set by Save so the sheet animates closed first, then saves instead of dismissing.
    var saveRequested by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val pickIconLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                withContext(Dispatchers.IO) {
                    loadCustomIconAsBase64(context, uri, maxSizePx = CUSTOM_ICON_MAX_SIZE_PX)
                }?.let { customIconBase64 = it }
            }
        }

    AppBottomSheet(
        onDismissRequest = {
            if (saveRequested) {
                onSave(name.trim(), url.trim(), browserPackageName, customIconBase64)
            } else {
                onDismiss()
            }
        },
        // Dragging would fight the form's scrolling and could throw away typed input.
        swipeToDismissEnabled = false,
        // Bring up the keyboard only after the open animation settles, as AppPickerDrawer does.
        onFullyExpanded =
            if (bookmark == null) {
                {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            } else {
                null
            },
        onDismissStarted = { keyboardController?.hide() },
    ) { dismiss ->
        BackHandler(enabled = page == BookmarkEditorPage.Browsers) { page = BookmarkEditorPage.Form }
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val forward = targetState == BookmarkEditorPage.Browsers
                val duration = DesignTokens.AnimationDurationShort
                (slideInHorizontally(tween(duration)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(duration)))
                    .togetherWith(
                        slideOutHorizontally(tween(duration)) { if (forward) -it / 4 else it / 4 } +
                            fadeOut(tween(duration)),
                    )
            },
            label = "bookmarkEditorPage",
        ) { current ->
            when (current) {
                BookmarkEditorPage.Form ->
                    BookmarkEditorForm(
                        bookmark = bookmark,
                        name = name,
                        onNameChange = { name = it },
                        url = url,
                        onUrlChange = { url = it },
                        urlFocusRequester = focusRequester,
                        customIconBase64 = customIconBase64,
                        isIconLoading = isIconLoading,
                        onPickIcon = { pickIconLauncher.launch(arrayOf("image/*")) },
                        availableBrowsers = availableBrowsers,
                        browserPackageName = browserPackageName,
                        onOpenBrowsers = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            page = BookmarkEditorPage.Browsers
                        },
                        onClose = dismiss,
                        onSave = {
                            // A second tap during the close animation must not save twice.
                            if (!saveRequested) {
                                saveRequested = true
                                dismiss()
                            }
                        },
                    )
                BookmarkEditorPage.Browsers ->
                    BookmarkBrowserList(
                        availableBrowsers = availableBrowsers,
                        selectedPackage = browserPackageName,
                        onSelect = {
                            browserPackageName = it
                            page = BookmarkEditorPage.Form
                        },
                        onBack = { page = BookmarkEditorPage.Form },
                    )
            }
        }
    }
}

/** Form page: fixed header and Save button, with the fields scrolling between them. */
@Composable
private fun BookmarkEditorForm(
    bookmark: BookmarkInfo?,
    name: String,
    onNameChange: (String) -> Unit,
    url: String,
    onUrlChange: (String) -> Unit,
    urlFocusRequester: FocusRequester,
    customIconBase64: String?,
    isIconLoading: Boolean,
    onPickIcon: () -> Unit,
    availableBrowsers: List<BrowserApp>,
    browserPackageName: String,
    onOpenBrowsers: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    val isUrlValid = remember(url) { BookmarkUrls.normalize(url) != null }
    val showUrlError = url.isNotBlank() && !isUrlValid

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.SpacingLarge),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        BookmarkSheetHeader(
            title =
                stringResource(
                    if (bookmark == null) R.string.bookmarks_add_title else R.string.bookmarks_edit_title,
                ),
            navigationIcon = null,
            onClose = onClose,
        )
        Column(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DesignTokens.ContentHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BookmarkIconPicker(
                bookmark = bookmark,
                customIconBase64 = customIconBase64,
                isLoading = isIconLoading,
                onClick = onPickIcon,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.settings_app_sort_name)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = dialogTextFieldColors(),
            )
            // Not single-line, so a long link wraps and stays fully visible. Whitespace
            // (including the Enter key's newline) is filtered out.
            OutlinedTextField(
                value = url,
                onValueChange = { onUrlChange(it.filterNot(Char::isWhitespace)) },
                modifier = Modifier.fillMaxWidth().focusRequester(urlFocusRequester),
                label = { Text(stringResource(R.string.bookmarks_link_label)) },
                isError = showUrlError,
                supportingText =
                    if (showUrlError) {
                        { Text(stringResource(R.string.bookmarks_invalid_link)) }
                    } else {
                        null
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = dialogTextFieldColors(),
            )
            BookmarkBrowserField(
                availableBrowsers = availableBrowsers,
                selectedPackage = browserPackageName,
                onClick = onOpenBrowsers,
                // Outlined text fields reserve space above their border for the floating label;
                // match it so the visible gaps between all three boxes are equal.
                modifier = Modifier.padding(top = OutlinedTextFieldLabelInset),
            )
        }
        Button(
            onClick = onSave,
            enabled = isUrlValid,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.ContentHorizontalPadding)
                    .padding(top = DesignTokens.SpacingSmall),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent,
                    contentColor = AppColors.OnAccent,
                ),
        ) {
            Text(
                text =
                    stringResource(
                        if (bookmark == null) R.string.common_action_add else R.string.dialog_save,
                    ),
            )
        }
    }
}

/** The bookmark's icon with an edit badge: the newly picked image, else the saved favicon or a globe. */
@Composable
private fun BookmarkIconPicker(
    bookmark: BookmarkInfo?,
    customIconBase64: String?,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickedBitmap =
        remember(customIconBase64) {
            val encoded = customIconBase64 ?: return@remember null
            val bytes = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull() ?: return@remember null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    // The edit badge sits in an unclipped outer box so it isn't cut off by the icon's circle.
    Box(modifier = modifier.size(68.dp)) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .clickable(enabled = !isLoading, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                pickedBitmap != null ->
                    Image(
                        bitmap = pickedBitmap,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Fit,
                    )
                bookmark != null -> BookmarkFavicon(bookmark = bookmark, size = 48.dp)
                else ->
                    Icon(
                        imageVector = MaterialSymbols.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp),
                    )
            }
        }
        if (!isLoading) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(AppColors.Accent.copy(alpha = 0.3f).compositeOver(AppColors.DialogBackground))
                        .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Edit,
                    contentDescription = stringResource(R.string.settings_edit_label),
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

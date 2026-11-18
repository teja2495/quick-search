package com.tk.quicksearch.shared.ui.components

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * The search field of an [AppPickerDrawer].
 *
 * @param autoFocus Focuses the field and opens the keyboard once the drawer has opened. Use it
 *   when the list is empty until the user types.
 * @param onSubmit Turns the field into an entry field: it shows an add icon instead of the search
 *   icon, uses [AppShapes.large] corners instead of a pill, and the keyboard's Done key
 *   submits the typed text.
 */
class AppPickerDrawerSearch(
    val query: TextFieldValue,
    val onQueryChange: (TextFieldValue) -> Unit,
    val placeholder: String? = null,
    val autoFocus: Boolean = false,
    val onSubmit: ((String) -> Unit)? = null,
)

/**
 * Bottom drawer for picking one item from a list: title with a close button, an optional pill
 * search field, then [content], which usually holds a `LazyColumn` of [AppPickerDrawerRow]s
 * spaced by [AppPickerDrawerRowSpacing].
 *
 * With a [search] field the drawer opens at a fixed height so it doesn't jump while results
 * change; without one it wraps short lists.
 *
 * @param titleIcon Shown before [title], such as the app's icon.
 * @param header Shown between the search field and [content], such as filter chips or a note.
 * @param footer Pinned below [content], such as an option that applies to the whole list.
 * @param onDismissStarted Called as soon as the drawer starts closing.
 * @param content Receives `dismiss`, which animates the drawer closed and then calls [onDismiss].
 */
@Composable
fun AppPickerDrawer(
    title: String,
    onDismiss: () -> Unit,
    search: AppPickerDrawerSearch? = null,
    dismissOnClickOutside: Boolean = true,
    onDismissStarted: (() -> Unit)? = null,
    titleIcon: (@Composable () -> Unit)? = null,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable BoxScope.(dismiss: () -> Unit) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * DRAWER_HEIGHT_FRACTION
    val fixedHeight = search != null

    AppBottomSheet(
        onDismissRequest = onDismiss,
        swipeToDismissEnabled = false,
        dismissOnClickOutside = dismissOnClickOutside,
        // Show the keyboard only after the open animation settles; bringing it up mid-animation
        // resizes the sheet and makes the slide-in stutter.
        onFullyExpanded =
            if (search?.autoFocus == true) {
                {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            } else {
                null
            },
        onDismissStarted = {
            keyboardController?.hide()
            onDismissStarted?.invoke()
        },
    ) { dismiss ->
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (fixedHeight) {
                            Modifier.fillMaxHeight(DRAWER_HEIGHT_FRACTION)
                        } else {
                            Modifier.heightIn(max = maxHeight)
                        },
                    )
                    .padding(
                        top = DesignTokens.SpacingLarge,
                        start = DesignTokens.ContentHorizontalPadding,
                        end = DesignTokens.ContentHorizontalPadding,
                        bottom = DesignTokens.SpacingLarge,
                    ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            AppDrawerHeader(
                onClose = {
                    keyboardController?.hide()
                    dismiss()
                },
                leadingContent = titleIcon,
            ) {
                Text(text = title)
            }

            search?.let { AppPickerDrawerSearchField(search = it, focusRequester = focusRequester) }

            header?.invoke(this)

            Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = fixedHeight)) {
                content(dismiss)
            }

            footer?.invoke(this)
        }
    }
}

@Composable
private fun AppPickerDrawerSearchField(
    search: AppPickerDrawerSearch,
    focusRequester: FocusRequester,
) {
    val searchLabel = search.placeholder ?: stringResource(R.string.common_search)
    OutlinedTextField(
        value = search.query,
        onValueChange = search.onQueryChange,
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).semantics {
            contentDescription = searchLabel
        },
        shape = if (search.onSubmit != null) AppShapes.large else AppShapes.full,
        leadingIcon = {
            Icon(
                imageVector = if (search.onSubmit != null) MaterialSymbols.Add else MaterialSymbols.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (search.query.text.isNotBlank()) {
                AppIconButton(onClick = { search.onQueryChange(TextFieldValue("")) }, tooltip = stringResource(R.string.desc_clear_search)) {
                    Icon(
                        imageVector = MaterialSymbols.Close,
                        contentDescription = stringResource(R.string.desc_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        placeholder =
            search.placeholder?.let {
                {
                    Text(
                        text = it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
        singleLine = true,
        keyboardOptions =
            if (search.onSubmit != null) KeyboardOptions(imeAction = ImeAction.Done) else KeyboardOptions.Default,
        keyboardActions =
            search.onSubmit?.let { submit -> KeyboardActions(onDone = { submit(search.query.text) }) }
                ?: KeyboardActions.Default,
        colors = dialogTextFieldColors(unfocusedIndicatorColor = Color.Transparent),
    )
}

/**
 * One option in an [AppPickerDrawer]. When [selected], the row is tinted with the accent color
 * and shows a filled check, unless a [trailing] slot replaces it. A null [selected] means the row
 * is an action without selection state.
 */
@Composable
fun AppPickerDrawerRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    selected: Boolean? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    titleMaxLines: Int = Int.MAX_VALUE,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = AppPickerDrawerRowMinHeight)
                .clip(AppPickerDrawerRowShape)
                .background(appPickerDrawerItemBackground(selected == true))
                .then(
                    if (selected == null) {
                        Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                    } else {
                        Modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
                    },
                )
                .padding(
                    vertical = DesignTokens.SpacingMedium,
                    horizontal = DesignTokens.SpacingMedium,
                ),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            supportingContent?.invoke()
        }
        when {
            trailing != null -> trailing()
            selected == true -> AppPickerDrawerSelectedCheck(contentDescription = null)
        }
    }
}

/** Background of a drawer option: the accent tint when [selected], otherwise [unselected]. */
@Composable
fun appPickerDrawerItemBackground(
    selected: Boolean,
    unselected: Color = Color.Transparent,
): Color = if (selected) AppColors.AccentTintTonal else unselected

/** Accent check that marks the selected option in an [AppPickerDrawer]. */
@Composable
fun AppPickerDrawerSelectedCheck(contentDescription: String? = stringResource(R.string.desc_selected)) {
    Icon(
        imageVector = MaterialSymbols.Check,
        contentDescription = contentDescription,
        tint = AppColors.Accent,
        modifier = Modifier.size(SELECTED_CHECK_SIZE),
    )
}

/** Leading icon for an [AppPickerDrawerRow] option that has no app icon of its own. */
@Composable
fun AppPickerDrawerIconBadge(icon: ImageVector) {
    TonalIconBadgeContainer(size = AppPickerDrawerLeadingSize, shape = AppShapes.small) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(ICON_BADGE_ICON_SIZE),
        )
    }
}

/** Leading app icon for an [AppPickerDrawerRow], with a generic icon until it loads. */
@Composable
fun AppPickerDrawerAppIcon(packageName: String) {
    val bitmap = rememberAppIcon(packageName = packageName).bitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(AppPickerDrawerLeadingSize),
        )
    } else {
        AppPickerDrawerIconBadge(icon = MaterialSymbols.Android)
    }
}

/** Hint shown in place of the list, such as before the user types or when nothing matches. */
@Composable
fun BoxScope.AppPickerDrawerMessage(
    text: String,
    centered: Boolean = false,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        modifier =
            if (centered) {
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.75f)
                    .padding(top = DesignTokens.SpacingSmall)
            } else {
                Modifier.padding(horizontal = DesignTokens.SpacingMedium)
            },
    )
}

val AppPickerDrawerRowMinHeight = 56.dp
val AppPickerDrawerLeadingSize = 32.dp

/** Space between rows of an [AppPickerDrawer] list: `Arrangement.spacedBy(AppPickerDrawerRowSpacing)`. */
val AppPickerDrawerRowSpacing = 2.dp

val AppPickerDrawerRowShape = AppShapes.large
private val SELECTED_CHECK_SIZE = 24.dp
private val ICON_BADGE_ICON_SIZE = 18.dp

private const val DRAWER_HEIGHT_FRACTION = 0.85f

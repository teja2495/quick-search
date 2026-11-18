package com.tk.quicksearch.appNotifications

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.components.AppButton
import com.tk.quicksearch.shared.ui.components.AppIconButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerAppIcon
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerIconBadge
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerSearch
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** Adds [keyword] unless it is blank or already there in any case. */
internal fun List<String>.plusKeyword(keyword: String): List<String> {
    val trimmed = keyword.trim()
    return if (trimmed.isEmpty() || any { it.equals(trimmed, ignoreCase = true) }) this else this + trimmed
}

/** The icon for a source row: the app's icon, or a generic badge for Any App. */
@Composable
internal fun AppNotificationSourceIcon(packageName: String?) {
    if (packageName != null) AppPickerDrawerAppIcon(packageName) else AppPickerDrawerIconBadge(MaterialSymbols.Apps)
}

/**
 * Lists one source's keywords, divided by lines. The drawer's field only adds a word or phrase
 * (Done key or the Add row); it doesn't filter the list. Each row deletes its keyword. An app with no keywords shows
 * all its notifications. For an app ([appName] set), the title shows its icon, the intro names it, and it offers a button that
 * closes the drawer, keeping all of its notifications.
 */
@Composable
internal fun AppNotificationKeywordsDrawer(
    title: String,
    keywords: List<String>,
    appName: String? = null,
    packageName: String? = null,
    onKeywordsChange: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val typed = query.text.trim()
    val add: () -> Unit = {
        onKeywordsChange(keywords.plusKeyword(typed))
        query = TextFieldValue("")
    }
    val canAdd = typed.isNotEmpty() && keywords.none { it.equals(typed, ignoreCase = true) }

    AppPickerDrawer(
        title = title,
        onDismiss = onDismiss,
        titleIcon = packageName?.let { { AppPickerDrawerAppIcon(it) } },
        search = AppPickerDrawerSearch(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.app_notifications_keyword_placeholder),
            autoFocus = keywords.isEmpty(),
            onSubmit = { add() },
        ),
    ) { dismiss ->
        if (keywords.isEmpty() && !canAdd) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter)
                    .fillMaxWidth(KeywordsIntroWidthFraction)
                    .padding(top = DesignTokens.SpacingXXLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
            ) {
                Text(
                    text = if (appName != null) {
                        stringResource(R.string.app_notifications_keywords_intro_app, appName)
                    } else {
                        stringResource(R.string.app_notifications_keywords_intro)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (appName != null) {
                    WantAllPillButton(
                        text = stringResource(R.string.app_notifications_want_all),
                        onClick = dismiss,
                    )
                }
            }
        }
        LazyColumn {
            if (canAdd) {
                item(key = "add") {
                    KeywordRow(
                        text = stringResource(R.string.app_notifications_add_keyword, typed),
                        icon = MaterialSymbols.Add,
                        iconDescription = stringResource(R.string.common_action_add),
                        iconTint = AppColors.LinkColor,
                        onClick = add,
                        rowClickable = true,
                    )
                }
            }
            // Prefixed so a keyword such as "add" can't collide with the Add row's key.
            itemsIndexed(keywords, key = { _, keyword -> "keyword:$keyword" }) { index, keyword ->
                if (index > 0 || canAdd) HorizontalDivider(color = AppColors.SettingsDivider)
                KeywordRow(
                    text = keyword,
                    icon = MaterialSymbols.DeleteOutline,
                    iconDescription = stringResource(R.string.app_notifications_remove_app, keyword),
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onKeywordsChange(keywords - keyword) },
                )
            }
        }
    }
}

/** Filled button that keeps every notification from the app. */
@Composable
private fun WantAllPillButton(
    text: String,
    onClick: () -> Unit,
) {
    AppButton(onClick = onClick) {
        Text(text = text, textAlign = TextAlign.Center)
    }
}

private val KeywordRowHeight = 52.dp
private const val KeywordsIntroWidthFraction = 0.9f

/**
 * One compact line of the keywords list: text, then an icon button at the end. Every row has the
 * same height and insets so the Add row and keyword rows line up. With [rowClickable] the whole row
 * runs [onClick], not only the icon.
 */
@Composable
private fun KeywordRow(
    text: String,
    icon: ImageVector,
    iconDescription: String,
    iconTint: Color,
    onClick: () -> Unit,
    rowClickable: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = KeywordRowHeight)
            .then(if (rowClickable) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = DesignTokens.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(vertical = DesignTokens.SpacingSmall),
        )
        if (rowClickable) {
            Box(modifier = Modifier.size(DesignTokens.MinTouchTarget), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            }
        } else {
            AppIconButton(onClick = onClick, tooltip = iconDescription) {
                Icon(imageVector = icon, contentDescription = iconDescription, tint = iconTint)
            }
        }
    }
}

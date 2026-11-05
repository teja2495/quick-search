package com.tk.quicksearch.settings.bookmarksSettings

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.BrowserApp
import com.tk.quicksearch.searchEngines.IN_APP_BROWSER_PACKAGE
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerLeadingSize
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRow
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRowSpacing
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** Header for a bookmark editor page, laid out like AppPickerDrawer's: optional back, title, close. */
@Composable
internal fun BookmarkSheetHeader(
    title: String,
    navigationIcon: (() -> Unit)?,
    onClose: (() -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = if (navigationIcon != null) 8.dp else DesignTokens.ContentHorizontalPadding,
                    end = 8.dp,
                    top = DesignTokens.SpacingLarge,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        if (navigationIcon != null) {
            IconButton(onClick = navigationIcon) {
                Icon(
                    imageVector = MaterialSymbols.ArrowBack,
                    contentDescription = stringResource(R.string.desc_navigate_back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (onClose != null) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = MaterialSymbols.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "Open in" row on the form page: the chosen browser's icon and name; tapping opens the list page. */
@Composable
internal fun BookmarkBrowserField(
    availableBrowsers: List<BrowserApp>,
    selectedPackage: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val browsers = bookmarkBrowserOptions(availableBrowsers)
    val selectedLabel =
        browsers.firstOrNull { it.packageName == selectedPackage }?.label
            ?: stringResource(R.string.browser_in_app_name)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(OutlinedTextFieldDefaults.shape)
                .border(1.dp, MaterialTheme.colorScheme.outline, OutlinedTextFieldDefaults.shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrowserIcon(packageName = selectedPackage, size = 24.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_search_engine_browser_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Icon(
            imageVector = MaterialSymbols.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Browser list page shown in place of the form. */
@Composable
internal fun BookmarkBrowserList(
    availableBrowsers: List<BrowserApp>,
    selectedPackage: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.SpacingLarge),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        BookmarkSheetHeader(
            title = stringResource(R.string.settings_search_engine_browser_label),
            navigationIcon = onBack,
            onClose = null,
        )
        BookmarkBrowserRows(
            availableBrowsers = availableBrowsers,
            selectedPackage = selectedPackage,
            onSelect = onSelect,
            modifier = Modifier.weight(1f, fill = false).padding(horizontal = DesignTokens.ContentHorizontalPadding),
        )
    }
}

/** Drawer from the page's menu for the browser that new and imported bookmarks open in. */
@Composable
internal fun BookmarkDefaultBrowserDrawer(
    availableBrowsers: List<BrowserApp>,
    selectedPackage: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AppPickerDrawer(
        title = stringResource(R.string.settings_search_engine_browser_default),
        onDismiss = onDismiss,
        header = {
            Text(
                text = stringResource(R.string.bookmarks_default_browser_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) { dismiss ->
        BookmarkBrowserRows(
            availableBrowsers = availableBrowsers,
            selectedPackage = selectedPackage,
            onSelect = { packageName ->
                onSelect(packageName)
                dismiss()
            },
        )
    }
}

@Composable
private fun BookmarkBrowserRows(
    availableBrowsers: List<BrowserApp>,
    selectedPackage: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val browsers = bookmarkBrowserOptions(availableBrowsers)
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing),
    ) {
        items(browsers, key = BrowserApp::packageName) { browser ->
            AppPickerDrawerRow(
                title = browser.label,
                selected = browser.packageName == selectedPackage,
                leading = { BrowserIcon(packageName = browser.packageName, size = AppPickerDrawerLeadingSize) },
                onClick = { onSelect(browser.packageName) },
            )
        }
    }
}

/** Installed browsers with the in-app browser first, which is always offered. */
@Composable
private fun bookmarkBrowserOptions(availableBrowsers: List<BrowserApp>): List<BrowserApp> {
    val inAppLabel = stringResource(R.string.browser_in_app_name)
    return listOf(BrowserApp(packageName = IN_APP_BROWSER_PACKAGE, label = inAppLabel)) +
        availableBrowsers.filterNot { it.packageName == IN_APP_BROWSER_PACKAGE }
}

/**
 * [packageName] when it is still offered (the in-app browser or an installed browser), else the
 * in-app browser, so a bookmark or default pointing at an uninstalled browser still has a choice.
 */
internal fun resolveBookmarkBrowser(
    packageName: String?,
    availableBrowsers: List<BrowserApp>,
): String =
    packageName?.takeIf { it == IN_APP_BROWSER_PACKAGE || availableBrowsers.any { b -> b.packageName == it } }
        ?: IN_APP_BROWSER_PACKAGE

/** The browser's launcher icon; the in-app browser uses Quick Search's own icon, default a globe. */
@Composable
private fun BrowserIcon(
    packageName: String?,
    size: Dp,
) {
    val iconPackage =
        when (packageName) {
            null -> null
            IN_APP_BROWSER_PACKAGE -> LocalContext.current.packageName
            else -> packageName
        }
    val bitmap = iconPackage?.let { rememberAppIcon(packageName = it).bitmap }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(size).clip(CircleShape),
        )
    } else {
        Icon(
            imageVector = MaterialSymbols.Language,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size),
        )
    }
}

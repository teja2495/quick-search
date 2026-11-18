package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

/**
 * Material 3 small top app bar for full-screen pages (Settings and its detail pages).
 *
 * The container stays transparent so the page background (theme gradient, AMOLED black) shows
 * through, and it skips the on-scroll fill because callers clip their scrolling content below the
 * bar, so content never passes under it. The caller's root normally applies the window insets;
 * a page hosted in a `Scaffold` passes the status bar insets as [windowInsets] instead. A
 * full-screen dialog (such as a widget configuration screen) passes a close [navigationIcon].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopAppBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector = MaterialSymbols.ArrowBack,
    navigationContentDescription: String = stringResource(R.string.desc_navigate_back),
    windowInsets: WindowInsets = WindowInsets(0),
    actions: @Composable RowScope.() -> Unit = {},
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                AppIconButton(onClick = onBack, tooltip = navigationContentDescription) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = navigationContentDescription,
                    )
                }
            }
        },
        actions = actions,
        windowInsets = windowInsets,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                navigationIconContentColor = onSurface,
                titleContentColor = onSurface,
                actionIconContentColor = onSurface,
            ),
    )
}

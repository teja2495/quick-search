package com.tk.quicksearch.search.apps.privateSpace

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.AppTextButton

/** Creates a [PrivateSpaceController] that watches the Private Space while in composition. */
@Composable
internal fun rememberPrivateSpaceController(): PrivateSpaceController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember(context) { PrivateSpaceController(context, scope) }
    DisposableEffect(controller) {
        controller.start()
        onDispose { controller.stop() }
    }
    return controller
}

/** Appends the Private Space header and, while unlocked, its apps to an All Apps grid. */
internal fun LazyGridScope.privateSpaceItems(
    state: PrivateSpaceState,
    onLockedChange: (Boolean) -> Unit,
    appContent: @Composable (AppInfo) -> Unit,
) {
    if (!state.isAvailable) return
    item(key = PrivateSpaceHeaderKey, span = { GridItemSpan(maxLineSpan) }) {
        PrivateSpaceHeader(
            isLocked = state.isLocked,
            onLockedChange = onLockedChange,
        )
    }
    items(
        items = state.apps,
        key = { app -> "$PrivateSpaceHeaderKey:${app.launchCountKey()}" },
    ) { app -> appContent(app) }
}

@Composable
private fun PrivateSpaceHeader(
    isLocked: Boolean,
    onLockedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        Icon(
            imageVector = if (isLocked) MaterialSymbols.Lock else MaterialSymbols.LockOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.all_apps_private_space),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        AppTextButton(onClick = { onLockedChange(!isLocked) }) {
            Text(
                text =
                    stringResource(
                        if (isLocked) R.string.action_unlock_app else R.string.action_lock_app,
                    ),
            )
        }
    }
}

private const val PrivateSpaceHeaderKey = "private_space"

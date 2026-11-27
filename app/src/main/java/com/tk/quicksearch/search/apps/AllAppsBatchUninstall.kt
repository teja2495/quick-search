package com.tk.quicksearch.search.apps

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.appLock.AppLockGate
import com.tk.quicksearch.search.core.intentHelpers.AppManagementIntents
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.shared.ui.components.AppMessages
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens

internal class AllAppsBatchUninstall {
    private var authenticating by mutableStateOf(false)
    private var remainingKeys by mutableStateOf(emptyList<String>())
    private var awaitingResult by mutableStateOf(false)

    val isBusy: Boolean
        get() = authenticating || remainingKeys.isNotEmpty()

    fun start(
        context: Context,
        apps: List<AppInfo>,
        onStarted: () -> Unit,
    ) {
        if (isBusy || apps.isEmpty()) return
        authenticating = true
        AppLockGate.runAfterUnlockAny(
            context = context,
            packageNames = apps.map { it.packageName },
            onCancelled = { authenticating = false },
        ) {
            remainingKeys = apps.map { it.launchCountKey() }
            authenticating = false
            onStarted()
        }
    }

    @Composable
    fun LaunchPendingUninstalls(availableApps: Map<String, AppInfo>) {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            advance()
        }
        LaunchedEffect(remainingKeys, awaitingResult, availableApps) {
            // Each installer result (including cancellation) advances exactly one slot.
            if (awaitingResult) return@LaunchedEffect
            val nextKey = remainingKeys.firstOrNull() ?: return@LaunchedEffect
            val app = availableApps[nextKey]
            if (app == null) {
                advance()
                return@LaunchedEffect
            }
            awaitingResult = true
            try {
                launcher.launch(AppManagementIntents.uninstallIntent(app, returnResult = true))
            } catch (_: RuntimeException) {
                AppMessages.show(context, context.getString(R.string.error_uninstall_app, app.appName))
                advance()
            }
        }
    }

    private fun advance() {
        remainingKeys = remainingKeys.drop(1)
        awaitingResult = false
    }
}

@Composable
internal fun rememberAllAppsBatchUninstall(availableApps: Map<String, AppInfo>): AllAppsBatchUninstall {
    val batch = remember { AllAppsBatchUninstall() }
    batch.LaunchPendingUninstalls(availableApps)
    return batch
}

private val SelectionBadgeSize = 22.dp
private val SelectionBadgeRingWidth = 2.dp

/** Icon scale while selected: the tonal tile shows around the icon, as in Material photo grids. */
internal const val SelectedAppIconScale = 0.82f

/**
 * The selection mark at an app icon's top-end corner: an empty outlined circle until picked, then
 * a filled primary circle with a check that springs in.
 */
@Composable
internal fun BoxScope.UninstallSelectionBadge(selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.surfaceContainerHighest.copy(alpha = 0.85f),
        animationSpec = AppMotion.fastSpatial(),
        label = "selectionBadgeFill",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = AppMotion.fastSpatial(),
        label = "selectionCheckScale",
    )
    Box(
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .size(SelectionBadgeSize)
                .clip(AppShapes.full)
                .background(fill)
                .then(
                    if (selected) Modifier
                    else Modifier.border(SelectionBadgeRingWidth, colors.outline, AppShapes.full),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MaterialSymbols.Check,
            contentDescription = null,
            tint = colors.onPrimary,
            modifier = Modifier.size(SelectionBadgeSize - DesignTokens.SpacingXSmall).scale(checkScale),
        )
    }
}

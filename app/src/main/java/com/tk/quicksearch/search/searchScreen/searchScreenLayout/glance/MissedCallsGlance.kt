package com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import com.tk.quicksearch.shared.ui.components.AppButtonSize
import com.tk.quicksearch.shared.ui.components.AppButtonTone
import com.tk.quicksearch.shared.ui.components.AppOutlinedButton
import com.tk.quicksearch.shared.ui.components.AppIconButton
import android.content.Context
import android.content.Intent
import android.provider.CallLog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.appLock.AppLockGate
import com.tk.quicksearch.search.glance.MissedCallNotification
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.sendFromUserTap
import java.text.NumberFormat

/**
 * One summary row for missed call notifications, [calls] newest first. It names the callers, with
 * the app in brackets for calls from apps other than the phone app, and counts them in the title
 * when there is more than one. Tapping the row opens the newest call's app; when the calls come from more than
 * one app, a chip for each app opens that app's calls. A single phone app call gets a call back button.
 */
@Composable
internal fun MissedCallsRow(
    calls: List<MissedCallNotification>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val callerLabels = calls.map { call -> callerLabel(call) }.filterNotNull().distinct()
    val subtitle = callerLabels.joinToString(", ").ifBlank { null }
    val title =
        if (calls.size == 1) {
            stringResource(R.string.home_missed_call)
        } else {
            val count = remember(calls.size) { NumberFormat.getIntegerInstance().format(calls.size) }
            stringResource(R.string.home_missed_calls_count, count)
        }
    // The newest call from each app, phone app first.
    val appCalls = remember(calls) { calls.distinctBy { it.packageName }.sortedByDescending { it.fromPhoneApp } }
    val hasAppChips = appCalls.size > 1
    // GlanceStatusRow's layout, but with app chips the dismiss button stays beside the title rather than centered.
    val verticalPadding = glanceRowVerticalPadding()
    Row(
        modifier =
            Modifier.fillMaxWidth().heightIn(min = glanceRowMinHeight(DesignTokens.GlanceRowMinHeight)).padding(
                start = DesignTokens.GlanceRowHorizontalPadding,
                top = verticalPadding,
                bottom = verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).clickable(role = Role.Button) { openCallHistory(context, calls.first()) },
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = MaterialSymbols.PhoneMissed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.TextColumnSpacing)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = GlanceTextMaxLines,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (hasAppChips) {
                Row(
                    modifier = Modifier.padding(start = DesignTokens.SpacingXXLarge + DesignTokens.SpacingMedium, top = DesignTokens.SpacingXSmall),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                ) {
                    appCalls.forEach { call ->
                        MissedCallAppChip(call = call, onClick = { openCallHistory(context, call) })
                    }
                }
            }
        }
        // A single phone call gets the notification's call back action as a pill beside the dismiss button.
        val callBackIntent = calls.singleOrNull()?.takeIf { it.fromPhoneApp }?.callBackIntent
        if (callBackIntent != null) {
            Box(modifier = Modifier.padding(start = DesignTokens.SpacingSmall)) {
                GlanceActionChip(
                    label = stringResource(R.string.contact_method_call_label),
                    onClick = { callBackIntent.sendFromUserTap() },
                    icon = MaterialSymbols.Call,
                    colors =
                        GlancePillColors(
                            container = MaterialTheme.colorScheme.primaryContainer,
                            content = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                )
            }
        }
        AppIconButton(
            onClick = onDismiss,
            tooltip = stringResource(R.string.common_close),
            modifier = Modifier.size(GlanceDismissButtonSize),
            anchorModifier =
                Modifier
                    .align(if (hasAppChips) Alignment.Top else Alignment.CenterVertically)
                    .padding(start = DesignTokens.SpacingSmall),
        ) {
            Icon(
                imageVector = MaterialSymbols.Close,
                contentDescription = stringResource(R.string.common_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** The caller, followed by the app in brackets when the call did not come through the phone app. */
@Composable
private fun callerLabel(call: MissedCallNotification): String? {
    if (call.fromPhoneApp) return call.caller
    val appLabel = rememberAppLabel(call.packageName)
    return call.caller?.let { stringResource(R.string.home_missed_call_from_app, it, appLabel) } ?: appLabel
}

@Composable
private fun MissedCallAppChip(
    call: MissedCallNotification,
    onClick: () -> Unit,
) {
    AppOutlinedButton(
        onClick = onClick,
        tone = AppButtonTone.Custom(MaterialTheme.colorScheme.onSurface),
        size = AppButtonSize.ExtraSmall,
        contentPadding = PaddingValues(start = DesignTokens.SpacingSmall, end = DesignTokens.SpacingMedium),
    ) {
        Box(modifier = Modifier.size(DesignTokens.ButtonIconSizeExtraSmall), contentAlignment = Alignment.Center) {
            NotificationAppIcon(call.packageName)
        }
        Spacer(Modifier.width(DesignTokens.SpacingSmall))
        Text(
            text = rememberAppLabel(call.packageName),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Opens the call history of the app that posted [latest] when it has one (the phone app), and
 * otherwise the notification's own target, such as WhatsApp's calls.
 */
internal fun openCallHistory(
    context: Context,
    latest: MissedCallNotification,
) {
    AppLockGate.runAfterUnlock(context, latest.packageName) { openCallHistoryUnlocked(context, latest) }
}

/** [openCallHistory] for callers that have already passed the app lock. */
internal fun openCallHistoryUnlocked(
    context: Context,
    latest: MissedCallNotification,
) {
    // Only the app that posted the call is asked for its call history, so a WhatsApp or other
    // app's missed call opens that app (through its notification) rather than the phone app.
    val callLog =
        Intent(Intent.ACTION_VIEW)
            .setType(CallLog.Calls.CONTENT_TYPE)
            .setPackage(latest.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val opened = runCatching { context.startActivity(callLog) }.isSuccess
    if (!opened) launchNotificationTarget(context, latest.packageName, latest.contentIntent)
}

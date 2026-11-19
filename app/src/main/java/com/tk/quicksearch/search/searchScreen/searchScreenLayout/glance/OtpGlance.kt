package com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance

import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.search.glance.OtpNotification
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.delay

/** How long the copy icon shows a check after copying. */
private const val COPIED_FEEDBACK_MILLIS = 2_000L

/** Codes younger than this read "Now" rather than "0 sec ago". */
private const val JUST_NOW_MILLIS = 5_000L

/**
 * The newest one-time code in large capitals with a copy icon beside it, under "From <sender> •
 * <age>" (the app stands in when the message doesn't name a sender). Tapping the code or its icon
 * copies the code, and the icon turns into a check for a moment; tapping the rest of the row opens
 * the message.
 */
@Composable
internal fun OtpCodeRow(
    otp: OtpNotification,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var copied by remember(otp.key, otp.code) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MILLIS)
            copied = false
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(otp.key) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                delay(1_000)
            }
        }
    }
    val copyLabel = stringResource(R.string.calculator_copy_result)
    val source = stringResource(R.string.home_otp_from, otp.sender ?: rememberAppLabel(otp.packageName))
    val age =
        if (nowMillis - otp.postTime < JUST_NOW_MILLIS) {
            stringResource(R.string.calendar_relative_now)
        } else {
            DateUtils.getRelativeTimeSpanString(
                otp.postTime,
                nowMillis,
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            ).toString().replace(".", "")
        }
    GlanceStatusRow(
        icon = { NotificationAppIcon(otp.packageName) },
        title = "$source • $age",
        titleStyle = MaterialTheme.typography.bodyMedium,
        titleMaxLines = GlanceTextMaxLines,
        onClick = { openNotificationTarget(context, otp.packageName, otp.contentIntent) },
        onDismiss = onDismiss,
        belowText = {
            // The code and its icon copy; the rest of the row opens the message.
            Row(
                modifier =
                    Modifier
                        .clip(AppShapes.small)
                        .clickable(role = Role.Button) { copied = copySensitiveText(context, copyLabel, otp.code) },
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = otp.code,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Icon(
                    imageVector = if (copied) MaterialSymbols.Check else MaterialSymbols.ContentCopy,
                    contentDescription = stringResource(if (copied) R.string.home_copied else R.string.calculator_copy_result),
                    tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DesignTokens.IconSizeSmall),
                )
            }
        },
    )
}

/** Copies [text], by default marked sensitive so Android 13+ hides it from the clipboard preview. */
internal fun copySensitiveText(
    context: Context,
    label: String,
    text: String,
    sensitive: Boolean = true,
): Boolean {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
    val clip = ClipData.newPlainText(label, text)
    if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    }
    return runCatching { clipboard.setPrimaryClip(clip) }.isSuccess
}

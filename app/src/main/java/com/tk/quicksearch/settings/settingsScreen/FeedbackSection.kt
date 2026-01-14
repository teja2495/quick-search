package com.tk.quicksearch.settings.settingsScreen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.SettingsCard
import com.tk.quicksearch.settings.SettingsSectionTitle
import com.tk.quicksearch.ui.theme.DesignTokens

// Constants for consistent spacing
private object FeedbackSpacing {
    val cardPaddingHorizontal = DesignTokens.CardHorizontalPadding
    val cardPaddingVertical = DesignTokens.CardVerticalPadding
    val iconEndPadding = DesignTokens.ItemRowSpacing
    val chevronMinWidth = 80.dp
    val chevronMinHeight = 40.dp
    val chevronStartPadding = 8.dp
}

/**
 * Data class representing a feedback item.
 */
private data class FeedbackItem(
    val title: String,
    val description: AnnotatedString? = null,
    val iconVector: ImageVector? = null,
    val iconResId: Int? = null,
    val iconTint: Color? = null,
    val onClick: () -> Unit
)

@Composable
fun FeedbackSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val onSendFeedback = {
        val versionName = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
        
        val androidVersion = android.os.Build.VERSION.RELEASE
        val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        
        val emailBody = """
            

            
            ---
            Android Version: $androidVersion
            Device: $deviceModel
        """.trimIndent()

        val subject = "Quick Search Feedback - v$versionName"

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:tejakarlapudi.apps@gmail.com?subject=${Uri.encode(subject)}&body=${Uri.encode(emailBody)}")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle case where no email app is installed
        }
    }
    
    val onRateApp = {
        val packageName = context.packageName
        try {
            // Try to open Google Play Store app
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                setPackage("com.android.vending")
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback to web browser if Play Store app is not available
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Handle case where browser is not available
            }
        }
    }

    val onOpenGitHub = {
        val url = "https://github.com/teja2495/quick-search"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle case where browser is not available
        }
    }
    
    Column(modifier = modifier) {
        SettingsSectionTitle(
            title = stringResource(R.string.settings_section_feedback)
        )

        SettingsCard {
            Column {
                val feedbackItems = listOf(
                    FeedbackItem(
                        title = stringResource(R.string.settings_feedback_send_title),
                        description = AnnotatedString(stringResource(R.string.settings_feedback_send_desc)),
                        iconVector = Icons.Rounded.Email,
                        onClick = onSendFeedback
                    ),
                    FeedbackItem(
                        title = stringResource(R.string.settings_feedback_rate_title),
                        description = AnnotatedString(stringResource(R.string.settings_feedback_rate_desc)),
                        iconResId = R.drawable.google_play,
                        onClick = onRateApp
                    ),
                    FeedbackItem(
                        title = stringResource(R.string.settings_feedback_github_title),
                        description = AnnotatedString(stringResource(R.string.settings_feedback_github_desc)),
                        iconResId = R.drawable.ic_github,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onOpenGitHub
                    )
                )
                
                feedbackItems.forEachIndexed { index, item ->
                    FeedbackRow(
                        item = item,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (index != feedbackItems.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackRow(
    item: FeedbackItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = item.onClick)
            .padding(horizontal = FeedbackSpacing.cardPaddingHorizontal, vertical = FeedbackSpacing.cardPaddingVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            item.iconVector != null -> {
                Icon(
                    imageVector = item.iconVector,
                    contentDescription = item.title,
                    tint = item.iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = FeedbackSpacing.iconEndPadding)
                )
            }
            item.iconResId != null -> {
                Icon(
                    painter = painterResource(id = item.iconResId),
                    contentDescription = item.title,
                    tint = item.iconTint ?: Color.Unspecified,
                    modifier = Modifier.padding(end = FeedbackSpacing.iconEndPadding)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            item.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .widthIn(min = FeedbackSpacing.chevronMinWidth)
                .heightIn(min = FeedbackSpacing.chevronMinHeight),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.desc_navigate_forward),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = FeedbackSpacing.chevronStartPadding)
            )
        }
    }
}


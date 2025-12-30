package com.tk.quicksearch.settings

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
import androidx.compose.material3.ElevatedCard
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
import com.tk.quicksearch.R

/**
 * Data class representing a feedback item.
 */
private data class FeedbackItem(
    val title: String,
    val iconVector: ImageVector? = null,
    val iconResId: Int? = null,
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
    
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_section_feedback),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = SettingsSpacing.sectionTitleBottomPadding)
        )
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column {
                val feedbackItems = listOf(
                    FeedbackItem(
                        title = stringResource(R.string.settings_feedback_send_title),
                        iconVector = Icons.Rounded.Email,
                        onClick = onSendFeedback
                    ),
                    FeedbackItem(
                        title = stringResource(R.string.settings_feedback_rate_title),
                        iconResId = R.drawable.google_play,
                        onClick = onRateApp
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
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            item.iconVector != null -> {
                Icon(
                    imageVector = item.iconVector,
                    contentDescription = item.title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            item.iconResId != null -> {
                Icon(
                    painter = painterResource(id = item.iconResId),
                    contentDescription = item.title,
                    tint = Color.Unspecified,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .widthIn(min = 80.dp)
                .heightIn(min = 40.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.desc_navigate_forward),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}


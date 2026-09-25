package com.tk.quicksearch.pinnedNotifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.shared.util.lockPortraitOrientationOnPhones

class PinnedNotificationChoiceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPortraitOrientationOnPhones()
        val action = CustomWidgetButtonAction.fromJson(intent.getStringExtra(ExtraAction))
        if (action == null) {
            finish()
            return
        }
        val preferences = UserAppPreferences(this)
        setContent {
            QuickSearchTheme(
                fontScaleMultiplier = preferences.getFontScaleMultiplier(),
                appTheme = preferences.getAppTheme(),
                appThemeMode = preferences.getAppThemeMode(),
            ) {
                var selectedMode by remember {
                    mutableStateOf(PinnedNotifications.PinMode.WITH_OTHER_ITEMS)
                }
                AppAlertDialog(
                    onDismissRequest = ::finish,
                    title = { Text(stringResource(R.string.pin_notification_dialog_title)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PinModeRow(
                                label = stringResource(R.string.pin_notification_with_other_items),
                                selected = selectedMode == PinnedNotifications.PinMode.WITH_OTHER_ITEMS,
                                onClick = {
                                    selectedMode = PinnedNotifications.PinMode.WITH_OTHER_ITEMS
                                },
                            )
                            PinModeRow(
                                label = stringResource(R.string.pin_notification_separately),
                                selected = selectedMode == PinnedNotifications.PinMode.SEPARATELY,
                                onClick = { selectedMode = PinnedNotifications.PinMode.SEPARATELY },
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            colors =
                                ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ),
                            onClick = {
                                PinnedNotifications.pin(this, action, selectedMode)
                                finish()
                            },
                        ) {
                            Text(stringResource(R.string.action_pin_app))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = ::finish) {
                            Text(stringResource(R.string.dialog_cancel))
                        }
                    },
                )
            }
        }
    }

    companion object {
        private const val ExtraAction = "pinned_notification_action"

        fun createIntent(context: Context, action: CustomWidgetButtonAction): Intent =
            Intent(context, PinnedNotificationChoiceActivity::class.java)
                .putExtra(ExtraAction, action.toJson())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@androidx.compose.runtime.Composable
private fun PinModeRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val selectedBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val horizontalBackgroundExtension = 12.dp
    val backgroundCornerRadius = 14.dp
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (selected) {
                        val horizontalExtensionPx = horizontalBackgroundExtension.toPx()
                        drawRoundRect(
                            color = selectedBackgroundColor,
                            topLeft = Offset(x = -horizontalExtensionPx, y = 0f),
                            size = Size(
                                width = size.width + horizontalExtensionPx * 2,
                                height = size.height,
                            ),
                            cornerRadius = CornerRadius(backgroundCornerRadius.toPx()),
                        )
                    }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

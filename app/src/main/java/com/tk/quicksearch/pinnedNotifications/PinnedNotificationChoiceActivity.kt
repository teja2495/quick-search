package com.tk.quicksearch.pinnedNotifications

import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRow
import androidx.compose.foundation.selection.selectableGroup
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.shared.util.lockPortraitOrientationOnPhones
import com.tk.quicksearch.shared.ui.components.AppTextButton

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
                        Column(
                            modifier = Modifier.selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                        ) {
                            AppPickerDrawerRow(
                                title = stringResource(R.string.pin_notification_with_other_items),
                                selected = selectedMode == PinnedNotifications.PinMode.WITH_OTHER_ITEMS,
                                onClick = {
                                    selectedMode = PinnedNotifications.PinMode.WITH_OTHER_ITEMS
                                },
                            )
                            AppPickerDrawerRow(
                                title = stringResource(R.string.pin_notification_separately),
                                selected = selectedMode == PinnedNotifications.PinMode.SEPARATELY,
                                onClick = { selectedMode = PinnedNotifications.PinMode.SEPARATELY },
                            )
                        }
                    },
                    confirmButton = {
                        AppTextButton(
                            onClick = {
                                PinnedNotifications.pin(this, action, selectedMode)
                                finish()
                            },
                        ) {
                            Text(stringResource(R.string.action_pin_app))
                        }
                    },
                    dismissButton = {
                        AppTextButton(onClick = ::finish) {
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

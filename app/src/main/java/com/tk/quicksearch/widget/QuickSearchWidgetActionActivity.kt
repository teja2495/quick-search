package com.tk.quicksearch.widget

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.utils.ContactIntentHelpers
import com.tk.quicksearch.search.core.IntentHelpers
import com.tk.quicksearch.search.data.launchStaticShortcut

class QuickSearchWidgetActionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = CustomWidgetButtonAction.fromJson(
            intent.getStringExtra(EXTRA_CUSTOM_BUTTON_ACTION)
        )
        if (action != null) {
            handleAction(action)
        }
        finish()
    }

    private fun handleAction(action: CustomWidgetButtonAction) {
        val app = application as Application
        when (action) {
            is CustomWidgetButtonAction.App -> {
                IntentHelpers.launchApp(app, action.toAppInfo()) { resId, arg ->
                    showToast(resId, arg)
                }
            }
            is CustomWidgetButtonAction.Contact -> {
                ContactIntentHelpers.openContact(app, action.toContactInfo()) { resId ->
                    showToast(resId)
                }
            }
            is CustomWidgetButtonAction.File -> {
                IntentHelpers.openFile(app, action.toDeviceFile()) { resId, arg ->
                    showToast(resId, arg)
                }
            }
            is CustomWidgetButtonAction.Setting -> {
                val setting = action.toDeviceSetting()
                if (!setting.isSupported()) {
                    showToast(R.string.error_open_setting, setting.title)
                    return
                }
                runCatching { startActivity(setting.toIntent(this)) }
                    .onFailure { showToast(R.string.error_open_setting, setting.title) }
            }
            is CustomWidgetButtonAction.AppShortcut -> {
                val error = launchStaticShortcut(this, action.toStaticShortcut())
                if (error != null) {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showToast(resId: Int, formatArg: String? = null) {
        val message = if (formatArg.isNullOrBlank()) {
            getString(resId)
        } else {
            getString(resId, formatArg)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_CUSTOM_BUTTON_ACTION =
            "com.tk.quicksearch.extra.CUSTOM_WIDGET_BUTTON_ACTION"

        fun createIntent(context: Context, action: CustomWidgetButtonAction): Intent {
            return Intent(context, QuickSearchWidgetActionActivity::class.java).apply {
                putExtra(EXTRA_CUSTOM_BUTTON_ACTION, action.toJson())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}

package com.tk.quicksearch.widgets.searchWidget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Invisible trampoline for the widget mic's Digital Assistant action. It draws nothing, so the
 * search screen doesn't flash before the assistant opens, and finishes once the assistant is up.
 */
class AssistantLaunchActivity : ComponentActivity() {
    private val unusedVoiceInputLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            VoiceSearchHandler(this, unusedVoiceInputLauncher)
                .handleMicAction(MicAction.DIGITAL_ASSISTANT)
        }
    }

    override fun onStop() {
        super.onStop()
        // Nothing to come back to once the assistant covers this window or the user leaves.
        if (!isChangingConfigurations) finish()
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, AssistantLaunchActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}

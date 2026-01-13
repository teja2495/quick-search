package com.tk.quicksearch.search.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.tk.quicksearch.R
import com.tk.quicksearch.search.ui.MicAction

/**
 * Handler for voice search and digital assistant functionality.
 * Encapsulates all voice-related operations to keep MainActivity focused on lifecycle management.
 */
class VoiceSearchHandler(
    private val context: Activity,
    private val voiceInputLauncher: ActivityResultLauncher<Intent>
) {

    fun handleMicAction(micAction: MicAction) {
        when (micAction) {
            MicAction.DEFAULT_VOICE_SEARCH -> startVoiceInput()
            MicAction.DIGITAL_ASSISTANT -> startDigitalAssistant()
        }
    }

    private fun startVoiceInput() {
        val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.widget_label_text))
        }
        try {
            voiceInputLauncher.launch(voiceIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.voice_input_not_available),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun startDigitalAssistant() {
        val assistantIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (assistantIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(assistantIntent)
            context.finish()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.voice_input_not_available),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun processVoiceInputResult(
        result: androidx.activity.result.ActivityResult,
        onQueryChange: (String) -> Unit
    ) {
        val data = result.data
        val spokenText = data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spokenText.isNullOrBlank()) {
            onQueryChange(spokenText)
        }
    }
}
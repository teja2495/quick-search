package com.tk.quicksearch.widgets.searchWidget

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.util.defaultAssistantPackage

private const val CHATGPT_PACKAGE = "com.openai.chatgpt"
private const val CHATGPT_VOICE_MODE_URL = "https://chatgpt.com/?mode=voice"/**
 * Handler for voice search and digital assistant functionality.
 * Encapsulates all voice-related operations to keep MainActivity focused on lifecycle management.
 */
class VoiceSearchHandler(
    private val context: Activity,
    private val voiceInputLauncher: ActivityResultLauncher<Intent>,
) {
    fun handleMicAction(micAction: MicAction) {
        when (micAction) {
            MicAction.DEFAULT_VOICE_SEARCH -> {
                startVoiceInput()
            }

            MicAction.DIGITAL_ASSISTANT -> {
                startDigitalAssistant()
            }

            MicAction.OFF -> {
                // Mic is disabled, do nothing
            }
        }
    }

    private fun startVoiceInput() {
        val voiceIntent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.app_name))
            }
        try {
            voiceInputLauncher.launch(voiceIntent)
        } catch (e: ActivityNotFoundException) {
            Toast
                .makeText(
                    context,
                    context.getString(R.string.voice_input_not_available),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    private fun startDigitalAssistant() {
        // A candidate can resolve yet still be refused at launch (e.g. not exported), so try each
        // in turn; the assistant app's own launcher entry comes after its voice and assist entries.
        val launched =
            assistantIntentCandidates().any { intent ->
                try {
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    true
                } catch (e: ActivityNotFoundException) {
                    false
                } catch (e: SecurityException) {
                    false
                }
            }
        if (launched) {
            context.finish()
        } else {
            Toast
                .makeText(
                    context,
                    context.getString(R.string.voice_input_not_available),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    /**
     * Opens the user's default assistant listening for speech. Unscoped VOICE_COMMAND can land on any
     * app that handles it, and on recent Google app versions it opens nothing. For Google, hands-free
     * voice search is what opens the Gemini voice overlay; ChatGPT has no voice intent but opens voice
     * mode from its voice link; other assistants take VOICE_COMMAND or ASSIST, and an assistant that
     * handles none of them is opened from its launcher entry.
     */
    private fun assistantIntentCandidates(): List<Intent> {
        val packageManager = context.packageManager
        val assistantPackage = context.defaultAssistantPackage()?.takeIf { it != context.packageName }
        val candidates =
            buildList {
                if (assistantPackage == CHATGPT_PACKAGE) {
                    add(Intent(Intent.ACTION_VIEW, Uri.parse(CHATGPT_VOICE_MODE_URL)).setPackage(assistantPackage))
                }
                if (assistantPackage != null) {
                    add(Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE).setPackage(assistantPackage))
                    add(Intent(Intent.ACTION_VOICE_COMMAND).setPackage(assistantPackage))
                    add(Intent(Intent.ACTION_ASSIST).setPackage(assistantPackage))
                    // Fallback for every assistant: open the app itself when none of the above launches.
                    packageManager.getLaunchIntentForPackage(assistantPackage)?.let(::add)
                }
                add(Intent(Intent.ACTION_VOICE_COMMAND))
            }
        return candidates.filter { it.resolveActivity(packageManager) != null }
    }

    fun processVoiceInputResult(
        result: androidx.activity.result.ActivityResult,
        onQueryChange: (String) -> Unit,
    ) {
        val data = result.data
        val spokenText =
            data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
        if (!spokenText.isNullOrBlank()) {
            onQueryChange(spokenText)
        }
    }
}

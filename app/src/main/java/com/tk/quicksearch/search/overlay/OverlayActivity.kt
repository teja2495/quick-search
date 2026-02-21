package com.tk.quicksearch.search.overlay

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.ui.theme.QuickSearchTheme
import com.tk.quicksearch.util.WallpaperUtils
import com.tk.quicksearch.widget.voiceSearch.MicAction
import com.tk.quicksearch.widget.voiceSearch.VoiceSearchHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OverlayActivity : ComponentActivity() {
    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var voiceSearchHandler: VoiceSearchHandler
    private val voiceInputLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            voiceSearchHandler.processVoiceInputResult(result, searchViewModel::onQueryChange)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set transparent background for seamless overlay appearance
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        val navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle, navigationBarStyle)

        super.onCreate(savedInstanceState)

        // Disable activity opening animation for instant appearance
        overridePendingTransition(0, 0)

        if (intent?.getBooleanExtra(OverlayModeController.EXTRA_CLOSE_OVERLAY, false) == true) {
            finish()
            return
        }
        initializeVoiceSearchHandler()
        handleVoiceIntentIfNeeded(intent)

        // Match MainActivity behavior so cold overlay opens with wallpaper already warming.
        lifecycleScope.launch(Dispatchers.IO) { WallpaperUtils.preloadWallpaper(this@OverlayActivity) }

        setContent {
            QuickSearchTheme {
                Box(
                        modifier = Modifier.fillMaxSize().background(Color.Transparent),
                ) {
                    OverlayRoot(
                            viewModel = searchViewModel,
                            onCloseRequested = { finish() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(OverlayModeController.EXTRA_CLOSE_OVERLAY, false)) {
            finish()
            return
        }
        handleVoiceIntentIfNeeded(intent)
    }

    override fun onStop() {
        super.onStop()
        searchViewModel.handleOnStop()
        if (!isChangingConfigurations && !isFinishing) {
            finishAndRemoveTask()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    private fun initializeVoiceSearchHandler() {
        voiceSearchHandler = VoiceSearchHandler(this, voiceInputLauncher)
    }

    private fun handleVoiceIntentIfNeeded(intent: android.content.Intent?) {
        val shouldStartVoiceSearch =
            intent?.getBooleanExtra(OverlayModeController.EXTRA_START_VOICE_SEARCH, false) ?: false
        if (!shouldStartVoiceSearch) return

        intent?.removeExtra(OverlayModeController.EXTRA_START_VOICE_SEARCH)
        val micAction =
            intent
                ?.getStringExtra(OverlayModeController.EXTRA_MIC_ACTION)
                ?.let { actionString ->
                    MicAction.entries.find { it.value == actionString }
                }
                ?: MicAction.DEFAULT_VOICE_SEARCH
        intent?.removeExtra(OverlayModeController.EXTRA_MIC_ACTION)
        voiceSearchHandler.handleMicAction(micAction)
    }
}

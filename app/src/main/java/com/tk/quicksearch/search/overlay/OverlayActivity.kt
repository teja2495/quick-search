package com.tk.quicksearch.search.overlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.ui.theme.QuickSearchTheme

class OverlayActivity : ComponentActivity() {

    private val searchViewModel: SearchViewModel by viewModels()

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

        setContent {
            QuickSearchTheme {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(Color.Transparent)
                ) {
                    OverlayRoot(
                            viewModel = searchViewModel,
                            onCloseRequested = { finish() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(OverlayModeController.EXTRA_CLOSE_OVERLAY, false)) {
            finish()
        }
    }

    override fun onStop() {
        super.onStop()
        searchViewModel.handleOnStop()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}

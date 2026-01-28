package com.tk.quicksearch.search.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.tk.quicksearch.search.core.SearchViewModel

class OverlayWindowController(
        private val context: Context,
        private val lifecycleOwner: LifecycleOwner,
        private val viewModelStoreOwner: ViewModelStoreOwner,
        private val savedStateRegistryOwner: SavedStateRegistryOwner,
        private val onBackPressedDispatcherOwner: OnBackPressedDispatcherOwner,
        private val searchViewModel: SearchViewModel,
        private val onCloseRequested: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null

    fun show() {
        if (overlayView != null) return
        val composeView =
                ComposeView(context).apply {
                    // Set ViewTree owners using resource IDs directly
                    // These IDs are defined by AndroidX libraries and merged into the app
                    setTag(0x7f09024e, lifecycleOwner) // view_tree_lifecycle_owner
                    setTag(0x7f090252, viewModelStoreOwner) // view_tree_view_model_store_owner
                    setTag(
                            0x7f090251,
                            savedStateRegistryOwner
                    ) // view_tree_saved_state_registry_owner
                    setTag(
                            0x7f090250,
                            onBackPressedDispatcherOwner
                    ) // view_tree_on_back_pressed_dispatcher_owner

                    setContent {
                        CompositionLocalProvider(
                                LocalLifecycleOwner provides lifecycleOwner,
                                LocalViewModelStoreOwner provides viewModelStoreOwner,
                                LocalSavedStateRegistryOwner provides savedStateRegistryOwner
                        ) {
                            OverlayRoot(
                                    viewModel = searchViewModel,
                                    onCloseRequested = onCloseRequested
                            )
                        }
                    }
                }

        val params =
                WindowManager.LayoutParams(
                                WindowManager.LayoutParams.MATCH_PARENT,
                                WindowManager.LayoutParams.MATCH_PARENT,
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                                } else {
                                    WindowManager.LayoutParams.TYPE_PHONE
                                },
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                                PixelFormat.TRANSLUCENT
                        )
                        .apply {
                            gravity = Gravity.TOP or Gravity.START
                            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        }

        windowManager.addView(composeView, params)
        overlayView = composeView
    }

    fun dismiss() {
        overlayView?.let { view ->
            // Always remove the overlay view to prevent window leaks.
            windowManager.removeView(view)
            overlayView = null
        }
    }
}

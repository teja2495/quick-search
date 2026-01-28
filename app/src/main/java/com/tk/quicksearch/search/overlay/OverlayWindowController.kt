package com.tk.quicksearch.search.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.annotation.RequiresApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
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
    private var onBackInvokedCallback: OnBackInvokedCallback? = null

    fun show() {
        if (overlayView != null) return
        val composeView =
                ComposeView(context).apply {
                    setViewTreeLifecycleOwner(lifecycleOwner)
                    setViewTreeViewModelStoreOwner(viewModelStoreOwner)
                    setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
                    setViewTreeOnBackPressedDispatcherOwner(onBackPressedDispatcherOwner)
                    
                    isFocusable = true
                    isFocusableInTouchMode = true
                    
                    setOnKeyListener { _, keyCode, event ->
                        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                            onBackPressedDispatcherOwner.onBackPressedDispatcher.onBackPressed()
                            true
                        } else {
                            false
                        }
                    }

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
                    
                    requestFocus()
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
        
        // Register OnBackInvokedCallback for gesture navigation (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerBackCallback(composeView)
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun registerBackCallback(view: ComposeView) {
        view.post {
            try {
                val dispatcher = view.findOnBackInvokedDispatcher()
                if (dispatcher != null) {
                    val callback = OnBackInvokedCallback {
                        onCloseRequested()
                    }
                    dispatcher.registerOnBackInvokedCallback(
                        OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                        callback
                    )
                    onBackInvokedCallback = callback
                }
            } catch (e: Exception) {
                // Fallback to existing behavior
            }
        }
    }

    fun dismiss() {
        overlayView?.let { view ->
            // Unregister back callback (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                unregisterBackCallback(view)
            }
            
            // Always remove the overlay view to prevent window leaks.
            windowManager.removeView(view)
            overlayView = null
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun unregisterBackCallback(view: ComposeView) {
        try {
            val dispatcher = view.findOnBackInvokedDispatcher()
            val callback = onBackInvokedCallback
            if (dispatcher != null && callback != null) {
                dispatcher.unregisterOnBackInvokedCallback(callback)
                onBackInvokedCallback = null
            }
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }
}

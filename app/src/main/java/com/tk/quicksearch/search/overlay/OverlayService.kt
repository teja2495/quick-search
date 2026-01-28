package com.tk.quicksearch.search.overlay

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.utils.PermissionUtils

class OverlayService :
        Service(),
        LifecycleOwner,
        ViewModelStoreOwner,
        SavedStateRegistryOwner,
        OnBackPressedDispatcherOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val backPressedDispatcher = OnBackPressedDispatcher()
    override val onBackPressedDispatcher: OnBackPressedDispatcher
        get() = backPressedDispatcher

    private lateinit var overlayWindowController: OverlayWindowController
    private lateinit var searchViewModel: SearchViewModel

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        searchViewModel =
                ViewModelProvider(
                        this,
                        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                )[SearchViewModel::class.java]
        overlayWindowController =
                OverlayWindowController(
                        context = this,
                        lifecycleOwner = this,
                        viewModelStoreOwner = this,
                        savedStateRegistryOwner = this,
                        onBackPressedDispatcherOwner = this,
                        searchViewModel = searchViewModel,
                        onCloseRequested = {
                            searchViewModel.handleOnStop()
                            stopSelf()
                        }
                )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!PermissionUtils.hasOverlayPermission(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        overlayWindowController.show()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        overlayWindowController.dismiss()
        searchViewModel.handleOnStop()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

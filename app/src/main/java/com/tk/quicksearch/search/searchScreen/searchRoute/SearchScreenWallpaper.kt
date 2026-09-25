package com.tk.quicksearch.search.searchScreen.searchRoute

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.tk.quicksearch.app.HomeActivity
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.shared.util.WallpaperUtils

internal data class SearchScreenWallpaperState(
    val imageBitmap: ImageBitmap?,
    val usesWallpaperBackground: Boolean,
    val usesSystemWallpaperBackdrop: Boolean,
    val usesMonoThemeFallback: Boolean,
)

private data class WallpaperBitmapState(
    val imageBitmap: ImageBitmap?,
    val loadResult: WallpaperUtils.WallpaperLoadResult?,
)

@Composable
internal fun SearchScreenWallpaperLogic(
    state: SearchUiState,
    onWallpaperLoaded: (() -> Unit)? = null,
    onWallpaperUnavailable: (() -> Unit)? = null,
    onSystemWallpaperChanged: (() -> Unit)? = null,
    isOverlayPresentation: Boolean = false,
): SearchScreenWallpaperState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val canShowSystemWallpaperBackdrop =
        !isOverlayPresentation &&
            (context as? HomeActivity)?.canShowSystemWallpaperBackdrop == true
    var wallpaperChangeVersion by remember { mutableIntStateOf(0) }

    DisposableEffect(context, state.backgroundSource) {
        if (state.backgroundSource != BackgroundSource.SYSTEM_WALLPAPER) {
            onDispose { }
        } else {
            val appContext = context.applicationContext
            @Suppress("DEPRECATION")
            val wallpaperChangedAction = Intent.ACTION_WALLPAPER_CHANGED
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context?,
                        intent: Intent?,
                    ) {
                        if (intent?.action != wallpaperChangedAction) return
                        WallpaperUtils.invalidateWallpaperCache()
                        wallpaperChangeVersion++
                        onSystemWallpaperChanged?.invoke()
                    }
                }
            val filter = IntentFilter(wallpaperChangedAction)
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            onDispose {
                appContext.unregisterReceiver(receiver)
            }
        }
    }

    DisposableEffect(lifecycleOwner, state.backgroundSource) {
        if (state.backgroundSource != BackgroundSource.SYSTEM_WALLPAPER) {
            onDispose { }
        } else {
            val observer =
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        wallpaperChangeVersion++
                    }
                }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    val shouldUseStartupPreview = wallpaperChangeVersion == 0
    val sourceWallpaperState =
        produceState<WallpaperBitmapState>(
            initialValue =
                if (state.backgroundSource == BackgroundSource.SYSTEM_WALLPAPER) {
                    if (WallpaperUtils.hasWallpaperAccessPermission(context)) {
                        WallpaperUtils.getCachedWallpaperBitmap()?.let {
                            WallpaperBitmapState(
                                imageBitmap = it.asImageBitmap(),
                                loadResult = WallpaperUtils.WallpaperLoadResult.Success(it),
                            )
                        } ?: WallpaperBitmapState(imageBitmap = null, loadResult = null)
                    } else {
                        WallpaperBitmapState(
                            imageBitmap = null,
                            loadResult = WallpaperUtils.WallpaperLoadResult.PermissionRequired,
                        )
                    }
                } else {
                    WallpaperBitmapState(imageBitmap = null, loadResult = null)
                },
            state.backgroundSource,
            state.hasWallpaperPermission,
            state.wallpaperAvailable,
            state.startupBackgroundPreviewPath,
            wallpaperChangeVersion,
        ) {
            if (state.backgroundSource != BackgroundSource.SYSTEM_WALLPAPER) {
                value = WallpaperBitmapState(imageBitmap = null, loadResult = null)
                return@produceState
            }

            if (WallpaperUtils.hasWallpaperAccessPermission(context)) {
                // Render from memory immediately. File decode remains off the composition thread.
                val cachedWallpaper = WallpaperUtils.getCachedWallpaperBitmap()?.asImageBitmap()
                if (cachedWallpaper != null) {
                    value = WallpaperBitmapState(imageBitmap = cachedWallpaper, loadResult = null)
                } else if (shouldUseStartupPreview) {
                    WallpaperUtils.loadStartupBackgroundPreviewBitmap(
                        previewPath = state.startupBackgroundPreviewPath,
                    )?.asImageBitmap()?.let {
                        value = WallpaperBitmapState(imageBitmap = it, loadResult = null)
                    }
                }
            }

            when (val result = WallpaperUtils.getWallpaperBitmapResult(context)) {
                is WallpaperUtils.WallpaperLoadResult.Success -> {
                    value =
                        WallpaperBitmapState(
                            imageBitmap = result.bitmap.asImageBitmap(),
                            loadResult = result,
                        )
                    if (!isOverlayPresentation) {
                        onWallpaperLoaded?.invoke()
                    }
                }

                else -> {
                    value = WallpaperBitmapState(imageBitmap = null, loadResult = result)
                    if (!isOverlayPresentation) {
                        onWallpaperUnavailable?.invoke()
                    }
                }
            }
        }
    val sourceCustomBitmap =
        produceState<ImageBitmap?>(
            initialValue = null,
            key1 = state.backgroundSource,
            key2 = state.customImageUri,
            key3 = state.startupBackgroundPreviewPath,
        ) {
            if (state.backgroundSource != BackgroundSource.CUSTOM_IMAGE) {
                value = null
                return@produceState
            }

            WallpaperUtils.loadStartupBackgroundPreviewBitmap(
                previewPath = state.startupBackgroundPreviewPath,
            )?.asImageBitmap()?.let { value = it }

            WallpaperUtils.getOverlayCustomImageBitmap(context, state.customImageUri)?.let {
                value = it
                if (!isOverlayPresentation) {
                    onWallpaperLoaded?.invoke()
                }
            }
        }
    val imageBitmap =
        when (state.backgroundSource) {
            BackgroundSource.SYSTEM_WALLPAPER -> sourceWallpaperState.value.imageBitmap
            BackgroundSource.CUSTOM_IMAGE -> sourceCustomBitmap.value
            BackgroundSource.THEME -> null
        }
    val usesSystemWallpaperBackdrop =
        state.backgroundSource == BackgroundSource.SYSTEM_WALLPAPER &&
            canShowSystemWallpaperBackdrop &&
            (sourceWallpaperState.value.loadResult ==
                WallpaperUtils.WallpaperLoadResult.PermissionRequired ||
                sourceWallpaperState.value.loadResult == WallpaperUtils.WallpaperLoadResult.SecurityError)
    val useBitmapBackground =
        WallpaperUtils.shouldUseImageBackground(
            backgroundSource = state.backgroundSource,
            hasImageBitmap = imageBitmap != null,
            wallpaperAvailable = state.wallpaperAvailable,
            requireWallpaperAvailableForSystemSource =
                !(shouldUseStartupPreview && sourceWallpaperState.value.imageBitmap != null),
        )
    val usesWallpaperBackground = usesSystemWallpaperBackdrop || useBitmapBackground
    val useMonoThemeFallback =
        !isOverlayPresentation &&
            state.backgroundSource != BackgroundSource.THEME &&
            !usesWallpaperBackground

    return SearchScreenWallpaperState(
        imageBitmap = imageBitmap,
        usesWallpaperBackground = usesWallpaperBackground,
        usesSystemWallpaperBackdrop = usesSystemWallpaperBackdrop,
        usesMonoThemeFallback = useMonoThemeFallback,
    )
}

package com.tk.quicksearch.search.searchScreen

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
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.shared.util.WallpaperUtils

@Composable
internal fun SearchScreenWallpaperLogic(
    state: SearchUiState,
    onWallpaperLoaded: (() -> Unit)? = null,
    isOverlayPresentation: Boolean = false,
): Triple<ImageBitmap?, Boolean, Boolean> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
    val sourceWallpaperBitmap =
        produceState<ImageBitmap?>(
            initialValue =
                if (state.backgroundSource == BackgroundSource.SYSTEM_WALLPAPER) {
                    WallpaperUtils.getCachedWallpaperBitmap()?.asImageBitmap()
                } else {
                    null
                },
            state.backgroundSource,
            state.wallpaperAvailable,
            state.startupBackgroundPreviewPath,
            wallpaperChangeVersion,
        ) {
            if (state.backgroundSource != BackgroundSource.SYSTEM_WALLPAPER) {
                value = null
                return@produceState
            }

            // Render from memory immediately. File decode remains off the composition thread.
            val cachedWallpaper = WallpaperUtils.getCachedWallpaperBitmap()?.asImageBitmap()
            if (cachedWallpaper != null) {
                value = cachedWallpaper
            } else if (shouldUseStartupPreview) {
                WallpaperUtils.loadStartupBackgroundPreviewBitmap(
                    previewPath = state.startupBackgroundPreviewPath,
                )?.asImageBitmap()?.let { value = it }
            }

            when (val result = WallpaperUtils.getWallpaperBitmapResult(context)) {
                is WallpaperUtils.WallpaperLoadResult.Success -> {
                    value = result.bitmap.asImageBitmap()
                    if (!isOverlayPresentation) {
                        onWallpaperLoaded?.invoke()
                    }
                }

                else -> {
                    // Force mono/theme fallback when system wallpaper cannot be loaded.
                    value = null
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
            BackgroundSource.SYSTEM_WALLPAPER -> sourceWallpaperBitmap
            BackgroundSource.CUSTOM_IMAGE -> sourceCustomBitmap
            BackgroundSource.THEME -> null
        }
    val useImageBackground =
        WallpaperUtils.shouldUseImageBackground(
            backgroundSource = state.backgroundSource,
            hasImageBitmap = imageBitmap?.value != null,
            wallpaperAvailable = state.wallpaperAvailable,
            requireWallpaperAvailableForSystemSource =
                !(shouldUseStartupPreview && sourceWallpaperBitmap.value != null),
        )
    val useMonoThemeFallback =
        !isOverlayPresentation &&
            state.backgroundSource != BackgroundSource.THEME &&
            !useImageBackground

    return Triple(imageBitmap?.value, useImageBackground, useMonoThemeFallback)
}

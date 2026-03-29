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

    val shouldUseStartupPreview = wallpaperChangeVersion == 0

    val sourceWallpaperBitmap =
        produceState<ImageBitmap?>(
            initialValue = null,
            state.backgroundSource,
            state.hasWallpaperPermission,
            state.startupBackgroundPreviewPath,
            wallpaperChangeVersion,
        ) {
            value =
                if (state.backgroundSource == BackgroundSource.SYSTEM_WALLPAPER) {
                    WallpaperUtils.getCachedWallpaperBitmap()?.asImageBitmap()?.also {
                        if (!isOverlayPresentation) {
                            onWallpaperLoaded?.invoke()
                        }
                    }
                        ?: if (shouldUseStartupPreview) {
                            WallpaperUtils.getStartupBackgroundPreviewBitmap(
                                context = context,
                                previewPath = state.startupBackgroundPreviewPath,
                            )?.asImageBitmap()?.also {
                                if (!isOverlayPresentation) {
                                    onWallpaperLoaded?.invoke()
                                }
                            }
                        } else {
                            null
                        }
                        ?: when (val result = WallpaperUtils.getWallpaperBitmapResult(context)) {
                            is WallpaperUtils.WallpaperLoadResult.Success -> {
                                if (!isOverlayPresentation) {
                                    onWallpaperLoaded?.invoke()
                                }
                                result.bitmap.asImageBitmap()
                            }

                            else -> {
                                null
                            }
                        }
                } else {
                    null
                }
        }
    val sourceCustomBitmap =
        produceState<ImageBitmap?>(
            initialValue = null,
            key1 = state.backgroundSource,
            key2 = state.customImageUri,
            key3 = state.startupBackgroundPreviewPath,
        ) {
            value =
                if (state.backgroundSource == BackgroundSource.CUSTOM_IMAGE) {
                    WallpaperUtils.getStartupBackgroundPreviewBitmap(
                        context = context,
                        previewPath = state.startupBackgroundPreviewPath,
                    )?.asImageBitmap()?.also {
                        if (!isOverlayPresentation) {
                            onWallpaperLoaded?.invoke()
                        }
                    }
                        ?: WallpaperUtils.getOverlayCustomImageBitmap(context, state.customImageUri)?.also {
                            if (!isOverlayPresentation) {
                                onWallpaperLoaded?.invoke()
                            }
                        }
                } else {
                    null
                }
        }
    val imageBitmap =
        when (state.backgroundSource) {
            BackgroundSource.SYSTEM_WALLPAPER -> sourceWallpaperBitmap
            BackgroundSource.CUSTOM_IMAGE -> sourceCustomBitmap
            BackgroundSource.THEME -> null
        }
    val useImageBackground =
        state.backgroundSource != BackgroundSource.THEME && imageBitmap != null
    val useMonoThemeFallback =
        !isOverlayPresentation &&
            state.backgroundSource != BackgroundSource.THEME &&
            imageBitmap == null

    return Triple(imageBitmap?.value, useImageBackground, useMonoThemeFallback)
}

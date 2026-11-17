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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.tk.quicksearch.app.HomeActivity
import com.tk.quicksearch.app.MainActivity
import com.tk.quicksearch.search.core.AccentColorMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.shared.util.StartupBackgroundPreview
import com.tk.quicksearch.shared.util.WallpaperUtils

internal data class SearchScreenWallpaperState(
    val imageBitmap: ImageBitmap?,
    val usesWallpaperBackground: Boolean,
    val usesSystemWallpaperBackdrop: Boolean,
    val usesMonoThemeFallback: Boolean,
    /** The window's own system wallpaper shows under the blurred bitmap instead of a solid fallback. */
    val systemWallpaperUnderBitmap: Boolean = false,
)

private data class CustomBitmapState(
    val imageBitmap: ImageBitmap?,
    val isLoadFinished: Boolean,
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
    /** False for surfaces drawn over other content, which need an opaque wallpaper image. */
    allowUnblurredSystemBackdrop: Boolean = true,
    retainBitmap: Boolean = true,
): SearchScreenWallpaperState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val canShowSystemWallpaperBackdrop =
        !isOverlayPresentation &&
            (context as? MainActivity)?.canShowSystemWallpaperBackdrop == true
    // Only Home's opaque window shows nothing but the wallpaper behind it. MainActivity's window is
    // translucent: left transparent, it also shows the app underneath (another launcher), at least
    // during its opening transition. There the wallpaper is drawn from a bitmap when readable, and
    // the system wallpaper behind the window is only the fallback when it isn't.
    val systemWallpaperIsOnlyContentBehind = canShowSystemWallpaperBackdrop && context is HomeActivity
    // Without blur Android composites the same wallpaper behind the window, so the bitmap is only
    // read for its colors and a thumbnail is enough. Blurred wallpapers are drawn from a bitmap
    // scaled to what the blur leaves visible.
    val usesUnblurredSystemBackdrop =
        allowUnblurredSystemBackdrop &&
            systemWallpaperIsOnlyContentBehind &&
            state.backgroundSource == BackgroundSource.SYSTEM_WALLPAPER &&
            state.wallpaperBlurRadius <= 0f
    val wallpaperDivisor =
        if (usesUnblurredSystemBackdrop) {
            WallpaperUtils.WALLPAPER_THUMBNAIL_DIVISOR
        } else {
            WallpaperUtils.wallpaperDivisorForBlur(state.wallpaperBlurRadius)
        }
    var wallpaperChangeVersion by remember { mutableIntStateOf(0) }
    val onWallpaperChanged by rememberUpdatedState(onSystemWallpaperChanged)

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
                        onWallpaperChanged?.invoke()
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
            // The first ON_RESUME is the launch itself (replayed when the observer is added). Counting
            // it as a wallpaper change would drop the startup preview on the first frame and fall back
            // to the theme background until the full wallpaper decodes, so only later resumes count.
            var isInitialResume = true
            val observer =
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        if (isInitialResume) {
                            isInitialResume = false
                        } else {
                            wallpaperChangeVersion++
                        }
                    }
                }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    if (!retainBitmap) return SearchScreenWallpaperState(null, false, false, false)

    val shouldUseStartupPreview = wallpaperChangeVersion == 0
    val sourceWallpaperState =
        produceState<WallpaperBitmapState>(
            initialValue =
                if (state.backgroundSource == BackgroundSource.SYSTEM_WALLPAPER) {
                    if (WallpaperUtils.hasWallpaperAccessPermission(context)) {
                        WallpaperUtils.getCachedWallpaperBitmap(wallpaperDivisor)?.let {
                            WallpaperBitmapState(
                                imageBitmap = it.asImageBitmap(),
                                loadResult = WallpaperUtils.WallpaperLoadResult.Success(it),
                            )
                        } ?: StartupBackgroundPreview.peek(state.startupBackgroundPreviewPath)?.let {
                            // Decoded off the main thread while the activity was being created.
                            WallpaperBitmapState(imageBitmap = it.asImageBitmap(), loadResult = null)
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
            wallpaperDivisor,
        ) {
            if (state.backgroundSource != BackgroundSource.SYSTEM_WALLPAPER) {
                value = WallpaperBitmapState(imageBitmap = null, loadResult = null)
                return@produceState
            }

            if (WallpaperUtils.hasWallpaperAccessPermission(context)) {
                // Render from memory immediately. File decode remains off the composition thread.
                val cachedWallpaper = WallpaperUtils.getCachedWallpaperBitmap(wallpaperDivisor)?.asImageBitmap()
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

            when (val result = WallpaperUtils.getWallpaperBitmapResult(context, wallpaperDivisor)) {
                is WallpaperUtils.WallpaperLoadResult.Success -> {
                    value =
                        WallpaperBitmapState(
                            imageBitmap = result.bitmap.asImageBitmap(),
                            loadResult = result,
                        )
                    // The full wallpaper is in memory now; the startup preview isn't needed.
                    StartupBackgroundPreview.release()
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
    val customImageDivisor = WallpaperUtils.wallpaperDivisorForBlur(state.wallpaperBlurRadius)
    val sourceCustomBitmap =
        produceState(
            CustomBitmapState(
                // A recreated activity reuses the decoded image; a cold start uses the preview
                // decoded off the main thread while the activity was being created.
                imageBitmap =
                    if (state.backgroundSource == BackgroundSource.CUSTOM_IMAGE) {
                        WallpaperUtils.getCachedOverlayCustomImageBitmap(state.customImageUri)
                            ?: StartupBackgroundPreview.peek(state.startupBackgroundPreviewPath)
                                ?.asImageBitmap()
                    } else {
                        null
                    },
                isLoadFinished = false,
            ),
            state.backgroundSource,
            state.customImageUri,
            state.startupBackgroundPreviewPath,
            customImageDivisor,
        ) {
            if (state.backgroundSource != BackgroundSource.CUSTOM_IMAGE) {
                value = CustomBitmapState(imageBitmap = null, isLoadFinished = true)
                return@produceState
            }

            // A blur change only reloads the image at a new size; keep showing the current one.
            if (value.imageBitmap == null) {
                WallpaperUtils.loadStartupBackgroundPreviewBitmap(
                    previewPath = state.startupBackgroundPreviewPath,
                )?.asImageBitmap()?.let { value = value.copy(imageBitmap = it) }
            }

            val customBitmap =
                WallpaperUtils.getOverlayCustomImageBitmap(context, state.customImageUri, customImageDivisor)
            value =
                CustomBitmapState(
                    imageBitmap = customBitmap ?: value.imageBitmap,
                    isLoadFinished = true,
                )
            if (customBitmap != null) StartupBackgroundPreview.release()
            if (customBitmap != null && !isOverlayPresentation) {
                onWallpaperLoaded?.invoke()
            }
        }
    val imageBitmap =
        when (state.backgroundSource) {
            BackgroundSource.SYSTEM_WALLPAPER -> sourceWallpaperState.value.imageBitmap
            BackgroundSource.CUSTOM_IMAGE -> sourceCustomBitmap.value.imageBitmap
            BackgroundSource.THEME -> null
        }
    // The cached startup preview decodes off the main thread and lands a few hundred ms after the
    // first frame. Styling the screen for the theme fallback until then and switching when it
    // arrives rebuilds the keyed result content (restarting the app grid's fade), so while the
    // image background is still loading the screen is styled for it from the first frame.
    val isAwaitingStartupImage =
        shouldUseStartupPreview &&
            imageBitmap == null &&
            !state.startupBackgroundPreviewPath.isNullOrBlank() &&
            when (state.backgroundSource) {
                BackgroundSource.SYSTEM_WALLPAPER -> sourceWallpaperState.value.loadResult == null
                BackgroundSource.CUSTOM_IMAGE -> !sourceCustomBitmap.value.isLoadFinished
                BackgroundSource.THEME -> false
            }
    val usesSystemWallpaperBackdrop =
        state.backgroundSource == BackgroundSource.SYSTEM_WALLPAPER &&
            canShowSystemWallpaperBackdrop &&
            (sourceWallpaperState.value.loadResult ==
                WallpaperUtils.WallpaperLoadResult.PermissionRequired ||
                sourceWallpaperState.value.loadResult == WallpaperUtils.WallpaperLoadResult.SecurityError ||
                (usesUnblurredSystemBackdrop &&
                    sourceWallpaperState.value.loadResult != WallpaperUtils.WallpaperLoadResult.Unavailable))
    // While the blurred bitmap loads, Android already draws the system wallpaper behind this window,
    // so show it (sharp) instead of a solid fallback; the blurred image then fades in over it.
    val isLoadingBlurredSystemWallpaper =
        allowUnblurredSystemBackdrop &&
            systemWallpaperIsOnlyContentBehind &&
            state.backgroundSource == BackgroundSource.SYSTEM_WALLPAPER &&
            !usesSystemWallpaperBackdrop &&
            imageBitmap == null &&
            sourceWallpaperState.value.loadResult == null
    val useBitmapBackground =
        WallpaperUtils.shouldUseImageBackground(
            backgroundSource = state.backgroundSource,
            hasImageBitmap = imageBitmap != null,
            wallpaperAvailable = state.wallpaperAvailable,
            requireWallpaperAvailableForSystemSource =
                !(shouldUseStartupPreview && sourceWallpaperState.value.imageBitmap != null),
        )
    val usesWallpaperBackground =
        usesSystemWallpaperBackdrop ||
            useBitmapBackground ||
            isAwaitingStartupImage ||
            isLoadingBlurredSystemWallpaper
    val useMonoThemeFallback =
        !isOverlayPresentation &&
            state.backgroundSource != BackgroundSource.THEME &&
            !usesWallpaperBackground

    return SearchScreenWallpaperState(
        imageBitmap = imageBitmap,
        usesWallpaperBackground = usesWallpaperBackground,
        usesSystemWallpaperBackdrop = usesSystemWallpaperBackdrop,
        usesMonoThemeFallback = useMonoThemeFallback,
        // The window's own system wallpaper is the fallback under the blurred bitmap, so the screen
        // never paints a solid color over it while that bitmap loads or fades in.
        systemWallpaperUnderBitmap =
            allowUnblurredSystemBackdrop &&
                systemWallpaperIsOnlyContentBehind &&
                state.backgroundSource == BackgroundSource.SYSTEM_WALLPAPER &&
                !usesSystemWallpaperBackdrop &&
                !useMonoThemeFallback,
    )
}

internal fun SearchViewModel.onWallpaperUnavailable() {
    setWallpaperAvailable(false)
    if (
        uiState.value.backgroundSource == BackgroundSource.SYSTEM_WALLPAPER &&
        uiState.value.accentColorMode == AccentColorMode.FROM_WALLPAPER
    ) {
        setAccentColorMode(AccentColorMode.NONE)
    }
}

package com.tk.quicksearch.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * In-memory cache for app icons to avoid repeated loading.
 */
private object AppIconCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap?>()

    fun get(packageName: String): ImageBitmap? = cache[packageName]

    fun put(packageName: String, bitmap: ImageBitmap?) {
        if (bitmap != null) {
            cache[packageName] = bitmap
        }
    }
}

/**
 * Loads an app icon from cache or package manager.
 * Returns a State that holds the ImageBitmap, or null if the icon cannot be loaded.
 */
@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    
    val iconState = produceState<ImageBitmap?>(initialValue = AppIconCache.get(packageName), key1 = packageName) {
        // Check cache first
        val cached = AppIconCache.get(packageName)
        if (cached != null) {
            value = cached
            return@produceState
        }

        // Load from package manager on IO thread
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap()
                    .asImageBitmap()
            }.getOrNull()
        }

        // Cache and return the result
        if (bitmap != null) {
            AppIconCache.put(packageName, bitmap)
        }
        value = bitmap
    }
    
    return iconState.value
}

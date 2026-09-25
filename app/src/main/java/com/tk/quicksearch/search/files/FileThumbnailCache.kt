package com.tk.quicksearch.search.files

import android.os.Build
import android.os.SystemClock
import android.util.Size
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.FileIntents
import com.tk.quicksearch.search.core.LocalItemCustomizationRemover
import com.tk.quicksearch.search.contacts.components.ContactUiConstants
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.models.FileTypeUtils
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppTheme
import com.tk.quicksearch.shared.util.hapticConfirm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

// ============================================================================
// Constants
// ============================================================================

internal object FileThumbnailCache {
    private const val MAX_CACHE_SIZE_BYTES = 6 * 1024 * 1024
    private const val BYTES_PER_PIXEL = 4L
    private val loadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache =
            object : LinkedHashMap<String, ImageBitmap>(THUMBNAIL_CACHE_MAX_SIZE, 0.75f, true) {
                private var sizeBytes = 0L

                override fun put(key: String, value: ImageBitmap): ImageBitmap? {
                    val previous = super.put(key, value)
                    sizeBytes += value.byteCount() - (previous?.byteCount() ?: 0L)
                    trimToSize()
                    return previous
                }

                override fun remove(key: String): ImageBitmap? =
                    super.remove(key)?.also { sizeBytes -= it.byteCount() }

                override fun clear() {
                    super.clear()
                    sizeBytes = 0L
                }

                override fun removeEldestEntry(
                        eldest: MutableMap.MutableEntry<String, ImageBitmap>
                ) = false

                private fun trimToSize() {
                    val iterator = entries.iterator()
                    while (
                        iterator.hasNext() &&
                            (size > THUMBNAIL_CACHE_MAX_SIZE || sizeBytes > MAX_CACHE_SIZE_BYTES)
                    ) {
                        val entry = iterator.next()
                        sizeBytes -= entry.value.byteCount()
                        iterator.remove()
                    }
                }

                private fun ImageBitmap.byteCount(): Long =
                    width.toLong() * height.toLong() * BYTES_PER_PIXEL
            }
    private val inFlightLoads = mutableMapOf<String, Deferred<ImageBitmap?>>()
    private val failureTimestamps = mutableMapOf<String, Long>()

    @Synchronized fun get(uri: String): ImageBitmap? = cache[uri]

    @Synchronized
    fun put(uri: String, bitmap: ImageBitmap) {
        cache[uri] = bitmap
        failureTimestamps.remove(uri)
    }

    @Synchronized
    fun clear() {
        cache.clear()
        failureTimestamps.clear()
    }

    suspend fun getOrLoad(uri: String, loader: suspend () -> ImageBitmap?): ImageBitmap? {
        get(uri)?.let {
            return it
        }

        val deferred =
                synchronized(this) {
                    cache[uri]?.let {
                        return it
                    }

                    inFlightLoads[uri]?.let {
                        return@synchronized it
                    }

                    val now = SystemClock.elapsedRealtime()
                    val lastFailure = failureTimestamps[uri]
                    if (lastFailure != null && now - lastFailure < THUMBNAIL_FAILURE_RETRY_DELAY_MS
                    ) {
                        return@synchronized null
                    }

                    loadScope
                            .async(start = CoroutineStart.LAZY) {
                                var loadedBitmap: ImageBitmap? = null
                                try {
                                    loadedBitmap = loader()
                                } catch (_: Exception) {
                                    loadedBitmap = null
                                } finally {
                                    synchronized(this@FileThumbnailCache) {
                                        inFlightLoads.remove(uri)
                                        if (loadedBitmap != null) {
                                            cache[uri] = loadedBitmap!!
                                            failureTimestamps.remove(uri)
                                        } else {
                                            failureTimestamps[uri] = SystemClock.elapsedRealtime()
                                        }
                                    }
                                }
                                loadedBitmap
                            }
                            .also { inFlightLoads[uri] = it }
                }
                        ?: return null

        if (!deferred.isActive && !deferred.isCompleted) deferred.start()
        return try {
            deferred.await()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
    }
}

fun clearFileThumbnailMemoryCache() {
    FileThumbnailCache.clear()
}

// ============================================================================
// Public API
// ============================================================================

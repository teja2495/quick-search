package com.tk.quicksearch.widgetsPanel

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import android.widget.Toast
import androidx.core.os.bundleOf
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.preferences.NotesPreferences
import com.tk.quicksearch.search.searchScreen.searchRoute.SearchScreenWallpaperLogic
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalHomeTextColorOverride
import com.tk.quicksearch.shared.ui.theme.LocalImageBackgroundIsDark
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.shared.util.ImageAppearanceUtils
import android.util.SizeF
import kotlin.math.roundToInt

internal fun initialSpanFor(provider: AppWidgetProviderInfo): Pair<Int, Int> {
    val targetW =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) provider.targetCellWidth else 0
    val targetH =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) provider.targetCellHeight else 0
    val columnSpan =
        if (targetW > 0) targetW.coerceIn(1, WIDGET_PANEL_GRID_COLUMNS)
        else WIDGET_PANEL_DEFAULT_COLUMN_SPAN
    val rowSpan =
        if (targetH > 0) targetH.coerceIn(1, WIDGET_PANEL_MAX_ROW_SPAN)
        else WIDGET_PANEL_DEFAULT_ROW_SPAN
    return columnSpan to rowSpan
}

internal class WidgetOptionsFactory(
    private val screenWidthDp: Int,
    private val density: Float,
    private val orientation: Int,
) {
    fun create(
        columnSpan: Int,
        rowSpan: Int,
    ): Bundle {
        val cellWidthDp = estimateGridCellWidthDp()
        val minWidthDp = spanToSizeDp(columnSpan, cellWidthDp, WidgetPanelGridGap.value)
        val minHeightDp =
            spanToSizeDp(rowSpan, WidgetPanelGridRowHeight.value, WidgetPanelGridGap.value)
        val maxWidthDp = minWidthDp
        val maxHeightDp = minHeightDp

        return bundleOf(
            AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH to minWidthDp.roundToInt(),
            AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT to minHeightDp.roundToInt(),
            AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH to maxWidthDp.roundToInt(),
            AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT to maxHeightDp.roundToInt(),
            AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY to
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        ).applySamsungHostCompatExtras(
            columnSpan = columnSpan,
            rowSpan = rowSpan,
            density = density,
            orientation = orientation,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                putParcelableArrayList(
                    AppWidgetManager.OPTION_APPWIDGET_SIZES,
                    arrayListOf(
                        SizeF(minWidthDp, minHeightDp),
                        SizeF(maxWidthDp, maxHeightDp),
                    ),
                )
            }
        }
    }

    private fun estimateGridCellWidthDp(): Float {
        val horizontalPaddingDp = DesignTokens.ContentHorizontalPadding.value
        val gapTotalDp = WidgetPanelGridGap.value * (WIDGET_PANEL_GRID_COLUMNS - 1)
        return (
            screenWidthDp - (horizontalPaddingDp * 2) - gapTotalDp
        ) / WIDGET_PANEL_GRID_COLUMNS.toFloat()
    }
}

private fun spanToSizeDp(
    span: Int,
    cellSizeDp: Float,
    gapDp: Float,
): Float = (cellSizeDp * span) + (gapDp * (span - 1).coerceAtLeast(0))

internal fun createDisplayedWidgetOptions(
    widthDp: Int,
    heightDp: Int,
    columnSpan: Int,
    rowSpan: Int,
    density: Float,
    orientation: Int,
): Bundle =
    bundleOf(
        AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH to widthDp,
        AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT to heightDp,
        AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH to widthDp,
        AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT to heightDp,
        AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY to
            AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
    ).applySamsungHostCompatExtras(
        columnSpan = columnSpan,
        rowSpan = rowSpan,
        density = density,
        orientation = orientation,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            putParcelableArrayList(
                AppWidgetManager.OPTION_APPWIDGET_SIZES,
                arrayListOf(SizeF(widthDp.toFloat(), heightDp.toFloat())),
            )
        }
    }

private fun Bundle.applySamsungHostCompatExtras(
    columnSpan: Int,
    rowSpan: Int,
    density: Float,
    orientation: Int,
): Bundle {
    if (!Build.MANUFACTURER.equals("samsung", ignoreCase = true)) return this

    putInt("semAppWidgetColumnSpan", columnSpan)
    putInt("semAppWidgetRowSpan", rowSpan)
    putInt("semHostType", 1)
    putString("hsMode", "OneUI")
    putInt("hsWidgetDisplayId", 0)
    putFloat("hsResizeRatio", 1f)
    putFloat("semDisplayDensity", density)
    putInt("semWidgetStyle", 1)
    putInt("semWidgetSize", columnSpan * rowSpan)
    putInt("hsCurrentOrientation", if (orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1)
    putInt("hsForcedOrientation", 0)
    return this
}

internal fun isWidgetConfigurationOptional(provider: AppWidgetProviderInfo): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    return provider.widgetFeatures and
        AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL != 0
}

internal fun isWidgetConfigureActivityAccessible(
    packageManager: PackageManager,
    componentName: android.content.ComponentName,
): Boolean {
    val activityInfo =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getActivityInfo(
                    componentName,
                    PackageManager.ComponentInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getActivityInfo(componentName, 0)
            }
        }.getOrNull() ?: return false

    return activityInfo.enabled && activityInfo.exported
}

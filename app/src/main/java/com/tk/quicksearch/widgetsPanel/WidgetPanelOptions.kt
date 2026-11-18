package com.tk.quicksearch.widgetsPanel

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.homeTextColor
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

        return Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidthDp.roundToInt())
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeightDp.roundToInt())
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidthDp.roundToInt())
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeightDp.roundToInt())
            putInt(
                AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            )
        }.applySamsungHostCompatExtras(
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
    Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
        putInt(
            AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
            AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        )
    }.applySamsungHostCompatExtras(
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

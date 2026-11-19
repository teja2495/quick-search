package com.tk.quicksearch.search.apps.speedBump

import android.os.SystemClock
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.components.AppButtonSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.apps.AppIconShape
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.TodayAppUsage
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin
import com.tk.quicksearch.shared.ui.components.PreserveHostKeyboardStateEffect
import com.tk.quicksearch.shared.ui.components.AppButton
import com.tk.quicksearch.shared.ui.components.AppButtonTone
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuDivider
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuItem
import com.tk.quicksearch.shared.ui.components.AppTextButton

private const val BreathCycleMillis = 3_400
private val IconSize = 96.dp
private val RingSize = 156.dp
private val PauseDurationsMinutes = listOf(5, 15, 30)

/**
 * Opacity of the words around the usage numbers. Held well below the emphasised values so the
 * "9 min / 9 opens" pair carries the line and the connective text stays in the background.
 */

/**
 * Corner radius of the usage pill. `RoundedCornerShape` clamps the radius to half the height, so
 * a single line still renders as an exact stadium; long locales that wrap to two lines settle
 * into a rounded card instead of a stadium with oversized semicircular ends.
 */
private val UsagePillShape = AppShapes.largeIncreased

/** How far the gradient edge is pulled toward the theme's foreground colour. */
private const val VignetteStrength = 0.10f

/**
 * Calming interstitial shown before a bumped app opens.
 *
 * The ring fills over [SpeedBump.DELAY_MILLIS] while the icon breathes; when it completes,
 * [onOpen] launches the app. "Don't Open", system back, and leaving the surface abandon the
 * launch via [onCancel].
 */
@Composable
fun SpeedBumpOverlay(
    appInfo: AppInfo,
    iconPackPackage: String?,
    appIconShape: AppIconShape,
    onOpen: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnOpen by rememberUpdatedState(onOpen)
    val currentOnCancel by rememberUpdatedState(onCancel)
    val lifecycleOwner = LocalLifecycleOwner.current
    // The host drops the pending launch on configuration change, so this only needs to
    // survive recomposition.
    var deadline by remember(appInfo.packageName) {
        mutableStateOf<Long?>(SystemClock.elapsedRealtime() + SpeedBump.DELAY_MILLIS)
    }
    var remainingAtMenuOpen by remember(appInfo.packageName) { mutableLongStateOf(0L) }
    val progress = remember(appInfo.packageName) { mutableFloatStateOf(0f) }
    val breathAmount = remember(appInfo.packageName) { mutableFloatStateOf(0.5f) }
    val todayUsage by produceState<TodayAppUsage?>(
        initialValue = null,
        key1 = appInfo.packageName,
    ) {
        value = withContext(Dispatchers.IO) {
            AppsRepository(context.applicationContext).getTodayAppUsage(appInfo.packageName)
        }
    }

    // Leaving the surface (home gesture, recents, screen off) abandons the launch; otherwise the
    // wall-clock timer would finish in the background and pull the app back to the foreground.
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) currentOnCancel()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val runningDeadline = deadline
    LaunchedEffect(runningDeadline) {
        if (runningDeadline == null) return@LaunchedEffect
        coroutineScope {
            val motionScale = coroutineContext[MotionDurationScale]
            val frames = launch {
                while (true) {
                    withFrameNanos { }
                    val remaining = (runningDeadline - SystemClock.elapsedRealtime()).coerceIn(0, SpeedBump.DELAY_MILLIS)
                    val elapsed = SpeedBump.DELAY_MILLIS - remaining
                    progress.floatValue = elapsed.toFloat() / SpeedBump.DELAY_MILLIS
                    val scale = motionScale?.scaleFactor ?: 1f
                    breathAmount.floatValue =
                        if (scale > 0f) {
                            (sin(elapsed / (BreathCycleMillis * scale) * 2f * Math.PI.toFloat()) + 1f) / 2f
                        } else {
                            0.5f
                        }
                }
            }
            // Launch timing stays independent of the frame clock and system animation scale.
            delay((runningDeadline - SystemClock.elapsedRealtime()).coerceAtLeast(0))
            frames.cancel()
        }
        // Opening the menu invalidates the timer immediately, before recomposition cancels it.
        if (deadline == runningDeadline) currentOnOpen()
    }

    val iconResult =
        rememberAppIcon(
            packageName = appInfo.packageName,
            iconPackPackage = iconPackPackage,
            userHandleId = appInfo.userHandleId,
            forceCircularMask = appIconShape == AppIconShape.CIRCLE,
        )

    PreserveHostKeyboardStateEffect()

    Dialog(
        onDismissRequest = onCancel,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    // Opaque and derived from the theme's surface, so the vignette follows the
                    // selected light/dark theme instead of always fading to black.
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    lerp(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.onSurface,
                                        VignetteStrength,
                                    ),
                                ),
                        ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(
                            start = DesignTokens.SpacingHuge,
                            end = DesignTokens.SpacingHuge,
                            top = DesignTokens.Spacing40,
                            bottom = DesignTokens.SpacingXXLarge,
                        ),
            ) {
                // The breathing icon sits a little above the optical centre so the way out can
                // stay anchored to the bottom edge instead of floating in the empty half.
                Spacer(modifier = Modifier.weight(1f))

                Box(contentAlignment = Alignment.Center) {
                    BreathingRing(
                        progress = { progress.floatValue },
                        breathAmount = { breathAmount.floatValue },
                        ringColor = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.TintLightAlpha),
                    )
                    iconResult.bitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = appInfo.appName,
                            modifier =
                                Modifier
                                    .size(IconSize)
                                    .graphicsLayer {
                                        val iconScale = 0.94f + 0.06f * breathAmount.floatValue
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                                    .clip(AppShapes.largeIncreased),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing40))

                // Title and prompt are one thought, so they sit tight together; the usage stat is
                // context, so it is set apart and styled as a quiet chip rather than a headline.
                Text(
                    text = stringResource(R.string.speed_bump_overlay_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSmall))
                Text(
                    text = stringResource(R.string.speed_bump_overlay_prompt, appInfo.appName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                // Today's context is supporting detail, so it reads as a quiet outlined pill
                // with a clock rather than a headline competing with the breathing icon.
                todayUsage
                    ?.takeIf { it.openedCount > 0 }
                    ?.let { usage ->
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingXLarge))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.spacedBy(DesignTokens.SpacingSmall),
                            modifier =
                                Modifier
                                    .clip(UsagePillShape)
                                    .border(
                                        width = 1.dp,
                                        color =
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.TintLightAlpha),
                                        shape = UsagePillShape,
                                    )
                                    .padding(
                                        horizontal = DesignTokens.SpacingMedium,
                                        vertical = DesignTokens.SpacingSmall,
                                    ),
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.Schedule,
                                contentDescription = null,
                                tint =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                        .copy(alpha = AppColors.ContentSecondaryAlpha),
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = emphasizedUsageText(
                                    template = stringResource(R.string.app_menu_usage_today),
                                    duration = formatUsageDuration(usage.foregroundTimeMillis),
                                    opens = pluralStringResource(
                                        R.plurals.app_menu_opened_count,
                                        usage.openedCount,
                                        usage.openedCount,
                                    ),
                                    emphasisColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                Spacer(modifier = Modifier.weight(1f))

                // Small but prominent: a filled pill rather than a bare text button, so the
                // way out reads as a real action without competing with the breathing icon.
                AppButton(
                    onClick = onCancel,
                    size = AppButtonSize.Medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.speed_bump_dont_open),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Pausing bypasses the friction, so it lives in the top-end corner as a deliberate
            // reach instead of in the thumb zone next to "Don't Open".
            SpeedBumpPauseMenu(
                appName = appInfo.appName,
                expanded = runningDeadline == null,
                onExpand = {
                    deadline?.let {
                        remainingAtMenuOpen = (it - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                        deadline = null
                    }
                },
                onDismiss = {
                    deadline = SystemClock.elapsedRealtime() + remainingAtMenuOpen
                },
                onSelectDuration = { minutes ->
                    SpeedBump.pause(context, appInfo.packageName, minutes)
                    onOpen()
                },
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(DesignTokens.SpacingSmall),
            )
        }
    }
}

@Composable
private fun SpeedBumpPauseMenu(
    appName: String,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onSelectDuration: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        AppTextButton(
            onClick = onExpand,
            tone = AppButtonTone.Neutral,
            size = AppButtonSize.Small,
            modifier = Modifier.heightIn(min = DesignTokens.Spacing48),
        ) {
            Text(stringResource(R.string.speed_bump_pause_action))
        }
        AppDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            focusable = true,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = stringResource(R.string.speed_bump_pause_menu_title, appName),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .semantics { heading() }
                        .padding(
                            horizontal = DesignTokens.SpacingXLarge,
                            vertical = DesignTokens.SpacingMedium,
                        ),
            )
            PauseDurationsMinutes.forEachIndexed { index, minutes ->
                if (index > 0) AppDropdownMenuDivider()
                AppDropdownMenuItem(
                    text = pluralStringResource(R.plurals.calendar_relative_minutes, minutes, minutes),
                    onClick = { onSelectDuration(minutes) },
                )
            }
        }
    }
}

/**
 * Formats [template] (`app_menu_usage_today`) with the two values emphasised, so the numbers
 * carry the line and the connecting words recede — the stat has to be readable in a glance
 * before the bump finishes.
 *
 * Substitution is done by hand rather than via `String.format` because the spans have to land
 * on the substituted values, and locales are free to reorder the two placeholders.
 */
private fun emphasizedUsageText(
    template: String,
    duration: String,
    opens: String,
    emphasisColor: Color,
): AnnotatedString {
    val emphasis = SpanStyle(color = emphasisColor, fontWeight = FontWeight.SemiBold)
    return buildAnnotatedString {
        var index = 0
        while (index < template.length) {
            val next =
                listOf("%1\$s" to duration, "%2\$s" to opens)
                    .mapNotNull { (token, value) ->
                        template.indexOf(token, index).takeIf { it >= 0 }?.let {
                            Triple(it, token, value)
                        }
                    }
                    .minByOrNull { it.first }
            if (next == null) {
                append(template.substring(index))
                break
            }
            val (at, token, value) = next
            append(template.substring(index, at))
            withStyle(emphasis) { append(value) }
            index = at + token.length
        }
    }
}

@Composable
private fun formatUsageDuration(durationMillis: Long): String {
    val totalMinutes = (durationMillis / 60_000L).toInt()
    val halfHours = totalMinutes / 30
    val hours = halfHours / 2
    return when {
        totalMinutes >= 60 && halfHours % 2 == 1 ->
            stringResource(R.string.app_menu_usage_hours_decimal, "$hours.5")
        totalMinutes >= 60 -> pluralStringResource(R.plurals.app_menu_usage_hours, hours, hours)
        else -> pluralStringResource(R.plurals.app_menu_usage_minutes, totalMinutes, totalMinutes)
    }
}

@Composable
private fun BreathingRing(
    progress: () -> Float,
    breathAmount: () -> Float,
    ringColor: Color,
    trackColor: Color,
) {
    androidx.compose.foundation.Canvas(
        modifier =
            Modifier.size(RingSize).semantics {
                // Expose whole seconds of progress while keeping the drawn ring smooth.
                val totalSeconds = SpeedBump.DELAY_MILLIS / 1_000f
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = kotlin.math.floor(progress() * totalSeconds),
                    range = 0f..totalSeconds,
                )
            }.graphicsLayer {
                val ringScale = 0.9f + 0.1f * breathAmount()
                scaleX = ringScale
                scaleY = ringScale
            },
    ) {
        val stroke = Stroke(width = 4.dp.toPx())
        val inset = stroke.width / 2f
        val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke,
        )
        drawArc(
            color = ringColor.copy(alpha = AppColors.ContentSecondaryAlpha + (1f - AppColors.ContentSecondaryAlpha) * breathAmount()),
            startAngle = -90f,
            sweepAngle = 360f * progress(),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke,
        )
    }
}

package com.tk.quicksearch.search.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import com.tk.quicksearch.ui.theme.AppColors
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.IntentHelpers
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.searchEngines.getDisplayNameResId
import com.tk.quicksearch.search.searchEngines.getDrawableResId
import com.tk.quicksearch.util.hapticStrong

@Composable
internal fun PermissionDisabledCard(
    title: String,
    message: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onActionClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PersistentSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    enabledEngines: List<SearchEngine>,
    onSearchAction: () -> Unit,
    shouldUseNumberKeyboard: Boolean,
    detectedShortcutEngine: SearchEngine? = null,
    showWelcomeAnimation: Boolean = false,
    onClearDetectedShortcut: () -> Unit = {},
    onWelcomeAnimationCompleted: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    // Set search bar background to black with slight transparency
    val searchBarBackground = Color.Black.copy(alpha = 0.5f)
    // Light color for icons and text on dark grey background
    val iconAndTextColor = Color(0xFFE0E0E0)

    // Local text field value maintains cursor position even when state query changes from voice input.
    var textFieldValue by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }

    LaunchedEffect(query) {
        if (query != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = query,
                selection = TextRange(query.length)
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Animation constants
    val animationDuration = 4000 
    
    // Animation state
    // We use a linear progression 0 -> 1 to scan the gradient exactly once
    val animationProgress = remember { Animatable(0f) }
    
    val glowAlpha = remember { Animatable(0f) }
    // If we aren't showing the welcome animation, start with the standard UI (0.3f alpha)
    val borderAlpha = remember { Animatable(if (showWelcomeAnimation) 0f else 0.3f) }

    LaunchedEffect(showWelcomeAnimation) {
        if (showWelcomeAnimation) {
            // Setup Start State
            glowAlpha.snapTo(1f)
            borderAlpha.snapTo(0f)
            animationProgress.snapTo(0f)

            // Phase 1: Animate the gradient flow (0 -> 1)
            // This scans the colors and arrives at the end (White)
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(animationDuration, easing = LinearEasing)
            )

            // Phase 2: Arrived at White. Make it permanent.
            // We DO NOT snap border to 1f. We rely on the White Brush from Phase 1 to hold the white state.
            // This maintains the "Glow" look during the hold.
            
            // Hold for a tiny beat (imperceptible, just ensures scan completion)
            delay(50)

            // Phase 3: Dissipate Heat / Cool Down
            // Quicker fade out (500ms) to prevent lingering
            launch {
                glowAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(500, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                borderAlpha.animateTo(
                    targetValue = 0.3f,
                    animationSpec = tween(500, easing = LinearOutSlowInEasing)
                )
            }

            // Wait for fade out to complete, then reset the animation flag
            delay(500)
            onWelcomeAnimationCompleted?.invoke()
        }
    }

    // --- Color Palettes ---
    
    // 1. Northern Lights (Cool & Mystical) - Best for dark calm themes
    val auroraColors = listOf(
        Color(0xFF00E5FF), // Cyan Accent
        Color(0xFF2979FF), // Royal Blue
        Color(0xFF651FFF), // Deep Purple
        Color(0xFFD500F9), // Neon Violet
        Color(0xFF2979FF), // Back to Blue
        Color(0xFF00E5FF)  // Back to Cyan loop
    )

    // 2. Electric Cyberpunk (Vibrant & High Energy) - Best for "expensive" tech look
    val electricColors = listOf(
        Color(0xFFD500F9), // Neon Purple
        Color(0xFFFF00CC), // Hot Pink
        Color(0xFFFF3D00), // Electric Orange
        Color(0xFFFF00CC), // Hot Pink
        Color(0xFFD500F9), // Neon Purple
        Color(0xFF2979FF), // Electric Blue
        Color(0xFFD500F9)  // Loop
    )

    // 3. Golden Luxury (Warm & Premium) - Best for "Gold" status feel
    val goldenColors = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFFF9100), // Deep Orange
        Color(0xFFFFEA00), // Bright Yellow
        Color(0xFFFFD700), // Gold
        Color(0xFFFFA000), // Amber
        Color(0xFFFFD700)  // Loop
    )

    // 4. Google Brand Colors (Familiar & Playful)
    // "Vibrant Path": High-fidelity spectrum to avoid muddy RGB blends
    val googleColors = listOf(
        Color(0xFF4285F4), // 1. Blue
        Color(0xFF5E35B1), // 1.1 Indigo (Bridge to Purple)
        Color(0xFF9C27B0), // 1.2 Purple (Bridge to Red)
        Color(0xFFE91E63), // 1.3 Pink (Bridge to Red)
        Color(0xFFEA4335), // 2. Red
        Color(0xFFFF5722), // 2.1 Deep Orange
        Color(0xFFFF9800), // 2.2 Orange
        Color(0xFFFFC107), // 2.3 Amber
        Color(0xFFFBBC05), // 3. Yellow
        Color(0xFFD4E157), // 3.1 Lime
        Color(0xFFCDDC39), // 3.2 Light Green
        Color(0xFF34A853), // 4. Green
        Color(0xFF00BFA5), // 4.1 Teal Accent
        Color(0xFF00BCD4), // 4.2 Cyan
        Color(0xFF03A9F4), // 4.3 Light Blue
        Color(0xFF4285F4), // 5. Back to Blue
        
        // End Block: Solid White
        // We need a long tail of white (>25% of total list) to ensure the screen is fully white 
        // when the scan reaches the end.
        Color.White, Color.White, Color.White, Color.White,
        Color.White, Color.White, Color.White, Color.White,
        Color.White, Color.White, Color.White, Color.White
    )

    // Select the active palette here (Change this to try others!)
    val activeColors = googleColors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer() // GPU acceleration
            .drawBehind {
                val alpha = glowAlpha.value
                if (alpha > 0f) {
                    val strokeWidth = 2.dp.toPx()
                    val cornerRadiusVal = 28.dp.toPx()
                    
 
                    
                    // Calculate gradient movement based on animation progress
                    // We want to SCAN the gradient from Start (Colors) to End (White)
                    // At t=0, we want offset=0 (Start of colors aligned with left edge)
                    // At t=1, we want to look at the End (White).
                    // So we slide the brush to the LEFT (negative offset) until the end is visible.
                    
                    val gradientWidth = size.width * 4 // Ultra wide gradient
                    
                    // xOffset moves from 0 down to -3*width.
                    // At -3*width, the brush starts 3 screens to the left.
                    // The visible part [0, width] is at offset +3*width = [3*width, 4*width] of the gradient.
                    // This is the last 25% of the gradient, which is White.
                    val xOffset = -(animationProgress.value * size.width * 3)
                    
                    val brush = Brush.linearGradient(
                        colors = activeColors,
                        start = Offset(xOffset, 0f),
                        end = Offset(xOffset + gradientWidth, 0f),
                        // Tilt slightly for more dynamic look? No, straight looks cleaner for border
                    )

                    // 1. Draw "Outer Glow" (Simulated Blur)
                    // We draw wider, lower alpha strokes behind
                     drawRoundRect(
                        brush = brush,
                        cornerRadius = CornerRadius(cornerRadiusVal),
                        style = Stroke(width = strokeWidth * 4f), // Wide spill
                        alpha = alpha * 0.3f // Low opacity
                    )
                    drawRoundRect(
                        brush = brush,
                        cornerRadius = CornerRadius(cornerRadiusVal),
                        style = Stroke(width = strokeWidth * 2f), // Medium spill
                        alpha = alpha * 0.5f
                    )

                    // 2. Draw "Core" sharp line
                    drawRoundRect(
                        brush = brush,
                        cornerRadius = CornerRadius(cornerRadiusVal),
                        style = Stroke(width = strokeWidth),
                        alpha = alpha
                    )
                }
            }
            .border(
                width = 2.dp,
                color = Color.White.copy(alpha = borderAlpha.value),
                shape = RoundedCornerShape(28.dp)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(searchBarBackground)
    ) {
        TextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onQueryChange(newValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .animateContentSize(),
            shape = RoundedCornerShape(28.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.titleMedium,
                    color = iconAndTextColor.copy(alpha = 0.6f)
                )
            },
            textStyle = MaterialTheme.typography.titleMedium.copy(color = iconAndTextColor),
            singleLine = false,
            maxLines = 3,
            leadingIcon = {
                if (detectedShortcutEngine != null) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = detectedShortcutEngine.getDrawableResId()),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.desc_search_icon),
                        tint = iconAndTextColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    // Show X icon when query is not empty, otherwise show settings icon
                    // (whether shortcut is detected or not when query is empty)
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClearQuery) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.desc_clear_search),
                                tint = iconAndTextColor
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            hapticStrong(view)()
                            onSettingsClick()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.desc_open_settings),
                                tint = iconAndTextColor
                            )
                        }
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                keyboardType = if (shouldUseNumberKeyboard) KeyboardType.Number else KeyboardType.Text
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchAction()
                    if (query.isNotBlank()) {
                        // Only hide keyboard if the first engine is not DIRECT_ANSWER
                        val firstEngine = enabledEngines.firstOrNull()
                        if (firstEngine != SearchEngine.DIRECT_SEARCH) {
                            keyboardController?.hide()
                        }
                    }
                }
            ),
            colors = TextFieldDefaults.colors(
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedTextColor = iconAndTextColor,
                unfocusedTextColor = iconAndTextColor
            )
        )
    }
}

@Composable
internal fun UsagePermissionCard(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.usage_permission_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.desc_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.usage_permission_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = stringResource(R.string.action_open_settings))
            }
        }
    }
}

@Composable
internal fun InfoBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
internal fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.empty_state_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.empty_state_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun KeyboardSwitchPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.4f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Section displaying search engine cards when there are no search results.
 * Shows all enabled search engines as individual cards with icons.
 */
@Composable
internal fun NoResultsSearchEngineCards(
    query: String,
    enabledEngines: List<SearchEngine>,
    onSearchEngineClick: (String, SearchEngine) -> Unit,
    onCustomizeClick: () -> Unit,
    modifier: Modifier = Modifier,
    isReversed: Boolean = false,
    showWallpaperBackground: Boolean = false
) {
    // Reverse the engine list when results are at the bottom
    val orderedEngines = if (isReversed) {
        enabledEngines.reversed()
    } else {
        enabledEngines
    }

    // Don't show customize card when only one engine is shown (shortcut detected)
    val showCustomizeCard = enabledEngines.size > 1

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // When reversed (results at bottom), show customize card at the top
        if (isReversed && showCustomizeCard) {
            CustomizeSearchEnginesCard(
                onClick = onCustomizeClick,
                showWallpaperBackground = showWallpaperBackground
            )
        }

        orderedEngines.forEach { engine ->
            SearchEngineCard(
                engine = engine,
                query = query,
                onClick = { onSearchEngineClick(query, engine) },
                showWallpaperBackground = showWallpaperBackground
            )
        }

        // When not reversed, show customize card at the bottom
        if (!isReversed && showCustomizeCard) {
            CustomizeSearchEnginesCard(
                onClick = onCustomizeClick,
                showWallpaperBackground = showWallpaperBackground
            )
        }
    }
}

/**
 * Individual search engine card with icon and name.
 */
@Composable
internal fun SearchEngineCard(
    engine: SearchEngine,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showWallpaperBackground: Boolean = false,
    onClear: (() -> Unit)? = null
) {
    val view = LocalView.current
    val context = LocalContext.current

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable {
                com.tk.quicksearch.util.hapticConfirm(view)()
                onClick()
            },
        colors = AppColors.getCardColors(showWallpaperBackground),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = AppColors.getCardElevation(showWallpaperBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Search engine icon
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = engine.getDrawableResId()),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))

            // Search engine name
            Text(
                text = stringResource(R.string.search_on_engine, stringResource(engine.getDisplayNameResId())),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            if (onClear != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.desc_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Card for customizing search engines - always available at the bottom.
 */
@Composable
private fun CustomizeSearchEnginesCard(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    showWallpaperBackground: Boolean = false
) {
    val view = LocalView.current

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable {
                com.tk.quicksearch.util.hapticConfirm(view)()
                onClick()
                // Navigation is handled by the onClick callback which should navigate to search engine settings
                // This is passed down from MainActivity -> SearchRoute -> SearchScreenContent -> SearchContentArea -> NoResultsSearchEngineCards -> CustomizeSearchEnginesCard
            },
        colors = AppColors.getCardColors(showWallpaperBackground),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = AppColors.getCardElevation(showWallpaperBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Settings icon
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))

            // Customize text
            Text(
                text = stringResource(R.string.customize_search_engines),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

package com.tk.quicksearch.searchEngines.inline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.compact.SearchEngineCard
import com.tk.quicksearch.searchEngines.extendToScreenEdges
import com.tk.quicksearch.searchEngines.shared.SearchTargetConstants
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.shared.util.isLandscape
import com.tk.quicksearch.shared.util.isTablet
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm

@Composable
internal fun AiFollowUpInputSection(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    showWallpaperBackground: Boolean,
    modifier: Modifier = Modifier,
    useInsetContainer: Boolean = false,
    insetOverlap: Dp = SearchEngineSectionConstants.INSET_CONTAINER_OVERLAP,
    insetFullBleedFraction: Float = 0f,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val submit = {
        if (value.isNotBlank()) {
            onSend()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val dividerColor =
        if (showWallpaperBackground) {
            AppColors.WallpaperDivider
        } else {
            AppColors.Accent.copy(alpha = 0.22f)
        }
    Surface(
        modifier =
            modifier.then(
                if (useInsetContainer) {
                    Modifier.attachToBottomSearchBar(
                        overlap = insetOverlap,
                        horizontalExtension =
                            InsetSearchBarGeometry.containerHorizontalExtension(
                                insetFullBleedFraction,
                            ),
                    )
                } else {
                    Modifier.extendToScreenEdges()
                },
            ),
        color =
            if (useInsetContainer) {
                AppColors.getSearchBarBackground(showWallpaperBackground)
            } else {
                AppColors.getSearchEngineSectionBackground(showWallpaperBackground)
            },
        shape =
            if (useInsetContainer) {
                SearchEngineSectionConstants.insetContainerShape(insetFullBleedFraction)
            } else {
                RectangleShape
            },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom =
                            if (useInsetContainer) insetOverlap else 0.dp,
                    ),
        ) {
            if (!useInsetContainer) {
                HorizontalDivider(
                    color = dividerColor,
                    thickness = SearchEngineSectionConstants.COMPACT_TOP_DIVIDER_THICKNESS,
                )
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = SearchEngineSectionConstants.HORIZONTAL_PADDING,
                            vertical = SearchEngineSectionConstants.VERTICAL_PADDING,
                        )
                        .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.direct_search_follow_up_hint)) },
                trailingIcon = {
                    IconButton(
                        onClick = submit,
                        enabled = value.isNotBlank(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = stringResource(R.string.dialog_send),
                        )
                    }
                },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    SearchEngineSectionConstants.TOOL_BUTTON_CORNER_RADIUS,
                ),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
            )
        }
    }
}

/** Overlay expand/collapse chevron shown when overlay controls are enabled. */

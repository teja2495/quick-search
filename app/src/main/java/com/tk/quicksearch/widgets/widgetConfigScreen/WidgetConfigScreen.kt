package com.tk.quicksearch.widgets.WidgetConfigScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonsSection
import com.tk.quicksearch.widgets.utils.WidgetButtonSlotConfig
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.WidgetPreviewCard
import com.tk.quicksearch.widgets.utils.enforceVariantConstraints
import com.tk.quicksearch.widgets.utils.WidgetVariant
import com.tk.quicksearch.widgets.WidgetConfigScreen.components.WidgetLoadingState
import com.tk.quicksearch.widgets.WidgetConfigScreen.components.WidgetMicIconSection
import com.tk.quicksearch.widgets.WidgetConfigScreen.components.WidgetInternalPaddingSection
import com.tk.quicksearch.widgets.WidgetConfigScreen.components.WidgetSearchIconSection
import com.tk.quicksearch.widgets.WidgetConfigScreen.components.WidgetSlidersSection
import com.tk.quicksearch.widgets.WidgetConfigScreen.components.WidgetTextIconColorSection
import com.tk.quicksearch.widgets.WidgetConfigScreen.components.WidgetThemeSection
import com.tk.quicksearch.widgets.WidgetConfigScreen.components.WidgetToggleSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    state: WidgetPreferences,
    isLoaded: Boolean,
    isSaveEnabled: Boolean = isLoaded,
    onStateChange: (WidgetPreferences) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    searchViewModel: SearchViewModel,
    widgetVariant: WidgetVariant = WidgetVariant.STANDARD,
    titleResId: Int = R.string.widget_settings_title,
) {
    val constrainedState = state.enforceVariantConstraints(widgetVariant)
    val onConstrainedStateChange: (WidgetPreferences) -> Unit = { updated ->
        onStateChange(updated.enforceVariantConstraints(widgetVariant))
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(titleResId),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription =
                                stringResource(
                                    R.string
                                        .dialog_cancel,
                                ),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = WidgetConfigConstants.SURFACE_ELEVATION) {
                androidx.compose.foundation.layout.Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start =
                                    WidgetConfigConstants
                                        .BOTTOM_BAR_HORIZONTAL_PADDING,
                                end =
                                    WidgetConfigConstants
                                        .BOTTOM_BAR_HORIZONTAL_PADDING,
                                top =
                                    WidgetConfigConstants
                                        .BOTTOM_BAR_VERTICAL_PADDING,
                                bottom =
                                    WidgetConfigConstants
                                        .BOTTOM_BAR_BOTTOM_PADDING,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = onApply,
                        enabled = isSaveEnabled,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    WidgetConfigConstants
                                        .BOTTOM_BUTTON_HEIGHT,
                                ),
                    ) {
                        Text(
                            text = stringResource(R.string.dialog_save),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        if (!isLoaded) {
            WidgetLoadingState(innerPadding = innerPadding)
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Fixed preview section at the top
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal =
                                WidgetConfigConstants
                                    .HORIZONTAL_PADDING,
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        WidgetConfigConstants.PREVIEW_SECTION_SPACING,
                    ),
            ) {
                WidgetPreviewCard(
                    state = constrainedState,
                    widgetVariant = widgetVariant,
                )
            }

            // Scrollable preferences section
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start =
                                WidgetConfigConstants
                                    .HORIZONTAL_PADDING,
                            end =
                                WidgetConfigConstants
                                    .HORIZONTAL_PADDING,
                            bottom =
                                WidgetConfigConstants
                                    .SCROLLABLE_SECTION_BOTTOM_PADDING,
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(WidgetConfigConstants.SECTION_SPACING),
            ) {
                if (
                    widgetVariant == WidgetVariant.CUSTOM_BUTTONS_ONLY ||
                    widgetVariant == WidgetVariant.STANDARD
                ) {
                    CustomWidgetButtonsSection(
                        state = constrainedState,
                        searchViewModel = searchViewModel,
                        maxButtons =
                            if (widgetVariant == WidgetVariant.CUSTOM_BUTTONS_ONLY) {
                                WidgetButtonSlotConfig.CUSTOM_ONLY_COUNT
                            } else {
                                WidgetButtonSlotConfig.STANDARD_COUNT
                            },
                        onStateChange = onConstrainedStateChange,
                    )
                }

                WidgetThemeSection(state = constrainedState, onStateChange = onConstrainedStateChange)

                WidgetSlidersSection(state = constrainedState, onStateChange = onConstrainedStateChange)
                if (widgetVariant == WidgetVariant.STANDARD) {
                    WidgetSearchIconSection(
                        state = constrainedState,
                        onStateChange = onConstrainedStateChange,
                    )
                    WidgetMicIconSection(state = constrainedState, onStateChange = onConstrainedStateChange)
                    WidgetToggleSection(
                        state = constrainedState,
                        hasCustomButtons = constrainedState.hasCustomButtons,
                        onStateChange = onConstrainedStateChange,
                    )
                    WidgetTextIconColorSection(
                        state = constrainedState,
                        onStateChange = onConstrainedStateChange,
                    )
                }
                WidgetInternalPaddingSection(
                    state = constrainedState,
                    onStateChange = onConstrainedStateChange,
                )
            }
        }
    }
}

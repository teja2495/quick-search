package com.tk.quicksearch.widgets.widgetConfigScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsPermission
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.TipBanner
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetConfigScaffold
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetItemsSection
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonsSection
import com.tk.quicksearch.widgets.utils.WidgetButtonSlotConfig
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetBackgroundSection
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetSelectedNoteSection
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetTextColorSection
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.WidgetPreviewCard
import com.tk.quicksearch.widgets.utils.enforceVariantConstraints
import com.tk.quicksearch.widgets.utils.WidgetVariant
import com.tk.quicksearch.widgets.widgetConfigScreen.components.MediaControlsTextColorSection
import com.tk.quicksearch.widgets.widgetConfigScreen.components.MediaControlsThemeSection
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetLoadingState
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetMicIconSection
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetInternalPaddingSection
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetIconSizeSection
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetSearchIconSection
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetSlidersSection
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetTextSizeSection
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetTextIconColorSection
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetThemeSection
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetToggleSection

@Composable
fun WidgetConfigScreen(
    state: WidgetPreferences,
    showDeviceThemeOption: Boolean = false,
    isLoaded: Boolean,
    isSaveEnabled: Boolean = isLoaded,
    showNotificationAccessBanner: Boolean = false,
    saveLabelResId: Int = R.string.dialog_save,
    onStateChange: (WidgetPreferences) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    searchViewModel: SearchViewModel,
    widgetVariant: WidgetVariant = WidgetVariant.STANDARD,
    titleResId: Int = R.string.widget_settings_title,
    onChangeNote: () -> Unit = {},
    launcherContentScale: Float = 1f,
) {
    val constrainedState = state.enforceVariantConstraints(widgetVariant)
    val onConstrainedStateChange: (WidgetPreferences) -> Unit = { updated ->
        onStateChange(updated.enforceVariantConstraints(widgetVariant))
    }

    WidgetConfigScaffold(
        title = stringResource(titleResId),
        onCancel = onCancel,
        onSave = onApply,
        saveEnabled = isSaveEnabled,
        saveLabel = stringResource(saveLabelResId),
    ) { innerPadding ->
        if (!isLoaded) {
            WidgetLoadingState(innerPadding = innerPadding)
            return@WidgetConfigScaffold
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
                    launcherContentScale = launcherContentScale,
                )
            }

            // The media controls widget only offers a background, corner radius and transparency.
            val isMediaControls = widgetVariant == WidgetVariant.MEDIA_CONTROLS
            // The note widget offers the note, background, text color, corner radius and transparency.
            val isNote = widgetVariant == WidgetVariant.NOTE
            // At a Glance offers the note widget's settings without the note.
            val isAtAGlance = widgetVariant == WidgetVariant.AT_A_GLANCE

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
                            // Its first setting sits right under the preview otherwise.
                            top = WidgetConfigConstants.SECTION_SPACING,
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
                if (showNotificationAccessBanner) {
                    val context = LocalContext.current
                    TipBanner(
                        text = stringResource(R.string.widget_media_controls_permission_hint),
                        icon = MaterialSymbols.Info,
                        onContentClick = { NotificationDotsPermission.openNotificationListenerSettings(context) },
                        showDismissButton = false,
                    )
                }
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

                if (isNote || isAtAGlance) {
                    if (isNote) {
                        NoteWidgetSelectedNoteSection(noteId = constrainedState.noteId, onChangeNote = onChangeNote)
                    } else {
                        AtAGlanceWidgetItemsSection(
                            state = constrainedState,
                            onStateChange = onConstrainedStateChange,
                            searchViewModel = searchViewModel,
                        )
                    }
                    SegmentedGroup {
                        NoteWidgetBackgroundSection(
                            state = constrainedState,
                            index = 0,
                            count = 2,
                            onStateChange = onConstrainedStateChange,
                        )
                        NoteWidgetTextColorSection(
                            state = constrainedState,
                            index = 1,
                            count = 2,
                            onStateChange = onConstrainedStateChange,
                        )
                    }
                } else if (isMediaControls) {
                    SegmentedGroup {
                        MediaControlsThemeSection(
                            state = constrainedState,
                            index = 0,
                            count = 2,
                            onStateChange = onConstrainedStateChange,
                        )
                        MediaControlsTextColorSection(
                            state = constrainedState,
                            index = 1,
                            count = 2,
                            onStateChange = onConstrainedStateChange,
                        )
                    }
                } else {
                    WidgetThemeSection(
                        state = constrainedState,
                        showDeviceThemeOption = showDeviceThemeOption,
                        onStateChange = onConstrainedStateChange,
                    )
                }

                WidgetSlidersSection(
                    state = constrainedState,
                    showBorderControls = !isMediaControls && !isNote && !isAtAGlance,
                    onStateChange = onConstrainedStateChange,
                )
                if (isAtAGlance) {
                    WidgetTextSizeSection(state = constrainedState, onStateChange = onConstrainedStateChange)
                }
                if (widgetVariant == WidgetVariant.STANDARD) {
                    // The search bar's own options as one group of tiles.
                    SegmentedGroup {
                        WidgetSearchIconSection(
                            state = constrainedState,
                            index = 0,
                            count = STANDARD_SEARCH_BAR_TILES,
                            onStateChange = onConstrainedStateChange,
                        )
                        WidgetMicIconSection(
                            state = constrainedState,
                            index = 1,
                            count = STANDARD_SEARCH_BAR_TILES,
                            onStateChange = onConstrainedStateChange,
                        )
                        WidgetToggleSection(
                            state = constrainedState,
                            hasCustomButtons = constrainedState.hasCustomButtons,
                            index = 2,
                            count = STANDARD_SEARCH_BAR_TILES,
                            onStateChange = onConstrainedStateChange,
                        )
                        WidgetTextIconColorSection(
                            state = constrainedState,
                            index = 3,
                            count = STANDARD_SEARCH_BAR_TILES,
                            onStateChange = onConstrainedStateChange,
                        )
                        WidgetIconSizeSection(
                            state = constrainedState,
                            index = 4,
                            count = STANDARD_SEARCH_BAR_TILES,
                            onStateChange = onConstrainedStateChange,
                        )
                    }
                }
                if (!isMediaControls && !isNote && !isAtAGlance) {
                    WidgetInternalPaddingSection(
                        state = constrainedState,
                        onStateChange = onConstrainedStateChange,
                    )
                }
            }
        }
    }
}

private const val STANDARD_SEARCH_BAR_TILES = 5

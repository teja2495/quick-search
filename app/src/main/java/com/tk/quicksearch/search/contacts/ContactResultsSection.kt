package com.tk.quicksearch.search.contacts

import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.search.searchScreen.components.ExpandButtonSegment
import com.tk.quicksearch.search.searchScreen.shared.segmentedRowsActive
import com.tk.quicksearch.search.searchScreen.shared.resultRowsContainerPadding
import com.tk.quicksearch.search.searchScreen.shared.resultRowsContainerVerticalPadding
import com.tk.quicksearch.search.searchScreen.shared.ResultSegment
import com.tk.quicksearch.search.searchScreen.shared.ResultRowDivider
import com.tk.quicksearch.search.searchScreen.shared.compactEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.components.ContactResultRow
import com.tk.quicksearch.search.contacts.components.ContactUiConstants
import com.tk.quicksearch.search.contacts.models.ContactButtonAction
import com.tk.quicksearch.search.contacts.utils.ContactButtonResolver
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.ExpandButton
import com.tk.quicksearch.search.searchScreen.components.ExpandButtonDivider
import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

// ============================================================================
// Public API
// ============================================================================

@Composable
fun ContactResultsSection(
    modifier: Modifier = Modifier,
    hasPermission: Boolean,
    contacts: List<ContactInfo>,
    isExpanded: Boolean,
    primaryContactButton: ContactButtonAction,
    secondaryContactButton: ContactButtonAction,
    onContactClick: (ContactInfo) -> Unit,
    onShowContactMethods: (ContactInfo) -> Unit = {},
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onContactMethodClick: (ContactInfo, ContactMethod) -> Unit = { _, _ -> },
    pinnedContactIds: Set<Long> = emptySet(),
    onTogglePin: (ContactInfo) -> Unit = {},
    onMovePinned: (ContactInfo, Boolean) -> Unit = { _, _ -> },
    onExclude: (ContactInfo) -> Unit = {},
    onNicknameClick: (ContactInfo) -> Unit = {},
    onTriggerClick: (ContactInfo) -> Unit = {},
    getContactNickname: (Long) -> String? = { null },
    getContactTrigger: (Long) -> com.tk.quicksearch.search.data.preferences.ResultTrigger? = { null },
    getPrimaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction? =
        {
            null
        },
    getSecondaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction? =
        {
            null
        },
    onPrimaryActionLongPress: (ContactInfo) -> Unit = {},
    onSecondaryActionLongPress: (ContactInfo) -> Unit = {},
    onCustomAction: (ContactInfo, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit =
        { _, _ ->
        },
    onOpenAppSettings: () -> Unit,
    showAllResults: Boolean = false,
    showExpandControls: Boolean = false,
    onExpandClick: () -> Unit,
    expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    showContactActionHint: Boolean = false,
    onContactActionHintDismissed: () -> Unit = {},
    permissionDisabledCard: @Composable (String, String, String, () -> Unit) -> Unit,
    showWallpaperBackground: Boolean = false,
    predictedTarget: PredictedSubmitTarget? = null,
    fillExpandedHeight: Boolean = false,
    showPinnedItemMenu: Boolean = false,
) {
    val hasVisibleContent = (hasPermission && contacts.isNotEmpty()) || !hasPermission
    if (!hasVisibleContent) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        when {
            hasPermission && contacts.isNotEmpty() -> {
                ContactsResultCard(
                    contacts = contacts,
                    isExpanded = isExpanded,
                    showAllResults = showAllResults,
                    showExpandControls = showExpandControls,
                    primaryContactButton = primaryContactButton,
                    secondaryContactButton = secondaryContactButton,
                    onContactClick = onContactClick,
                    onShowContactMethods = onShowContactMethods,
                    onCallContact = onCallContact,
                    onSmsContact = onSmsContact,
                    onContactMethodClick = onContactMethodClick,
                    pinnedContactIds = pinnedContactIds,
                    onTogglePin = onTogglePin,
                    onMovePinned = onMovePinned,
                    onExclude = onExclude,
                    onNicknameClick = onNicknameClick,
                    onTriggerClick = onTriggerClick,
                    getContactNickname = getContactNickname,
                    getContactTrigger = getContactTrigger,
                    getPrimaryContactCardAction = getPrimaryContactCardAction,
                    getSecondaryContactCardAction =
                    getSecondaryContactCardAction,
                    onPrimaryActionLongPress = onPrimaryActionLongPress,
                    onSecondaryActionLongPress = onSecondaryActionLongPress,
                    onCustomAction = onCustomAction,
                    onExpandClick = onExpandClick,
                    expandedCardMaxHeight = expandedCardMaxHeight,
                    showContactActionHint = showContactActionHint,
                    onContactActionHintDismissed = onContactActionHintDismissed,
                    showWallpaperBackground = showWallpaperBackground,
                    predictedTarget = predictedTarget,
                    fillExpandedHeight = fillExpandedHeight,
                    showPinnedItemMenu = showPinnedItemMenu,
                )
            }

            !hasPermission -> {
                permissionDisabledCard(
                    stringResource(R.string.permission_required_title),
                    stringResource(R.string.contacts_section_permission_subtitle),
                    stringResource(R.string.permission_action_manage_android),
                    onOpenAppSettings,
                )
            }
        }
    }
}

// ============================================================================
// Result Card
// ============================================================================

@Composable
private fun ContactsResultCard(
    contacts: List<ContactInfo>,
    isExpanded: Boolean,
    showAllResults: Boolean,
    showExpandControls: Boolean,
    primaryContactButton: ContactButtonAction,
    secondaryContactButton: ContactButtonAction,
    onContactClick: (ContactInfo) -> Unit,
    onShowContactMethods: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onContactMethodClick: (ContactInfo, ContactMethod) -> Unit,
    pinnedContactIds: Set<Long>,
    onTogglePin: (ContactInfo) -> Unit,
    onMovePinned: (ContactInfo, Boolean) -> Unit,
    onExclude: (ContactInfo) -> Unit,
    onNicknameClick: (ContactInfo) -> Unit,
    onTriggerClick: (ContactInfo) -> Unit,
    getContactNickname: (Long) -> String?,
    getContactTrigger: (Long) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    getPrimaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    getSecondaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    onPrimaryActionLongPress: (ContactInfo) -> Unit,
    onSecondaryActionLongPress: (ContactInfo) -> Unit,
    onCustomAction: (ContactInfo, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit,
    onExpandClick: () -> Unit,
    expandedCardMaxHeight: Dp,
    showContactActionHint: Boolean,
    onContactActionHintDismissed: () -> Unit,
    showWallpaperBackground: Boolean = false,
    predictedTarget: PredictedSubmitTarget?,
    fillExpandedHeight: Boolean,
    showPinnedItemMenu: Boolean,
) {
    val overlayCardColor = LocalOverlayResultCardColor.current
    val overlayDividerColor = LocalOverlayDividerColor.current
    val predictedContactId = (predictedTarget as? PredictedSubmitTarget.Contact)?.contactId
    val hasPredictedContact =
        predictedContactId != null && contacts.any { it.contactId == predictedContactId }
    val displayAsExpanded = isExpanded || showAllResults
    val useCardLevelPrediction =
        hasPredictedContact && (!displayAsExpanded || contacts.size == 1)

    val scrollState = androidx.compose.foundation.rememberScrollState()
    // The expanded card scrolls a plain Column, so alias searches (hundreds of rows) are composed
    // a page at a time as the user nears the end instead of all at once.
    val expandedRowCount =
        rememberProgressiveRowCount(
            contacts = contacts,
            scrollState = scrollState,
            enabled = isExpanded,
        )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        ExpandableResultsCard(
            resultCount = contacts.size,
            isExpanded = isExpanded,
            showAllResults = showAllResults,
            isTopPredicted = useCardLevelPrediction,
            showExpandControls = showExpandControls,
            expandedCardMaxHeight = expandedCardMaxHeight,
            hasScrollableContent = scrollState.maxValue > 0,
            fillExpandedHeight = fillExpandedHeight,
            showWallpaperBackground = showWallpaperBackground,
            overlayCardColor = overlayCardColor,
            segmented = true,
        ) { contentModifier, cardState ->
            val displayContacts =
                if (cardState.displayAsExpanded) {
                    if (isExpanded) contacts.take(expandedRowCount) else contacts
                } else {
                    contacts.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
                }

            Column(
                modifier =
                    contentModifier.then(
                        if (isExpanded) {
                            Modifier.verticalScroll(scrollState)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                ContactList(
                    displayContacts = displayContacts,
                    overlayDividerColor = overlayDividerColor,
                    showWallpaperBackground = showWallpaperBackground,
                    primaryContactButton = primaryContactButton,
                    secondaryContactButton = secondaryContactButton,
                    onContactClick = onContactClick,
                    onShowContactMethods = onShowContactMethods,
                    onCallContact = onCallContact,
                    onSmsContact = onSmsContact,
                    onContactMethodClick = onContactMethodClick,
                    pinnedContactIds = pinnedContactIds,
                    onTogglePin = onTogglePin,
                    onMovePinned = onMovePinned,
                    onExclude = onExclude,
                    onNicknameClick = onNicknameClick,
                    onTriggerClick = onTriggerClick,
                    getContactNickname = getContactNickname,
                    getContactTrigger = getContactTrigger,
                    getPrimaryContactCardAction =
                    getPrimaryContactCardAction,
                    getSecondaryContactCardAction =
                    getSecondaryContactCardAction,
                    onPrimaryActionLongPress = onPrimaryActionLongPress,
                    onSecondaryActionLongPress =
                    onSecondaryActionLongPress,
                    onCustomAction = onCustomAction,
                    shouldShowExpandButton = cardState.shouldShowExpandButton,
                    onExpandClick = onExpandClick,
                    showContactActionHint = showContactActionHint,
                    onContactActionHintDismissed =
                    onContactActionHintDismissed,
                    predictedContactId = predictedContactId,
                    useCardLevelPrediction = useCardLevelPrediction,
                    bottomContentPadding =
                        if (cardState.shouldFillExpandedHeight) {
                            DesignTokens.SpacingSmall
                        } else {
                            0.dp
                        },
                    showPinnedItemMenu = showPinnedItemMenu,
                )
            }
        }
    }
}

// ============================================================================
// Contact List
// ============================================================================

@Composable
private fun ContactList(
    displayContacts: List<ContactInfo>,
    overlayDividerColor: Color?,
    showWallpaperBackground: Boolean = false,
    primaryContactButton: ContactButtonAction,
    secondaryContactButton: ContactButtonAction,
    onContactClick: (ContactInfo) -> Unit,
    onShowContactMethods: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onContactMethodClick: (ContactInfo, ContactMethod) -> Unit,
    pinnedContactIds: Set<Long>,
    onTogglePin: (ContactInfo) -> Unit,
    onMovePinned: (ContactInfo, Boolean) -> Unit,
    onExclude: (ContactInfo) -> Unit,
    onNicknameClick: (ContactInfo) -> Unit,
    onTriggerClick: (ContactInfo) -> Unit,
    getContactNickname: (Long) -> String?,
    getContactTrigger: (Long) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    getPrimaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    getSecondaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    onPrimaryActionLongPress: (ContactInfo) -> Unit,
    onSecondaryActionLongPress: (ContactInfo) -> Unit,
    onCustomAction: (ContactInfo, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit,
    shouldShowExpandButton: Boolean,
    onExpandClick: () -> Unit,
    showContactActionHint: Boolean,
    onContactActionHintDismissed: () -> Unit,
    predictedContactId: Long?,
    useCardLevelPrediction: Boolean,
    bottomContentPadding: Dp,
    showPinnedItemMenu: Boolean,
) {
    val segmentCount = displayContacts.size + if (shouldShowExpandButton) 1 else 0
    Column(
        modifier =
            Modifier.padding(
                horizontal = resultRowsContainerPadding(DesignTokens.SpacingMedium),
                vertical = resultRowsContainerVerticalPadding(DesignTokens.SpacingXSmall),
            ).padding(bottom = bottomContentPadding),
    ) {
        displayContacts.forEachIndexed { index, contactInfo ->
            val isPredictedContact =
                predictedContactId != null &&
                    contactInfo.contactId == predictedContactId
            val showPredictedOnRow = isPredictedContact && !useCardLevelPrediction
            ResultSegment(index = index, count = segmentCount) {
            Column {
            key(contactInfo.contactId) {
                ContactResultRow(
                    contactInfo = contactInfo,
                    primaryContactButton =
                        ContactButtonResolver.resolveForContact(contactInfo, primaryContactButton, isPrimary = true),
                    secondaryContactButton =
                        ContactButtonResolver.resolveForContact(contactInfo, secondaryContactButton, isPrimary = false),
                    onContactClick = onContactClick,
                    onShowContactMethods = onShowContactMethods,
                    onCallContact = onCallContact,
                    onSmsContact = onSmsContact,
                    onContactMethodClick = { method ->
                        onContactMethodClick(contactInfo, method)
                    },
                    isPinned = pinnedContactIds.contains(contactInfo.contactId),
                    onTogglePin = onTogglePin,
                    onMovePinned = onMovePinned,
                    onExclude = onExclude,
                    onNicknameClick = onNicknameClick,
                    onTriggerClick = onTriggerClick,
                    hasNickname =
                        !getContactNickname(contactInfo.contactId)
                            .isNullOrBlank(),
                    hasTrigger = getContactTrigger(contactInfo.contactId)?.word?.isNotBlank() == true,
                    primaryAction =
                        getPrimaryContactCardAction(contactInfo.contactId),
                    secondaryAction =
                        getSecondaryContactCardAction(
                            contactInfo.contactId,
                        ),
                    onPrimaryActionLongPress = onPrimaryActionLongPress,
                    onSecondaryActionLongPress = onSecondaryActionLongPress,
                    onCustomAction = onCustomAction,
                    isPredicted = showPredictedOnRow,
                    showPinnedItemMenu = showPinnedItemMenu,
                )
            }
            if (index == 0 && showContactActionHint) {
                ContactActionHintBubble(
                    onDismiss = onContactActionHintDismissed,
                    modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
                )
            }
            }
            }
            if (index != displayContacts.lastIndex && !showPredictedOnRow) {
                ResultRowDivider(showWallpaperBackground, color = overlayDividerColor)
            }
        }

        if (shouldShowExpandButton && segmentedRowsActive()) {
            ExpandButtonSegment(
                index = segmentCount - 1,
                count = segmentCount,
                onClick = onExpandClick,
                textResId = R.string.action_expand_more_contacts,
            )
        } else if (shouldShowExpandButton) {
            ExpandButtonDivider(showWallpaperBackground)
            // Flat sections (card layout off) keep the standard button size, like other More buttons.
            val cardLayoutEnabled = LocalSearchCardLayoutEnabled.current
            ExpandButton(
                onClick = onExpandClick,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .then(
                            if (cardLayoutEnabled) {
                                Modifier
                                    .height(ContactUiConstants.EXPAND_BUTTON_HEIGHT.dp)
                                    .padding(top = DesignTokens.SpacingXSmall)
                            } else {
                                Modifier
                            },
                        ),
                textResId = R.string.action_expand_more_contacts,
            )
            if (cardLayoutEnabled) Spacer(modifier = Modifier.height(DesignTokens.SpacingSmall))
        }
    }
}

@Composable
private fun ContactActionHintBubble(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val arrowHeight = 10.dp
    val arrowWidth = 28.dp
    val cornerRadius = DesignTokens.CornerLarge
    val arrowInset =
        DesignTokens.MinTouchTarget * 0.5f + DesignTokens.SpacingSmall + 24.dp
    val backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = AppColors.TintSubtleAlpha)
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = AppColors.ContentFaintAlpha)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = DesignTokens.SpacingMedium)
                .drawBehind {
                    val arrowHeightPx = arrowHeight.toPx()
                    val arrowWidthPx = arrowWidth.toPx()
                    val cornerRadiusPx = cornerRadius.toPx()
                    val arrowInsetPx = arrowInset.toPx()

                    val rectTop = arrowHeightPx
                    val rect = Rect(0f, rectTop, size.width, size.height)

                    val minArrowCenter = cornerRadiusPx + (arrowWidthPx / 2f)
                    val maxArrowCenter =
                        size.width - cornerRadiusPx - (arrowWidthPx / 2f)
                    val arrowCenterX =
                        (size.width - arrowInsetPx).coerceIn(
                            minArrowCenter,
                            maxArrowCenter,
                        )

                    val path =
                        Path().apply {
                            moveTo(cornerRadiusPx, rect.top)
                            lineTo(
                                arrowCenterX - (arrowWidthPx / 2f),
                                rect.top,
                            )
                            lineTo(arrowCenterX, 0f)
                            lineTo(
                                arrowCenterX + (arrowWidthPx / 2f),
                                rect.top,
                            )
                            lineTo(
                                rect.width - cornerRadiusPx,
                                rect.top,
                            )
                            arcTo(
                                rect =
                                    Rect(
                                        rect.width -
                                            cornerRadiusPx *
                                            2f,
                                        rect.top,
                                        rect.width,
                                        rect.top +
                                            cornerRadiusPx *
                                            2f,
                                    ),
                                startAngleDegrees = 270f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false,
                            )
                            lineTo(
                                rect.width,
                                rect.bottom - cornerRadiusPx,
                            )
                            arcTo(
                                rect =
                                    Rect(
                                        rect.width -
                                            cornerRadiusPx *
                                            2f,
                                        rect.bottom -
                                            cornerRadiusPx *
                                            2f,
                                        rect.width,
                                        rect.bottom,
                                    ),
                                startAngleDegrees = 0f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false,
                            )
                            lineTo(cornerRadiusPx, rect.bottom)
                            arcTo(
                                rect =
                                    Rect(
                                        0f,
                                        rect.bottom -
                                            cornerRadiusPx *
                                            2f,
                                        cornerRadiusPx * 2f,
                                        rect.bottom,
                                    ),
                                startAngleDegrees = 90f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false,
                            )
                            lineTo(0f, rect.top + cornerRadiusPx)
                            arcTo(
                                rect =
                                    Rect(
                                        0f,
                                        rect.top,
                                        cornerRadiusPx * 2f,
                                        rect.top +
                                            cornerRadiusPx *
                                            2f,
                                    ),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false,
                            )
                            close()
                        }

                    drawPath(path = path, color = backgroundColor)
                    drawPath(
                        path = path,
                        color = borderColor,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = arrowHeight)
                    .padding(
                        horizontal = DesignTokens.SpacingMedium,
                        vertical = DesignTokens.SpacingMedium,
                    ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(R.string.contacts_action_hint_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.align(Alignment.CenterStart).padding(end = DesignTokens.Spacing40),
            )
            AppIconButton(
                onClick = onDismiss,
                tooltip = stringResource(R.string.common_close),
                modifier = Modifier.size(DesignTokens.ButtonHeightSmall),
                anchorModifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(DesignTokens.ButtonIconSizeSmall),
                )
            }
        }
    }
}

private const val EXPANDED_ROWS_PAGE_SIZE = 30
private val EXPANDED_ROWS_PREFETCH_DISTANCE = 600.dp

/** How many expanded rows to compose; grows a page whenever the scroll nears the end. */
@Composable
private fun rememberProgressiveRowCount(
    contacts: List<ContactInfo>,
    scrollState: ScrollState,
    enabled: Boolean,
): Int {
    // A new result set starts from one page again; a re-emission of the same rows does not.
    val rowCountState =
        remember(contacts.size, contacts.firstOrNull()?.contactId) {
            mutableIntStateOf(EXPANDED_ROWS_PAGE_SIZE)
        }
    var rowCount by rowCountState
    val prefetchDistancePx = with(LocalDensity.current) { EXPANDED_ROWS_PREFETCH_DISTANCE.roundToPx() }
    val latestContactCount by rememberUpdatedState(contacts.size)
    LaunchedEffect(rowCountState, scrollState, enabled, prefetchDistancePx) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow {
            // Includes rowCount so a page that still leaves the viewport unfilled loads the next.
            (scrollState.maxValue - scrollState.value <= prefetchDistancePx) to rowCount
        }.collect { (isNearEnd, count) ->
            if (isNearEnd && count < latestContactCount) {
                rowCount = count + EXPANDED_ROWS_PAGE_SIZE
            }
        }
    }
    return if (enabled) rowCount.coerceAtMost(contacts.size) else contacts.size
}

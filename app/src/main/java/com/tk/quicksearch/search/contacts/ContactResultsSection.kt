package com.tk.quicksearch.search.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
import com.tk.quicksearch.search.contacts.utils.ContactCallingAppResolver
import com.tk.quicksearch.search.contacts.utils.ContactMessagingAppResolver
import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.ExpandButton
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
    callingApp: CallingApp,
    messagingApp: MessagingApp,
    onContactClick: (ContactInfo) -> Unit,
    onShowContactMethods: (ContactInfo) -> Unit = {},
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onContactMethodClick: (ContactInfo, ContactMethod) -> Unit = { _, _ -> },
    pinnedContactIds: Set<Long> = emptySet(),
    onTogglePin: (ContactInfo) -> Unit = {},
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
                    callingApp = callingApp,
                    messagingApp = messagingApp,
                    onContactClick = onContactClick,
                    onShowContactMethods = onShowContactMethods,
                    onCallContact = onCallContact,
                    onSmsContact = onSmsContact,
                    onContactMethodClick = onContactMethodClick,
                    pinnedContactIds = pinnedContactIds,
                    onTogglePin = onTogglePin,
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
    callingApp: CallingApp,
    messagingApp: MessagingApp,
    onContactClick: (ContactInfo) -> Unit,
    onShowContactMethods: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onContactMethodClick: (ContactInfo, ContactMethod) -> Unit,
    pinnedContactIds: Set<Long>,
    onTogglePin: (ContactInfo) -> Unit,
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
        ) { contentModifier, cardState ->
            val displayContacts =
                if (cardState.displayAsExpanded) {
                    contacts
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
                    callingApp = callingApp,
                    messagingApp = messagingApp,
                    onContactClick = onContactClick,
                    onShowContactMethods = onShowContactMethods,
                    onCallContact = onCallContact,
                    onSmsContact = onSmsContact,
                    onContactMethodClick = onContactMethodClick,
                    pinnedContactIds = pinnedContactIds,
                    onTogglePin = onTogglePin,
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
    callingApp: CallingApp,
    messagingApp: MessagingApp,
    onContactClick: (ContactInfo) -> Unit,
    onShowContactMethods: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onContactMethodClick: (ContactInfo, ContactMethod) -> Unit,
    pinnedContactIds: Set<Long>,
    onTogglePin: (ContactInfo) -> Unit,
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
) {
    Column(
        modifier =
            Modifier.padding(
                horizontal = DesignTokens.SpacingMedium,
                vertical = DesignTokens.SpacingXSmall,
            ).padding(bottom = bottomContentPadding),
    ) {
        displayContacts.forEachIndexed { index, contactInfo ->
            val isPredictedContact =
                predictedContactId != null &&
                    contactInfo.contactId == predictedContactId
            val showPredictedOnRow = isPredictedContact && !useCardLevelPrediction
            key(contactInfo.contactId) {
                ContactResultRow(
                    contactInfo = contactInfo,
                    callingApp =
                        ContactCallingAppResolver
                            .resolveCallingAppForContact(
                                contactInfo,
                                callingApp,
                            ),
                    messagingApp =
                        ContactMessagingAppResolver
                            .resolveMessagingAppForContact(
                                contactInfo,
                                messagingApp,
                            ),
                    onContactClick = onContactClick,
                    onShowContactMethods = onShowContactMethods,
                    onCallContact = onCallContact,
                    onSmsContact = onSmsContact,
                    onContactMethodClick = { method ->
                        onContactMethodClick(contactInfo, method)
                    },
                    isPinned = pinnedContactIds.contains(contactInfo.contactId),
                    onTogglePin = onTogglePin,
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
                )
            }
            if (index == 0 && showContactActionHint) {
                ContactActionHintBubble(
                    onDismiss = onContactActionHintDismissed,
                    modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
                )
            }
            if (index != displayContacts.lastIndex && !showPredictedOnRow) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = overlayDividerColor ?: if (showWallpaperBackground) AppColors.WallpaperDivider else MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }

        if (shouldShowExpandButton) {
            ExpandButton(
                onClick = onExpandClick,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .height(ContactUiConstants.EXPAND_BUTTON_HEIGHT.dp)
                        .padding(top = DesignTokens.SpacingXXSmall),
                textResId = R.string.action_expand_more_contacts,
            )
            Spacer(modifier = Modifier.height(8.dp))
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
    val cornerRadius = 16.dp
    val arrowInset =
        ContactUiConstants.ACTION_BUTTON_SIZE.dp * 0.5f + DesignTokens.SpacingSmall + 24.dp
    val backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)

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
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2f,
                modifier =
                    Modifier.align(Alignment.CenterStart).padding(end = 28.dp),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterEnd).size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.desc_close),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

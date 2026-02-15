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
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.components.ContactResultRow
import com.tk.quicksearch.search.contacts.components.ContactUiConstants
import com.tk.quicksearch.search.contacts.utils.ContactMessagingAppResolver
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.ui.theme.AppColors
import com.tk.quicksearch.ui.theme.DesignTokens

// ============================================================================
// Public API
// ============================================================================

@Composable
fun ContactResultsSection(
    modifier: Modifier = Modifier,
    hasPermission: Boolean,
    contacts: List<ContactInfo>,
    isExpanded: Boolean,
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
    getContactNickname: (Long) -> String? = { null },
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
    showContactActionHint: Boolean = false,
    onContactActionHintDismissed: () -> Unit = {},
    permissionDisabledCard: @Composable (String, String, String, () -> Unit) -> Unit,
    showWallpaperBackground: Boolean = false,
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
                    getContactNickname = getContactNickname,
                    getPrimaryContactCardAction = getPrimaryContactCardAction,
                    getSecondaryContactCardAction =
                    getSecondaryContactCardAction,
                    onPrimaryActionLongPress = onPrimaryActionLongPress,
                    onSecondaryActionLongPress = onSecondaryActionLongPress,
                    onCustomAction = onCustomAction,
                    onExpandClick = onExpandClick,
                    showContactActionHint = showContactActionHint,
                    onContactActionHintDismissed = onContactActionHintDismissed,
                    showWallpaperBackground = showWallpaperBackground,
                )
            }

            !hasPermission -> {
                permissionDisabledCard(
                    stringResource(R.string.contacts_permission_title),
                    stringResource(R.string.contacts_permission_subtitle),
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
    getContactNickname: (Long) -> String?,
    getPrimaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    getSecondaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    onPrimaryActionLongPress: (ContactInfo) -> Unit,
    onSecondaryActionLongPress: (ContactInfo) -> Unit,
    onCustomAction: (ContactInfo, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit,
    onExpandClick: () -> Unit,
    showContactActionHint: Boolean,
    onContactActionHintDismissed: () -> Unit,
    showWallpaperBackground: Boolean = false,
) {
    val overlayCardColor = LocalOverlayResultCardColor.current
    val overlayDividerColor = LocalOverlayDividerColor.current
    val displayAsExpanded = isExpanded || showAllResults
    val canShowExpand =
        showExpandControls && contacts.size > SearchScreenConstants.INITIAL_RESULT_COUNT
    val shouldShowExpandButton = !displayAsExpanded && canShowExpand
    val shouldShowCollapseButton = isExpanded && showExpandControls

    val displayContacts =
        if (displayAsExpanded) {
            contacts
        } else {
            contacts.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
        }

    val scrollState = androidx.compose.foundation.rememberScrollState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        val cardModifier = Modifier.fillMaxWidth()

        val cardContent =
            @Composable
            {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if (isExpanded) {
                                    Modifier
                                        .heightIn(
                                            max =
                                                SearchScreenConstants
                                                    .EXPANDED_CARD_MAX_HEIGHT,
                                        ).verticalScroll(
                                            scrollState,
                                        )
                                } else {
                                    Modifier
                                },
                            ),
                ) {
                    ContactList(
                        displayContacts = displayContacts,
                        overlayDividerColor = overlayDividerColor,
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
                        getContactNickname = getContactNickname,
                        getPrimaryContactCardAction =
                        getPrimaryContactCardAction,
                        getSecondaryContactCardAction =
                        getSecondaryContactCardAction,
                        onPrimaryActionLongPress = onPrimaryActionLongPress,
                        onSecondaryActionLongPress =
                        onSecondaryActionLongPress,
                        onCustomAction = onCustomAction,
                        shouldShowExpandButton = shouldShowExpandButton,
                        onExpandClick = onExpandClick,
                        showContactActionHint = showContactActionHint,
                        onContactActionHintDismissed =
                        onContactActionHintDismissed,
                    )
                }
            }

        val cardColors =
            if (overlayCardColor != null) {
                CardDefaults.cardColors(containerColor = overlayCardColor)
            } else {
                AppColors.getCardColors(showWallpaperBackground = showWallpaperBackground)
            }

        if (showWallpaperBackground) {
            Card(
                modifier = cardModifier,
                colors = cardColors,
                shape = MaterialTheme.shapes.extraLarge,
                elevation =
                    AppColors.getCardElevation(showWallpaperBackground = true),
            ) { cardContent() }
        } else {
            ElevatedCard(
                modifier = cardModifier,
                colors = cardColors,
                shape = MaterialTheme.shapes.extraLarge,
                elevation =
                    AppColors.getCardElevation(showWallpaperBackground = false),
            ) { cardContent() }
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
    getContactNickname: (Long) -> String?,
    getPrimaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    getSecondaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    onPrimaryActionLongPress: (ContactInfo) -> Unit,
    onSecondaryActionLongPress: (ContactInfo) -> Unit,
    onCustomAction: (ContactInfo, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit,
    shouldShowExpandButton: Boolean,
    onExpandClick: () -> Unit,
    showContactActionHint: Boolean,
    onContactActionHintDismissed: () -> Unit,
) {
    Column(
        modifier =
            Modifier.padding(
                horizontal = DesignTokens.SpacingMedium,
                vertical = DesignTokens.SpacingXSmall,
            ),
    ) {
        displayContacts.forEachIndexed { index, contactInfo ->
            key(contactInfo.contactId) {
                ContactResultRow(
                    contactInfo = contactInfo,
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
                    hasNickname =
                        !getContactNickname(contactInfo.contactId)
                            .isNullOrBlank(),
                    primaryAction =
                        getPrimaryContactCardAction(contactInfo.contactId),
                    secondaryAction =
                        getSecondaryContactCardAction(
                            contactInfo.contactId,
                        ),
                    onPrimaryActionLongPress = onPrimaryActionLongPress,
                    onSecondaryActionLongPress = onSecondaryActionLongPress,
                    onCustomAction = onCustomAction,
                )
            }
            if (index == 0 && showContactActionHint) {
                ContactActionHintBubble(
                    onDismiss = onContactActionHintDismissed,
                    modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
                )
            }
            if (index != displayContacts.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = overlayDividerColor ?: MaterialTheme.colorScheme.outlineVariant,
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
    val backgroundColor = MaterialTheme.colorScheme.secondaryContainer

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
                color = MaterialTheme.colorScheme.onSecondaryContainer,
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

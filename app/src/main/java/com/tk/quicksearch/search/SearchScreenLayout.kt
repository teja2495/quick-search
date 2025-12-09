package com.tk.quicksearch.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.util.PhoneNumberUtils

/**
 * Data class holding all the state needed for section rendering.
 */
data class SectionRenderingState(
    val isSearching: Boolean,
    val expandedSection: ExpandedSection,
    val hasAppResults: Boolean,
    val hasContactResults: Boolean,
    val hasFileResults: Boolean,
    val hasSettingResults: Boolean,
    val hasPinnedContacts: Boolean,
    val hasPinnedFiles: Boolean,
    val hasPinnedSettings: Boolean,
    val shouldShowApps: Boolean,
    val shouldShowContacts: Boolean,
    val shouldShowFiles: Boolean,
    val shouldShowSettings: Boolean,
    val autoExpandFiles: Boolean,
    val autoExpandContacts: Boolean,
    val autoExpandSettings: Boolean,
    val hasMultipleExpandableSections: Boolean,
    val displayApps: List<AppInfo>,
    val contactResults: List<ContactInfo>,
    val fileResults: List<DeviceFile>,
    val settingResults: List<SettingShortcut>,
    val pinnedContacts: List<ContactInfo>,
    val pinnedFiles: List<DeviceFile>,
    val pinnedSettings: List<SettingShortcut>,
    val orderedSections: List<SearchSection>
)

/**
 * Renders the scrollable content area with sections based on layout mode.
 */
@Composable
fun SearchContentArea(
    modifier: Modifier = Modifier,
    state: SearchUiState,
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    settingsParams: SettingsSectionParams,
    appsParams: AppsSectionParams,
    onRequestUsagePermission: () -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
    onRetryDirectAnswer: () -> Unit,
    onPhoneNumberClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {}
) {
    val useKeyboardAlignedLayout = state.keyboardAlignedLayout &&
        renderingState.expandedSection == ExpandedSection.NONE
    val directAnswerState = state.directAnswerState
    val showDirectAnswer = directAnswerState.status != DirectAnswerStatus.Idle
    val hideResultsForDirectAnswer = directAnswerState.status != DirectAnswerStatus.Idle
    val alignResultsToBottom = useKeyboardAlignedLayout && !showDirectAnswer

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        // Ignore bottom alignment when direct answer card is showing
        val verticalArrangement = if (alignResultsToBottom) {
            Arrangement.spacedBy(12.dp, Alignment.Bottom)
        } else {
            Arrangement.spacedBy(12.dp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(
                    scrollState,
                    reverseScrolling = alignResultsToBottom
                )
                .padding(vertical = 12.dp),
            verticalArrangement = verticalArrangement
        ) {
            if (showDirectAnswer) {
                DirectAnswerResult(
                    directAnswerState = directAnswerState,
                    onRetry = onRetryDirectAnswer,
                    showWallpaperBackground = state.showWallpaperBackground,
                    onPhoneNumberClick = onPhoneNumberClick,
                    onEmailClick = onEmailClick
                )
            }
            ContentLayout(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                renderingState = renderingState,
                contactsParams = contactsParams,
                filesParams = filesParams,
                settingsParams = settingsParams,
                appsParams = appsParams,
                onRequestUsagePermission = onRequestUsagePermission,
                // Ignore keyboard-aligned layout when direct answer card is showing
                isReversed = useKeyboardAlignedLayout && !showDirectAnswer,
                hideResults = hideResultsForDirectAnswer
            )
        }
    }
}

/**
 * Unified content layout that handles both keyboard-aligned and top-aligned layouts.
 */
@Composable
private fun ContentLayout(
    modifier: Modifier = Modifier,
    state: SearchUiState,
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    settingsParams: SettingsSectionParams,
    appsParams: AppsSectionParams,
    onRequestUsagePermission: () -> Unit,
    isReversed: Boolean,
    hideResults: Boolean
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Show error banner if there's an error message
        state.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            InfoBanner(message = message)
        }

        if (hideResults) {
            return
        }

        val hasQuery = state.query.isNotBlank()
        val isExpanded = renderingState.expandedSection != ExpandedSection.NONE

        when {
            isReversed -> {
                // Keyboard-aligned: search results first, then pinned items
                if (hasQuery) {
                    SearchResultsSections(
                        renderingState = renderingState,
                        contactsParams = contactsParams,
                        filesParams = filesParams,
                        settingsParams = settingsParams,
                        appsParams = appsParams,
                        isReversed = true,
                        keyboardAlignedLayout = state.keyboardAlignedLayout
                    )
                }
                if (!isExpanded) {
                    PinnedItemsSections(
                        renderingState = renderingState,
                        contactsParams = contactsParams,
                        filesParams = filesParams,
                        settingsParams = settingsParams,
                        appsParams = appsParams,
                        isReversed = true
                    )
                }
            }
            else -> {
                // Top-aligned: pinned items first, then search results
                when {
                    !isExpanded -> {
                        PinnedItemsSections(
                            renderingState = renderingState,
                            contactsParams = contactsParams,
                            filesParams = filesParams,
                        settingsParams = settingsParams,
                            appsParams = appsParams,
                            isReversed = false
                        )
                    }
                    !hasQuery -> {
                        ExpandedPinnedSections(
                            renderingState = renderingState,
                            contactsParams = contactsParams,
                        filesParams = filesParams,
                        settingsParams = settingsParams
                        )
                    }
                }
                if (hasQuery) {
                    SearchResultsSections(
                        renderingState = renderingState,
                        contactsParams = contactsParams,
                        filesParams = filesParams,
                    settingsParams = settingsParams,
                        appsParams = appsParams,
                        isReversed = false,
                        keyboardAlignedLayout = state.keyboardAlignedLayout
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectAnswerResult(
    directAnswerState: DirectAnswerState,
    onRetry: () -> Unit,
    showWallpaperBackground: Boolean = false,
    onPhoneNumberClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {}
) {
    if (directAnswerState.status == DirectAnswerStatus.Idle) return

    val showAttribution = directAnswerState.status == DirectAnswerStatus.Success &&
        !directAnswerState.answer.isNullOrBlank()

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (directAnswerState.status) {
                DirectAnswerStatus.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = stringResource(R.string.direct_answer_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                DirectAnswerStatus.Success -> {
                    directAnswerState.answer?.let { answer ->
                        ClickableDirectAnswerText(
                            text = answer,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            onPhoneNumberClick = onPhoneNumberClick,
                            onEmailClick = onEmailClick
                        )
                    }
                }
                DirectAnswerStatus.Error -> {
                    Text(
                        text = directAnswerState.errorMessage
                            ?: stringResource(R.string.direct_answer_error_generic),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (directAnswerState.activeQuery != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(onClick = onRetry) {
                                Text(text = stringResource(R.string.direct_answer_action_retry))
                            }
                        }
                    }
                }
                DirectAnswerStatus.Idle -> {}
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val minCardHeight = 140.dp

        if (showWallpaperBackground) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minCardHeight),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.4f)
                ),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                content()
            }
        } else {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minCardHeight),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                content()
            }
        }

        if (showAttribution) {
            GeminiAttributionRow(
                modifier = Modifier.fillMaxWidth(),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GeminiAttributionRow(
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.direct_answer_powered_by),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
        Image(
            painter = painterResource(id = R.drawable.gemini_logo),
            contentDescription = stringResource(R.string.direct_answer_powered_by),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(12.dp)
                .aspectRatio(288f / 65f)
        )
    }
}

/**
 * Renders search result sections.
 */
@Composable
private fun SearchResultsSections(
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    appsParams: AppsSectionParams,
    settingsParams: SettingsSectionParams,
    isReversed: Boolean,
    keyboardAlignedLayout: Boolean
) {
    val isContactsExpanded = renderingState.expandedSection == ExpandedSection.CONTACTS
    val isFilesExpanded = renderingState.expandedSection == ExpandedSection.FILES
    val isSettingsExpanded = renderingState.expandedSection == ExpandedSection.SETTINGS

    val context = SectionRenderContext(
        shouldRenderFiles = shouldShowFilesSection(renderingState, filesParams) && !isContactsExpanded,
        shouldRenderContacts = shouldShowContactsSection(renderingState, contactsParams) && !isFilesExpanded,
        shouldRenderApps = shouldShowAppsSection(renderingState),
        shouldRenderSettings = shouldShowSettingsSection(renderingState) && !isFilesExpanded && !isContactsExpanded,
        isFilesExpanded = isFilesExpanded,
        isContactsExpanded = isContactsExpanded,
        isSettingsExpanded = isSettingsExpanded,
        filesList = getFileListForRendering(renderingState, isFilesExpanded, keyboardAlignedLayout),
        contactsList = getContactListForRendering(renderingState, isContactsExpanded, keyboardAlignedLayout),
        settingsList = getSettingsListForRendering(renderingState, isSettingsExpanded, keyboardAlignedLayout),
        showAllFilesResults = renderingState.autoExpandFiles,
        showAllContactsResults = renderingState.autoExpandContacts,
        showAllSettingsResults = renderingState.autoExpandSettings,
        showFilesExpandControls = renderingState.hasMultipleExpandableSections,
        showContactsExpandControls = renderingState.hasMultipleExpandableSections,
        showSettingsExpandControls = renderingState.hasMultipleExpandableSections || renderingState.hasSettingResults,
        filesExpandClick = filesParams.onExpandClick,
        contactsExpandClick = contactsParams.onExpandClick,
        settingsExpandClick = settingsParams.onExpandClick
    )

    val params = SectionRenderParams(
        renderingState = renderingState,
        contactsParams = contactsParams,
        filesParams = filesParams,
        settingsParams = settingsParams,
        appsParams = appsParams,
        isReversed = isReversed
    )

    getOrderedSections(renderingState, isReversed).forEach { section ->
        renderSection(section, params, context)
    }
}

/**
 * Renders pinned items sections.
 */
@Composable
private fun PinnedItemsSections(
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    appsParams: AppsSectionParams,
    settingsParams: SettingsSectionParams,
    isReversed: Boolean
) {
    val shouldShowPinned = !renderingState.isSearching

    val context = SectionRenderContext(
        shouldRenderFiles = shouldShowPinned && renderingState.hasPinnedFiles && renderingState.shouldShowFiles,
        shouldRenderContacts = shouldShowPinned && renderingState.hasPinnedContacts && renderingState.shouldShowContacts,
        shouldRenderApps = shouldShowPinned && shouldShowAppsSection(renderingState),
        shouldRenderSettings = shouldShowPinned && renderingState.hasPinnedSettings && renderingState.shouldShowSettings,
        isFilesExpanded = true,
        isContactsExpanded = true,
        isSettingsExpanded = true,
        filesList = renderingState.pinnedFiles,
        contactsList = renderingState.pinnedContacts,
        settingsList = renderingState.pinnedSettings,
        showAllFilesResults = true,
        showAllContactsResults = true,
        showAllSettingsResults = true,
        showFilesExpandControls = false,
        showContactsExpandControls = false,
        showSettingsExpandControls = false,
        filesExpandClick = {},
        contactsExpandClick = {},
        settingsExpandClick = {}
    )

    val params = SectionRenderParams(
        renderingState = renderingState,
        contactsParams = contactsParams,
        filesParams = filesParams,
        settingsParams = settingsParams,
        appsParams = appsParams,
        isReversed = isReversed
    )

    getOrderedSections(renderingState, isReversed).forEach { section ->
        renderSection(section, params, context)
    }
}

/**
 * Renders expanded pinned sections when query is blank.
 */
@Composable
private fun ExpandedPinnedSections(
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    settingsParams: SettingsSectionParams
) {
    val isContactsExpanded = renderingState.expandedSection == ExpandedSection.CONTACTS
    val isFilesExpanded = renderingState.expandedSection == ExpandedSection.FILES
    val isSettingsExpanded = renderingState.expandedSection == ExpandedSection.SETTINGS

    val context = SectionRenderContext(
        shouldRenderFiles = isFilesExpanded && renderingState.hasPinnedFiles && renderingState.shouldShowFiles,
        shouldRenderContacts = isContactsExpanded && renderingState.hasPinnedContacts && renderingState.shouldShowContacts,
        shouldRenderSettings = isSettingsExpanded && renderingState.hasPinnedSettings && renderingState.shouldShowSettings,
        shouldRenderApps = false, // Apps section doesn't need expansion handling
        isFilesExpanded = true,
        isContactsExpanded = true,
        isSettingsExpanded = true,
        filesList = renderingState.pinnedFiles,
        contactsList = renderingState.pinnedContacts,
        settingsList = renderingState.pinnedSettings,
        showAllFilesResults = true,
        showAllContactsResults = true,
        showAllSettingsResults = true,
        showFilesExpandControls = false,
        showContactsExpandControls = false,
        showSettingsExpandControls = false
    )

    val params = SectionRenderParams(
        renderingState = renderingState,
        contactsParams = contactsParams,
        filesParams = filesParams,
        settingsParams = settingsParams,
        appsParams = null, // Apps section doesn't need expansion handling
        isReversed = false
    )

    renderingState.orderedSections.forEach { section ->
        renderSection(section, params, context)
    }
}

/**
 * Composable that displays text with clickable phone numbers and email IDs.
 */
@Composable
private fun ClickableDirectAnswerText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    onPhoneNumberClick: (String) -> Unit,
    onEmailClick: (String) -> Unit
) {
    // Regex patterns to match different entities
    val phonePattern = Regex("""(\+?\d{1,3}[\s.-]?)?(\(?\d{3}\)?[\s.-]?)?\d{3}[\s.-]?\d{4}""")
    val emailPattern = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")

    data class ClickableMatch(
        val range: IntRange,
        val tag: String,
        val value: String
    )

    fun rangesOverlap(a: IntRange, b: IntRange): Boolean {
        return a.first <= b.last && b.first <= a.last
    }

    val matches = mutableListOf<ClickableMatch>()

    phonePattern.findAll(text).forEach { matchResult ->
        val phoneNumber = matchResult.value
        val cleanedNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanedNumber != null) {
            matches.add(ClickableMatch(matchResult.range, "PHONE", cleanedNumber))
        }
    }

    emailPattern.findAll(text).forEach { matchResult ->
        matches.add(ClickableMatch(matchResult.range, "EMAIL", matchResult.value))
    }

    // Remove overlapping matches by keeping the earliest occurrences
    val dedupedMatches = matches
        .sortedBy { it.range.first }
        .fold(mutableListOf<ClickableMatch>()) { acc, match ->
            if (acc.none { rangesOverlap(it.range, match.range) }) {
                acc.add(match)
            }
            acc
        }

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0

        dedupedMatches.forEach { match ->
            val startIndex = match.range.first
            val endIndex = match.range.last + 1

            if (startIndex > lastIndex) {
                append(text.substring(lastIndex, startIndex))
            }

            pushStringAnnotation(
                tag = match.tag,
                annotation = match.value
            )
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(text.substring(startIndex, endIndex))
            }
            pop()

            lastIndex = endIndex
        }

        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }

    ClickableText(
        text = annotatedString,
        style = style.copy(color = color),
        onClick = { offset ->
            annotatedString.getStringAnnotations(
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                when (annotation.tag) {
                    "PHONE" -> onPhoneNumberClick(annotation.item)
                    "EMAIL" -> onEmailClick(annotation.item)
                }
            }
        }
    )
}

package com.tk.quicksearch.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import android.net.Uri
import android.graphics.BitmapFactory
import java.io.InputStream
import androidx.compose.ui.layout.layout
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.window.PopupProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.search.SearchSection
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class ExpandedSection {
    NONE,
    CONTACTS,
    FILES
}

private const val INITIAL_RESULT_COUNT = 1
private const val ROW_COUNT = 2
private const val SEARCH_ROW_COUNT = 1
private const val COLUMNS = 5

@Composable
fun SearchRoute(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.handleOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SearchScreen(
        modifier = modifier,
        state = uiState,
        onQueryChanged = viewModel::onQueryChange,
        onClearQuery = viewModel::clearQuery,
        onRequestUsagePermission = viewModel::openUsageAccessSettings,
        onSettingsClick = onSettingsClick,
        onAppClick = viewModel::launchApp,
        onAppInfoClick = viewModel::openAppInfo,
        onUninstallClick = viewModel::requestUninstall,
        onHideApp = viewModel::hideApp,
        onPinApp = viewModel::pinApp,
        onUnpinApp = viewModel::unpinApp,
        onContactClick = viewModel::openContact,
        onCallContact = viewModel::callContact,
        onSmsContact = viewModel::smsContact,
        onFileClick = viewModel::openFile,
        onPinContact = viewModel::pinContact,
        onUnpinContact = viewModel::unpinContact,
        onExcludeContact = viewModel::excludeContact,
        onPinFile = viewModel::pinFile,
        onUnpinFile = viewModel::unpinFile,
        onExcludeFile = viewModel::excludeFile,
        onPhoneNumberSelected = viewModel::onPhoneNumberSelected,
        onDismissPhoneNumberSelection = viewModel::dismissPhoneNumberSelection,
        onSearchEngineClick = { query, engine -> viewModel.openSearchUrl(query, engine) },
        onOpenAppSettings = viewModel::openAppSettings,
        onOpenStorageAccessSettings = viewModel::openAllFilesAccessSettings
    )
}

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    state: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    onRequestUsagePermission: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppInfoClick: (AppInfo) -> Unit,
    onUninstallClick: (AppInfo) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onPinApp: (AppInfo) -> Unit,
    onUnpinApp: (AppInfo) -> Unit,
    onContactClick: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onFileClick: (DeviceFile) -> Unit,
    onPinContact: (ContactInfo) -> Unit,
    onUnpinContact: (ContactInfo) -> Unit,
    onExcludeContact: (ContactInfo) -> Unit,
    onPinFile: (DeviceFile) -> Unit,
    onUnpinFile: (DeviceFile) -> Unit,
    onExcludeFile: (DeviceFile) -> Unit,
    onSearchEngineClick: (String, SearchEngine) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenStorageAccessSettings: () -> Unit,
    onPhoneNumberSelected: (String, Boolean) -> Unit,
    onDismissPhoneNumberSelection: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isSearching = state.query.isNotBlank()
    val hasPinnedContacts = state.pinnedContacts.isNotEmpty() && state.hasContactPermission
    val hasPinnedFiles = state.pinnedFiles.isNotEmpty() && state.hasFilePermission
    val visibleRowCount = if (isSearching || hasPinnedContacts || hasPinnedFiles) SEARCH_ROW_COUNT else ROW_COUNT
    val visibleAppLimit = visibleRowCount * COLUMNS
    val displayApps = remember(
        state.query,
        state.recentApps,
        state.searchResults,
        state.pinnedApps,
        visibleAppLimit
    ) {
        if (!isSearching) {
            val pinnedPackages = state.pinnedApps.map { it.packageName }.toSet()
            (state.pinnedApps + state.recentApps.filterNot { pinnedPackages.contains(it.packageName) })
                .take(visibleAppLimit)
        } else {
            state.searchResults.take(visibleAppLimit)
        }
    }
    val pinnedPackageNames = remember(state.pinnedApps) {
        state.pinnedApps.map { it.packageName }.toSet()
    }
    val hasAppResults = displayApps.isNotEmpty()
    val hasContactResults = state.contactResults.isNotEmpty()
    val hasFileResults = state.fileResults.isNotEmpty()
    val pinnedContactIds = remember(state.pinnedContacts) {
        state.pinnedContacts.map { it.contactId }.toSet()
    }
    val pinnedFileUris = remember(state.pinnedFiles) {
        state.pinnedFiles.map { it.uri.toString() }.toSet()
    }
    val autoExpandFiles = hasFileResults && !hasContactResults
    val autoExpandContacts = hasContactResults && !hasFileResults
    val hasBothContactsAndFiles = hasContactResults && hasFileResults
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Get ordered sections, filtering out disabled ones
    val orderedSections = remember(state.sectionOrder, state.disabledSections) {
        state.sectionOrder.filter { it !in state.disabledSections }
    }
    
    // Check if sections should be shown (considering disabled state)
    val shouldShowApps = SearchSection.APPS !in state.disabledSections && hasAppResults
    val shouldShowContacts = SearchSection.CONTACTS !in state.disabledSections && 
        (!state.hasContactPermission || hasContactResults || hasPinnedContacts)
    val shouldShowFiles = SearchSection.FILES !in state.disabledSections && 
        (!state.hasFilePermission || hasFileResults || hasPinnedFiles)
    
    var expandedSection by remember { mutableStateOf<ExpandedSection>(ExpandedSection.NONE) }
    val scrollState = rememberScrollState()
    
    // Helper lambda for conditionally showing section titles
    val resultSectionTitleLambda: @Composable (String) -> Unit = { text ->
        if (state.showSectionTitles) {
            ResultSectionTitle(text = text)
        }
    }
    
    // Reset expansion when query changes
    LaunchedEffect(state.query) {
        expandedSection = ExpandedSection.NONE
    }
    
    BackHandler(enabled = expandedSection != ExpandedSection.NONE) {
        keyboardController?.show()
        expandedSection = ExpandedSection.NONE
    }

    // Scroll behavior:
    // - For keyboard-aligned layout when not expanded: scroll to bottom
    // - For keyboard-aligned layout when expanded: scroll to top (towards search bar)
    LaunchedEffect(
        state.query,
        displayApps.size,
        state.contactResults.size,
        state.fileResults.size,
        state.pinnedContacts.size,
        state.pinnedFiles.size,
        state.hasUsagePermission,
        state.errorMessage,
        expandedSection,
        state.keyboardAlignedLayout
    ) {
        if (state.keyboardAlignedLayout) {
            // Wait briefly for layout to start
            delay(50)
            // Wait for content to be laid out and maxValue to stabilize
            var attempts = 0
            var lastMaxValue = 0
            var stableCount = 0
            while (attempts < 10) {
                delay(20)
                val currentMaxValue = scrollState.maxValue
                if (currentMaxValue > 0) {
                    if (currentMaxValue == lastMaxValue) {
                        stableCount++
                        // Content is stable if maxValue hasn't changed for 2 checks
                        if (stableCount >= 2) {
                            break
                        }
                    } else {
                        stableCount = 0
                        lastMaxValue = currentMaxValue
                    }
                }
                attempts++
            }
            if (expandedSection == ExpandedSection.NONE) {
                // Scroll to bottom when showing the bottom-aligned layout
                // Get the latest maxValue after checks
                val targetScroll = scrollState.maxValue
                if (targetScroll > 0) {
                    // Use faster animation for responsive scrolling
                    scrollState.animateScrollTo(
                        value = targetScroll,
                        animationSpec = spring(
                            dampingRatio = 0.9f,
                            stiffness = 500f
                        )
                    )
                }
            } else {
                // When expanded, smoothly scroll back to top (towards search bar)
                scrollState.animateScrollTo(
                    value = 0,
                    animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = 500f
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .systemBarsPadding()
            .navigationBarsPadding()
            .padding(
                start = 20.dp,
                top = 16.dp,
                end = 20.dp
            ),
        verticalArrangement = Arrangement.Top
    ) {
        // Calculate enabled engines
        val enabledEngines: List<SearchEngine> = remember(
            state.searchEngineOrder,
            state.disabledSearchEngines
        ) {
            val order: List<SearchEngine> = state.searchEngineOrder
            val disabled: Set<SearchEngine> = state.disabledSearchEngines
            order.filter { engine -> engine !in disabled }
        }

        // Fixed search bar at the top
        PersistentSearchField(
            query = state.query,
            onQueryChange = onQueryChanged,
            onClearQuery = onClearQuery,
            onSettingsClick = onSettingsClick,
            onSearchAction = {
                if (state.query.isBlank()) return@PersistentSearchField

                val firstApp = displayApps.firstOrNull()
                if (firstApp != null) {
                    onAppClick(firstApp)
                } else {
                    val primaryEngine = enabledEngines.firstOrNull()
                    if (primaryEngine != null) {
                        onSearchEngineClick(state.query, primaryEngine)
                    }
                }
            }
        )

        // Scrollable content between search bar and search engines
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // When contacts or files are expanded, use top-aligned layout (towards search bar)
            // even if keyboardAlignedLayout is enabled
            if (state.keyboardAlignedLayout && expandedSection == ExpandedSection.NONE) {
                // Keyboard-aligned layout: reverse priority order (bottom to top)
                // Files → Contacts → Search Engines → Apps
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!state.hasUsagePermission) {
                        UsagePermissionCard(
                            modifier = Modifier.fillMaxWidth(),
                            onRequestPermission = onRequestUsagePermission
                        )
                    }

                    state.errorMessage?.takeIf { it.isNotBlank() }?.let { errorMessage ->
                        InfoBanner(message = errorMessage)
                    }

                    if (state.query.isNotBlank()) {
                        val isContactsExpanded = expandedSection == ExpandedSection.CONTACTS
                        val isFilesExpanded = expandedSection == ExpandedSection.FILES
                        val shouldShowFilesSection = shouldShowFiles && (!state.hasFilePermission || hasFileResults)
                        val shouldShowContactsSection = shouldShowContacts && (!state.hasContactPermission || hasContactResults)

                        // When using bottom-aligned layout, reverse the order only when a section
                        // is showing all results (expanded or auto-expanded). This keeps the
                        // single collapsed row (with a "More" button) on the top result, while
                        // full lists are shown bottom-to-top.
                        val collapsedContacts = if (isContactsExpanded || autoExpandContacts) {
                            state.contactResults.asReversed()
                        } else {
                            state.contactResults
                        }
                        val collapsedFiles = if (isFilesExpanded || autoExpandFiles) {
                            state.fileResults.asReversed()
                        } else {
                            state.fileResults
                        }

                        // Render sections in order (reversed for bottom-aligned layout)
                        val sectionsToRender = orderedSections.reversed() // Reverse for bottom-aligned
                        sectionsToRender.forEach { section ->
                            when (section) {
                                SearchSection.FILES -> {
                                    if (shouldShowFilesSection && !isContactsExpanded) {
                            FileResultsSection(
                                modifier = Modifier,
                                hasPermission = state.hasFilePermission,
                                files = collapsedFiles,
                                isExpanded = isFilesExpanded,
                                onFileClick = onFileClick,
                                onRequestPermission = onOpenStorageAccessSettings,
                                pinnedFileUris = pinnedFileUris,
                                onTogglePin = { file ->
                                    if (pinnedFileUris.contains(file.uri.toString())) {
                                        onUnpinFile(file)
                                    } else {
                                        onPinFile(file)
                                    }
                                },
                                onExclude = onExcludeFile,
                                showAllResults = autoExpandFiles,
                                showExpandControls = hasBothContactsAndFiles,
                                onExpandClick = {
                                    if (isFilesExpanded) {
                                        keyboardController?.show()
                                        expandedSection = ExpandedSection.NONE
                                    } else {
                                        keyboardController?.hide()
                                        expandedSection = ExpandedSection.FILES
                                    }
                                },
                                resultSectionTitle = resultSectionTitleLambda,
                                permissionDisabledCard = { title, message, actionLabel, onActionClick ->
                                    PermissionDisabledCard(
                                        title = title,
                                        message = message,
                                        actionLabel = actionLabel,
                                        onActionClick = onActionClick
                                    )
                                }
                            )
                                    }
                                }
                                SearchSection.CONTACTS -> {
                                    if (shouldShowContactsSection && !isFilesExpanded) {
                                        ContactResultsSection(
                                modifier = Modifier,
                                hasPermission = state.hasContactPermission,
                                contacts = collapsedContacts,
                                isExpanded = isContactsExpanded,
                                useWhatsAppForMessages = state.useWhatsAppForMessages,
                                onContactClick = onContactClick,
                                onCallContact = onCallContact,
                                onSmsContact = onSmsContact,
                                pinnedContactIds = pinnedContactIds,
                                onTogglePin = { contact ->
                                    if (pinnedContactIds.contains(contact.contactId)) {
                                        onUnpinContact(contact)
                                    } else {
                                        onPinContact(contact)
                                    }
                                },
                                onExclude = onExcludeContact,
                                onOpenAppSettings = onOpenAppSettings,
                                showAllResults = autoExpandContacts,
                                showExpandControls = hasBothContactsAndFiles,
                                onExpandClick = {
                                    if (isContactsExpanded) {
                                        keyboardController?.show()
                                        expandedSection = ExpandedSection.NONE
                                    } else {
                                        keyboardController?.hide()
                                        expandedSection = ExpandedSection.CONTACTS
                                    }
                                },
                                resultSectionTitle = resultSectionTitleLambda,
                                permissionDisabledCard = { title, message, actionLabel, onActionClick ->
                                    PermissionDisabledCard(
                                        title = title,
                                        message = message,
                                        actionLabel = actionLabel,
                                        onActionClick = onActionClick
                                    )
                                }
                            )
                                    }
                                }
                                SearchSection.APPS -> {
                                    if (hasAppResults && shouldShowApps) {
                                        val shouldShowAppLabels = state.showAppLabels || isSearching
                                        AppGridSection(
                                            apps = displayApps,
                                            isSearching = isSearching,
                                            hasAppResults = hasAppResults,
                                            onAppClick = onAppClick,
                                            onAppInfoClick = onAppInfoClick,
                                            onUninstallClick = onUninstallClick,
                                            onHideApp = onHideApp,
                                            onPinApp = onPinApp,
                                            onUnpinApp = onUnpinApp,
                                            pinnedPackageNames = pinnedPackageNames,
                                            showAppLabels = shouldShowAppLabels,
                                            rowCount = visibleRowCount,
                                            resultSectionTitle = resultSectionTitleLambda
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (expandedSection == ExpandedSection.NONE) {
                        // For keyboard-aligned layout, render pinned items and apps in order (reversed for bottom-aligned)
                        val sectionsForPinned = orderedSections.reversed()
                        sectionsForPinned.forEach { section ->
                            when (section) {
                                SearchSection.FILES -> {
                                    if (!isSearching && hasPinnedFiles && shouldShowFiles) {
                            FileResultsSection(
                                modifier = Modifier,
                                hasPermission = state.hasFilePermission,
                                files = state.pinnedFiles,
                                isExpanded = true,
                                onFileClick = onFileClick,
                                onRequestPermission = onOpenStorageAccessSettings,
                                pinnedFileUris = pinnedFileUris,
                                onTogglePin = { file ->
                                    if (pinnedFileUris.contains(file.uri.toString())) {
                                        onUnpinFile(file)
                                    } else {
                                        onPinFile(file)
                                    }
                                },
                                onExclude = onExcludeFile,
                                showAllResults = true,
                                showExpandControls = false,
                                onExpandClick = {},
                                resultSectionTitle = resultSectionTitleLambda,
                                permissionDisabledCard = { title, message, actionLabel, onActionClick ->
                                    PermissionDisabledCard(
                                        title = title,
                                        message = message,
                                        actionLabel = actionLabel,
                                        onActionClick = onActionClick
                                    )
                                }
                            )
                                    }
                                }
                                SearchSection.CONTACTS -> {
                                    if (!isSearching && hasPinnedContacts && shouldShowContacts) {
                                        ContactResultsSection(
                                modifier = Modifier,
                                hasPermission = state.hasContactPermission,
                                contacts = state.pinnedContacts,
                                isExpanded = true,
                                useWhatsAppForMessages = state.useWhatsAppForMessages,
                                onContactClick = onContactClick,
                                onCallContact = onCallContact,
                                onSmsContact = onSmsContact,
                                pinnedContactIds = pinnedContactIds,
                                onTogglePin = { contact ->
                                    if (pinnedContactIds.contains(contact.contactId)) {
                                        onUnpinContact(contact)
                                    } else {
                                        onPinContact(contact)
                                    }
                                },
                                onExclude = onExcludeContact,
                                onOpenAppSettings = onOpenAppSettings,
                                showAllResults = true,
                                showExpandControls = false,
                                onExpandClick = {},
                                resultSectionTitle = resultSectionTitleLambda,
                                permissionDisabledCard = { title, message, actionLabel, onActionClick ->
                                    PermissionDisabledCard(
                                        title = title,
                                        message = message,
                                        actionLabel = actionLabel,
                                        onActionClick = onActionClick
                                    )
                                }
                            )
                                    }
                                }
                                SearchSection.APPS -> {
                                    if (!isSearching && hasAppResults && shouldShowApps) {
                                        val shouldShowAppLabels = state.showAppLabels

                                        AppGridSection(
                                apps = displayApps,
                                isSearching = isSearching,
                                hasAppResults = hasAppResults,
                                onAppClick = onAppClick,
                                onAppInfoClick = onAppInfoClick,
                                onUninstallClick = onUninstallClick,
                                onHideApp = onHideApp,
                                onPinApp = onPinApp,
                                onUnpinApp = onUnpinApp,
                                pinnedPackageNames = pinnedPackageNames,
                                showAppLabels = shouldShowAppLabels,
                                rowCount = visibleRowCount,
                                resultSectionTitle = resultSectionTitleLambda
                            )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Top-aligned layout: render sections in order
                // Also used when keyboardAlignedLayout is true but contacts/files are expanded
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!state.hasUsagePermission) {
                        UsagePermissionCard(
                            modifier = Modifier.fillMaxWidth(),
                            onRequestPermission = onRequestUsagePermission
                        )
                    }

                    state.errorMessage?.takeIf { it.isNotBlank() }?.let { errorMessage ->
                        InfoBanner(message = errorMessage)
                    }

                    if (expandedSection == ExpandedSection.NONE) {
                        // Render sections in order for top-aligned layout
                        orderedSections.forEach { section ->
                            when (section) {
                                SearchSection.APPS -> {
                                    if (!isSearching && hasAppResults && shouldShowApps) {
                                        val shouldShowAppLabels = state.showAppLabels

                                        AppGridSection(
                                            apps = displayApps,
                                            isSearching = isSearching,
                                            hasAppResults = hasAppResults,
                                            onAppClick = onAppClick,
                                            onAppInfoClick = onAppInfoClick,
                                            onUninstallClick = onUninstallClick,
                                            onHideApp = onHideApp,
                                            onPinApp = onPinApp,
                                            onUnpinApp = onUnpinApp,
                                            pinnedPackageNames = pinnedPackageNames,
                                            showAppLabels = shouldShowAppLabels,
                                            rowCount = visibleRowCount,
                                            resultSectionTitle = { text -> ResultSectionTitle(text = text) }
                                        )
                                    }
                                }
                                SearchSection.CONTACTS -> {
                                    if (!isSearching && hasPinnedContacts && shouldShowContacts) {
                            ContactResultsSection(
                                modifier = Modifier,
                                hasPermission = state.hasContactPermission,
                                contacts = state.pinnedContacts,
                                isExpanded = true,
                                useWhatsAppForMessages = state.useWhatsAppForMessages,
                                onContactClick = onContactClick,
                                onCallContact = onCallContact,
                                onSmsContact = onSmsContact,
                                pinnedContactIds = pinnedContactIds,
                                onTogglePin = { contact ->
                                    if (pinnedContactIds.contains(contact.contactId)) {
                                        onUnpinContact(contact)
                                    } else {
                                        onPinContact(contact)
                                    }
                                },
                                onExclude = onExcludeContact,
                                onOpenAppSettings = onOpenAppSettings,
                                showAllResults = true,
                                showExpandControls = false,
                                onExpandClick = {},
                                resultSectionTitle = resultSectionTitleLambda,
                                permissionDisabledCard = { title, message, actionLabel, onActionClick ->
                                    PermissionDisabledCard(
                                        title = title,
                                        message = message,
                                        actionLabel = actionLabel,
                                        onActionClick = onActionClick
                                    )
                                }
                            )
                                    }
                                }
                                SearchSection.FILES -> {
                                    if (!isSearching && hasPinnedFiles && shouldShowFiles) {
                                        FileResultsSection(
                                modifier = Modifier,
                                hasPermission = state.hasFilePermission,
                                files = state.pinnedFiles,
                                isExpanded = true,
                                onFileClick = onFileClick,
                                onRequestPermission = onOpenStorageAccessSettings,
                                pinnedFileUris = pinnedFileUris,
                                onTogglePin = { file ->
                                    if (pinnedFileUris.contains(file.uri.toString())) {
                                        onUnpinFile(file)
                                    } else {
                                        onPinFile(file)
                                    }
                                },
                                onExclude = onExcludeFile,
                                showAllResults = true,
                                showExpandControls = false,
                                onExpandClick = {},
                                resultSectionTitle = resultSectionTitleLambda,
                                permissionDisabledCard = { title, message, actionLabel, onActionClick ->
                                    PermissionDisabledCard(
                                        title = title,
                                        message = message,
                                        actionLabel = actionLabel,
                                        onActionClick = onActionClick
                                    )
                                }
                            )
                                    }
                                }
                            }
                        }

                    if (state.query.isNotBlank()) {
                        val isContactsExpanded = expandedSection == ExpandedSection.CONTACTS
                        val isFilesExpanded = expandedSection == ExpandedSection.FILES
                        val shouldShowContactsSection = shouldShowContacts && (!state.hasContactPermission || hasContactResults)
                        val shouldShowFilesSection = shouldShowFiles && (!state.hasFilePermission || hasFileResults)

                        // Render sections in order for search results
                        orderedSections.forEach { section ->
                            when (section) {
                                SearchSection.CONTACTS -> {
                                    if (shouldShowContactsSection && !isFilesExpanded) {
                                        ContactResultsSection(
                                modifier = Modifier,
                                hasPermission = state.hasContactPermission,
                                contacts = state.contactResults,
                                isExpanded = isContactsExpanded,
                                useWhatsAppForMessages = state.useWhatsAppForMessages,
                                onContactClick = onContactClick,
                                onCallContact = onCallContact,
                                onSmsContact = onSmsContact,
                                pinnedContactIds = pinnedContactIds,
                                onTogglePin = { contact ->
                                    if (pinnedContactIds.contains(contact.contactId)) {
                                        onUnpinContact(contact)
                                    } else {
                                        onPinContact(contact)
                                    }
                                },
                                onExclude = onExcludeContact,
                                onOpenAppSettings = onOpenAppSettings,
                                showAllResults = autoExpandContacts,
                                showExpandControls = hasBothContactsAndFiles,
                                onExpandClick = {
                                    if (isContactsExpanded) {
                                        keyboardController?.show()
                                        expandedSection = ExpandedSection.NONE
                                    } else {
                                        keyboardController?.hide()
                                        expandedSection = ExpandedSection.CONTACTS
                                    }
                                },
                                resultSectionTitle = resultSectionTitleLambda,
                                permissionDisabledCard = { title, message, actionLabel, onActionClick ->
                                    PermissionDisabledCard(
                                        title = title,
                                        message = message,
                                        actionLabel = actionLabel,
                                        onActionClick = onActionClick
                                    )
                                }
                            )
                                    }
                                }
                                SearchSection.FILES -> {
                                    if (shouldShowFilesSection && !isContactsExpanded) {
                                        FileResultsSection(
                                modifier = Modifier,
                                hasPermission = state.hasFilePermission,
                                files = state.fileResults,
                                isExpanded = isFilesExpanded,
                                onFileClick = onFileClick,
                                onRequestPermission = onOpenStorageAccessSettings,
                                pinnedFileUris = pinnedFileUris,
                                onTogglePin = { file ->
                                    if (pinnedFileUris.contains(file.uri.toString())) {
                                        onUnpinFile(file)
                                    } else {
                                        onPinFile(file)
                                    }
                                },
                                onExclude = onExcludeFile,
                                showAllResults = autoExpandFiles,
                                showExpandControls = hasBothContactsAndFiles,
                                onExpandClick = {
                                    if (isFilesExpanded) {
                                        keyboardController?.show()
                                        expandedSection = ExpandedSection.NONE
                                    } else {
                                        keyboardController?.hide()
                                        expandedSection = ExpandedSection.FILES
                                    }
                                },
                                resultSectionTitle = resultSectionTitleLambda,
                                permissionDisabledCard = { title, message, actionLabel, onActionClick ->
                                    PermissionDisabledCard(
                                        title = title,
                                        message = message,
                                        actionLabel = actionLabel,
                                        onActionClick = onActionClick
                                    )
                                }
                            )
                                    }
                                }
                                SearchSection.APPS -> {
                                    if (hasAppResults && shouldShowApps) {
                                        val shouldShowAppLabels = state.showAppLabels || isSearching
                                        AppGridSection(
                                            apps = displayApps,
                                            isSearching = isSearching,
                                            hasAppResults = hasAppResults,
                                            onAppClick = onAppClick,
                                            onAppInfoClick = onAppInfoClick,
                                            onUninstallClick = onUninstallClick,
                                            onHideApp = onHideApp,
                                            onPinApp = onPinApp,
                                            onUnpinApp = onUnpinApp,
                                            pinnedPackageNames = pinnedPackageNames,
                                            showAppLabels = shouldShowAppLabels,
                                            rowCount = visibleRowCount,
                                            resultSectionTitle = resultSectionTitleLambda
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // Fixed search engines section at the bottom (above keyboard, not scrollable)
        // Hide when files or contacts are expanded, or when search engine section is disabled
        if (expandedSection == ExpandedSection.NONE && state.searchEngineSectionEnabled) {
            SearchEnginesSection(
                query = state.query,
                hasAppResults = hasAppResults,
                enabledEngines = enabledEngines,
                onSearchEngineClick = onSearchEngineClick,
                modifier = Modifier.imePadding()
            )
        } else if (expandedSection == ExpandedSection.NONE && !state.searchEngineSectionEnabled) {
            // Add padding when search engine section is disabled to prevent keyboard from covering content
            Spacer(modifier = Modifier.imePadding())
        }
    }
    
    // Phone number selection dialog
    state.phoneNumberSelection?.let { selection ->
        PhoneNumberSelectionDialog(
            contactInfo = selection.contactInfo,
            isCall = selection.isCall,
            onPhoneNumberSelected = onPhoneNumberSelected,
            onDismiss = onDismissPhoneNumberSelection
        )
    }
}

@Composable
private fun ResultSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    )
}


@Composable
private fun PermissionDisabledCard(
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
private fun PersistentSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Track if text is multi-line to adjust text size
    var isMultiLine by remember { mutableStateOf(false) }

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

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Calculate available width for text (accounting for icons and padding)
        // Leading icon: ~48dp, trailing icons: ~48-96dp, horizontal padding: ~32dp
        val availableTextWidth = maxWidth - 176.dp
        
        // Hidden Text composable to measure text layout and detect line count
        // Positioned absolutely and made invisible so it doesn't affect layout
        Text(
            text = query.ifEmpty { " " },
            style = MaterialTheme.typography.titleLarge,
            maxLines = 3,
            onTextLayout = { layoutResult ->
                isMultiLine = layoutResult.lineCount > 1
            },
            modifier = Modifier
                .width(availableTextWidth)
                .alpha(0f)
                .layout { measurable, constraints ->
                    // Measure but don't take up any space in layout
                    val placeable = measurable.measure(constraints)
                    layout(0, 0) {
                        // Don't place it - it's just for measurement
                    }
                }
        )

        // Determine text style based on whether text is multi-line
        val textStyle = if (isMultiLine) {
            MaterialTheme.typography.titleMedium
        } else {
            MaterialTheme.typography.titleLarge
        }

        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface),
        placeholder = {
            Text(
                text = stringResource(R.string.search_hint),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        textStyle = textStyle,
        singleLine = false,
        maxLines = 3,
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = stringResource(R.string.desc_search_icon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        },
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearQuery) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.desc_clear_search),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (query.isEmpty()) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.desc_open_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                if (query.isNotBlank()) {
                    onSearchAction()
                }
                keyboardController?.hide()
            }
        ),
        colors = TextFieldDefaults.colors(
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
    }
}

@Composable
private fun UsagePermissionCard(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit
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
            Text(
                text = stringResource(R.string.usage_permission_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
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
private fun InfoBanner(message: String) {
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
private fun EmptyState() {
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


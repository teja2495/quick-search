package com.tk.quicksearch.search

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
import androidx.compose.material.icons.rounded.Call
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
    onSearchEngineClick: (String, SearchEngine) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenStorageAccessSettings: () -> Unit,
    onPhoneNumberSelected: (String, Boolean) -> Unit,
    onDismissPhoneNumberSelection: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isSearching = state.query.isNotBlank()
    val visibleRowCount = if (isSearching) SEARCH_ROW_COUNT else ROW_COUNT
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
    val autoExpandFiles = hasFileResults && !hasContactResults
    val hasBothContactsAndFiles = hasContactResults && hasFileResults
    val keyboardController = LocalSoftwareKeyboardController.current
    
    var expandedSection by remember { mutableStateOf<ExpandedSection>(ExpandedSection.NONE) }
    val scrollState = rememberScrollState()
    
    // Reset expansion when query changes
    LaunchedEffect(state.query) {
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
        state.hasUsagePermission,
        state.errorMessage,
        expandedSection,
        state.keyboardAlignedLayout
    ) {
        if (state.keyboardAlignedLayout) {
            // Wait for layout to be complete
            delay(150)
            if (expandedSection == ExpandedSection.NONE) {
                // Jump to bottom instantly (no animation)
                scrollState.scrollTo(scrollState.maxValue)
            } else {
                // When expanded, scroll to top (towards search bar)
                scrollState.scrollTo(0)
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

        Spacer(modifier = Modifier.height(12.dp))
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
                        .padding(bottom = 12.dp),
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
                        val shouldShowFilesSection = !state.hasFilePermission || hasFileResults
                        val shouldShowContactsSection = !state.hasContactPermission || hasContactResults

                        if (shouldShowFilesSection && !isContactsExpanded) {
                            FileResultsSection(
                                modifier = Modifier,
                                hasPermission = state.hasFilePermission,
                                files = state.fileResults,
                                isExpanded = isFilesExpanded,
                                onFileClick = onFileClick,
                                onRequestPermission = onOpenStorageAccessSettings,
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
                                }
                            )
                        }

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
                                onOpenAppSettings = onOpenAppSettings,
                                showAllResults = false,
                                showExpandControls = hasBothContactsAndFiles,
                                onExpandClick = {
                                    if (isContactsExpanded) {
                                        keyboardController?.show()
                                        expandedSection = ExpandedSection.NONE
                                    } else {
                                        keyboardController?.hide()
                                        expandedSection = ExpandedSection.CONTACTS
                                    }
                                }
                            )
                        }
                    }

                    if (expandedSection == ExpandedSection.NONE) {
                        if (hasAppResults) {
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
                                rowCount = visibleRowCount
                            )
                        }
                    }
                }
            } else {
                // Top-aligned layout: Apps, Contacts, Files at top; Search Engines always at bottom
                // Top section: Apps → Contacts → Files
                // Also used when keyboardAlignedLayout is true but contacts/files are expanded
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
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
                            rowCount = visibleRowCount
                        )
                    }

                    if (state.query.isNotBlank()) {
                        val isContactsExpanded = expandedSection == ExpandedSection.CONTACTS
                        val isFilesExpanded = expandedSection == ExpandedSection.FILES
                        val shouldShowContactsSection = !state.hasContactPermission || hasContactResults
                        val shouldShowFilesSection = !state.hasFilePermission || hasFileResults

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
                                onOpenAppSettings = onOpenAppSettings,
                                showAllResults = false,
                                showExpandControls = hasBothContactsAndFiles,
                                onExpandClick = {
                                    if (isContactsExpanded) {
                                        keyboardController?.show()
                                        expandedSection = ExpandedSection.NONE
                                    } else {
                                        keyboardController?.hide()
                                        expandedSection = ExpandedSection.CONTACTS
                                    }
                                }
                            )
                        }

                        if (shouldShowFilesSection && !isContactsExpanded) {
                            FileResultsSection(
                                modifier = Modifier,
                                hasPermission = state.hasFilePermission,
                                files = state.fileResults,
                                isExpanded = isFilesExpanded,
                                onFileClick = onFileClick,
                                onRequestPermission = onOpenStorageAccessSettings,
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
                                }
                            )
                        }
                    }
                }
            }
        }

        // Fixed search engines section at the bottom (above keyboard, not scrollable)
        // Hide when files or contacts are expanded
        if (expandedSection == ExpandedSection.NONE) {
            SearchEnginesSection(
                query = state.query,
                hasAppResults = hasAppResults,
                enabledEngines = enabledEngines,
                onSearchEngineClick = onSearchEngineClick,
                modifier = Modifier.imePadding()
            )
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
private fun ContactResultsSection(
    modifier: Modifier = Modifier,
    hasPermission: Boolean,
    contacts: List<ContactInfo>,
    isExpanded: Boolean,
    useWhatsAppForMessages: Boolean,
    onContactClick: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onOpenAppSettings: () -> Unit,
    showAllResults: Boolean = false,
    showExpandControls: Boolean = false,
    onExpandClick: () -> Unit
) {
    val hasVisibleContent = (hasPermission && contacts.isNotEmpty()) || !hasPermission
    if (!hasVisibleContent) return
    val orderedContacts = contacts

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ResultSectionTitle(text = stringResource(R.string.contacts_section_title))
        when {
            hasPermission && contacts.isNotEmpty() -> {
                val displayAsExpanded = isExpanded || showAllResults
                val canShowExpand = showExpandControls && orderedContacts.size > INITIAL_RESULT_COUNT
                val expandHandler = if (!displayAsExpanded && canShowExpand) onExpandClick else null
                val collapseHandler = if (isExpanded && showExpandControls) onExpandClick else null
                val displayContacts = if (displayAsExpanded) {
                    orderedContacts
                } else {
                    orderedContacts.take(INITIAL_RESULT_COUNT)
                }
                ContactsResultCard(
                    contacts = displayContacts,
                    allContacts = orderedContacts,
                    isExpanded = displayAsExpanded,
                    useWhatsAppForMessages = useWhatsAppForMessages,
                    onContactClick = onContactClick,
                    onCallContact = onCallContact,
                    onSmsContact = onSmsContact,
                    onExpandClick = expandHandler,
                    onCollapseClick = collapseHandler
                )
            }

            !hasPermission -> {
                PermissionDisabledCard(
                    title = stringResource(R.string.contacts_permission_title),
                    message = stringResource(R.string.contacts_permission_subtitle),
                    actionLabel = stringResource(R.string.permission_action_manage_android),
                    onActionClick = onOpenAppSettings
                )
            }
        }
    }
}

@Composable
private fun FileResultsSection(
    modifier: Modifier = Modifier,
    hasPermission: Boolean,
    files: List<DeviceFile>,
    isExpanded: Boolean,
    onFileClick: (DeviceFile) -> Unit,
    onRequestPermission: () -> Unit,
    showAllResults: Boolean = false,
    showExpandControls: Boolean = false,
    onExpandClick: () -> Unit
) {
    val hasVisibleContent = (hasPermission && files.isNotEmpty()) || !hasPermission
    if (!hasVisibleContent) return
    val orderedFiles = files

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ResultSectionTitle(text = stringResource(R.string.files_section_title))
        when {
            hasPermission && files.isNotEmpty() -> {
                val displayAsExpanded = isExpanded || showAllResults
                val canShowExpand = showExpandControls && orderedFiles.size > INITIAL_RESULT_COUNT
                val expandHandler = if (!displayAsExpanded && canShowExpand) onExpandClick else null
                val collapseHandler = if (isExpanded && showExpandControls) onExpandClick else null
                val displayFiles = if (displayAsExpanded) {
                    orderedFiles
                } else {
                    orderedFiles.take(INITIAL_RESULT_COUNT)
                }
                FilesResultCard(
                    files = displayFiles,
                    allFiles = orderedFiles,
                    isExpanded = displayAsExpanded,
                    onFileClick = onFileClick,
                    onExpandClick = expandHandler,
                    onCollapseClick = collapseHandler
                )
            }

            !hasPermission -> {
                PermissionDisabledCard(
                    title = stringResource(R.string.files_permission_title),
                    message = stringResource(R.string.files_permission_subtitle),
                    actionLabel = stringResource(R.string.permission_action_manage_android),
                    onActionClick = onRequestPermission
                )
            }
        }
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
private fun ContactsResultCard(
    contacts: List<ContactInfo>,
    allContacts: List<ContactInfo>,
    isExpanded: Boolean,
    useWhatsAppForMessages: Boolean,
    onContactClick: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onExpandClick: (() -> Unit)?,
    onCollapseClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                contacts.forEachIndexed { index, contactInfo ->
                    ContactResultRow(
                        contactInfo = contactInfo,
                        useWhatsAppForMessages = useWhatsAppForMessages,
                        onContactClick = onContactClick,
                        onCallContact = onCallContact,
                        onSmsContact = onSmsContact
                    )
                    if (index != contacts.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
                if (onExpandClick != null && !isExpanded) {
                    TextButton(
                        onClick = { onExpandClick() },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .height(28.dp)
                            .padding(top = 2.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "More",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        if (onCollapseClick != null && isExpanded) {
            TextButton(
                onClick = { onCollapseClick() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Collapse",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandLess,
                    contentDescription = "Collapse",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


@Composable
private fun ContactResultRow(
    contactInfo: ContactInfo,
    useWhatsAppForMessages: Boolean,
    onContactClick: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit
) {
    val context = LocalContext.current
    val hasNumber = contactInfo.primaryNumber != null
    
    // Load contact photo
    val contactPhoto by produceState<ImageBitmap?>(initialValue = null, key1 = contactInfo.photoUri) {
        val photoUri = contactInfo.photoUri
        if (photoUri != null) {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(photoUri)
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    inputStream?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
            value = bitmap
        }
    }
    
    val placeholderInitials = remember(contactInfo.displayName) {
        contactInfo.displayName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2).joinToString("")
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable { onContactClick(contactInfo) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Contact photo/avatar
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (contactPhoto != null) {
                    Image(
                        bitmap = contactPhoto!!,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = placeholderInitials,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        
        Text(
            text = contactInfo.displayName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(
            onClick = { onCallContact(contactInfo) },
            enabled = hasNumber,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = stringResource(R.string.contacts_action_call),
                tint = if (hasNumber) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        IconButton(
            onClick = { onSmsContact(contactInfo) },
            enabled = hasNumber,
            modifier = Modifier.size(40.dp)
        ) {
            if (useWhatsAppForMessages) {
                Icon(
                    painter = painterResource(id = R.drawable.whatsapp),
                    contentDescription = stringResource(R.string.contacts_action_whatsapp),
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Sms,
                    contentDescription = stringResource(R.string.contacts_action_sms),
                    tint = if (hasNumber) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun FilesResultCard(
    files: List<DeviceFile>,
    allFiles: List<DeviceFile>,
    isExpanded: Boolean,
    onFileClick: (DeviceFile) -> Unit,
    onExpandClick: (() -> Unit)?,
    onCollapseClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                files.forEachIndexed { index, file ->
                    FileResultRow(
                        deviceFile = file,
                        onClick = onFileClick,
                        isExpanded = isExpanded
                    )
                    if (index != files.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
                if (onExpandClick != null && !isExpanded) {
                    TextButton(
                        onClick = { onExpandClick() },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .height(28.dp)
                            .padding(top = 2.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "More",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        if (onCollapseClick != null && isExpanded) {
            TextButton(
                onClick = { onCollapseClick() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Collapse",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandLess,
                    contentDescription = "Collapse",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


@Composable
private fun FileResultRow(
    deviceFile: DeviceFile,
    onClick: (DeviceFile) -> Unit,
    isExpanded: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isExpanded) 0.dp else 52.dp)
            .clickable { onClick(deviceFile) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .size(24.dp)
                .padding(start = 4.dp)
        )
        Text(
            text = deviceFile.displayName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
            overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
        )
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchEnginesSection(
    modifier: Modifier = Modifier,
    query: String,
    hasAppResults: Boolean,
    enabledEngines: List<SearchEngine>,
    onSearchEngineClick: (String, SearchEngine) -> Unit
) {
    if (enabledEngines.isEmpty()) return

    val scrollState = rememberLazyListState()

    Surface(
        modifier = modifier
            .layout { measurable, constraints ->
                // Parent has 20dp padding on each side
                // Extend width by 40dp (20dp on each side) to reach screen edges
                val parentPadding = 20.dp.roundToPx()
                val extendedWidth = constraints.maxWidth + (parentPadding * 2)
                val extendedConstraints = constraints.copy(
                    minWidth = extendedWidth,
                    maxWidth = extendedWidth
                )
                val placeable = measurable.measure(extendedConstraints)
                layout(
                    width = constraints.maxWidth, // Report parent width to avoid overflow
                    height = placeable.height
                ) {
                    // Place 20dp to the left so it extends to screen edges
                    placeable.placeRelative(x = -parentPadding, y = 0)
                }
            },
        color = Color.Black,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fixed search icon
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(20.dp))
            
            // Scrollable search engine icons with pagination (6 items at a time)
            BoxWithConstraints(
                modifier = Modifier.weight(1f)
            ) {
                val iconSize = 24.dp
                val spacing = 20.dp
                // Calculate item width: (available width - 5 spacings) / 6 items
                // maxWidth is already in Dp, so we can do arithmetic directly
                val totalSpacing = spacing * 5 // 5 spacings between 6 items
                val itemWidthDp = (maxWidth - totalSpacing) / 6 // 6 items with 5 spacings
                
                LazyRow(
                    state = scrollState,
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(enabledEngines) { index, engine ->
                        val drawableId = when (engine) {
                            SearchEngine.GOOGLE -> R.drawable.google
                            SearchEngine.CHATGPT -> R.drawable.chatgpt
                            SearchEngine.PERPLEXITY -> R.drawable.perplexity
                            SearchEngine.GROK -> R.drawable.grok
                            SearchEngine.GOOGLE_MAPS -> R.drawable.google_maps
                            SearchEngine.GOOGLE_PLAY -> R.drawable.google_play
                            SearchEngine.REDDIT -> R.drawable.reddit
                            SearchEngine.YOUTUBE -> R.drawable.youtube
                            SearchEngine.AMAZON -> R.drawable.amazon
                            SearchEngine.AI_MODE -> R.drawable.ai_mode
                        }
                        
                        val contentDescription = when (engine) {
                            SearchEngine.GOOGLE -> "Google"
                            SearchEngine.CHATGPT -> "ChatGPT"
                            SearchEngine.PERPLEXITY -> "Perplexity"
                            SearchEngine.GROK -> "Grok"
                            SearchEngine.GOOGLE_MAPS -> "Google Maps"
                            SearchEngine.GOOGLE_PLAY -> "Google Play"
                            SearchEngine.REDDIT -> "Reddit"
                            SearchEngine.YOUTUBE -> "YouTube"
                            SearchEngine.AMAZON -> "Amazon"
                            SearchEngine.AI_MODE -> "AI mode"
                        }
                        
                        Box(
                            modifier = Modifier
                                .width(itemWidthDp)
                                .clickable {
                                    onSearchEngineClick(query, engine)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = drawableId),
                                contentDescription = contentDescription,
                                modifier = Modifier
                                    .size(iconSize),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppGridSection(
    apps: List<AppInfo>,
    isSearching: Boolean,
    hasAppResults: Boolean,
    onAppClick: (AppInfo) -> Unit,
    onAppInfoClick: (AppInfo) -> Unit,
    onUninstallClick: (AppInfo) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onPinApp: (AppInfo) -> Unit,
    onUnpinApp: (AppInfo) -> Unit,
    pinnedPackageNames: Set<String>,
    showAppLabels: Boolean,
    modifier: Modifier = Modifier,
    rowCount: Int = ROW_COUNT
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (hasAppResults && isSearching) {
            ResultSectionTitle(text = stringResource(R.string.apps_section_title))
        }
        Crossfade(targetState = apps, label = "grid") { items ->
            if (items.isEmpty()) {
                Box {}
            } else {
                AppGrid(
                    apps = items,
                    onAppClick = onAppClick,
                    onAppInfoClick = onAppInfoClick,
                    onUninstallClick = onUninstallClick,
                    onHideApp = onHideApp,
                    onPinApp = onPinApp,
                    onUnpinApp = onUnpinApp,
                    pinnedPackageNames = pinnedPackageNames,
                    showAppLabels = showAppLabels,
                    rowCount = rowCount
                )
            }
        }
    }
}

@Composable
private fun AppGrid(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppInfoClick: (AppInfo) -> Unit,
    onUninstallClick: (AppInfo) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onPinApp: (AppInfo) -> Unit,
    onUnpinApp: (AppInfo) -> Unit,
    pinnedPackageNames: Set<String>,
    showAppLabels: Boolean,
    rowCount: Int = ROW_COUNT
) {
    val rows = remember(apps) {
        apps.take(rowCount * COLUMNS).chunked(COLUMNS)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(rowCount) { rowIndex ->
            val rowApps = rows.getOrNull(rowIndex).orEmpty()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(COLUMNS) { columnIndex ->
                    val app = rowApps.getOrNull(columnIndex)
                    if (app != null) {
                        AppGridItem(
                            modifier = Modifier.weight(1f),
                            appInfo = app,
                            onClick = { onAppClick(app) },
                            onAppInfoClick = { onAppInfoClick(app) },
                            onUninstallClick = { onUninstallClick(app) },
                        onHideApp = { onHideApp(app) },
                        onPinApp = { onPinApp(app) },
                        onUnpinApp = { onUnpinApp(app) },
                        isPinned = pinnedPackageNames.contains(app.packageName),
                        showUninstall = !app.isSystemApp,
                        showAppLabel = showAppLabels
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(
    modifier: Modifier = Modifier,
    appInfo: AppInfo,
    onClick: () -> Unit,
    onAppInfoClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onHideApp: () -> Unit,
    onPinApp: () -> Unit,
    onUnpinApp: () -> Unit,
    isPinned: Boolean,
    showUninstall: Boolean,
    showAppLabel: Boolean
) {
    val context = LocalContext.current
    val packageName = appInfo.packageName
    val cachedIcon = remember(packageName) { AppIconCache.get(packageName) }
    val iconBitmap by produceState(initialValue = cachedIcon, key1 = packageName) {
        val existing = AppIconCache.get(packageName)
        if (existing != null) {
            value = existing
            return@produceState
        }

        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap()
                    .asImageBitmap()
            }.getOrNull()
        }

        if (bitmap != null) {
            AppIconCache.put(packageName, bitmap)
        }
        value = bitmap
    }

    var showOptions by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        val placeholderLabel = remember(appInfo.appName) {
            appInfo.appName.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(
                            onClick = onClick,
                            onLongClick = { showOptions = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = iconBitmap
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = stringResource(
                                R.string.desc_launch_app,
                                appInfo.appName
                            ),
                            modifier = Modifier.size(52.dp)
                        )
                    } else {
                        Text(
                            text = placeholderLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (showAppLabel) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        DropdownMenu(
            expanded = showOptions,
            onDismissRequest = { showOptions = false },
            shape = RoundedCornerShape(24.dp),
            properties = PopupProperties(focusable = false)
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_app_info)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null
                    )
                },
                onClick = {
                    showOptions = false
                    onAppInfoClick()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_hide_app)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null
                    )
                },
                onClick = {
                    showOptions = false
                    onHideApp()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(
                            if (isPinned) R.string.action_unpin_app else R.string.action_pin_app
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = null
                    )
                },
                onClick = {
                    showOptions = false
                    if (isPinned) {
                        onUnpinApp()
                    } else {
                        onPinApp()
                    }
                }
            )
            if (showUninstall) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.action_uninstall_app)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showOptions = false
                        onUninstallClick()
                    }
                )
            }
        }
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

@Composable
private fun PhoneNumberSelectionDialog(
    contactInfo: ContactInfo,
    isCall: Boolean,
    onPhoneNumberSelected: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var rememberChoice by remember { mutableStateOf(false) }
    var selectedNumber by remember { mutableStateOf<String?>(contactInfo.phoneNumbers.firstOrNull()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_select_phone_number_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_select_phone_number_message, contactInfo.displayName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // List of phone numbers
                contactInfo.phoneNumbers.forEach { number ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedNumber = number },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = selectedNumber == number,
                            onClick = { selectedNumber = number }
                        )
                        Text(
                            text = number,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Remember choice checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = it }
                    )
                    Text(
                        text = stringResource(R.string.dialog_remember_choice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedNumber?.let { number ->
                        onPhoneNumberSelected(number, rememberChoice)
                    }
                },
                enabled = selectedNumber != null
            ) {
                Text(text = if (isCall) stringResource(R.string.dialog_call) else stringResource(R.string.dialog_sms))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        }
    )
}

private const val ROW_COUNT = 2
private const val SEARCH_ROW_COUNT = 1
private const val COLUMNS = 5

private object AppIconCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap?>()

    fun get(packageName: String): ImageBitmap? = cache[packageName]

    fun put(packageName: String, bitmap: ImageBitmap?) {
        if (bitmap == null) return
        cache[packageName] = bitmap
    }
}


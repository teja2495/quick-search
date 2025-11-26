package com.tk.quicksearch.search

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        onSearchEngineClick = { query, engine -> viewModel.openSearchUrl(query, engine) }
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
    onSearchEngineClick: (String, SearchViewModel.SearchEngine) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val displayApps = remember(state.query, state.recentApps, state.searchResults) {
        if (state.query.isBlank()) state.recentApps else state.searchResults
    }
    val hasAppResults = displayApps.isNotEmpty()
    val isSearching = state.query.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .systemBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            .padding(
                start = 20.dp,
                top = 16.dp,
                end = 20.dp,
                bottom = if (isSearching && !hasAppResults) 8.dp else 16.dp
            ),
        verticalArrangement = Arrangement.Top
    ) {
        PersistentSearchField(
            query = state.query,
            onQueryChange = onQueryChanged,
            onClearQuery = onClearQuery,
            onSettingsClick = onSettingsClick
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (!state.hasUsagePermission) {
            UsagePermissionCard(
                modifier = Modifier.fillMaxWidth(),
                onRequestPermission = onRequestUsagePermission
            )
        }

        state.errorMessage?.takeIf { it.isNotBlank() }?.let { errorMessage ->
            Spacer(modifier = Modifier.height(16.dp))
            InfoBanner(message = errorMessage)
        }

        Spacer(modifier = Modifier.weight(1f))

        if (state.query.isNotBlank()) {
            SearchEnginesSection(
                query = state.query,
                hasAppResults = hasAppResults,
                onSearchEngineClick = onSearchEngineClick
            )
        }

        AppGridSection(
            apps = displayApps,
            isSearching = isSearching,
            hasAppResults = hasAppResults,
            query = state.query,
            onAppClick = onAppClick,
            onAppInfoClick = onAppInfoClick,
            onUninstallClick = onUninstallClick
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PersistentSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
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
        textStyle = MaterialTheme.typography.titleLarge,
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = stringResource(R.string.desc_search_icon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.desc_open_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.show()
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
    onSearchEngineClick: (String, SearchViewModel.SearchEngine) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                modifier = Modifier.size(if (hasAppResults) 24.dp else 32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Surface(
                modifier = Modifier.size(if (hasAppResults) 24.dp else 44.dp),
                onClick = { onSearchEngineClick(query, SearchViewModel.SearchEngine.GOOGLE) },
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Image(
                    painter = painterResource(id = R.drawable.google),
                    contentDescription = "Google",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Surface(
                modifier = Modifier.size(if (hasAppResults) 28.dp else 48.dp),
                onClick = { onSearchEngineClick(query, SearchViewModel.SearchEngine.CHATGPT) },
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                Image(
                    painter = painterResource(id = R.drawable.chatgpt),
                    contentDescription = "ChatGPT",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Surface(
                modifier = Modifier.size(if (hasAppResults) 28.dp else 48.dp),
                onClick = { onSearchEngineClick(query, SearchViewModel.SearchEngine.PERPLEXITY) },
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent
            ) {
                Image(
                    painter = painterResource(id = R.drawable.perplexity),
                    contentDescription = "Perplexity",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        if (hasAppResults) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun AppGridSection(
    apps: List<AppInfo>,
    isSearching: Boolean,
    hasAppResults: Boolean,
    query: String,
    onAppClick: (AppInfo) -> Unit,
    onAppInfoClick: (AppInfo) -> Unit,
    onUninstallClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(if (isSearching && !hasAppResults) 0.dp else 12.dp))

        Crossfade(targetState = apps, label = "grid") { items ->
            if (items.isEmpty()) {
                Box {}
            } else {
                AppGrid(
                    apps = items,
                    onAppClick = onAppClick,
                    onAppInfoClick = onAppInfoClick,
                    onUninstallClick = onUninstallClick
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
    onUninstallClick: (AppInfo) -> Unit
) {
    val rows = remember(apps) {
        apps.take(GRID_APP_COUNT).chunked(COLUMNS)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(ROW_COUNT) { rowIndex ->
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
                            showUninstall = !app.isSystemApp
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
    showUninstall: Boolean
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

private const val ROW_COUNT = 2
private const val COLUMNS = 5
private const val GRID_APP_COUNT = ROW_COUNT * COLUMNS

private object AppIconCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap?>()

    fun get(packageName: String): ImageBitmap? = cache[packageName]

    fun put(packageName: String, bitmap: ImageBitmap?) {
        if (bitmap == null) return
        cache[packageName] = bitmap
    }
}


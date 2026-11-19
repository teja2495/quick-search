package com.tk.quicksearch.customInfo

import com.tk.quicksearch.shared.ui.components.AppButtonSize
import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tk.quicksearch.shared.ui.components.PredictiveBackSlideHandler
import com.tk.quicksearch.shared.ui.components.predictiveBackSlide
import com.tk.quicksearch.shared.ui.components.rememberPredictiveBackSlideState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.reminders.ReminderPermissions
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.settings.remindersSettings.ReminderFormDialog
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailHeader
import com.tk.quicksearch.settings.shared.ModelFeatureSettingsCard
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.shared.util.AppLanguageManager
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderRegistry
import com.tk.quicksearch.tools.aiSearch.LlmModelCatalogCache
import com.tk.quicksearch.tools.aiSearch.supportsThinkingControl
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.ui.components.AppButton

class CustomInfoActivity : ComponentActivity() {
    /** Set when the back gesture has already slid the page away, so closing adds no animation. */
    private var closedByBackGesture = false

    /** Every way out (header back, back key, back gesture) ends here. */
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        if (closedByBackGesture) {
            overridePendingTransition(0, 0)
        } else {
            overridePendingTransition(R.anim.custom_info_slide_in_left, R.anim.custom_info_slide_out_right)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLanguageManager.applySavedAppLanguage(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val preferences = UserAppPreferences(applicationContext)
        val repository = CustomInfoRepository(applicationContext)
        val editingItem = intent.getIntExtra(EXTRA_ITEM_ID, 0).takeIf { it > 0 }?.let(repository::get)
        // The time the editor last saved, to tell a changed time from one left as it was.
        var savedDueMillis = editingItem?.dueMillis
        setContent {
            val useDarkSystemBars = when (preferences.getAppThemeMode()) {
                com.tk.quicksearch.search.core.AppThemeMode.LIGHT -> false
                com.tk.quicksearch.search.core.AppThemeMode.DARK -> true
                com.tk.quicksearch.search.core.AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            SideEffect {
                val style = if (useDarkSystemBars) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            QuickSearchTheme(
                fontScaleMultiplier = preferences.getFontScaleMultiplier(),
                useSystemFont = preferences.shouldUseSystemFont(),
                appTheme = preferences.getAppTheme(),
                appThemeMode = preferences.getAppThemeMode(),
                backgroundSource = preferences.getBackgroundSource(),
                customImageUri = preferences.getCustomImageUri(),
                accentColorMode = preferences.getAccentColorMode(),
                customAccentColorArgb = preferences.getCustomAccentColorArgb(),
                deviceThemeEnabled = preferences.isDeviceThemeEnabled(),
            ) {
                val backSlide = rememberPredictiveBackSlideState()
                PredictiveBackSlideHandler(backSlide) {
                    closedByBackGesture = true
                    finish()
                }
                SettingsScreenBackground(
                    appTheme = preferences.getAppTheme(),
                    overlayThemeIntensity = preferences.getOverlayThemeIntensity(),
                    deviceThemeEnabled = preferences.isDeviceThemeEnabled(),
                    amoledThemeEnabled = preferences.isAmoledThemeEnabled(),
                    modifier = Modifier.predictiveBackSlide(backSlide).background(MaterialTheme.colorScheme.background),
                ) {
                    CustomInfoEditor(preferences, initialItem = editingItem, onBack = ::finish) { item ->
                        val stored = if (editingItem == null) {
                            repository.add(item)
                        } else {
                            CustomInfoScheduler.cancel(applicationContext, editingItem.id)
                            repository.update(editingItem.id) { it.withEdits(item, savedDueMillis) }
                                .also { savedDueMillis = item.dueMillis }
                        }
                        stored?.let { CustomInfoScheduler.schedule(applicationContext, it) }
                        // Existing items save as they're edited, so only a new item closes the editor.
                        if (editingItem == null) finish()
                    }
                }
            }
        }
    }

    companion object {
        /** Opens the editor prefilled with an existing item; absent means a new item. */
        const val EXTRA_ITEM_ID = "custom_info_item_id"
    }
}

@Composable
private fun CustomInfoEditor(
    preferences: UserAppPreferences,
    initialItem: CustomInfoItem?,
    onBack: () -> Unit,
    onSave: (CustomInfoItem) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var title by remember { mutableStateOf(initialItem?.title.orEmpty()) }
    var prompt by remember { mutableStateOf(initialItem?.prompt.orEmpty()) }
    var providerId by remember { mutableStateOf(initialItem?.providerId ?: preferences.getAiSearchProviderId()) }
    var modelId by remember { mutableStateOf(initialItem?.modelId.orEmpty()) }
    var thinking by remember { mutableStateOf(initialItem?.thinking ?: false) }
    var webSearch by remember { mutableStateOf(initialItem?.webSearch ?: false) }
    var dueMillis by remember { mutableStateOf(initialItem?.dueMillis) }
    var hasExplicitTime by remember { mutableStateOf(initialItem?.dueMillis != null) }
    var repeat by remember { mutableStateOf(initialItem?.repeat) }
    var showDateDialog by remember { mutableStateOf(false) }
    var sendNotification by remember {
        mutableStateOf(initialItem?.sendNotification == true && ReminderPermissions.hasPostNotifications(context))
    }
    val cachedCatalogs = remember { LlmModelCatalogCache.snapshot() }
    var configuredIds by remember { mutableStateOf(cachedCatalogs.keys) }
    var modelsByProvider by remember { mutableStateOf(cachedCatalogs) }
    // Show one steady loading label until the selected model can be named, instead of flashing
    // "Select model" and fallback names while provider keys and live catalogs load.
    var catalogReady by remember {
        mutableStateOf(
            initialItem != null && cachedCatalogs[initialItem.providerId]?.any { it.id == initialItem.modelId } == true,
        )
    }
    val scope = rememberCoroutineScope()
    var showPreview by remember { mutableStateOf(false) }
    var previewLoading by remember { mutableStateOf(false) }
    var previewResponse by remember { mutableStateOf("") }
    var savedCount by remember { mutableIntStateOf(0) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        sendNotification = granted
    }
    LaunchedEffect(Unit) {
        val ids = withContext(Dispatchers.IO) {
            LlmModelCatalogCache.restore(context)
            preferences.getConfiguredLlmProviderIds().filter { !preferences.getLlmApiKey(it).isNullOrBlank() }.toSet()
        }
        configuredIds = ids
        val keepInitialModel = initialItem != null && providerId in ids
        if (providerId !in ids) providerId = AiSearchLlmProviderId.preferredOf(ids) ?: providerId
        val catalogs = ids.associateWith { id ->
            LlmModelCatalogCache.get(id) ?: AiSearchLlmProviderRegistry.get(id, context).fallbackTextModels
        }
        modelsByProvider = catalogs
        if (!keepInitialModel) {
            // The saved model may exist only in the live catalog, so keep it until that list arrives.
            modelId = preferences.getLlmModel(providerId).ifBlank { catalogs[providerId]?.firstOrNull()?.id.orEmpty() }
            thinking = preferences.isLlmThinkingEnabled(providerId)
            webSearch = preferences.isLlmGroundingEnabled(providerId)
        }
        if (ids.isEmpty() || catalogs[providerId]?.any { it.id == modelId } == true) catalogReady = true
        ids.forEach { id ->
            launch {
                val liveModels = withContext(Dispatchers.IO) {
                    val provider = AiSearchLlmProviderRegistry.get(id, context)
                    provider.fetchAvailableTextModels(preferences.getLlmApiKey(id).orEmpty(), context)
                        .onSuccess { LlmModelCatalogCache.put(context, id, it) }
                        .getOrElse { provider.fallbackTextModels }
                }
                modelsByProvider = modelsByProvider + (id to liveModels)
                if (id == providerId && liveModels.none { it.id == modelId }) {
                    val saved = initialItem?.modelId?.takeIf { initialItem.providerId == id } ?: preferences.getLlmModel(id)
                    modelId = saved.takeIf { selected -> liveModels.any { it.id == selected } }
                        ?: liveModels.firstOrNull()?.id.orEmpty()
                }
                if (id == providerId) catalogReady = true
            }
        }
    }

    val hasNotificationPermission = ReminderPermissions.hasPostNotifications(context)
    val draft = CustomInfoItem(
        id = 0,
        title = title.trim(),
        prompt = prompt.trim(),
        providerId = providerId,
        modelId = modelId,
        webSearch = webSearch,
        thinking = thinking && supportsThinkingControl(providerId, modelId),
        dueMillis = dueMillis,
        sendNotification = sendNotification && hasNotificationPermission,
        repeat = repeat,
    )
    // An existing item may keep its current time, including none once a one-time item has run,
    // but a repeating item needs a time to repeat from.
    val dueValid = (initialItem != null && dueMillis == initialItem.dueMillis && (dueMillis != null || repeat == null)) ||
        dueMillis?.let { it > System.currentTimeMillis() } == true
    val canSave = title.isNotBlank() && prompt.isNotBlank() && modelId.isNotBlank() && providerId in configuredIds && dueValid
    if (initialItem != null) {
        CustomInfoAutoSave(
            initialItem = initialItem,
            draft = draft,
            ready = catalogReady && canSave,
            hasNotificationPermission = hasNotificationPermission,
            onSave = {
                onSave(it)
                savedCount++
            },
        )
    }

    val runPreview: () -> Unit = {
        val previewItem = draft.copy(sendNotification = false)
        showPreview = true
        previewLoading = true
        previewResponse = ""
        scope.launch {
            val result = withContext(Dispatchers.IO) { fetchCustomInfoAnswer(context.applicationContext, previewItem) }
            previewResponse = result.getOrElse { it.message ?: context.getString(R.string.direct_search_error_generic) }
            previewLoading = false
        }
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        containerColor = Color.Transparent,
        topBar = {
            SettingsDetailHeader(
                title = stringResource(
                    if (initialItem == null) R.string.custom_info_title else R.string.custom_info_edit_title,
                ),
                onBack = onBack,
                trailingContent = if (initialItem != null) {
                    { CustomInfoSavedIndicator(savedCount) }
                } else {
                    null
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = DesignTokens.ContentHorizontalPadding)
                    .padding(bottom = DesignTokens.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            ) {
                CustomInfoContentCard(
                    title = title,
                    onTitleChange = { title = it },
                    prompt = prompt,
                    onPromptChange = { prompt = it },
                    previewEnabled = prompt.isNotBlank() && modelId.isNotBlank() && providerId in configuredIds && !previewLoading,
                    onPreview = runPreview,
                )
                if (showPreview) {
                    CustomInfoPreviewCard(
                        title = title.trim(),
                        loading = previewLoading,
                        response = previewResponse,
                        onDismiss = { showPreview = false },
                    )
                }
                ModelFeatureSettingsCard(
                    selectedModelId = modelId,
                    selectedProviderId = providerId,
                    availableModels = modelsByProvider[providerId].orEmpty(),
                    availableModelsByProvider = modelsByProvider,
                    configuredProviderIds = configuredIds,
                    thinkingLabel = stringResource(R.string.settings_direct_search_thinking_label),
                    webSearchLabel = stringResource(R.string.settings_direct_search_grounding_label),
                    thinkingEnabled = thinking,
                    groundingEnabled = webSearch,
                    onModelSelected = { modelId = it },
                    onProviderModelSelected = { id, selected ->
                        providerId = id
                        modelId = selected
                        thinking = preferences.isLlmThinkingEnabled(id)
                        webSearch = preferences.isLlmGroundingEnabled(id)
                    },
                    onThinkingChange = { thinking = it },
                    onGroundingChange = { webSearch = it },
                    showThinkingCheckbox = supportsThinkingControl(providerId, modelId),
                    isLoading = !catalogReady,
                )
                CustomInfoScheduleCard(
                    dueMillis = dueMillis,
                    onDateClick = { showDateDialog = true },
                    repeat = repeat,
                    onRepeatChange = { newRepeat ->
                        repeat = newRepeat
                        // A one-time item that already ran has no next run; repeating restarts it at its old time of day.
                        if (newRepeat != null && dueMillis == null) {
                            dueMillis = initialItem?.restartedRunAfter(newRepeat, System.currentTimeMillis())
                            hasExplicitTime = dueMillis != null
                        }
                    },
                    sendNotification = sendNotification,
                    onSendNotificationChange = { checked ->
                        if (!checked) sendNotification = false
                        else if (ReminderPermissions.hasPostNotifications(context)) sendNotification = true
                        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
            if (initialItem == null) {
                AppButton(
                    onClick = { onSave(draft) },
                    enabled = canSave,
                    size = AppButtonSize.Medium,
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.ContentHorizontalPadding),
                ) {
                    Text(stringResource(R.string.dialog_save))
                }
            }
        }
    }
    if (showDateDialog) {
        ReminderFormDialog(
            initialTitle = stringResource(R.string.custom_info_title),
            initialDateTimeMillis = dueMillis,
            initialAllDay = !hasExplicitTime,
            onDismiss = { showDateDialog = false },
            onConfirm = { _, millis, allDay ->
                hasExplicitTime = !allDay
                dueMillis = if (allDay) {
                    Calendar.getInstance().apply {
                        timeInMillis = millis
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                } else millis
                showDateDialog = false
            },
            titleResId = R.string.custom_info_set_date_time,
            confirmResId = R.string.dialog_save,
            extraActions = {},
            autoFocusTitle = false,
            showTitleInput = false,
            requireExplicitDateOrTime = true,
        )
    }
}

/**
 * Saves edits to an existing item shortly after each valid change, and flushes any pending edit
 * when the editor closes, so editing needs no Save button.
 */
@Composable
private fun CustomInfoAutoSave(
    initialItem: CustomInfoItem,
    draft: CustomInfoItem,
    ready: Boolean,
    hasNotificationPermission: Boolean,
    onSave: (CustomInfoItem) -> Unit,
) {
    // Normalized like the draft, so opening an item without changing anything doesn't rewrite it.
    var lastSaved by remember {
        mutableStateOf(
            CustomInfoItem(
                id = 0,
                title = initialItem.title.trim(),
                prompt = initialItem.prompt.trim(),
                providerId = initialItem.providerId,
                modelId = initialItem.modelId,
                webSearch = initialItem.webSearch,
                thinking = initialItem.thinking && supportsThinkingControl(initialItem.providerId, initialItem.modelId),
                dueMillis = initialItem.dueMillis,
                sendNotification = initialItem.sendNotification && hasNotificationPermission,
                repeat = initialItem.repeat,
            ),
        )
    }
    val latestDraft by rememberUpdatedState(draft)
    val latestReady by rememberUpdatedState(ready)
    val latestOnSave by rememberUpdatedState(onSave)
    LaunchedEffect(draft, ready) {
        if (!ready || draft == lastSaved) return@LaunchedEffect
        delay(AUTO_SAVE_DEBOUNCE_MS)
        latestOnSave(draft)
        lastSaved = draft
    }
    DisposableEffect(Unit) {
        onDispose {
            if (latestReady && latestDraft != lastSaved) latestOnSave(latestDraft)
        }
    }
}

private const val AUTO_SAVE_DEBOUNCE_MS = 500L

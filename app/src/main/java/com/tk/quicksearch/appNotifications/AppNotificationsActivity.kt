package com.tk.quicksearch.appNotifications

import com.tk.quicksearch.shared.ui.components.AppExtendedFloatingActionButton
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tk.quicksearch.shared.ui.components.PredictiveBackSlideHandler
import com.tk.quicksearch.shared.ui.components.predictiveBackSlide
import com.tk.quicksearch.shared.ui.components.rememberPredictiveBackSlideState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.appNotifications.AppNotificationFilters
import com.tk.quicksearch.search.data.preferences.GlancePreferences
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.rememberAppLabel
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailHeader
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.shared.util.AppLanguageManager
import com.tk.quicksearch.shared.util.setSystemBarsDark
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.components.AppIconButtonStyle

/** Holds the At a Glance widget's list that [AppNotificationsActivity] edits and returns. */
internal const val EXTRA_WIDGET_FILTERS = "com.tk.quicksearch.appNotifications.WIDGET_FILTERS"

/**
 * Opens [AppNotificationsActivity] on an At a Glance widget's own apps and keywords, starting from
 * the given list, and returns the list as left; null if the activity closed without one.
 */
internal object EditWidgetAppNotificationFilters : ActivityResultContract<AppNotificationFilters, AppNotificationFilters?>() {
    override fun createIntent(
        context: Context,
        input: AppNotificationFilters,
    ): Intent = Intent(context, AppNotificationsActivity::class.java).putExtra(EXTRA_WIDGET_FILTERS, input)

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): AppNotificationFilters? =
        intent
            ?.takeIf { resultCode == Activity.RESULT_OK }
            ?.let { IntentCompat.getParcelableExtra(it, EXTRA_WIDGET_FILTERS, AppNotificationFilters::class.java) }
}

/**
 * App notifications settings for At a Glance: the apps to show (each optionally narrowed by
 * keywords) and keywords that match any app. Changes save to [GlancePreferences] through
 * [AppNotificationsViewModel] as they're made, and Home picks them up right away. Started through
 * [EditWidgetAppNotificationFilters], it edits an At a Glance widget's list and returns it instead.
 */
class AppNotificationsActivity : ComponentActivity() {
    private val viewModel: AppNotificationsViewModel by viewModels()

    /** Set when the back gesture has already slid the page away, so closing adds no animation. */
    private var closedByBackGesture = false

    /** Every way out (header back, back key, back gesture) ends here. */
    @Suppress("DEPRECATION")
    override fun finish() {
        if (viewModel.isWidget) {
            viewModel.filters.value?.let { setResult(RESULT_OK, Intent().putExtra(EXTRA_WIDGET_FILTERS, it)) }
        }
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
        setContent {
            val useDarkSystemBars = when (preferences.getAppThemeMode()) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            SideEffect { setSystemBarsDark(useDarkSystemBars) }
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
                    AppNotificationsScreen(onBack = ::finish, viewModel = viewModel)
                }
            }
        }
    }
}

/** Which source's keywords drawer is open: an app by package name, or [ANY_APP]. */
private const val ANY_APP = ""

/**
 * One list of sources: Any App (keywords that match every app) and the added apps, with Add app
 * pinned at the bottom. Tapping a source opens its keywords drawer; Recent matches below shows what
 * the setup would catch. The list waits until the saved settings have been read.
 */
@Composable
private fun AppNotificationsScreen(
    onBack: () -> Unit,
    viewModel: AppNotificationsViewModel = viewModel(),
) {
    val loadedFilters by viewModel.filters.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    var editing by remember { mutableStateOf<String?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }
    // The app just added from the picker; its keywords drawer opens once the picker has closed.
    var addedApp by remember { mutableStateOf<String?>(null) }
    val filters = loadedFilters ?: AppNotificationFilters()
    val keywordsSummary: (List<String>) -> String = { words -> words.joinToString(", ") }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        containerColor = Color.Transparent,
        topBar = {
            SettingsDetailHeader(
                title = stringResource(R.string.settings_at_a_glance_app_notifications_title),
                onBack = onBack,
            )
        },
        bottomBar = {
            // Matches the Add button in the At a Glance bottom bar.
            AppExtendedFloatingActionButton(
                onClick = {
                    viewModel.loadInstalledApps()
                    showAppPicker = true
                },
                text = stringResource(R.string.app_notifications_add_app),
                icon = MaterialSymbols.Add,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = DesignTokens.ContentHorizontalPadding)
                    .padding(bottom = DesignTokens.SpacingLarge),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.ContentHorizontalPadding)
                .padding(bottom = DesignTokens.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            if (loadedFilters == null) return@Column
            Text(
                text = stringResource(R.string.app_notifications_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = DesignTokens.SpacingXSmall),
            )

            val sourceCount = filters.apps.size + 1
            SegmentedGroup {
                SourceRow(
                    packageName = null,
                    title = stringResource(R.string.app_notifications_any_app),
                    subtitle = if (filters.keywords.isEmpty()) {
                        stringResource(R.string.app_notifications_keyword_placeholder)
                    } else {
                        keywordsSummary(filters.keywords)
                    },
                    index = 0,
                    count = sourceCount,
                    onClick = { editing = ANY_APP },
                )
                filters.apps.forEachIndexed { index, app ->
                    SourceRow(
                        packageName = app.packageName,
                        title = rememberAppLabel(app.packageName),
                        subtitle = if (app.keywords.isEmpty()) {
                            stringResource(R.string.app_notifications_all_from_app)
                        } else {
                            keywordsSummary(app.keywords)
                        },
                        index = index + 1,
                        count = sourceCount,
                        onClick = { editing = app.packageName },
                        onRemove = { viewModel.removeApp(app.packageName) },
                    )
                }
            }

            if (!filters.isEmpty) AppNotificationRecentMatches(filters)
        }
    }

    val editingApp = filters.apps.firstOrNull { it.packageName == editing }
    when {
        editing == ANY_APP -> AppNotificationKeywordsDrawer(
            title = stringResource(R.string.app_notifications_any_app),
            keywords = filters.keywords,
            onKeywordsChange = viewModel::setAnyAppKeywords,
            onDismiss = { editing = null },
        )
        editingApp != null -> {
            val appLabel = rememberAppLabel(editingApp.packageName)
            AppNotificationKeywordsDrawer(
                title = appLabel,
                keywords = editingApp.keywords,
                appName = appLabel,
                packageName = editingApp.packageName,
                onKeywordsChange = { words -> viewModel.setAppKeywords(editingApp.packageName, words) },
                onDismiss = { editing = null },
            )
        }
    }

    if (showAppPicker) {
        AppNotificationAppPicker(
            allApps = installedApps,
            added = filters.apps.map { it.packageName }.toSet(),
            onPick = { packageName ->
                viewModel.addApp(packageName)
                addedApp = packageName
            },
            onDismiss = {
                showAppPicker = false
                addedApp?.let { editing = it }
                addedApp = null
            },
        )
    }
}

/**
 * A source tile at [index] of [count]: icon, name, a summary of what it shows (up to two lines), and
 * a chevron. With [onRemove], a tonal delete button replaces the chevron.
 */
@Composable
private fun SourceRow(
    packageName: String?,
    title: String,
    subtitle: String,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) = SegmentedTile(index = index, count = count, onClick = onClick) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = DesignTokens.SpacingLarge, vertical = DesignTokens.SpacingLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        AppNotificationSourceIcon(packageName)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onRemove != null) {
            val error = MaterialTheme.colorScheme.error
            AppIconButton(
                onClick = onRemove,
                tooltip = stringResource(R.string.app_notifications_remove_app, title),
                style = AppIconButtonStyle.Tonal,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = error.copy(alpha = AppColors.TintTonalAlpha),
                    contentColor = error,
                ),
            ) {
                Icon(
                    imageVector = MaterialSymbols.DeleteOutline,
                    contentDescription = stringResource(R.string.app_notifications_remove_app, title),
                )
            }
        } else {
            Icon(
                imageVector = MaterialSymbols.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

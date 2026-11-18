package com.tk.quicksearch.search.apps.privateSpace

import com.tk.quicksearch.shared.ui.components.AppMessages
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import com.tk.quicksearch.search.apps.sortedForAllApps
import com.tk.quicksearch.search.core.intentHelpers.AppLaunchingIntents
import com.tk.quicksearch.search.data.PrivateSpaceProfile
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.utils.UserHandleUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [isAvailable] is false when there is no Private Space or the system hides it from us (Quick
 * Search is not the default launcher). [apps] is always empty while [isLocked].
 */
internal data class PrivateSpaceState(
    val isAvailable: Boolean = false,
    val isLocked: Boolean = true,
    val apps: List<AppInfo> = emptyList(),
)

/**
 * Loads Private Space apps for the All Apps drawer while it is open.
 *
 * Apps are held in memory only: they are never cached, counted as launches, or recorded in search
 * history, so nothing about them survives the space being locked. Lock state changes and
 * installs inside the space refresh [state].
 */
internal class PrivateSpaceController(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val _state = MutableStateFlow(PrivateSpaceState())
    val state: StateFlow<PrivateSpaceState> = _state.asStateFlow()

    private var profileReceiver: BroadcastReceiver? = null
    private var packageCallback: LauncherApps.Callback? = null
    private var refreshJob: Job? = null

    fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        registerProfileReceiver()
        registerPackageCallback()
        refresh()
    }

    fun stop() {
        profileReceiver?.let { receiver ->
            profileReceiver = null
            runCatching { appContext.unregisterReceiver(receiver) }
        }
        packageCallback?.let { callback ->
            packageCallback = null
            runCatching { launcherApps?.unregisterCallback(callback) }
        }
        refreshJob?.cancel()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) { _state.value = load() }
    }

    /** Unlocking asks the system to authenticate the user; the profile broadcast refreshes us. */
    fun setLocked(locked: Boolean) {
        if (locked) _state.update { it.copy(isLocked = true, apps = emptyList()) }
        scope.launch(Dispatchers.IO) {
            val profile = PrivateSpaceProfile.find(appContext) ?: return@launch
            runCatching { userManager?.requestQuietModeEnabled(locked, profile) }
            refresh()
        }
    }

    /** Launches without going through the ViewModel so no launch count or history is recorded. */
    fun launch(
        context: Context,
        app: AppInfo,
    ) {
        AppLaunchingIntents.launchApp(context, app) { messageRes, appName ->
            AppMessages.show(context, context.getString(messageRes, appName))
        }
    }

    private fun load(): PrivateSpaceState {
        val profile = PrivateSpaceProfile.find(appContext) ?: return PrivateSpaceState()
        val isLocked = runCatching { userManager?.isQuietModeEnabled(profile) }.getOrNull() ?: true
        if (isLocked) return PrivateSpaceState(isAvailable = true, isLocked = true)
        val apps =
            runCatching { launcherApps?.getActivityList(null, profile) }
                .getOrNull()
                .orEmpty()
                .distinctBy { it.componentName }
                .map(::toAppInfo)
                .sortedForAllApps()
        return PrivateSpaceState(isAvailable = true, isLocked = false, apps = apps)
    }

    private fun toAppInfo(info: LauncherActivityInfo): AppInfo =
        AppInfo(
            appName = info.label?.toString().orEmpty().ifBlank { info.applicationInfo.packageName },
            packageName = info.applicationInfo.packageName,
            lastUsedTime = 0L,
            totalTimeInForeground = 0L,
            firstInstallTime = info.firstInstallTime,
            isSystemApp = info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            userHandleId = UserHandleUtils.getIdentifier(info.user),
            componentName = info.componentName.flattenToString(),
        )

    private fun registerProfileReceiver() {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) = refresh()
            }
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
                addAction(Intent.ACTION_PROFILE_ADDED)
                addAction(Intent.ACTION_PROFILE_REMOVED)
            }
        runCatching {
            ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }.onSuccess { profileReceiver = receiver }
    }

    private fun registerPackageCallback() {
        val service = launcherApps ?: return
        fun onChange(user: UserHandle) {
            if (PrivateSpaceProfile.isPrivateProfile(appContext, user)) refresh()
        }
        val callback =
            object : LauncherApps.Callback() {
                override fun onPackageAdded(packageName: String, user: UserHandle) = onChange(user)

                override fun onPackageRemoved(packageName: String, user: UserHandle) = onChange(user)

                override fun onPackageChanged(packageName: String, user: UserHandle) = onChange(user)

                override fun onPackagesAvailable(
                    packageNames: Array<out String>,
                    user: UserHandle,
                    replacing: Boolean,
                ) = onChange(user)

                override fun onPackagesUnavailable(
                    packageNames: Array<out String>,
                    user: UserHandle,
                    replacing: Boolean,
                ) = onChange(user)
            }
        runCatching { service.registerCallback(callback, Handler(Looper.getMainLooper())) }
            .onSuccess { packageCallback = callback }
    }
}

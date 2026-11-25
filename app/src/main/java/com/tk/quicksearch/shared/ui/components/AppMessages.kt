package com.tk.quicksearch.shared.ui.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Short in-app messages ("Saved to Downloads", "Unable to open X"). Material 3 shows these as a
 * snackbar, so while an app screen is resumed the message goes to that screen's snackbar host.
 * With no host on screen (the app is in the background, or the caller is a widget, tile, or
 * trampoline activity) it falls back to a system toast, which is the only thing that can show then.
 *
 * Call from any thread.
 */
object AppMessages {
    private class Host(
        val priority: Int,
        val show: (String, SnackbarDuration) -> Unit,
    )

    // Touched on the main thread only.
    private val hosts = ArrayList<Host>()
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    fun show(
        context: Context,
        message: CharSequence,
        long: Boolean = false,
    ) {
        val appContext = context.applicationContext
        val text = message.toString()
        val run = {
            val host = hosts.maxByOrNull { it.priority }
            if (host != null) {
                host.show(text, if (long) SnackbarDuration.Long else SnackbarDuration.Short)
            } else {
                Toast.makeText(appContext, text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) run() else mainHandler.post(run)
    }

    fun show(
        context: Context,
        @StringRes messageResId: Int,
        long: Boolean = false,
    ) = show(context, context.getString(messageResId), long)

    private fun register(host: Host) {
        if (host !in hosts) hosts.add(host)
    }

    private fun unregister(host: Host) {
        hosts.remove(host)
    }

    /**
     * Routes messages to [controller] while this composable's lifecycle is resumed. The host with
     * the highest [priority] wins, so a screen's own host (which knows to stay clear of the bottom
     * search bar) beats the app-wide fallback host.
     */
    @Composable
    fun HostEffect(
        controller: UndoSnackbarController,
        priority: Int,
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, controller, priority) {
            val host = Host(priority) { message, duration -> controller.showMessage(message, duration) }
            val observer =
                LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> register(host)
                        Lifecycle.Event.ON_PAUSE -> unregister(host)
                        else -> Unit
                    }
                }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                unregister(host)
            }
        }
    }

    /** Priority of a screen's own snackbar host. */
    const val ScreenHostPriority = 1

    /** Priority of [AppMessageSnackbarHost], the app-wide fallback. */
    const val FallbackHostPriority = 0
}

/**
 * App-wide host for [AppMessages], pinned to the bottom of the window above the navigation bar.
 * Put it last in a full-screen root `Box` (settings and the widgets panel use the one in
 * navigation); screens with their own undo snackbar register that with [AppMessages.HostEffect].
 * Inside a `Scaffold`'s `snackbarHost` slot, pass [inScaffold] so the scaffold places it above its
 * bottom bar and system insets instead.
 */
@Composable
fun AppMessageSnackbarHost(
    modifier: Modifier = Modifier,
    inScaffold: Boolean = false,
) {
    val controller = rememberUndoSnackbarController()
    AppMessages.HostEffect(controller, AppMessages.FallbackHostPriority)
    Box(modifier = modifier) {
        AppUndoSnackbarHost(
            hostState = controller.hostState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (inScaffold) Modifier else Modifier.navigationBarsPadding())
                    .padding(
                        start = DesignTokens.SpacingLarge,
                        end = DesignTokens.SpacingLarge,
                        bottom = if (inScaffold) DesignTokens.SpacingLarge else DesignTokens.SpacingHuge,
                    ),
            anchor = null,
        )
    }
}
